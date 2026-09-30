# 03 – Architecture & Class Design (Java)

> Chỉ signature/skeleton. Body method để trống ở giai đoạn plan.

## 1. Maven & cấu hình

`pom.xml` dependencies chính: `spring-boot-starter-web`, `-validation`, `-data-jpa`, `-security`, `-oauth2-resource-server`, `-actuator`, `-mail`, `postgresql`, `flyway-core` + `flyway-database-postgresql`, `mapstruct` (+ processor), `lombok` (+ `lombok-mapstruct-binding`), `springdoc-openapi-starter-webmvc-ui`, `bucket4j`, `shedlock`; test: `spring-boot-starter-test`, `spring-security-test`, `testcontainers` (`postgresql`, `junit-jupiter`).

`application.yml` khoá chính:

```yaml
spring:
  jpa: { hibernate.ddl-auto: validate, open-in-view: false, properties.hibernate.jdbc.batch_size: 50 }
  flyway: { enabled: true }
app:
  public-web-url: ${PUBLIC_WEB_URL}
  cors.allowed-origins: ${CORS_ORIGINS}
  jwt: { issuer: toneshift, access-ttl: 15m, refresh-ttl: 30d, secret-or-key: ${JWT_KEY} }
  google.client-id: ${GOOGLE_CLIENT_ID}
```

`open-in-view: false` bắt buộc: chống lazy-load ngầm trong controller/serializer.

## 2. Package structure

```
vn.toneshift.api
├── ToneShiftApplication
├── config/            SecurityConfig, JwtConfig, CorsConfig, JpaAuditingConfig, OpenApiConfig, SchedulingConfig, JacksonConfig
├── common/
│   ├── dto/           PageResponse<T>, CursorResponse<T>, ErrorResponse, UserRef
│   ├── exception/     ApiException(+ subclasses), ErrorCode, GlobalExceptionHandler
│   ├── enums/         MusicalKey, Visibility
│   ├── util/          CursorCodec, Slugger, Base62CodeGenerator, HashUtils
│   └── web/           RateLimitFilter, TraceIdFilter, CurrentUser (argument resolver), IdempotencyService
├── security/          JwtService, JwtAuthConverter, AppUserPrincipal, CustomAccessDeniedHandler, CustomAuthEntryPoint
├── auth/              controller / service / repository / entity / dto / mapper
├── user/              …
├── song/              …
├── community/         rating, comment, report (3 sub-package)
├── setlist/           …
├── share/             …
└── admin/             …
```

Mỗi module con: `controller/ service/ repository/ entity/ dto/ mapper/`. Interface service chỉ tạo khi có >1 implementation hoặc cần mock ranh giới module (`SongService`, `UserService`, `TokenService`); còn lại dùng class trực tiếp `@Service` để tránh boilerplate. (Yêu cầu "Interface & Impl": áp dụng cho các service được module khác gọi.)

## 3. Common

```java
public enum MusicalKey { C, CS("C#"), D, DS("D#"), E, F, FS("F#"), G, GS("G#"), A, AS("A#"), B;
    @JsonValue String label(); @JsonCreator static MusicalKey from(String s); }   // serialize "C#", không "CS"

public record PageResponse<T>(List<T> data, int page, int limit, long total, int totalPages, boolean hasNext) {
    static <T> PageResponse<T> of(Page<T> p); }
public record CursorResponse<T>(List<T> data, String nextCursor, boolean hasNext, long total) {}
public record UserRef(UUID id, String name, String image) {}
public record ErrorResponse(Instant timestamp, int status, String code, String message, String path, String traceId, List<FieldErrorItem> errors) {}

public enum ErrorCode { VALIDATION_FAILED(400), UNAUTHORIZED(401), ..., INTERNAL_ERROR(500);
    HttpStatus status(); String defaultMessage(); }          // 1 nguồn duy nhất cho code ↔ HTTP status
public class ApiException extends RuntimeException { ErrorCode code; }   // NotFoundException, ConflictException, BusinessRuleException(422), ForbiddenException, UnauthorizedException
```

