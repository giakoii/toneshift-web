# 01 – API Contract (FE ↔ BE)

> Nguồn sự thật cho tích hợp. Quy ước chung (error format, pagination, header, giới hạn) xem [`00-Backend-Overview.md`](./00-Backend-Overview.md) mục 5.
> Ký hiệu Auth: 🌐 public · 🔓 public nhưng dùng token nếu có · 🔒 yêu cầu đăng nhập · 🛡️ Admin.

## Mã lỗi (`code`) dùng chung

| HTTP | code | Khi nào |
|---|---|---|
| 400 | `VALIDATION_FAILED` | Bean Validation lỗi (kèm `errors[]`) |
| 400 | `MALFORMED_REQUEST` | JSON hỏng, sai kiểu, enum lạ, `limit`/`cursor` sai |
| 401 | `UNAUTHORIZED` | Thiếu/hết hạn/sai access token |
| 401 | `INVALID_CREDENTIALS` | Sai email/mật khẩu |
| 401 | `REFRESH_TOKEN_INVALID` | Refresh token sai/hết hạn/đã dùng lại |
| 403 | `FORBIDDEN` | Không đủ quyền (không phải chủ, không phải admin) |
| 403 | `ACCOUNT_BLOCKED` | User bị khoá |
| 404 | `NOT_FOUND` (+ `SONG_NOT_FOUND`, `COMMENT_NOT_FOUND`, `SETLIST_NOT_FOUND`, `SHARE_NOT_FOUND`, `USER_NOT_FOUND`) | Không tồn tại / đã xoá / không có quyền xem (private) |
| 409 | `EMAIL_ALREADY_EXISTS` | Đăng ký trùng email |
| 409 | `ALREADY_REPORTED` | User đã report comment này |
| 409 | `SONG_ALREADY_IN_SETLIST` | Thêm bài trùng vào setlist |
| 409 | `VERSION_CONFLICT` | `version` lệch (sửa đồng thời) |
| 422 | `COMMENT_REPLY_DEPTH_EXCEEDED` | Reply vào reply |
| 422 | `COMMENT_DELETED` | Reply/like/report comment đã xoá |
| 422 | `SONG_NOT_PUBLIC` | Rating/comment/thêm vào community trên bài không public |
| 422 | `CANNOT_RATE_OWN_SONG` | Tự đánh giá bài của mình |
| 422 | `SETLIST_LIMIT_EXCEEDED` | Vượt giới hạn setlist/bài |
| 422 | `REORDER_MISMATCH` | Danh sách reorder không khớp tập bài hiện tại |
| 422 | `SONG_UNAVAILABLE` | Thêm bài của người khác mà không public |
| 422 | `RESET_TOKEN_INVALID` | Token reset sai/hết hạn/đã dùng |
| 422 | `FORBIDDEN_OPERATION` | Admin khoá chính mình/admin khác |
| 409 | `CONFLICT` | Vi phạm ràng buộc DB khác (không lộ chi tiết SQL) |
| 429 | `RATE_LIMITED` | Vượt rate limit (kèm `Retry-After`) |
| 500 | `INTERNAL_ERROR` | Lỗi không mong đợi (message chung, chi tiết chỉ ở log theo `traceId`) |

> Quy ước 400 vs 422: 400 = sai **hình dạng** request (validation). 422 = request hợp lệ nhưng vi phạm **quy tắc nghiệp vụ**.

---

## 1. Auth (`auth` module) – Feature 02

### 1.1 `POST /api/auth/register` 🌐

Request:

```json
{ "name": "Khoi PG", "email": "khoi@example.com", "password": "Passw0rd!x" }
```

| Field | Java | Validation |
|---|---|---|
| name | `String` | `@NotBlank @Size(max=100)` |
| email | `String` | `@NotBlank @Email @Size(max=254)` (lưu lowercase) |
| password | `String` | `@NotBlank @Size(min=8, max=72)` + có chữ và số (`@Pattern`). Max 72 do giới hạn bcrypt |

Response **201**: `AuthResponse` (mục 1.6). Lỗi: 400, 409 `EMAIL_ALREADY_EXISTS`, 429.

### 1.2 `POST /api/auth/login` 🌐

