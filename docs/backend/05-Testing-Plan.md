# 05 – Testing & Verification Plan (Backend)

Công cụ: JUnit 5, Mockito, AssertJ, Spring Boot Test, Testcontainers (PostgreSQL 16 — **không dùng H2**, vì dùng `citext`, partial index, `ON CONFLICT`, deferrable constraint), MockMvc, `spring-security-test` (`jwt()` post-processor).

## 1. Repository tests – `@DataJpaTest` + Testcontainers

| Test | Kiểm tra |
|---|---|
| `UserRepositoryTest` | `findByEmail` case-insensitive (`A@x.com` = `a@x.com`); trùng email → `DataIntegrityViolationException` |
| `SongRepositoryTest` | `findPublicBySlug` không trả bài private/đã xoá; `SongSpecs` kết hợp `q` + `key` + `notDeleted`; slug unique |
| `CommentRepositoryTest` | Keyset `newest`/`top` không trùng/thiếu khi có cùng `created_at`/`like_count` (tie-break `id`); comment xoá & không reply bị loại; comment xoá còn reply được giữ |
| `CommentLikeRepositoryTest` | `insertIgnore` lần 2 trả 0; `likedIds` đúng tập |
| `RatingRepositoryTest` | `upsert` 2 lần → 1 dòng, score mới; `recompute` avg làm tròn đúng; distribution đủ |
| `SetlistRepositoryTest` | Unique deferrable: hoán vị 2 bài trong 1 transaction thành công; trùng `order_index` khi commit → lỗi |
| `SharedLinkRepositoryTest` | Partial unique: 2 link active cùng bài → lỗi; sau `revoke` tạo được link mới; `findActiveByCode` loại link hết hạn/bài đã xoá |
| Query count | Danh sách comment 50 phần tử ≤ 2 query; community list ≤ 2 query; setlist detail 1 query |

## 2. Service tests – JUnit 5 + Mockito

| Service | Case |
|---|---|
| `AuthService` | register ok / trùng email 409; login sai pass & sai email → cùng lỗi; Google: email đã có → link, không tạo user; `email_verified=false` → 401; user BLOCKED → 403; refresh rotate ok; refresh dùng lại token cũ → revoke family + 401; forgot email không tồn tại vẫn thành công (không gửi mail); reset token hết hạn/đã dùng → 422 |
| `SongService` | update bài người khác → 404; `version` lệch → 409; publish sinh slug, trùng slug → retry; unpublish giữ slug; delete gọi `revokeBySong`; sync 11 item → validation; sync lỗi item giữa chừng → rollback (không item nào lưu); sync cùng `Idempotency-Key` → không tạo trùng |
| `RatingService` | tự chấm bài mình → 422; bài private → 422; avg/count cập nhật đúng sau upsert & xoá; score 0/6 (kiểm ở controller) |
| `CommentService` | reply vào reply → 422; parent khác song → 400; reply vào comment xoá → 422; xoá comment cha còn reply → node giữ, `deleted=true`; xoá 2 lần không giảm counter 2 lần; admin xoá comment người khác ok, user thường → 403; like 2 lần → `likes` +1 duy nhất; unlike khi chưa like → không âm |
| `SetlistService` | vượt 100 setlist / 200 bài → 422; thêm trùng → 409; thêm bài private của người khác → 422; reorder thiếu/thừa id → 422 `REORDER_MISMATCH`; remove đánh lại index liên tục; bài bị xoá → `available=false` |
| `ShareService` | bài người khác → 404; đã có link → trả link cũ (200); code trùng → retry; view: `canEdit` true chỉ với chủ; bài xoá/link revoked → 404 |
| `CursorCodec` | encode/decode round trip; cursor rác → `MALFORMED_REQUEST` |
| `Base62CodeGenerator` | độ dài 10, chỉ `[0-9A-Za-z]`, 100k mẫu không trùng (sanity), phân bố ký tự không lệch |
| `Slugger` | "Nơi này có anh" → `noi-nay-co-anh-xxxxxx`; `đ`/`Đ`; ký tự đặc biệt/emoji; chuỗi rỗng sau lọc → fallback `song-xxxxxx` |

## 3. Controller tests – `@WebMvcTest` + MockMvc

Mỗi endpoint kiểm: **status**, **JSON shape** (`jsonPath` cho field bắt buộc trong contract), và **validation**.

| Nhóm | Case |
|---|---|
| Auth | `register` password 7 ký tự / không có số / >72 → 400 với `errors[].field=password`; email sai định dạng; body rỗng; JSON hỏng → 400 `MALFORMED_REQUEST` |
| Security | Endpoint 🔒 không token → 401 JSON `ErrorResponse` (không HTML); token hết hạn → 401; role USER gọi `/api/admin/**` → 403; endpoint 🔓 + token rác → 200 như guest |
| Pagination | `limit=0`, `limit=51`, `page=0`, `cursor=abc` → 400; mặc định `limit=20`; `sort=xyz` → 400 |
| Song | `content` 50.001 ký tự → 400; `originalKey="H"` → 400; `originalKey="C#"` ok (serialize/deserialize đúng); title toàn khoảng trắng → 400 |
| Community | `score=6` → 400; comment `content` rỗng/2001 ký tự → 400; `reason` lạ → 400 |
| Share | `code` sai độ dài/ký tự lạ → 404; `Cache-Control: private, no-cache`; `Location` header trên 201 |
| Content-Type | Ký tự tiếng Việt có dấu round-trip không hỏng (UTF-8) |

## 4. Integration tests – `@SpringBootTest` + Testcontainers + MockMvc (luồng end-to-end)

