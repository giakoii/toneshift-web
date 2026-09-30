# 02 – Database Design & Domain Model

PostgreSQL 16, quản lý bằng Flyway. Mọi bảng dùng `uuid` PK (`gen_random_uuid()`), thời gian `timestamptz` (UTC).

## 1. ERD tóm tắt

```
users 1──* auth_providers
users 1──* refresh_tokens
users 1──* password_reset_tokens
users 1──* songs 1──* shared_links
songs 1──* ratings *──1 users
songs 1──* comments *──1 users        (comments.parent_id → comments.id, 1 cấp)
comments 1──* comment_likes *──1 users
comments 1──* reports *──1 users(reporter)
users 1──* setlists 1──* setlist_songs *──1 songs
users 1──* idempotency_keys
```

## 2. Migration files (`src/main/resources/db/migration`)

| File | Nội dung |
|---|---|
| `V1__extensions_and_users.sql` | `citext`, `users`, `auth_providers`, `refresh_tokens`, `password_reset_tokens` |
| `V2__songs.sql` | `songs` + index |
| `V3__community.sql` | `ratings`, `comments`, `comment_likes`, `reports` |
| `V4__setlists.sql` | `setlists`, `setlist_songs` |
| `V5__shared_links.sql` | `shared_links` |
| `V6__idempotency.sql` | `idempotency_keys` |
| `V7__seed_admin.sql` (chỉ profile `dev`) | 1 admin mẫu (mật khẩu đọc từ env, không hard-code) |

Quy tắc: không sửa file đã merge; thay đổi mới = file `V{n+1}`. `spring.jpa.hibernate.ddl-auto=validate` (không bao giờ `update`).

## 3. DDL

### V1 – users & auth

```sql
CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE users (
  id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  email             citext        NOT NULL,
  name              varchar(100)  NOT NULL,
  password_hash     varchar(100),                       -- NULL với tài khoản chỉ Google
  image_url         varchar(500),
  role              varchar(10)   NOT NULL DEFAULT 'USER',
  status            varchar(10)   NOT NULL DEFAULT 'ACTIVE',
  email_verified_at timestamptz,
  created_at        timestamptz   NOT NULL DEFAULT now(),
  updated_at        timestamptz   NOT NULL DEFAULT now(),
  CONSTRAINT uq_users_email UNIQUE (email),
  CONSTRAINT ck_users_role   CHECK (role   IN ('USER','ADMIN')),
  CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE','BLOCKED'))
);

CREATE TABLE auth_providers (
  id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id              uuid         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  provider             varchar(20)  NOT NULL,           -- GOOGLE
  provider_account_id  varchar(100) NOT NULL,           -- Google 'sub'
  created_at           timestamptz  NOT NULL DEFAULT now(),
  CONSTRAINT uq_provider_account UNIQUE (provider, provider_account_id),
  CONSTRAINT uq_provider_per_user UNIQUE (user_id, provider)
);

CREATE TABLE refresh_tokens (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      uuid        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash   char(64)    NOT NULL,                    -- SHA-256 hex
  family_id    uuid        NOT NULL,
  expires_at   timestamptz NOT NULL,
  revoked_at   timestamptz,
  replaced_by  uuid,
  user_agent   varchar(300),
  created_at   timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);
CREATE INDEX idx_refresh_user   ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_family ON refresh_tokens(family_id);
CREATE INDEX idx_refresh_expiry ON refresh_tokens(expires_at);   -- job dọn rác

CREATE TABLE password_reset_tokens (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     uuid        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash  char(64)    NOT NULL,
  expires_at  timestamptz NOT NULL,
  used_at     timestamptz,
  created_at  timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uq_reset_token_hash UNIQUE (token_hash)
);
CREATE INDEX idx_reset_user ON password_reset_tokens(user_id);
```

### V2 – songs

