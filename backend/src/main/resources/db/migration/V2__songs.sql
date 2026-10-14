-- Bài trong "Library" chính là songs với owner_id = me; visibility điều khiển public/private.
-- Các cột của Community (slug, published_at, rating_*, comment_count) tạo sẵn ở đây vì
-- community/share/setlist đều FK và denormalize vào bảng này.
CREATE TABLE songs (
  id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id       uuid          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  title          varchar(200)  NOT NULL,
  artist         varchar(200),
  content        text          NOT NULL,
  original_key   varchar(2)    NOT NULL,                  -- nhãn thăng: 'C', 'C#', ...
  target_key     varchar(2)    NOT NULL,
  visibility     varchar(10)   NOT NULL DEFAULT 'PRIVATE',
  slug           varchar(230),                            -- NULL khi PRIVATE
  published_at   timestamptz,                             -- NULL khi PRIVATE
  rating_avg     numeric(3,2)  NOT NULL DEFAULT 0,        -- denormalized
  rating_count   integer       NOT NULL DEFAULT 0,
  comment_count  integer       NOT NULL DEFAULT 0,
  version        bigint        NOT NULL DEFAULT 0,        -- optimistic locking
  deleted_at     timestamptz,                             -- soft delete
  created_at     timestamptz   NOT NULL DEFAULT now(),
  updated_at     timestamptz   NOT NULL DEFAULT now(),
  CONSTRAINT ck_song_visibility CHECK (visibility IN ('PRIVATE','PUBLIC')),
  CONSTRAINT ck_song_keys CHECK (
    original_key IN ('C','C#','D','D#','E','F','F#','G','G#','A','A#','B') AND
    target_key   IN ('C','C#','D','D#','E','F','F#','G','G#','A','A#','B')),
  CONSTRAINT ck_song_content_len CHECK (char_length(content) <= 50000),
  CONSTRAINT ck_song_public_slug CHECK (
    visibility = 'PRIVATE' OR (slug IS NOT NULL AND published_at IS NOT NULL))
);

CREATE UNIQUE INDEX uq_songs_slug ON songs(slug) WHERE slug IS NOT NULL;

-- Library của tôi: WHERE owner_id=? AND deleted_at IS NULL ORDER BY updated_at DESC
CREATE INDEX idx_songs_owner_updated ON songs(owner_id, updated_at DESC) WHERE deleted_at IS NULL;

CREATE INDEX idx_songs_public_newest ON songs(published_at DESC, id DESC)
  WHERE visibility = 'PUBLIC' AND deleted_at IS NULL;
CREATE INDEX idx_songs_public_top ON songs(rating_avg DESC, rating_count DESC, id DESC)
  WHERE visibility = 'PUBLIC' AND deleted_at IS NULL;
CREATE INDEX idx_songs_public_key ON songs(target_key, published_at DESC)
  WHERE visibility = 'PUBLIC' AND deleted_at IS NULL;

CREATE INDEX idx_songs_title_trgm  ON songs USING gin (title  gin_trgm_ops);
CREATE INDEX idx_songs_artist_trgm ON songs USING gin (artist gin_trgm_ops);
