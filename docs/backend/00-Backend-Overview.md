# ToneShift Backend – Technical Plan (Java / Spring Boot)

> Kế hoạch triển khai BACKEND cho toàn bộ MVP (features 02, 03, 08, 09 + Library/Songs). Feature 01, 04, 05, 06, 07 chạy hoàn toàn client-side, BE chỉ cung cấp endpoint `sync` cho cookie history (xem 01-API-Contract).
>
> **Chỉ gồm plan, contract, schema và class skeleton. Chưa viết source code chi tiết.**

## Mục lục tài liệu

| File | Nội dung |
|---|---|
| `00-Backend-Overview.md` | Kiến trúc, tech stack, quy ước chung, quyết định thiết kế, câu hỏi mở (file này) |
| [`01-API-Contract.md`](./01-API-Contract.md) | Toàn bộ endpoint, request/response, status code, error format, TypeScript types cho FE |
| [`02-Database-Design.md`](./02-Database-Design.md) | Flyway DDL, index, JPA entities |
| [`03-Class-Design.md`](./03-Class-Design.md) | Package structure, DTO, Repository, Service, Controller, Exception skeleton |
| [`04-Implementation-Checklist.md`](./04-Implementation-Checklist.md) | Checklist tuần tự Step 1 → Step 5 |
| [`05-Testing-Plan.md`](./05-Testing-Plan.md) | Unit / Integration test và edge case |

---

## 1. Hiện trạng FE (đã đối chiếu)

| Hạng mục | Hiện trạng repo | Ảnh hưởng tới BE |
|---|---|---|
| Converter (`store/converterStore.ts`, `lib/transpose.ts`) | Xử lý 100% client, chưa có API client | BE **không** transpose. Chỉ lưu `content` + `originalKey` + `targetKey` |
| Key values | `CHROMATIC_SCALE` = `C, C#, D, D#, E, F, F#, G, G#, A, A#, B` (`constants/musical-keys.ts`) | BE validate đúng 12 giá trị sharp này (enum `MusicalKey`) |
| Giới hạn text | Feature 01: tối đa 50.000 ký tự | `content` `@Size(max = 50000)` |
| Auth | Docs FE thiết kế NextAuth v5 + Prisma | **Thay đổi**: Prisma bị loại bỏ, DB do BE Java sở hữu. NextAuth chỉ còn là lớp session phía Next, gọi BE để xác thực (xem mục 4) |
| Types | `types/index.ts` chỉ có `NavLink`, `PricingPlan`, `Testimonial` | Cần thêm `types/api.ts` (gợi ý ở 01-API-Contract mục 9) |
| Chưa có | `lib/api-client.ts`, `services/*`, TanStack Query | Contract này là nguồn sự thật để FE viết các file đó |

> Ghi chú: FE docs (feature 03, 08, 09) mô tả API dạng `/api/...` của Next route handlers. Plan này giữ **nguyên path** `/api/...` nhưng host là service Java riêng (`NEXT_PUBLIC_API_URL`). Xem mục "Điểm lệch so với docs FE cũ" ở 01-API-Contract mục 10.

---

## 2. Tech stack đề xuất (production)