```sql
CREATE TABLE songs (
  id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id       uuid          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  title          varchar(200)  NOT NULL,
  artist         varchar(200),
  content        text          NOT NULL,
  original_key   varchar(2)    NOT NULL,
  target_key     varchar(2)    NOT NULL,
  visibility     varchar(10)   NOT NULL DEFAULT 'PRIVATE',
  slug           varchar(230),
  published_at   timestamptz,
  rating_avg     numeric(3,2)  NOT NULL DEFAULT 0,      -- denormalized (D9)
  rating_count   integer       NOT NULL DEFAULT 0,
  comment_count  integer       NOT NULL DEFAULT 0,      -- comment gốc + reply chưa xoá
  version        bigint        NOT NULL DEFAULT 0,
  deleted_at     timestamptz,
  created_at     timestamptz   NOT NULL DEFAULT now(),
  updated_at     timestamptz   NOT NULL DEFAULT now(),
  CONSTRAINT ck_song_visibility CHECK (visibility IN ('PRIVATE','PUBLIC')),
  CONSTRAINT ck_song_keys CHECK (
    original_key IN ('C','C#','D','D#','E','F','F#','G','G#','A','A#','B') AND
    target_key   IN ('C','C#','D','D#','E','F','F#','G','G#','A','A#','B')),
  CONSTRAINT ck_song_content_len CHECK (char_length(content) <= 50000),
  CONSTRAINT ck_song_public_slug CHECK (visibility = 'PRIVATE' OR (slug IS NOT NULL AND published_at IS NOT NULL))
);
CREATE UNIQUE INDEX uq_songs_slug ON songs(slug) WHERE slug IS NOT NULL;

-- Library của tôi: WHERE owner_id=? AND deleted_at IS NULL ORDER BY updated_at DESC
CREATE INDEX idx_songs_owner_updated ON songs(owner_id, updated_at DESC) WHERE deleted_at IS NULL;

-- Community: newest / top / lọc theo key (partial index chỉ bài public còn sống)
CREATE INDEX idx_songs_public_newest ON songs(published_at DESC, id DESC)
  WHERE visibility = 'PUBLIC' AND deleted_at IS NULL;
CREATE INDEX idx_songs_public_top ON songs(rating_avg DESC, rating_count DESC, id DESC)
  WHERE visibility = 'PUBLIC' AND deleted_at IS NULL;
CREATE INDEX idx_songs_public_key ON songs(target_key, published_at DESC)
  WHERE visibility = 'PUBLIC' AND deleted_at IS NULL;

-- Tìm kiếm ILIKE '%q%'
CREATE INDEX idx_songs_title_trgm  ON songs USING gin (title  gin_trgm_ops);
CREATE INDEX idx_songs_artist_trgm ON songs USING gin (artist gin_trgm_ops);
```

> Khi unpublish: giữ `slug` (để link cũ ổn định nếu publish lại), chỉ đổi `visibility='PRIVATE'`. Vì check `ck_song_public_slug` chỉ ép slug khi PUBLIC nên hợp lệ. Slug không bao giờ bị tái sử dụng cho bài khác (unique index không lọc `visibility`).

### V3 – community