Request: `{ "email": "...", "password": "..." }` (`@NotBlank @Email`, `@NotBlank`).
Response **200**: `AuthResponse`. Lỗi: 400, 401 `INVALID_CREDENTIALS` (cùng message cho email sai/mật khẩu sai, chống dò email), 403 `ACCOUNT_BLOCKED`, 429.
Tài khoản chỉ có Google (không có `password_hash`) đăng nhập bằng mật khẩu → 401 `INVALID_CREDENTIALS`.

### 1.3 `POST /api/auth/oauth/google` 🌐

Request: `{ "idToken": "<Google ID token>" }` (`@NotBlank`).
BE verify chữ ký, `aud` = Google client id, `exp`, `email_verified=true`. Email đã có → link thêm provider (không tạo user mới); chưa có → tạo user (`password_hash = NULL`, `email_verified_at = now`).
Response **200**: `AuthResponse`. Lỗi: 401 `INVALID_CREDENTIALS` (token sai/email chưa verified), 403 `ACCOUNT_BLOCKED`.

### 1.4 `POST /api/auth/refresh` 🌐

Request: `{ "refreshToken": "..." }` (`@NotBlank`). Rotate: token cũ bị revoke, trả cặp mới. Response **200**: `AuthResponse`. Lỗi: 401 `REFRESH_TOKEN_INVALID` (kể cả reuse → revoke cả family), 403 `ACCOUNT_BLOCKED`.

### 1.5 `POST /api/auth/logout` 🌐

Request: `{ "refreshToken": "..." }`. Response **204** (idempotent, token lạ vẫn 204).