`CursorCodec`: `String encode(Map<String,Object>)`, `Map<String,Object> decode(String)` (base64url JSON, decode lỗi → `MALFORMED_REQUEST`).
`CurrentUser`: `@CurrentUser UUID userId` / `Optional<UUID>` cho endpoint 🔓.

## 4. Security

```java
@Configuration @EnableMethodSecurity
class SecurityConfig {
  @Bean SecurityFilterChain chain(HttpSecurity http);          // stateless, CORS, authorizeHttpRequests (mục Overview §6), resource server jwt()
  @Bean PasswordEncoder passwordEncoder();                     // BCrypt(12)
  @Bean JwtEncoder jwtEncoder(); @Bean JwtDecoder jwtDecoder();
}
@Service class JwtService { String createAccessToken(User u); Duration accessTtl(); }
```

Endpoint 🔓: permitAll trong chain, nhưng bearer token hợp lệ vẫn được decode; token lỗi → `BearerTokenAuthenticationFilter` mặc định trả 401. **Để token lỗi ở route public vẫn coi guest**: cấu hình `AuthenticationEntryPoint` chỉ cho route không nằm trong danh sách public, và đặt `BearerTokenResolver` trả `null` khi request tới public route mà token không parse được (custom `OptionalBearerTokenResolver`). Ghi vào checklist Step 5.

## 5. Module `auth`

```java
// dto (records)
record RegisterRequest(@NotBlank @Size(max=100) String name, @NotBlank @Email @Size(max=254) String email,
                       @NotBlank @Size(min=8,max=72) @Pattern(regexp="^(?=.*[A-Za-z])(?=.*\\d).+$") String password) {}
record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
record GoogleLoginRequest(@NotBlank String idToken) {}
record RefreshRequest(@NotBlank String refreshToken) {}
record ForgotPasswordRequest(@NotBlank @Email String email) {}
record ResetPasswordRequest(@NotBlank String token, @NotBlank @Size(min=8,max=72) String newPassword) {}
record AuthResponse(String accessToken, String tokenType, long expiresIn, String refreshToken, UserResponse user) {}

interface UserRepository extends JpaRepository<User, UUID> { Optional<User> findByEmail(String e); boolean existsByEmail(String e); }
interface AuthProviderRepository extends JpaRepository<AuthProvider, UUID> { Optional<AuthProvider> findByProviderAndProviderAccountId(AuthProviderType p, String id); }
interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
  Optional<RefreshToken> findByTokenHash(String h);
  @Modifying @Query("update RefreshToken t set t.revokedAt=:now where t.familyId=:f and t.revokedAt is null") int revokeFamily(UUID f, Instant now);
  @Modifying @Query("update RefreshToken t set t.revokedAt=:now where t.user.id=:u and t.revokedAt is null") int revokeAllByUser(UUID u, Instant now);
}
interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> { Optional<PasswordResetToken> findByTokenHash(String h); }

class AuthService {                                   // @Transactional trên từng method ghi
  AuthResponse register(RegisterRequest r);           // @Transactional
  AuthResponse login(LoginRequest r);                 // @Transactional (ghi refresh token)
  AuthResponse loginWithGoogle(GoogleLoginRequest r); // @Transactional
  AuthResponse refresh(RefreshRequest r);             // @Transactional(rollbackFor = Exception.class); dùng SELECT ... FOR UPDATE trên token
  void logout(RefreshRequest r);                      // @Transactional
  void forgotPassword(ForgotPasswordRequest r);       // @Transactional; gửi mail sau commit (@TransactionalEventListener AFTER_COMMIT)
  void resetPassword(ResetPasswordRequest r);         // @Transactional
}
interface TokenService { IssuedRefreshToken issue(User u, UUID familyId, String ua); Rotated rotate(String raw); void revokeAll(UUID userId); }
interface GoogleIdTokenVerifier { GoogleIdentity verify(String idToken); }     // wrap Nimbus, mock được trong test
@RestController @RequestMapping("/api/auth") class AuthController { /* 8 endpoint mục 1 contract */ }
```