```sql
CREATE TABLE ratings (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     uuid        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  song_id     uuid        NOT NULL REFERENCES songs(id) ON DELETE CASCADE,
  score       smallint    NOT NULL,
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uq_rating_user_song UNIQUE (user_id, song_id),
  CONSTRAINT ck_rating_score CHECK (score BETWEEN 1 AND 5)
);
CREATE INDEX idx_ratings_song_score ON ratings(song_id, score);      -- distribution

CREATE TABLE comments (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  song_id      uuid          NOT NULL REFERENCES songs(id) ON DELETE CASCADE,
  user_id      uuid          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  parent_id    uuid          REFERENCES comments(id) ON DELETE CASCADE,
  content      varchar(2000) NOT NULL,
  like_count   integer       NOT NULL DEFAULT 0,               -- denormalized (D10)
  reply_count  integer       NOT NULL DEFAULT 0,               -- reply chưa xoá
  edited_at    timestamptz,
  deleted_at   timestamptz,
  created_at   timestamptz   NOT NULL DEFAULT now(),
  updated_at   timestamptz   NOT NULL DEFAULT now()
);
-- Danh sách gốc: newest (created_at,id) và top (like_count,created_at,id)
CREATE INDEX idx_comments_root_newest ON comments(song_id, created_at DESC, id DESC) WHERE parent_id IS NULL;
CREATE INDEX idx_comments_root_top    ON comments(song_id, like_count DESC, created_at DESC, id DESC) WHERE parent_id IS NULL;
CREATE INDEX idx_comments_replies     ON comments(parent_id, created_at ASC, id ASC) WHERE parent_id IS NOT NULL;
CREATE INDEX idx_comments_user        ON comments(user_id);

CREATE TABLE comment_likes (
  user_id     uuid        NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
  comment_id  uuid        NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
  created_at  timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, comment_id)
);
CREATE INDEX idx_comment_likes_comment ON comment_likes(comment_id);

CREATE TABLE reports (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  reporter_id  uuid         NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
  comment_id   uuid         NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
  reason       varchar(20)  NOT NULL,
  detail       varchar(500),
  status       varchar(10)  NOT NULL DEFAULT 'OPEN',
  resolved_by  uuid         REFERENCES users(id),
  resolved_at  timestamptz,
  created_at   timestamptz  NOT NULL DEFAULT now(),
  CONSTRAINT uq_report_once UNIQUE (reporter_id, comment_id),
  CONSTRAINT ck_report_reason CHECK (reason IN ('SPAM','OFFENSIVE','WRONG_CONTENT','OTHER')),
  CONSTRAINT ck_report_status CHECK (status IN ('OPEN','RESOLVED','DISMISSED'))
);
CREATE INDEX idx_reports_status ON reports(status, created_at DESC);
```

Ràng buộc chỉ 1 cấp reply kiểm ở Service (PostgreSQL CHECK không tham chiếu dòng khác); phòng thủ thêm bằng trigger (tuỳ chọn, ghi vào backlog).

### V4 – setlists

```sql
CREATE TABLE setlists (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id     uuid          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name         varchar(100)  NOT NULL,
  description  varchar(500),
  version      bigint        NOT NULL DEFAULT 0,
  created_at   timestamptz   NOT NULL DEFAULT now(),
  updated_at   timestamptz   NOT NULL DEFAULT now()
);
CREATE INDEX idx_setlists_owner ON setlists(owner_id, updated_at DESC);

CREATE TABLE setlist_songs (
  setlist_id   uuid        NOT NULL REFERENCES setlists(id) ON DELETE CASCADE,
  song_id      uuid        NOT NULL REFERENCES songs(id)    ON DELETE CASCADE,
  order_index  integer     NOT NULL,
  custom_key   varchar(2),
  added_at     timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (setlist_id, song_id),
  CONSTRAINT uq_setlist_order UNIQUE (setlist_id, order_index) DEFERRABLE INITIALLY DEFERRED,
  CONSTRAINT ck_setlist_custom_key CHECK (custom_key IS NULL OR custom_key IN
    ('C','C#','D','D#','E','F','F#','G','G#','A','A#','B'))
);
CREATE INDEX idx_setlist_songs_song ON setlist_songs(song_id);
```

`DEFERRABLE INITIALLY DEFERRED` cho phép đổi chỗ 2 bài trong một transaction mà không vướng unique giữa chừng.
Vì `songs` chỉ **soft delete**, FK không bao giờ bị kích hoạt bởi việc xoá bài; `ON DELETE CASCADE` chỉ chạy khi xoá cứng user.

### V5 – shared_links

```sql
CREATE TABLE shared_links (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  song_id      uuid        NOT NULL REFERENCES songs(id) ON DELETE CASCADE,
  owner_id     uuid        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  unique_code  varchar(10) NOT NULL,
  settings     jsonb       NOT NULL DEFAULT '{"allowCopy": true}',
  expires_at   timestamptz,
  revoked_at   timestamptz,
  created_at   timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uq_shared_code UNIQUE (unique_code)           -- đồng thời là index tra cứu
);
-- 1 bài chỉ có tối đa 1 link đang hoạt động (đảm bảo idempotent khi 2 request đồng thời)
CREATE UNIQUE INDEX uq_shared_active_per_song ON shared_links(song_id) WHERE revoked_at IS NULL;
CREATE INDEX idx_shared_owner ON shared_links(owner_id);
```