| Layer | Lựa chọn | Lý do |
|---|---|---|
| Language / JDK | Java 21 (LTS) | Records cho DTO, virtual threads (tuỳ chọn) |
| Framework | Spring Boot 3.x (bản stable mới nhất khi scaffold) | Chuẩn thị trường |
| Build | **Maven** | Theo yêu cầu |
| DB | PostgreSQL 16 | JSONB, `citext`, deferrable constraint, partial index |
| Migration | Flyway (`db/migration/V{n}__*.sql`) | Versioned, review được |
| ORM | Spring Data JPA (Hibernate 6) | + `JpaSpecificationExecutor` cho lọc động |
| Mapping | MapStruct + Lombok | Mapper compile-time, không reflection |
| Security | Spring Security 6 + `oauth2-resource-server` (JWT, Nimbus) | Stateless, ít dependency |
| Password | `BCryptPasswordEncoder` (strength 12) | Theo feature 02 |
| Google login | Verify Google ID token bằng Nimbus `JwtDecoder` (JWKS Google) | Không cần lưu Google access token |
| Validation | Jakarta Bean Validation | `@NotNull`, `@Size`, `@Pattern`… |
| API doc | springdoc-openapi (`/swagger-ui`) | Sinh OpenAPI cho FE |
| Rate limit | Bucket4j (in-memory, đổi sang Redis khi scale >1 instance) | Chống spam tạo share link, login |
| Mail | Spring Mail (SMTP/SES) cho reset password | Feature 02 |
| Test | JUnit 5, Mockito, Testcontainers (PostgreSQL), MockMvc | |
| Observability | Actuator + Micrometer, log JSON + `traceId` | |

---

## 3. Kiến trúc: Layered, "package by feature"

```
Request → Filter chain (CORS, JWT auth, rate limit)
        → Controller (validate @Valid, map DTO)
        → Service (@Transactional, business rule, quyền)
        → Repository (Spring Data JPA) → PostgreSQL
        ← Mapper (MapStruct) Entity → Response DTO
        ← GlobalExceptionHandler (@RestControllerAdvice) → ErrorResponse
```

Quy tắc phụ thuộc: `controller → service → repository`. Controller **không** gọi repository. Entity **không** ra khỏi service (luôn map sang DTO). Module này chỉ gọi module khác qua `service` public (ví dụ `share` gọi `SongService`, không gọi `SongRepository`).

Package gốc: `vn.toneshift.api` (chi tiết ở `03-Class-Design.md`).

Modules:

| Module | Phục vụ feature | Bảng chính |
|---|---|---|
| `auth` | 02 | `users`, `auth_providers`, `refresh_tokens`, `password_reset_tokens` |
| `user` | 02 | `users` |
| `song` | Library (02), Community list (03), sync cookie (05) | `songs` |
| `community` | 03 | `ratings`, `comments`, `comment_likes`, `reports` |
| `setlist` | 08 | `setlists`, `setlist_songs` |
| `share` | 09 | `shared_links` |
| `admin` | 02 (Admin) | `reports`, `users` |

---

## 4. Quyết định thiết kế quan trọng

