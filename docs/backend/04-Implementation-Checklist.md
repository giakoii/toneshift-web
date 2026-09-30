# 04 – Step-by-Step Backend Implementation Checklist

Triển khai từ lõi ra ngoài. Mỗi module đi hết Step 1→4 trước khi qua module kế; Step 5 làm cross-cutting **sớm** (Step 0/5a) vì mọi module cần.
Thứ tự module đề xuất (theo `MVP_ROADMAP`): **Foundation → Auth/User → Song(Library) → Share → Community → Setlist → Admin**.

---

## Step 0 – Foundation (làm trước)

- [ ] `pom.xml`, `ToneShiftApplication`, `application.yml` (+ `application-dev.yml`, `application-test.yml`)
- [ ] `docker-compose.yml` PostgreSQL 16 cho dev
- [ ] `common/exception/*` (`ErrorCode`, `ApiException`, `GlobalExceptionHandler`) + `ErrorResponse`
- [ ] `common/dto/*` (`PageResponse`, `CursorResponse`, `UserRef`), `CursorCodec`
- [ ] `config/*`: `JpaAuditingConfig`, `JacksonConfig` (ISO date, null giữ), `OpenApiConfig`, `CorsConfig`
- [ ] `TraceIdFilter` (MDC `traceId`, trả header `X-Trace-Id`)
- [ ] Testcontainers base class `AbstractIntegrationTest`
- **Edge case**: `open-in-view=false`; `ddl-auto=validate`; timezone JVM = UTC (`-Duser.timezone=UTC`)

## Step 1 – Database Migration / Entities / Repository

| Module | File (đường dẫn dự kiến) | Logic cốt lõi / edge case cần bẫy |
|---|---|---|
| auth/user | `db/migration/V1__extensions_and_users.sql`; `user/entity/User`, `auth/entity/{AuthProvider,RefreshToken,PasswordResetToken}`; repositories | `email` citext unique (case-insensitive); lưu lowercase; **duplicate key** khi 2 request đăng ký đồng thời → bắt `DataIntegrityViolation` |
| song | `V2__songs.sql`; `song/entity/Song`; `SongRepository`, `SongSpecs` | Partial index cho public; **N+1** ở list → projection, không load `content`; `ck_song_public_slug` |
| community | `V3__community.sql`; `Rating, Comment, CommentLike, Report` + repos | Keyset query đúng thứ tự cột index; native `INSERT ON CONFLICT` cho like/rating |
| setlist | `V4__setlists.sql`; `Setlist, SetlistSong(+Id)` + repo | Unique **deferrable**; `PESSIMISTIC_WRITE` lock query |
| share | `V5__shared_links.sql`; `SharedLink` + repo | Partial unique 1 link/bài; unique `unique_code` |
| infra | `V6__idempotency.sql`; `IdempotencyKey` | PK `(user_id,key)` |

- [ ] Viết migration V1–V6, chạy `flyway:migrate` trên DB sạch
- [ ] Entity + repository từng module
- [ ] `@DataJpaTest` (Testcontainers) cho custom query (xem 05-Testing-Plan)

## Step 2 – DTOs & Mappers

- [ ] Records request/response theo `03-Class-Design.md` (đúng tên field với `01-API-Contract.md` — **không đổi tên tuỳ ý**)
- [ ] `MusicalKey` `@JsonValue/@JsonCreator` (test serialize `"C#"`)
- [ ] MapStruct: `UserMapper, SongMapper, CommentMapper, SetlistMapper`
- **Edge case**: `CommentMapper` với comment đã xoá → `content=null`, `user=null` (mapper **phải** ẩn, không tin FE); `SongSummary.excerpt` cắt theo code point (không cắt giữa ký tự tiếng Việt tổ hợp — normalize NFC trước khi lưu content)
- [ ] Bật `-Amapstruct.unmappedTargetPolicy=ERROR` để lỡ thiếu field là fail build

## Step 3 – Service Layer & Business Validation Rules