### V6 – idempotency

```sql
CREATE TABLE idempotency_keys (
  user_id      uuid        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  key          varchar(64) NOT NULL,
  request_hash char(64)    NOT NULL,
  response     jsonb       NOT NULL,
  created_at   timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, key)
);
CREATE INDEX idx_idem_created ON idempotency_keys(created_at);   -- job xoá > 24h
```

## 4. JPA Entities (skeleton, không viết body)

Quy ước: `@Entity` + Lombok `@Getter @Setter @NoArgsConstructor(access = PROTECTED)`. Mọi quan hệ `@ManyToOne(fetch = LAZY)`. Không dùng `@OneToMany` nếu không cần duyệt từ phía cha (tránh N+1/collection khổng lồ): `Song.comments`, `Comment.likes` **không** map. Enum lưu `@Enumerated(EnumType.STRING)`.

```java
@MappedSuperclass @EntityListeners(AuditingEntityListener.class)
abstract class Auditable {
  @CreatedDate  @Column(updatable = false) Instant createdAt;
  @LastModifiedDate                        Instant updatedAt;
}

@Entity @Table(name = "users")
class User extends Auditable {
  @Id @GeneratedValue(strategy = UUID) UUID id;
  String email;            // citext, lưu lowercase
  String name; String passwordHash; String imageUrl;
  @Enumerated(STRING) Role role;          // USER, ADMIN
  @Enumerated(STRING) UserStatus status;  // ACTIVE, BLOCKED
  Instant emailVerifiedAt;
  @OneToMany(mappedBy = "user", cascade = ALL, orphanRemoval = true) Set<AuthProvider> providers;  // ít phần tử, LAZY mặc định
}

@Entity class AuthProvider { UUID id; @ManyToOne(LAZY) User user; AuthProviderType provider; String providerAccountId; Instant createdAt; }
@Entity class RefreshToken { UUID id; @ManyToOne(LAZY) User user; String tokenHash; UUID familyId; Instant expiresAt, revokedAt; UUID replacedBy; String userAgent; Instant createdAt; }
@Entity class PasswordResetToken { UUID id; @ManyToOne(LAZY) User user; String tokenHash; Instant expiresAt, usedAt, createdAt; }

@Entity @Table(name = "songs") @SQLRestriction("deleted_at IS NULL")
class Song extends Auditable {
  @Id UUID id; @ManyToOne(LAZY, optional = false) User owner;
  String title, artist, content, slug;
  @Enumerated(STRING) MusicalKey originalKey, targetKey;
  @Enumerated(STRING) Visibility visibility;
  Instant publishedAt, deletedAt;
  BigDecimal ratingAvg; int ratingCount; int commentCount;
  @Version long version;
}

@Entity class Rating extends Auditable { UUID id; @ManyToOne(LAZY) User user; @ManyToOne(LAZY) Song song; short score; }

@Entity @Table(name = "comments")
class Comment extends Auditable {
  @Id UUID id; @ManyToOne(LAZY) Song song; @ManyToOne(LAZY) User user;
  @ManyToOne(LAZY) Comment parent;                // null = gốc
  String content; int likeCount; int replyCount; Instant editedAt, deletedAt;
}

@Entity @IdClass(CommentLikeId.class) class CommentLike { @Id UUID userId; @Id UUID commentId; Instant createdAt; }
@Entity class Report { UUID id; @ManyToOne(LAZY) User reporter; @ManyToOne(LAZY) Comment comment; ReportReason reason; String detail; ReportStatus status; @ManyToOne(LAZY) User resolvedBy; Instant resolvedAt, createdAt; }

@Entity class Setlist extends Auditable {
  UUID id; @ManyToOne(LAZY) User owner; String name, description; @Version long version;
  @OneToMany(mappedBy = "setlist", cascade = ALL, orphanRemoval = true) @OrderBy("orderIndex ASC")
  List<SetlistSong> songs;                          // tối đa 200 → chấp nhận được, luôn fetch join khi cần
}
@Entity @IdClass(SetlistSongId.class)
class SetlistSong { @Id @ManyToOne(LAZY) Setlist setlist; @Id @ManyToOne(LAZY) Song song; int orderIndex; MusicalKey customKey; Instant addedAt; }

@Entity class SharedLink { UUID id; @ManyToOne(LAZY) Song song; @ManyToOne(LAZY) User owner; String uniqueCode;
  @JdbcTypeCode(SqlTypes.JSON) ShareSettings settings; Instant expiresAt, revokedAt, createdAt; }
```