## 6. Module `user`

```java
record UserResponse(UUID id, String name, String email, String image, Role role, boolean hasPassword, List<AuthProviderType> providers, Instant createdAt) {}
record UpdateProfileRequest(@NotBlank @Size(max=100) String name, @Size(max=500) @Pattern(regexp="^https://.+") String image) {}
record ChangePasswordRequest(String currentPassword, @NotBlank @Size(min=8,max=72) String newPassword) {}
interface UserService { UserResponse getMe(UUID id); UserResponse updateProfile(UUID id, UpdateProfileRequest r); void changePassword(UUID id, ChangePasswordRequest r); }
@RestController @RequestMapping("/api/user") class UserController {}
@Mapper(componentModel = "spring") interface UserMapper { UserResponse toResponse(User u); UserRef toRef(User u); }
```

## 7. Module `song`

```java
record SongRequest(@NotBlank @Size(max=200) String title, @Size(max=200) String artist,
                   @NotBlank @Size(max=50000) String content, @NotNull MusicalKey originalKey, @NotNull MusicalKey targetKey, Long version) {}
record SongResponse(UUID id, String slug, String title, String artist, String content, MusicalKey originalKey, MusicalKey targetKey,
                    Visibility visibility, Instant publishedAt, BigDecimal ratingAvg, int ratingCount, UserRef owner, long version,
                    Instant createdAt, Instant updatedAt, Short myScore) {}
record SongSummary(UUID id, String slug, String title, String artist, String excerpt, MusicalKey originalKey, MusicalKey targetKey,
                   Visibility visibility, BigDecimal ratingAvg, int ratingCount, int commentCount, UserRef owner, Instant publishedAt, Instant updatedAt) {}
record SyncRequest(@NotNull @Size(max=10) List<@Valid SongRequest> items) {}
record SyncResponse(int created, List<SongSummary> items) {}

interface SongRepository extends JpaRepository<Song, UUID>, JpaSpecificationExecutor<Song> {
  Optional<Song> findByIdAndOwnerIdAndDeletedAtIsNull(UUID id, UUID ownerId);
  @Query("select s from Song s join fetch s.owner where s.slug=:slug and s.visibility='PUBLIC' and s.deletedAt is null") Optional<Song> findPublicBySlug(String slug);
  @Query("select s from Song s where s.id=:id and s.visibility='PUBLIC' and s.deletedAt is null") Optional<Song> findPublicById(UUID id);
  boolean existsBySlug(String slug);
  @Modifying @Query(nativeQuery=true, value="update songs set comment_count = comment_count + :d where id = :id") void addCommentCount(UUID id, int d);
}
final class SongSpecs { static Specification<Song> mine(UUID owner); static Specification<Song> publicOnly(); static Specification<Song> titleOrArtistLike(String q);
                        static Specification<Song> targetKey(MusicalKey k); static Specification<Song> notDeleted(); }   // lọc động cho list

interface SongService {
  SongResponse create(UUID userId, SongRequest r);                                   // @Transactional
  PageResponse<SongSummary> listMine(UUID userId, String q, String sort, int page, int limit);   // @Transactional(readOnly=true)
  SongResponse getMine(UUID userId, UUID id);                                        // readOnly
  SongResponse update(UUID userId, UUID id, SongRequest r);                          // @Transactional; kiểm version → VERSION_CONFLICT
  void delete(UUID userId, UUID id);                                                 // @Transactional: set deletedAt, thu hồi share link
  SongResponse publish(UUID userId, UUID id);                                        // @Transactional: sinh slug (retry khi trùng)
  SongResponse unpublish(UUID userId, UUID id);                                      // @Transactional
  SyncResponse sync(UUID userId, String idempotencyKey, SyncRequest r);              // @Transactional(rollbackFor = Exception.class)
  PageResponse<SongSummary> listPublic(String q, MusicalKey key, String sort, int page, int limit);   // readOnly
  SongResponse getPublicBySlug(String slug, Optional<UUID> viewer);                  // readOnly
  Song requirePublic(UUID songId);                                                   // cho community module
  Song requireOwned(UUID userId, UUID songId);                                       // cho share, setlist
}
@RestController class LibraryController {}  // /api/library/**
@RestController class SongController {}     // /api/songs, /api/songs/{slug}
@Mapper(componentModel="spring", uses=UserMapper.class) interface SongMapper { SongResponse toResponse(Song s); SongSummary toSummary(Song s); }
```