| # | Vấn đề | Quyết định | Lý do |
|---|---|---|---|
| D1 | Auth giữa Next và Java | BE cấp **access JWT (15 phút)** + **refresh token opaque (30 ngày, rotate)**. NextAuth (Credentials + Google) gọi `POST /api/auth/login` / `/api/auth/oauth/google` phía server Next, lưu 2 token trong cookie HTTP-only đã mã hoá của NextAuth. FE gọi BE bằng `Authorization: Bearer <accessToken>` | Không lộ token cho JS trình duyệt, không cần CSRF cho BE (stateless, không dùng cookie auth) |
| D2 | Link account Google/Credentials | Tìm user theo `email` (đã verified bởi Google) → nếu có thì thêm dòng `auth_providers`, không tạo user mới | Feature 02 edge case |
| D3 | Refresh token reuse | Lưu hash (SHA-256), có `family_id`. Dùng lại token đã revoke → revoke cả family, trả 401 | Chống đánh cắp token |
| D4 | ID | UUID (`gen_random_uuid()`), không lộ số tự tăng | Chống enumeration |
| D5 | Xoá bài hát | Soft delete (`deleted_at`). Share link và setlist tham chiếu bài đã xoá → 404 / `available=false` | Feature 08 edge case |
| D6 | Xoá comment | Soft delete. Còn reply con thì giữ node, trả `deleted=true`, `content=null` | Feature 03 edge case |
| D7 | Reply depth | Tối đa 1 cấp. Reply vào reply → 422 `COMMENT_REPLY_DEPTH_EXCEEDED` | Feature 03 refactoring |
| D8 | Like | `POST /like` idempotent (đặt), `DELETE /like` (bỏ). Không dùng toggle | Retry-safe, khớp optimistic update |
| D9 | Average rating | Denormalize `songs.rating_avg`, `rating_count`, cập nhật trong cùng transaction với upsert rating | Sort `top` và list nhanh, không `AVG()` mỗi request |
| D10 | Like/reply count | Denormalize `comments.like_count`, `reply_count`, tăng giảm bằng `UPDATE ... SET x = x + 1` (atomic), chỉ khi insert/delete thật sự ảnh hưởng dòng | Tránh race condition, sort `top` không cần join |
| D11 | Pagination | Danh sách bài (community/library/setlist): **page-based** (`page` 1-based, `limit`). Comment: **cursor-based** (`cursor` opaque, `limit`) | Khớp docs FE (`page`,`limit` cho songs; `nextCursor` cho comments) |
| D12 | Share code | 10 ký tự Base62 từ `SecureRandom` (~8.4×10^17 tổ hợp), retry tối đa 5 lần khi trùng | Feature 09 (`VARCHAR(10)`) |
| D13 | Share luôn mới nhất | Share trỏ `song_id`, đọc live từ `songs` | Quyết định trong `designs/Shareable-Link.md` |
| D14 | Thứ tự setlist | `order_index` + unique **deferrable** `(setlist_id, order_index)`; mọi thao tác ghi khoá dòng `setlists` (`PESSIMISTIC_WRITE`) | Reorder/add đồng thời không tạo trùng vị trí |
| D15 | Optimistic locking | `songs.version`, `setlists.version` (`@Version`); `PUT` nhận `version` tuỳ chọn → lệch thì 409 | Tránh ghi đè khi sửa 2 tab |

---

## 5. Quy ước chung

### 5.1 Base URL, header

- Base: `https://api.toneshift.vn` (local: `http://localhost:8080`). Path bắt đầu `/api`.
- `Content-Type: application/json; charset=UTF-8` (BE hỗ trợ tiếng Việt có dấu).
- `Authorization: Bearer <accessToken>` cho endpoint yêu cầu đăng nhập. Endpoint public mà có token hợp lệ thì BE dùng để tính `isLiked`, `canEdit`, `myScore`. Token sai/hết hạn ở endpoint **public** → vẫn coi là guest (không 401).
- Thời gian: ISO-8601 UTC (`2026-05-30T10:15:30Z`).
- JSON: `camelCase`, field null thì **có mặt và bằng `null`** (FE dễ type). `Jackson: WRITE_DATES_AS_TIMESTAMPS=false`.

### 5.2 Error response (mọi lỗi)

```json
{
  "timestamp": "2026-05-30T10:15:30Z",
  "status": 422,
  "code": "COMMENT_REPLY_DEPTH_EXCEEDED",
  "message": "Chỉ được trả lời bình luận gốc",
  "path": "/api/comments",
  "traceId": "8f3c1b2a",
  "errors": [ { "field": "content", "message": "không được để trống" } ]
}
```

`errors` chỉ có khi `code = VALIDATION_FAILED` (còn lại là `[]`). `message` tiếng Việt, FE có thể hiển thị thẳng vào Toast; FE nên switch theo `code`.

### 5.3 Pagination response

Page-based:

```json
{ "data": [ ... ], "page": 1, "limit": 20, "total": 134, "totalPages": 7, "hasNext": true }
```

Cursor-based:

```json
{ "data": [ ... ], "nextCursor": "eyJ0IjoiMjAyNi0wNS0zMFQxMDoxNTozMFoiLCJpIjoiLi4uIn0", "hasNext": true, "total": 150 }
```

`nextCursor = null` khi hết. Cursor là chuỗi opaque (base64url của `{sortValue, id}`), FE **không** parse.