### 1.6 `AuthResponse`

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "refreshToken": "b1c9...",
  "user": { "id": "uuid", "name": "Khoi PG", "email": "khoi@example.com", "image": null, "role": "USER" }
}
```

Claims access JWT: `sub` (userId), `role`, `iat`, `exp`. Không chứa email/thông tin nhạy cảm.

### 1.7 `POST /api/auth/forgot-password` 🌐

Request: `{ "email": "..." }`. Response **202** luôn luôn (không lộ email có tồn tại). Rate limit theo email + IP.

### 1.8 `POST /api/auth/reset-password` 🌐

Request: `{ "token": "...", "newPassword": "..." }` (password rule như register). Response **204**, revoke mọi refresh token của user. Lỗi: 400, 422 `RESET_TOKEN_INVALID`.

---

## 2. User – Feature 02

### 2.1 `GET /api/user/me` 🔒 → **200** `UserResponse`

```json
{ "id": "uuid", "name": "Khoi PG", "email": "khoi@example.com", "image": null, "role": "USER", "hasPassword": true, "providers": ["GOOGLE"], "createdAt": "2026-05-30T10:15:30Z" }
```

### 2.2 `PUT /api/user/profile` 🔒

Request: `{ "name": "…", "image": "https://…" | null }` — `name @NotBlank @Size(max=100)`, `image @Size(max=500)` + phải là URL `https`.
Response **200** `UserResponse`.

### 2.3 `POST /api/user/change-password` 🔒

Request: `{ "currentPassword": "…", "newPassword": "…" }`. Response **204**, revoke refresh token khác của user. Lỗi: 400, 401 `INVALID_CREDENTIALS` (sai mật khẩu hiện tại; tài khoản Google-only chưa có password → được phép đặt mà không cần `currentPassword`: gửi `currentPassword: null`).

---

## 3. Library – bài hát của tôi (`song` module)

Bài lưu ở "Library" chính là bản ghi `songs` với `owner_id = me`. Public/private điều khiển bằng `visibility`.

`SongRequest` (create/update):

```json
{
  "title": "Nơi này có anh",
  "artist": "Sơn Tùng M-TP",
  "content": "C       G\nNơi này có anh ...",
  "originalKey": "C",
  "targetKey": "D"
}
```

| Field | Java | Validation |
|---|---|---|
| title | `String` | `@NotBlank @Size(max=200)` |
| artist | `String` | `@Size(max=200)` nullable |
| content | `String` | `@NotBlank @Size(max=50000)` |
| originalKey | `MusicalKey` | `@NotNull` (enum 12 giá trị sharp) |
| targetKey | `MusicalKey` | `@NotNull` |
| version | `Long` | chỉ ở PUT, nullable; lệch → 409 |

`SongResponse`:

```json
{
  "id": "uuid", "slug": null, "title": "…", "artist": "…", "content": "…",
  "originalKey": "C", "targetKey": "D",
  "visibility": "PRIVATE", "publishedAt": null,
  "ratingAvg": 0.0, "ratingCount": 0,
  "owner": { "id": "uuid", "name": "Khoi PG", "image": null },
  "version": 3, "createdAt": "…", "updatedAt": "…"
}
```

`slug` và `publishedAt` là `null` khi `PRIVATE`.
`SongSummary` (dùng ở danh sách, **không có `content`** để nhẹ): `id, slug, title, artist, originalKey, targetKey, visibility, ratingAvg, ratingCount, commentCount, owner{id,name,image}, publishedAt, updatedAt` + `excerpt` (200 ký tự đầu của `content`).

| Method & URL | Auth | Mô tả | Thành công | Lỗi |
|---|---|---|---|---|
| `POST /api/library` | 🔒 | Lưu bài mới (visibility mặc định `PRIVATE`) | **201** `SongResponse` + header `Location: /api/library/{id}` | 400, 401 |
| `GET /api/library?q=&page=1&limit=20&sort=updated` | 🔒 | Danh sách bài của tôi. `q` tìm theo title/artist (ILIKE); `sort` = `updated`(mặc định) \| `title` | **200** page `SongSummary` | 400, 401 |
| `GET /api/library/{id}` | 🔒 | Chi tiết bài của tôi | **200** `SongResponse` | 401, 404 (kể cả bài người khác) |
| `PUT /api/library/{id}` | 🔒 | Sửa bài | **200** `SongResponse` | 400, 401, 404, 409 `VERSION_CONFLICT` |
| `DELETE /api/library/{id}` | 🔒 | Xoá mềm; đồng thời gỡ khỏi community; share link → 404 | **204** | 401, 404 |
| `POST /api/library/{id}/publish` | 🔒 | Đưa lên Community: sinh `slug`, `visibility=PUBLIC`, `publishedAt=now` (idempotent) | **200** `SongResponse` | 401, 404 |
| `POST /api/library/{id}/unpublish` | 🔒 | Gỡ khỏi Community (`PRIVATE`); giữ rating/comment nhưng ẩn | **200** `SongResponse` | 401, 404 |
| `POST /api/library/sync` | 🔒 | Đồng bộ cookie history sau login (feature 05) | **200** `{ "created": 3, "items": [SongSummary] }` | 400, 401 |

`POST /api/library/sync` body: `{ "items": [SongRequest, …] }` với `@Size(max=10)`, mỗi item `title` có thể do FE tự đặt "Bản cảm âm chưa đặt tên". Chạy trong **1 transaction**: một item lỗi validate → 400 cả lô (không lưu dở). Idempotency: FE gửi header `Idempotency-Key` (UUID lưu ở localStorage); cùng key trong 24h → trả kết quả cũ, không tạo trùng (bảng `idempotency_keys`).

---

## 4. Community songs (public) – Feature 03

| Method & URL | Auth | Mô tả | Thành công | Lỗi |
|---|---|---|---|---|
| `GET /api/songs?q=&key=&sort=newest&page=1&limit=20` | 🌐 | Danh sách bài công khai. `q`: title/artist (ILIKE, tối đa 100 ký tự); `key`: lọc theo `targetKey` (enum, tuỳ chọn); `sort`: `newest`(mặc định) \| `top` (`ratingAvg` desc, `ratingCount` desc, `id`) | **200** page `SongSummary` | 400 |
| `GET /api/songs/{slug}` | 🔓 | Chi tiết bài public (có `content`) | **200** `SongResponse` (thêm `myScore` nếu có token) | 404 |

`GET /api/songs/{slug}` trả `SongResponse` + field `myScore: number | null`.
Chỉ bài `PUBLIC` và chưa xoá; ngược lại 404 (không phân biệt private hay không tồn tại).

---

## 5. Rating & Comments – Feature 03

### 5.1 Rating (1 user – 1 rating/bài)

| Method & URL | Auth | Body | Thành công | Lỗi |
|---|---|---|---|---|
| `GET /api/songs/{id}/rating-summary` | 🔓 | – | **200** `RatingSummary` | 404 |
| `PUT /api/songs/{id}/rating` | 🔒 | `{ "score": 5 }` `@NotNull @Min(1) @Max(5)` | **200** `RatingSummary` (upsert) | 400, 401, 404, 422 `SONG_NOT_PUBLIC`, 422 `CANNOT_RATE_OWN_SONG` |
| `DELETE /api/songs/{id}/rating` | 🔒 | – | **200** `RatingSummary` | 401, 404 |

```json
{ "average": 4.5, "count": 128, "distribution": { "1": 2, "2": 3, "3": 10, "4": 40, "5": 73 }, "myScore": 5 }
```

`average` làm tròn 1 chữ số thập phân (HALF_UP); `myScore = null` nếu guest/chưa chấm. `distribution` luôn đủ 5 khoá.

### 5.2 Comment

`CommentResponse`:

```json
{
  "id": "uuid", "songId": "uuid", "parentId": null,
  "content": "Hay quá", "deleted": false,
  "user": { "id": "uuid", "name": "An", "image": null },
  "likes": 5, "isLiked": false, "repliesCount": 2,
  "edited": false, "createdAt": "…", "updatedAt": "…"
}
```

Comment đã xoá nhưng còn reply: `"deleted": true, "content": null, "user": null, "likes": 0, "isLiked": false` (FE hiển thị "Comment đã bị xóa"). `isLiked=false` với guest.

| Method & URL | Auth | Mô tả | Thành công | Lỗi |
|---|---|---|---|---|
| `GET /api/songs/{id}/comments?cursor=&limit=20&sortBy=newest` | 🔓 | Comment **gốc** của bài. `sortBy`: `newest`(mặc định) \| `top` (`likes` desc, `createdAt` desc, `id`) | **200** cursor page `CommentResponse` | 400, 404 |
| `GET /api/comments/{id}/replies?cursor=&limit=20` | 🔓 | Reply của 1 comment gốc, thứ tự **cũ → mới** | **200** cursor page | 400, 404 |
| `POST /api/comments` | 🔒 | Tạo comment/reply | **201** `CommentResponse` | 400, 401, 404, 422 (`SONG_NOT_PUBLIC`, `COMMENT_REPLY_DEPTH_EXCEEDED`, `COMMENT_DELETED`) |
| `PUT /api/comments/{id}` | 🔒 | Sửa (chỉ chủ) | **200** `CommentResponse` | 400, 401, 403, 404, 422 `COMMENT_DELETED` |
| `DELETE /api/comments/{id}` | 🔒 | Xoá mềm (chủ hoặc Admin) | **204** | 401, 403, 404 |
| `POST /api/comments/{id}/like` | 🔒 | Đặt like (idempotent) | **200** `{ "likes": 6, "isLiked": true }` | 401, 404, 422 `COMMENT_DELETED` |
| `DELETE /api/comments/{id}/like` | 🔒 | Bỏ like (idempotent) | **200** `{ "likes": 5, "isLiked": false }` | 401, 404 |
| `POST /api/comments/{id}/report` | 🔒 | Báo cáo | **201** `{ "id": "uuid", "status": "OPEN" }` | 400, 401, 404, 409 `ALREADY_REPORTED`, 422 `COMMENT_DELETED` |

`CommentRequest` (POST): `{ "songId": "uuid", "content": "…", "parentId": "uuid" | null }` — `songId @NotNull`, `content @NotBlank @Size(max=2000)` (trim), `parentId` nullable. `parentId` phải thuộc cùng `songId`, nếu không → 400.
`PUT` body: `{ "content": "…" }`.
`ReportRequest`: `{ "reason": "SPAM|OFFENSIVE|WRONG_CONTENT|OTHER", "detail": "…" }` — `reason @NotNull`, `detail @Size(max=500)`.

Cursor list: bỏ comment đã xoá **và không có reply** khỏi kết quả; `total` = số comment gốc hiển thị được.

---

## 6. Setlist – Feature 08

`SetlistItem`:

```json
{
  "songId": "uuid", "orderIndex": 0, "customKey": "D",
  "available": true,
  "song": { "id": "uuid", "title": "…", "artist": "…", "originalKey": "C", "targetKey": "D" }
}
```

`available=false` khi bài đã xoá/không còn public với người khác → `song` chứa `null`, FE hiển thị "Bài hát không còn khả dụng". `content` **không** nằm trong `SetlistItem` (Player prefetch bằng `GET /api/setlists/{id}/songs/{songId}`).

`SetlistResponse`: `{ id, name, description, songCount, items: [SetlistItem], version, createdAt, updatedAt }` (`items` sắp theo `orderIndex`).
`SetlistSummary`: `{ id, name, description, songCount, updatedAt }`.

| Method & URL | Auth | Body | Thành công | Lỗi |
|---|---|---|---|---|
| `POST /api/setlists` | 🔒 | `{ "name": "Show T7", "description": "…" }` `name @NotBlank @Size(max=100)`, `description @Size(max=500)` | **201** `SetlistResponse` | 400, 401, 422 `SETLIST_LIMIT_EXCEEDED` |
| `GET /api/setlists?page=1&limit=20` | 🔒 | – | **200** page `SetlistSummary` (sắp `updatedAt` desc) | 400, 401 |
| `GET /api/setlists/{id}` | 🔒 | – | **200** `SetlistResponse` | 401, 404 |
| `PUT /api/setlists/{id}` | 🔒 | `{ "name", "description", "version" }` | **200** `SetlistResponse` | 400, 401, 404, 409 |
| `DELETE /api/setlists/{id}` | 🔒 | – | **204** | 401, 404 |
| `POST /api/setlists/{id}/add-song` | 🔒 | `{ "songId": "uuid", "customKey": "D" \| null }` `songId @NotNull`, `customKey` nullable enum | **201** `SetlistResponse` (thêm vào cuối) | 400, 401, 404, 409 `SONG_ALREADY_IN_SETLIST`, 422 (`SONG_UNAVAILABLE`, `SETLIST_LIMIT_EXCEEDED`) |
| `PATCH /api/setlists/{id}/songs/{songId}` | 🔒 | `{ "customKey": "E" \| null }` | **200** `SetlistResponse` | 400, 401, 404 |
| `DELETE /api/setlists/{id}/songs/{songId}` | 🔒 | – | **200** `SetlistResponse` (đánh lại `orderIndex` liên tục) | 401, 404 |
| `PUT /api/setlists/{id}/reorder` | 🔒 | `{ "songIds": ["uuid", …] }` `@NotNull @Size(max=200)`, phải là **hoán vị đầy đủ** của tập bài hiện tại | **200** `SetlistResponse` | 400, 401, 404, 422 `REORDER_MISMATCH` |
| `GET /api/setlists/{id}/songs/{songId}` | 🔒 | – | **200** `SongResponse` + `customKey` (nội dung đầy đủ cho Player Mode/prefetch) | 401, 404 |

Bài được thêm phải: (a) của chính user, hoặc (b) `PUBLIC`. Bài (b) sau đó bị chủ gỡ → item `available=false`, không xoá khỏi setlist.

---

## 7. Shareable Link – Feature 09

### 7.1 `POST /api/shares` 🔒

Request:

```json
{ "songId": "uuid", "settings": { "allowCopy": true } }
```

`songId @NotNull`; `settings` nullable, `allowCopy` mặc định `true`. Chỉ chủ bài mới tạo được (bài người khác → 404). Idempotent theo bài: bài đã có link (chưa thu hồi) → trả link cũ (**200**); chưa có → tạo mới (**201**).

Response:

```json
{ "uniqueCode": "aB3dE9xYzQ", "url": "https://toneshift.vn/s/aB3dE9xYzQ", "createdAt": "…" }
```

`url` build từ cấu hình `app.public-web-url` (không hard-code). Lỗi: 400, 401, 404, 429.

### 7.2 `GET /api/shares/{code}` 🔓

`{code}` phải khớp `^[0-9A-Za-z]{10}$`, sai định dạng → 404 (không 400, không lộ quy tắc).

Response **200**:

```json
{
  "uniqueCode": "aB3dE9xYzQ",
  "song": { "title": "…", "artist": "…", "content": "…", "originalKey": "C", "tone": "D", "updatedAt": "…" },
  "owner": { "name": "KhoiPG" },
  "permissions": { "canEdit": false, "canCopy": true },
  "settings": { "allowCopy": true }
}
```

- `song.tone` = `targetKey` (FE docs cũ dùng tên `tone`).
- `owner.name` chỉ tên hiển thị; **không** trả email/id.
- `permissions.canEdit = true` khi token hợp lệ và `sub == song.owner`; FE khi đó mời sang `/library/{songId}` — thêm `songId` chỉ khi `canEdit=true`: `"song": { "id": "uuid", … }`.
- Header: `Cache-Control: private, no-cache` (canEdit phụ thuộc người xem). Có `ETag` (hash `updatedAt`) để FE Server Component dùng `If-None-Match`.
- 404 `SHARE_NOT_FOUND` khi code không có, bị thu hồi, hết hạn, hoặc bài đã xoá.

### 7.3 `DELETE /api/shares/{code}` 🔒 (thu hồi)

Chỉ chủ. **204**. Lỗi: 401, 404.

---

## 8. Admin – Feature 02

| Method & URL | Auth | Mô tả | Thành công |
|---|---|---|---|
| `GET /api/admin/reports?status=OPEN&page=1&limit=20` | 🛡️ | Danh sách report (kèm comment + reporter) | **200** page |
| `PATCH /api/admin/reports/{id}` | 🛡️ | `{ "status": "RESOLVED"\|"DISMISSED", "deleteComment": true }` | **200** |
| `POST /api/admin/users/{id}/block` | 🛡️ | Khoá user, revoke mọi refresh token | **204** |
| `POST /api/admin/users/{id}/unblock` | 🛡️ | Mở khoá | **204** |

Admin không thể khoá chính mình hoặc admin khác (422 `FORBIDDEN_OPERATION`). Lỗi chung: 401, 403, 404.

---

## 9. TypeScript types gợi ý cho FE (`types/api.ts`)

```ts
export type MusicalKey = "C"|"C#"|"D"|"D#"|"E"|"F"|"F#"|"G"|"G#"|"A"|"A#"|"B";
export type Visibility = "PRIVATE" | "PUBLIC";