`Slugger.slugify(title)`: bỏ dấu tiếng Việt (`Normalizer NFD`, thay `đ→d`), lowercase, `-`; `+ "-" + 6 ký tự base36 ngẫu nhiên`. Trùng → thử lại tối đa 5 lần (unique index là chốt cuối).

## 8. Module `community`

```java
// rating
record RatingRequest(@NotNull @Min(1) @Max(5) Short score) {}
record RatingSummary(BigDecimal average, long count, Map<String,Long> distribution, Short myScore) {}
interface RatingRepository extends JpaRepository<Rating, UUID> {
  @Query(nativeQuery=true, value="insert into ratings(id,user_id,song_id,score) values (gen_random_uuid(),:u,:s,:score) on conflict (user_id,song_id) do update set score=:score, updated_at=now()") void upsert(UUID u, UUID s, short score);
  @Query("select r.score, count(r) from Rating r where r.song.id=:s group by r.score") List<Object[]> distribution(UUID s);
  Optional<Short> findScore(UUID userId, UUID songId);
  int deleteByUserIdAndSongId(UUID u, UUID s);
  @Modifying @Query(nativeQuery=true, value="update songs s set rating_count=x.c, rating_avg=x.a from (select count(*) c, coalesce(round(avg(score),2),0) a from ratings where song_id=:id) x where s.id=:id") void recompute(UUID id);
}
class RatingService {
  RatingSummary summary(UUID songId, Optional<UUID> viewer);     // readOnly
  RatingSummary rate(UUID userId, UUID songId, short score);     // @Transactional: requirePublic, chặn tự chấm, upsert + recompute (khoá dòng song FOR UPDATE để recompute không lệch)
  RatingSummary remove(UUID userId, UUID songId);                // @Transactional
}
@RestController class RatingController {}

// comment
record CommentRequest(@NotNull UUID songId, @NotBlank @Size(max=2000) String content, UUID parentId) {}
record CommentUpdateRequest(@NotBlank @Size(max=2000) String content) {}
record CommentResponse(UUID id, UUID songId, UUID parentId, String content, boolean deleted, UserRef user, int likes, boolean isLiked, int repliesCount, boolean edited, Instant createdAt, Instant updatedAt) {}
record LikeResponse(int likes, boolean isLiked) {}
interface CommentRepository extends JpaRepository<Comment, UUID> {
  @Query(...keyset newest...) List<Comment> findRootNewest(UUID songId, Instant t, UUID id, Pageable p);   // JOIN FETCH user
  @Query(...keyset top...)    List<Comment> findRootTop(UUID songId, int likes, Instant t, UUID id, Pageable p);
  @Query(...replies...)       List<Comment> findReplies(UUID parentId, Instant t, UUID id, Pageable p);
  long countVisibleRoots(UUID songId);   // deleted_at is null or reply_count>0
  @Modifying @Query(native) void addLike(UUID id, int d); void addReply(UUID parentId, int d);
}
interface CommentLikeRepository extends JpaRepository<CommentLike, CommentLikeId> {
  @Query("select l.commentId from CommentLike l where l.userId=:u and l.commentId in :ids") Set<UUID> likedIds(UUID u, Collection<UUID> ids);
  @Query(nativeQuery=true, value="insert into comment_likes(user_id,comment_id) values (:u,:c) on conflict do nothing") int insertIgnore(UUID u, UUID c);   // trả rowCount
  int deleteByUserIdAndCommentId(UUID u, UUID c);
}
class CommentService {
  CursorResponse<CommentResponse> listRoots(UUID songId, String sortBy, String cursor, int limit, Optional<UUID> viewer);   // readOnly
  CursorResponse<CommentResponse> listReplies(UUID commentId, String cursor, int limit, Optional<UUID> viewer);            // readOnly
  CommentResponse create(UUID userId, CommentRequest r);            // @Transactional: requirePublic, parent kiểm depth/cùng song, tăng reply_count & songs.comment_count
  CommentResponse update(UUID userId, UUID id, CommentUpdateRequest r);   // @Transactional
  void delete(UUID actorId, boolean isAdmin, UUID id);              // @Transactional: soft delete, giảm counter, idempotent
  LikeResponse like(UUID userId, UUID id);                          // @Transactional
  LikeResponse unlike(UUID userId, UUID id);                        // @Transactional
}
@RestController class CommentController {}

// report
record ReportRequest(@NotNull ReportReason reason, @Size(max=500) String detail) {}
class ReportService { ReportResponse report(UUID userId, UUID commentId, ReportRequest r); }   // @Transactional; DataIntegrityViolation(uq_report_once) → 409 ALREADY_REPORTED
```