| Module | Service | Rule bắt buộc | Race/Concurrency |
|---|---|---|---|
| auth | `AuthService`, `TokenService`, `GoogleIdTokenVerifier` | link Google theo email; refresh rotate + reuse detection; login không phân biệt sai email/sai pass; forgot luôn 202 | Refresh: `SELECT FOR UPDATE` token → 2 refresh song song chỉ 1 thành công (cái sau bị coi reuse → cân nhắc **grace 10s** cho retry mạng: cùng token bị dùng lại trong 10s → trả 401 nhưng **không** revoke family) |
| user | `UserService` | Google-only user đặt password lần đầu | – |
| song | `SongService` | ownership; publish sinh slug; delete → `revokeBySong`; sync ≤ 10 & idempotency | `@Version` → `VERSION_CONFLICT`; slug trùng retry; sync trùng key song song → PK `(user_id,key)` làm chốt |
| share | `ShareService` | chỉ chủ; trả link cũ nếu có; `canEdit` theo viewer | 2 request tạo link song song → `uq_shared_active_per_song` vi phạm → đọc lại link kia; code trùng → retry ≤5 |
| community | `RatingService` | không tự chấm; chỉ bài public; recompute avg | Khoá `songs` row (`FOR UPDATE`) khi recompute; **rating upsert** dùng `ON CONFLICT` |
| community | `CommentService` | depth 1; parent cùng song; reply vào comment xoá → 422; sửa/xoá ownership; admin xoá được | Counter **atomic UPDATE**; like `insertIgnore` rowCount; xoá idempotent (xoá 2 lần không giảm counter 2 lần: chỉ giảm khi `deleted_at` từ null→not null bằng `UPDATE … WHERE deleted_at IS NULL` đếm rowCount) |
| community | `ReportService` | không report comment xoá | `uq_report_once` → 409 |
| setlist | `SetlistService` | giới hạn 100/200; bài phải own/public; reorder = hoán vị đầy đủ; xoá bài đánh lại index | Lock `lockByIdAndOwner` mọi thao tác ghi; `order_index` deferrable |
| admin | `AdminReportService`, `AdminUserService` | không khoá admin; block → revoke refresh | – |

- [ ] `@Transactional(readOnly = true)` cho mọi method đọc; ghi mặc định `rollbackFor = Exception.class` nếu có checked exception
- [ ] Không gọi mail/HTTP ngoài trong transaction (dùng `@TransactionalEventListener(AFTER_COMMIT)`)
- [ ] Không trả entity ra ngoài service

## Step 4 – Controller Layer & Request Validation

- [ ] Controller theo contract: path, status (`ResponseEntity.created(location)`), `@Valid @RequestBody`, `@Validated` cho `@RequestParam` (`@Min(1) @Max(50) limit`)
- [ ] `sort`/`sortBy`/`key` binding: enum lạ → 400 `MALFORMED_REQUEST` (không 500)
- [ ] Springdoc: `@Operation`, `@ApiResponse` (mã lỗi trong contract), `@SecurityRequirement` — để FE sinh client bằng `openapi-typescript`
- [ ] `@CurrentUser` argument resolver
- **Edge case**: `GET /api/shares/{code}` code sai định dạng → 404; `GET /api/songs/{slug}` slug là UUID? → vẫn 404; `limit=0/1000` → 400; body > 1MB → 413 (`server.tomcat.max-swallow-size`, `spring.servlet.multipart` off, giới hạn `content` 50k ký tự ≈ ≤200KB UTF-8 → đặt `server.tomcat.max-http-form-post-size` & filter giới hạn 1MB)

## Step 5 – Global Exception Handling & Security / Permissions

- [ ] `GlobalExceptionHandler` đầy đủ bảng mục 12 (`03-Class-Design.md`), mọi lỗi cùng `ErrorResponse`
- [ ] `SecurityConfig`: rule public/authenticated/admin; `AuthenticationEntryPoint` & `AccessDeniedHandler` trả `ErrorResponse` JSON (không HTML mặc định)
- [ ] `OptionalBearerTokenResolver`: route public + token hỏng → guest
- [ ] `RateLimitFilter` (Bucket4j): key theo IP (auth) / userId (comment, like, share); trả 429 + `Retry-After`
- [ ] Kiểm tra `status=BLOCKED` khi refresh/login
- [ ] Security headers (`X-Content-Type-Options`, `Referrer-Policy`), HSTS ở proxy
- [ ] Dọn dẹp scheduled jobs (`RefreshToken`, `Idempotency`, soft-deleted songs)
- [ ] Actuator `health` public, còn lại bảo vệ; log không chứa token/password

## Step 6 – Tích hợp FE (sau khi BE xong từng module)

- [ ] Xuất OpenAPI → FE sinh `types/api.ts` (hoặc copy từ contract mục 9), kiểm khớp
- [ ] FE thêm `lib/api-client.ts` (base URL, Bearer, refresh-on-401 một lần, map `ApiError`)
- [ ] NextAuth Credentials/Google gọi `/api/auth/*`
- [ ] Contract test (Schemathesis hoặc Postman collection) chạy trên staging
- [ ] Cập nhật checkbox trong `docs/features/*.md` phần Development Checklist

---

## Definition of Done cho mỗi module

- [ ] Migration chạy sạch trên DB rỗng và DB đã có dữ liệu (test upgrade)
- [ ] Tất cả endpoint của module khớp `01-API-Contract.md` (status + JSON shape)
- [ ] Unit + integration test xanh, coverage service ≥ 80%
- [ ] Không có N+1 (bật `hibernate.generate_statistics` trong test, assert số query)
- [ ] OpenAPI hiển thị đủ; không log dữ liệu nhạy cảm