export interface ApiError {
  timestamp: string; status: number; code: string; message: string;
  path: string; traceId: string; errors: { field: string; message: string }[];
}
export interface PageResponse<T> { data: T[]; page: number; limit: number; total: number; totalPages: number; hasNext: boolean }
export interface CursorResponse<T> { data: T[]; nextCursor: string | null; hasNext: boolean; total: number }

export interface UserRef { id: string; name: string; image: string | null }
export interface SongSummary { id: string; slug: string | null; title: string; artist: string | null; excerpt: string;
  originalKey: MusicalKey; targetKey: MusicalKey; visibility: Visibility; ratingAvg: number; ratingCount: number;
  commentCount: number; owner: UserRef; publishedAt: string | null; updatedAt: string }
export interface SongResponse extends Omit<SongSummary,"excerpt"|"commentCount"> { content: string; version: number; createdAt: string; myScore?: number | null }
export interface RatingSummary { average: number; count: number; distribution: Record<"1"|"2"|"3"|"4"|"5", number>; myScore: number | null }
export interface CommentResponse { id: string; songId: string; parentId: string | null; content: string | null; deleted: boolean;
  user: UserRef | null; likes: number; isLiked: boolean; repliesCount: number; edited: boolean; createdAt: string; updatedAt: string }
export interface ShareView { uniqueCode: string; song: { id?: string; title: string; artist: string | null; content: string; originalKey: MusicalKey; tone: MusicalKey; updatedAt: string };
  owner: { name: string }; permissions: { canEdit: boolean; canCopy: boolean }; settings: { allowCopy: boolean } }