## 9. Module `setlist`

```java
record SetlistRequest(@NotBlank @Size(max=100) String name, @Size(max=500) String description, Long version) {}
record AddSongRequest(@NotNull UUID songId, MusicalKey customKey) {}
record UpdateSetlistSongRequest(MusicalKey customKey) {}
record ReorderRequest(@NotNull @Size(max=200) List<@NotNull UUID> songIds) {}
record SetlistItem(UUID songId, int orderIndex, MusicalKey customKey, boolean available, SongBrief song) {}
record SongBrief(UUID id, String title, String artist, MusicalKey originalKey, MusicalKey targetKey) {}
record SetlistResponse(UUID id, String name, String description, int songCount, List<SetlistItem> items, long version, Instant createdAt, Instant updatedAt) {}
record SetlistSummary(UUID id, String name, String description, int songCount, Instant updatedAt) {}

interface SetlistRepository extends JpaRepository<Setlist, UUID> {
  @Lock(PESSIMISTIC_WRITE) @Query("select s from Setlist s where s.id=:id and s.owner.id=:owner") Optional<Setlist> lockByIdAndOwner(UUID id, UUID owner);
  @EntityGraph(attributePaths={"songs","songs.song","songs.song.owner"}) Optional<Setlist> findDetailByIdAndOwnerId(UUID id, UUID owner);
  Page<SetlistSummary> findSummaries(UUID owner, Pageable p);           // projection, count(songs) bằng subquery
  long countByOwnerId(UUID owner);
}
class SetlistService {
  SetlistResponse create(UUID userId, SetlistRequest r);                       // @Transactional; kiểm giới hạn 100
  PageResponse<SetlistSummary> list(UUID userId, int page, int limit);         // readOnly
  SetlistResponse get(UUID userId, UUID id);                                   // readOnly
  SetlistResponse update(UUID userId, UUID id, SetlistRequest r);              // @Transactional
  void delete(UUID userId, UUID id);                                           // @Transactional
  SetlistResponse addSong(UUID userId, UUID id, AddSongRequest r);             // @Transactional: lockByIdAndOwner; max(order_index)+1
  SetlistResponse updateSong(UUID userId, UUID id, UUID songId, UpdateSetlistSongRequest r);   // @Transactional
  SetlistResponse removeSong(UUID userId, UUID id, UUID songId);               // @Transactional: đánh lại order_index 0..n-1
  SetlistResponse reorder(UUID userId, UUID id, ReorderRequest r);             // @Transactional(rollbackFor = Exception.class): lock; so tập songIds == tập hiện tại
  SongResponseWithKey getSong(UUID userId, UUID id, UUID songId);              // readOnly (Player prefetch)
}
@RestController @RequestMapping("/api/setlists") class SetlistController {}
```

## 10. Module `share`