1. **Auth flow**: register → login → gọi `/api/user/me` → refresh → refresh token cũ bị từ chối → logout → refresh bị từ chối.
2. **Library → Publish → Community**: tạo bài → publish → xuất hiện ở `GET /api/songs` (newest & top) → tìm theo `q` (có dấu/không dấu — ghi rõ: MVP dùng ILIKE nên **không** tìm không dấu; test khẳng định hành vi hiện tại) → unpublish → biến mất, nhưng chủ vẫn xem được ở library.
3. **Rating + Comment**: user B chấm 5 sao & bình luận → `rating-summary` đúng → user A (chủ) chấm bài mình → 422 → B like comment 2 lần → `likes=1` → B xoá comment có reply → list vẫn hiện node `deleted=true`.
4. **Share**: tạo link → guest `GET /api/shares/{code}` (`canEdit=false`) → chủ gọi (`canEdit=true`, có `song.id`) → chủ sửa bài → guest thấy nội dung mới → chủ xoá bài → guest 404.
5. **Setlist**: tạo → thêm 3 bài → reorder → reload thứ tự đúng → xoá bài giữa → index liên tục → chủ bài gốc xoá bài → item `available=false`.
6. **Sync cookie history**: 3 item với `Idempotency-Key` → gửi lại → không tạo trùng.
7. **Admin**: report comment → admin resolve + `deleteComment` → comment ẩn; admin block user → user login 403, refresh 401.

## 5. Concurrency / race tests (Testcontainers, `ExecutorService`)

| Kịch bản | Kỳ vọng |
|---|---|
| 20 thread cùng đăng ký 1 email | đúng 1 thành công, 19 lần 409 |
| 20 thread cùng like 1 comment (cùng user) | `like_count = 1` |
| 20 user khác nhau cùng like | `like_count = 20` (không mất cập nhật) |
| 2 thread cùng refresh 1 token | 1 thành công, 1 thất bại theo grace rule |
| 2 thread tạo share link cho cùng 1 bài | cả hai nhận **cùng** `uniqueCode`, DB có đúng 1 link active |
| 2 thread `add-song` vào cùng setlist | `order_index` liên tục 0,1 không trùng |
| 2 thread `reorder` khác nhau | tuần tự hoá (lock), kết quả là 1 trong 2 thứ tự hợp lệ |
| 2 tab `PUT` cùng bài với `version` cũ | 1 thành công, 1 → 409 `VERSION_CONFLICT` |
| 50 user cùng chấm sao 1 bài | `rating_count = 50`, `rating_avg` khớp `AVG(score)` |

## 6. Transaction rollback

- `sync` 3 item, item thứ 3 vi phạm DB constraint → không item nào được lưu, response 4xx.
- `SongService.delete` lỗi khi `revokeBySong` (mock throw) → `deleted_at` **không** được set.
- `CommentService.create` lỗi ở bước cập nhật counter → comment **không** được lưu.
- `resetPassword` lỗi sau khi đổi hash → token vẫn chưa bị đánh dấu `used`, không revoke dở.

## 7. Phân quyền (ma trận tối thiểu phải có test)

| Hành động | Guest | User (chủ) | User khác | Admin |
|---|---|---|---|---|
| Xem community list / chi tiết public | ✅ | ✅ | ✅ | ✅ |
| Xem library của người khác | ❌ 401 | ✅ | ❌ 404 | ❌ 404 |
| Sửa/xoá/publish bài | ❌ 401 | ✅ | ❌ 404 | ❌ 404 |
| Tạo share link cho bài | ❌ 401 | ✅ | ❌ 404 | ❌ 404 |
| Xem share link | ✅ | ✅ (`canEdit`) | ✅ | ✅ |
| Rating / comment / like / report | ❌ 401 | ✅ (rating bài mình ❌ 422) | ✅ | ✅ |
| Sửa comment | ❌ 401 | ✅ | ❌ 403 | ❌ 403 |
| Xoá comment | ❌ 401 | ✅ | ❌ 403 | ✅ |
| Setlist của người khác | ❌ 401 | ✅ | ❌ 404 | ❌ 404 |
| `/api/admin/**` | ❌ 401 | ❌ 403 | ❌ 403 | ✅ |

## 8. Edge cases theo input

- Chuỗi rỗng, chỉ khoảng trắng, null (mọi field `@NotBlank`), Unicode: tiếng Việt tổ hợp vs dựng sẵn (NFC), emoji, ký tự điều khiển `\u0000` (PostgreSQL từ chối `\0` → BE phải strip trước khi lưu).
- Boundary: `content` đúng 50.000 (ok) / 50.001 (400); `title` 200/201; comment 2000/2001; `limit` 1/50/51; `page` rất lớn → trả `data: []`, không lỗi.
- ID không phải UUID trong path → 400 `MALFORMED_REQUEST` (không 500).
- Enum sai chữ hoa/thường (`"c#"`, `"Db"`) → 400 (BE chỉ nhận 12 giá trị sharp; FE chịu trách nhiệm chuẩn hoá `Db → C#` như `FLAT_ALIASES`).

## 9. Non-functional

- **Performance**: seed 100k bài + 1M comment, đo `GET /api/songs?sort=top`, comments `top` p95 < 100 ms; `EXPLAIN` xác nhận dùng partial index đã tạo.
- **Migration**: chạy toàn bộ `V1..Vn` trên DB rỗng, kiểm `ddl-auto=validate` pass (entity ↔ schema đồng bộ).
- **Contract**: xuất OpenAPI, diff với `01-API-Contract.md`/`types/api.ts`; chạy Schemathesis (fuzz) trên staging — không có 500.
- **Security**: kiểm không log token; dependency scan (`mvn dependency-check`); thử JWT `alg=none`, JWT ký sai key → 401.