```

---

## 10. Điểm lệch so với docs FE cũ (FE cần chỉnh khi tích hợp)

| # | Docs FE cũ | Contract này | Lý do |
|---|---|---|---|
| 1 | `GET comments?page=1&limit=20` nhưng response `nextCursor` (mâu thuẫn nội bộ) | Cursor thật: `cursor` + `limit`, `nextCursor` opaque | Cursor ổn định khi có comment mới chèn vào đầu |
| 2 | `POST /api/comments/:id/like` (ngầm hiểu toggle) | `POST` = like, `DELETE` = unlike | Idempotent, an toàn khi retry/optimistic rollback |
| 3 | `nextCursor: 2` (số) trong `designs/Community-Rating.md` | Chuỗi opaque hoặc `null` | Cursor sort `top` cần 2 khoá |
| 4 | Rating nằm chung form comment | Rating là endpoint riêng `PUT /api/songs/{id}/rating` | Rating độc lập comment; comment không bắt buộc chấm |
| 5 | Share `Response: { uniqueCode, url }` | Thêm `createdAt`; 201 mới / 200 đã có | Idempotent theo bài |
| 6 | `owner_id` nullable ở `shared_links` (guest share) | Bắt buộc đăng nhập (Q1) | Bài phải có chủ |
| 7 | `/api/library` chỉ nhắc tên | Định nghĩa đầy đủ mục 3 | |
| 8 | Auth do NextAuth + Prisma | BE cấp JWT, NextAuth chỉ giữ session | Overview D1 |