```java
record CreateShareRequest(@NotNull UUID songId, @Valid ShareSettings settings) {}
record ShareSettings(Boolean allowCopy) {}                                   // mặc định true
record ShareCreatedResponse(String uniqueCode, String url, Instant createdAt) {}
record ShareViewResponse(String uniqueCode, ShareSong song, ShareOwner owner, SharePermissions permissions, ShareSettings settings) {}

interface SharedLinkRepository extends JpaRepository<SharedLink, UUID> {
  @Query("select l from SharedLink l join fetch l.song s join fetch s.owner where l.uniqueCode=:c and l.revokedAt is null and (l.expiresAt is null or l.expiresAt > :now) and s.deletedAt is null")
  Optional<SharedLink> findActiveByCode(String c, Instant now);
  Optional<SharedLink> findBySongIdAndRevokedAtIsNull(UUID songId);
  @Modifying @Query("update SharedLink l set l.revokedAt=:now where l.song.id=:songId and l.revokedAt is null") int revokeBySong(UUID songId, Instant now);
}
class ShareService {
  ShareResult create(UUID userId, CreateShareRequest r);               // @Transactional: requireOwned; nếu đã có link → trả cũ; insert, DataIntegrityViolation(code) → retry ≤5; (song) → đọc lại link do request song song tạo
  ShareViewResponse view(String code, Optional<UUID> viewer);          // @Transactional(readOnly=true)
  void revoke(UUID userId, String code);                               // @Transactional
}
@RestController @RequestMapping("/api/shares") class ShareController {}
```

`Base62CodeGenerator.next()`: 10 ký tự từ `SecureRandom` (loại bỏ modulo bias bằng rejection sampling).

## 11. Module `admin`

```java
class AdminReportService { PageResponse<AdminReportResponse> list(ReportStatus s, int page, int limit); AdminReportResponse resolve(UUID adminId, UUID id, ResolveReportRequest r); }  // @Transactional
class AdminUserService   { void block(UUID adminId, UUID targetId); void unblock(UUID targetId); }   // @Transactional: block → revokeAllByUser
@RestController @RequestMapping("/api/admin") @PreAuthorize("hasRole('ADMIN')") class AdminController {}
```

## 12. Exception handling (`GlobalExceptionHandler`)

| Exception | → HTTP / code |
|---|---|
| `MethodArgumentNotValidException`, `ConstraintViolationException` | 400 `VALIDATION_FAILED` + `errors[]` (field lấy từ `FieldError`, path `items[0].title`) |
| `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException`, `MissingServletRequestParameterException` | 400 `MALFORMED_REQUEST` |
| `NotFoundException` | 404 (code cụ thể) |
| `AuthenticationException` / `BadCredentialsException` | 401 |
| `AccessDeniedException` | 403 `FORBIDDEN` |
| `ConflictException`, `ObjectOptimisticLockingFailureException` | 409 (`VERSION_CONFLICT` cho lock) |
| `DataIntegrityViolationException` | map theo tên constraint: `uq_users_email`→409 `EMAIL_ALREADY_EXISTS`, `uq_report_once`→409 `ALREADY_REPORTED`; còn lại 409 `CONFLICT` (không lộ SQL) |
| `BusinessRuleException` | 422 |
| `RateLimitExceededException` | 429 + `Retry-After` |
| `Exception` (fallback) | 500 `INTERNAL_ERROR`, log `ERROR` kèm `traceId`, **không** trả stacktrace |

## 13. Data flow tóm tắt (ví dụ: gửi comment)

```
POST /api/comments {songId, content, parentId}
 → JwtAuth (userId) → RateLimitFilter (20/phút/user)
 → CommentController: @Valid CommentRequest
 → CommentService.create [@Transactional]
     SongService.requirePublic(songId)            → 404/422
     parent = commentRepo.findById(parentId)      → kiểm cùng song, parent.parent == null, chưa xoá
     commentRepo.save(new Comment)
     commentRepo.addReply(parentId,+1); songRepo.addCommentCount(songId,+1)   (native atomic)
 → CommentMapper.toResponse (user = current user, likes=0, isLiked=false)
 ← 201 CommentResponse
```