Lưu ý mapping:
- `@SQLRestriction("deleted_at IS NULL")` trên `Song` khiến `findById` không thấy bài đã xoá. Setlist cần biết "bài đã xoá" để hiện `available=false` → dùng **native/JPQL query riêng** không qua entity `Song` (projection `SetlistItemRow` LEFT JOIN songs, kiểm `deleted_at`), hoặc bỏ `@SQLRestriction` và tự lọc trong repository. **Chọn**: bỏ `@SQLRestriction`, mọi query có `AND s.deletedAt IS NULL` qua default method/Specification `SongSpecs.notDeleted()` — an toàn hơn về mặt tường minh.
- `Comment` cũng không dùng `@SQLRestriction` vì cần đọc node đã xoá còn reply.
- Auditing: `@EnableJpaAuditing`; `updated_at` trên bảng dùng `@UpdateTimestamp`/auditing, **không** trigger DB.
- Native `UPDATE` counter (like_count…) không đi qua entity → không tăng `version`, đúng ý (counter không được làm hỏng optimistic lock của người sửa nội dung).

## 5. Query pattern quan trọng (tránh N+1)

| Use case | Cách làm |
|---|---|
| Community list | 1 query `SongSummaryRow` (JPQL constructor expression, join fetch owner cột cần) + 1 count query. Không load `content` (dùng `substring(content,1,200)`) |
| Comments gốc (cursor) | Query 1: comment + `JOIN FETCH user` với keyset `WHERE (created_at,id) < (:t,:i)` `LIMIT limit+1`. Query 2: `SELECT comment_id FROM comment_likes WHERE user_id=:me AND comment_id IN (:ids)` để tính `isLiked`. `likes`/`repliesCount` đã denormalize ⇒ **2 query cố định**, không phụ thuộc số comment |
| Rating summary | 1 query `SELECT score, count(*) … GROUP BY score` + 1 query `myScore` |
| Setlist detail | 1 query fetch setlist + songs (`LEFT JOIN FETCH` sang `song` và `song.owner`), sắp `order_index` |
| Like | `INSERT … ON CONFLICT DO NOTHING` → nếu `rowCount=1` mới `UPDATE comments SET like_count = like_count + 1` |
| Upsert rating | `INSERT … ON CONFLICT (user_id,song_id) DO UPDATE` rồi recompute `rating_avg/count` bằng `UPDATE songs SET … = (SELECT …)` trong cùng transaction |

Keyset `top` cho comment: cursor mã hoá `{likeCount, createdAt, id}`; điều kiện `(like_count, created_at, id) < (:l,:t,:i)` (row-value comparison, dùng đúng index `idx_comments_root_top`).

## 6. Soft delete, audit, dọn dẹp

| Bảng | Soft delete | Audit |
|---|---|---|
| `songs` | `deleted_at` | `created_at/updated_at`, `version` |
| `comments` | `deleted_at` (giữ node nếu còn reply) | + `edited_at` |
| `shared_links` | `revoked_at` | `created_at` |
| Khác | Xoá cứng | `created_at` |

Scheduled jobs (`@Scheduled`, dùng ShedLock nếu chạy nhiều instance):
- Mỗi giờ: xoá `refresh_tokens` hết hạn > 7 ngày, `password_reset_tokens` hết hạn.
- Mỗi giờ: xoá `idempotency_keys` > 24h.
- Hằng ngày: xoá cứng `songs.deleted_at < now() - 30 days` (chỉ khi không còn tham chiếu cần giữ).