### 5.4 Giới hạn

| Field | Rule |
|---|---|
| `limit` | 1–50, mặc định 20; ngoài khoảng → 400 |
| `page` | ≥ 1 |
| Song `content` | 1–50.000 ký tự |
| Song `title` | 1–200 |
| Comment `content` | 1–2.000 |
| Setlist `name` | 1–100; tối đa 100 setlist/user; tối đa 200 bài/setlist |
| Rate limit | login 10/phút/IP, register 5/phút/IP, `POST /api/shares` 30/giờ/user, `POST /api/comments` 20/phút/user, like 60/phút/user → 429 + `Retry-After` |

---

## 6. Security tổng quan

- Stateless, `SessionCreationPolicy.STATELESS`, CSRF disabled (không dùng cookie auth).
- CORS: chỉ origin FE (`app.cors.allowed-origins`), methods `GET,POST,PUT,PATCH,DELETE,OPTIONS`, header `Authorization,Content-Type`, `maxAge=3600`.
- Phân quyền theo path + method security:
  - Public: `POST /api/auth/**`, `GET /api/songs/**`, `GET /api/shares/*`, `GET /api/songs/*/comments`, `GET /api/comments/*/replies`, `GET /api/songs/*/rating-summary`.
  - `ROLE_USER`: mọi endpoint còn lại.
  - `ROLE_ADMIN`: `/api/admin/**`, và xoá comment của người khác.
- Ownership check nằm trong **Service** (không chỉ controller): sai chủ → 403 (hoặc 404 nếu không muốn lộ tồn tại, áp dụng cho `library`/`setlists` của người khác).
- User `status = BLOCKED` → login 403 `ACCOUNT_BLOCKED`; access token còn hạn vẫn bị chặn bởi filter kiểm tra `status` (cache 60s) – hoặc chấp nhận tối đa 15 phút trễ (**quyết định: chấp nhận**, đơn giản hơn; refresh sẽ bị từ chối ngay).
- Không log password/token. Reset-password token lưu dạng hash, dùng 1 lần, hết hạn 30 phút.
- Nội dung comment: BE lưu plain text, **FE escape khi render** (không render HTML). BE strip ký tự điều khiển.

---

## 7. Câu hỏi mở / giả định cần bạn xác nhận

| # | Giả định hiện tại | Cần xác nhận |
|---|---|---|
| Q1 | Tạo share link **yêu cầu đăng nhập** (bài phải lưu vào Library trước) vì `songs.owner_id` NOT NULL. `shared_links.owner_id` nullable trong docs cũ bị bỏ (luôn có owner) | Có cho **guest** share bản chưa lưu không? Nếu có, cần bảng `anonymous_songs` hoặc `owner_id` nullable + TTL dọn rác |
| Q2 | Người dùng đã "publish" bài lên Community bằng `POST /api/library/{id}/publish` (docs cũ chỉ nhắc nút "Chia sẻ bản cảm âm" mà không định nghĩa API) | OK không? Có cần admin duyệt trước khi hiện (`PENDING`)? |
| Q3 | Bài public chỉ dùng slug để xem (`/community/[slug]`), API rating/comment dùng `id` (UUID) | OK |
| Q4 | Chưa có OAuth Facebook (docs 02 nhắc `SocialAuth` Google/Facebook) – chỉ Google | OK cho MVP? |
| Q5 | Email verification khi đăng ký **không** bắt buộc ở MVP (`email_verified_at` để sẵn) | OK? |
| Q6 | Share link không hết hạn, không password (`expires_at`, `settings` để sẵn trong DB nhưng chưa expose trong API MVP, trừ `allowCopy`) | Có cần expose `expiresAt` ngay? |
| Q7 | Cookie-sync (feature 05 → 02): endpoint `POST /api/library/sync` nhận tối đa 10 bài từ history | OK |
