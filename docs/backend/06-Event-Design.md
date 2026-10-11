# 06 – Event Design (In-Process Domain Events)

> Bổ sung cho kiến trúc layered ở [`00-Backend-Overview.md`](./00-Backend-Overview.md). Không đổi kiến trúc tổng (`controller → service → repository`), không thêm message broker (Kafka/RabbitMQ) ở MVP. Mục tiêu: dùng **domain event trong-process** (Spring `ApplicationEventPublisher`) để tách side-effect ra khỏi service chính, giữ invariant nghiệp vụ đúng, và chuẩn bị sẵn "điểm nối" nếu sau này cần nâng cấp lên event thật (outbox + broker) mà không phải thiết kế lại từ đầu.
>
> Chỉ gồm plan, catalog event và skeleton — chưa viết body method (giữ đúng tinh thần `03-Class-Design.md`).

## 1. Vì sao cần event design (và vì sao không cần broker ngay)

Nhìn lại `03-Class-Design.md`/`04-Implementation-Checklist.md`, nhiều service đang **gọi trực tiếp chéo module** để cập nhật side-effect:

- `CommentService.create` gọi `commentRepo.addReply(+1)` **và** `songRepo.addCommentCount(+1)` trong cùng method.
- `SongService.delete` gọi sang `ShareService`/`SharedLinkRepository` để `revokeBySong`.
- `AdminUserService.block` gọi `TokenService.revokeAllByUser`.
- `AuthService.forgotPassword` đã có ý định "gửi mail sau commit" nhưng chưa có cơ chế chung.

Vấn đề: service gốc phải **biết và import** mọi module tiêu thụ side-effect của nó → coupling tăng dần khi thêm feature (ví dụ sau này thêm "audit log", "notification", "search index" cho comment thì `CommentService` phải sửa thêm lần nữa). Event design giải quyết bằng cách **lật ngược hướng phụ thuộc**: service gốc chỉ "thông báo điều đã xảy ra" (publish event), còn ai quan tâm thì tự đăng ký lắng nghe (listener), không ai biết ai.

**Không cần message broker ở bước này** vì toàn bộ hệ thống vẫn là 1 Spring Boot monolith, 1 transaction DB, không cần durability qua mạng hay multi-service consumer. Dùng `ApplicationEventPublisher` nội bộ là đủ, **miễn phí hạ tầng**, test dễ (không cần Testcontainers Kafka), và transaction semantics map thẳng vào JPA `@Transactional` sẵn có.

## 2. Khi nào publish event, khi nào gọi method bình thường

Không phải mọi thay đổi state đều cần thành event — tránh "event-itis" (over-engineering). Quy tắc quyết định:

| Câu hỏi | Nếu YES |
|---|---|
| Side-effect thuộc **module khác** với nơi phát sinh hành động? | Có thể dùng event để không phải import chéo module |
| Có **>1 listener độc lập** cùng phản ứng với 1 hành động? | Nên dùng event (fan-out tự nhiên, thêm listener mới không sửa service gốc) |
| Side-effect chỉ là **gọi tiếp 1 method khác trong cùng service/module**, 1:1? | **Không** cần event — gọi trực tiếp (ví dụ `RatingService.rate` tự gọi `ratingRepo.recompute` nội bộ, không publish event) |
| Caller **cần giá trị trả về ngay** để build response? | **Không** dùng event (event là "bắn và quên", không có kênh trả kết quả về) |

Áp dụng quy tắc này, **không** đưa vào catalog: CRUD nội bộ của `setlist` (không ai ngoài module quan tâm), `RatingService` recompute (1:1 nội bộ). **Có** đưa vào catalog: các case ở mục 1 (comment counter fan-out 2 module, song delete → share revoke, admin block → token revoke, mail sau commit).

## 3. Cơ chế kỹ thuật & quy ước

### 3.1 Package

```
common/event/
  DomainEvent                 // marker interface: Instant occurredAt();
  EventPublisher               // thin wrapper quanh ApplicationEventPublisher (dễ mock trong unit test service)
config/
  AsyncEventConfig             // @EnableAsync, bean "eventTaskExecutor"
<module>/event/
  XxxEvent (record implements DomainEvent)   // định nghĩa bởi module PHÁT sinh event
<module>/listener/
  XxxListener (@Component)                   // định nghĩa bởi module LẮNG NGHE (được phép import event package của module khác, coi như import 1 DTO)
```

```java
public interface DomainEvent { Instant occurredAt(); }

@Component
public class EventPublisher {                         // wrapper để service unit test mock 1 interface duy nhất
  private final ApplicationEventPublisher delegate;
  public void publish(DomainEvent event);
}
```

Event record **chỉ chứa id/giá trị nguyên thuỷ**, không chứa entity (`Song`, `Comment`…) — tránh `LazyInitializationException` khi listener chạy sau commit/ở thread khác, lúc Hibernate session đã đóng (D19).

### 3.2 Hai loại event theo transaction phase

| Loại | Khi publish | Khi listener chạy | Dùng cho |
|---|---|---|---|
| **A – Same-transaction** (`@EventListener`, mặc định đồng bộ) | Trong method `@Transactional` của service gốc | **Ngay lập tức**, cùng thread, cùng transaction, **trước khi** service gốc return | Invariant nghiệp vụ bắt buộc đúng (counter, revoke token, revoke share link) — nếu listener lỗi, toàn bộ transaction **phải** rollback giống như gọi trực tiếp |
| **B – Post-commit** (`@TransactionalEventListener(phase = AFTER_COMMIT)`, thường kèm `@Async`) | Trong method `@Transactional` của service gốc | **Sau khi commit thành công**, có thể ở thread khác (nếu `@Async`) | Side-effect ngoài transaction, không ảnh hưởng tính đúng của response (gửi mail, audit log, tương lai: search index, webhook) |

Quy tắc chọn: nếu response HTTP hoặc 1 invariant DB phụ thuộc vào kết quả của listener → **Loại A**. Nếu không, hoặc là I/O ra ngoài (mail/HTTP) → **Loại B**.

### 3.3 Async executor cho Loại B

```java
@Configuration @EnableAsync
class AsyncEventConfig {
  @Bean("eventTaskExecutor")
  Executor eventTaskExecutor() {
    // core=2, max=8, queue=500, rejection=CallerRunsPolicy (backpressure thay vì mất event)
    // taskDecorator = MdcTaskDecorator (copy traceId từ thread request sang thread async để log vẫn có traceId)
  }
  @Bean AsyncUncaughtExceptionHandler asyncUncaughtExceptionHandler();  // log ERROR kèm event payload + traceId, KHÔNG rethrow (không có gì để rollback nữa)
}
```

### 3.4 Publish, không trả dữ liệu qua event

```java
@Service
class CommentService {
  private final EventPublisher events;
  @Transactional
  CommentResponse create(UUID userId, CommentRequest r) {
    // ... validate, save comment (như hiện tại) ...
    events.publish(new CommentCreatedEvent(comment.getId(), r.songId(), userId, r.parentId(), Instant.now()));
    return mapper.toResponse(comment, userId);   // response build từ dữ liệu đã có, KHÔNG chờ event trả gì
  }
}
```

### 3.5 Lỗi & thứ tự (ordering)

- Listener Loại A throw exception → lan lên như gọi method thường → transaction rollback → `GlobalExceptionHandler` xử lý như bình thường (không có thay đổi so với hiện tại).
- Listener Loại B throw exception → không rollback được (đã commit) → bắt bởi `AsyncUncaughtExceptionHandler`, log `ERROR` + `traceId` + payload, **không retry ở MVP** (xem mục 7 — backlog outbox nếu cần đảm bảo gửi).
- Nhiều listener cùng 1 event mà **thứ tự quan trọng** → khai báo rõ `@Order(n)`; nếu không khai báo, coi là **không được phép phụ thuộc thứ tự** (mặc định Spring không đảm bảo).
- Không cho phép listener của event A publish tiếp event B tạo thành chuỗi > 1 cấp, trừ khi ghi rõ lý do trong bảng catalog (mục 4) kèm sơ đồ — tránh event chain khó trace.

## 4. Event Catalog

Ký hiệu Phase: **A** = same-transaction · **B** = post-commit (async trừ khi ghi chú khác).

### 4.1 Module `auth` / `user` / `admin`

| Event | Publish bởi | Payload | Phase | Listener (module.Class) | Side effect | Ghi chú |
|---|---|---|---|---|---|---|
| `UserRegisteredEvent` | `AuthService.register` | `userId, email, name, provider, occurredAt` | B | `notification.WelcomeMailListener` | Gửi mail chào mừng (MVP: log/no-op, bật thật khi có template) | Không publish lại khi link Google vào user có sẵn (không phải "registered") |
| `PasswordResetRequestedEvent` | `AuthService.forgotPassword` | `userId, email, rawToken, expiresAt` | B | `auth.PasswordResetMailListener` | Gửi mail reset password | **Không** log `rawToken`; thay thế cơ chế "gửi mail sau commit" đang mô tả rời rạc ở `03-Class-Design.md` §5 bằng event chuẩn hoá |
| `PasswordChangedEvent` | `UserService.changePassword`, `AuthService.resetPassword` | `userId, occurredAt` | A | `auth.RevokeOtherSessionsListener` | `TokenService.revokeAllByUser` (trừ session hiện tại nếu cần giữ) | Ví dụ event **nội bộ cùng module** — vẫn đáng tách để `UserService` không cần biết `TokenService` tồn tại |
| `UserBlockedEvent` | `AdminUserService.block` | `targetUserId, adminId, occurredAt` | A | `auth.RevokeAllSessionsListener` | `TokenService.revokeAllByUser(targetUserId)` | Bắt buộc same-tx: nếu revoke thất bại, block phải rollback (không thể block mà token cũ vẫn sống) |
| `UserBlockedEvent` (cùng event, listener thứ 2) | — | — | B | `admin.AuditLogListener` | Ghi audit log "admin X blocked user Y" | 1 event, 2 listener ở 2 phase khác nhau — minh hoạ fan-out |
| `UserUnblockedEvent` | `AdminUserService.unblock` | `targetUserId, adminId, occurredAt` | B | `admin.AuditLogListener` | Ghi audit log | Không cần listener Loại A (không có invariant bắt buộc) |
| `RefreshTokenReuseDetectedEvent` | `TokenService.rotate` (khi phát hiện reuse) | `userId, familyId, occurredAt` | B | `security.SecurityAlertListener` | Log `WARN` kèm `traceId`; tương lai: mail cảnh báo "phát hiện đăng nhập lạ" | Không chặn response 401 hiện tại (vẫn trả ngay), event chỉ để cảnh báo phía sau |

### 4.2 Module `song` / `community`

| Event | Publish bởi | Payload | Phase | Listener | Side effect | Ghi chú |
|---|---|---|---|---|---|---|
| `SongDeletedEvent` | `SongService.delete` | `songId, ownerId, occurredAt` | A | `share.RevokeShareLinkListener` | `SharedLinkRepository`/`ShareService.revokeBySong(songId)` | Thay thế lời gọi trực tiếp `SongService → ShareService` hiện tại; `SongService` không còn phải import module `share` |
| `SongPublishedEvent` | `SongService.publish` | `songId, ownerId, slug, occurredAt` | B | `admin.AuditLogListener` (MVP) | Log; chỗ nối sẵn cho search-index/sitemap sau này | |
| `SongUnpublishedEvent` | `SongService.unpublish` | `songId, ownerId, occurredAt` | B | `admin.AuditLogListener` | Log | |
| `CommentCreatedEvent` | `CommentService.create` | `commentId, songId, authorId, parentId\|null, occurredAt` | A | `song.SongCommentCounterListener` | `songRepo.addCommentCount(songId, +1)` | |
| `CommentCreatedEvent` (listener 2, chỉ khi `parentId != null`) | — | — | A | `community.ParentReplyCounterListener` | `commentRepo.addReply(parentId, +1)` | Fan-out 2 module cho đúng 1 hành động — ví dụ rõ nhất của tài liệu này so với code hiện tại (xem so sánh mục 5) |
| `CommentDeletedEvent` | `CommentService.delete` | `commentId, songId, parentId\|null, occurredAt` | A | `song.SongCommentCounterListener`, `community.ParentReplyCounterListener` (nếu có `parentId`) | `addCommentCount(-1)`, `addReply(parentId,-1)` | Service gốc **chỉ** publish khi `UPDATE … WHERE deleted_at IS NULL` thực sự đổi 1 dòng (giữ đúng yêu cầu idempotent delete ở `04-Implementation-Checklist.md` §Step3) — tức là check rowCount **trước khi** publish, không phải trong listener |
| `CommentLikedEvent` | `CommentService.like` | `commentId, userId, occurredAt` | A | `community.CommentLikeCounterListener` | `commentRepo.addLike(commentId, +1)` | Chỉ publish khi `insertIgnore` rowCount = 1 (giữ idempotent) |
| `CommentUnlikedEvent` | `CommentService.unlike` | `commentId, userId, occurredAt` | A | `community.CommentLikeCounterListener` | `addLike(commentId, -1)` | Chỉ publish khi `deleteByUserIdAndCommentId` rowCount = 1 |
| `ReportCreatedEvent` | `ReportService.report` | `reportId, commentId, reporterId, reason, occurredAt` | B | `admin.ReportNotificationListener` | MVP: log; tương lai: mail/Slack cho admin khi `reason=OFFENSIVE` dồn nhiều report | |

### 4.3 Module `share`

| Event | Publish bởi | Payload | Phase | Listener | Side effect | Ghi chú |
|---|---|---|---|---|---|---|
| `ShareLinkCreatedEvent` | `ShareService.create` | `shareId, songId, ownerId, uniqueCode, occurredAt` | B | `admin.AuditLogListener` | Log | Không publish khi trả lại link cũ (idempotent create) — chỉ publish khi **thực sự insert mới** |
| `ShareLinkRevokedEvent` | `ShareService.revoke` (chủ động) | `shareId, songId, ownerId, occurredAt` | B | `admin.AuditLogListener` | Log | Revoke do `SongDeletedEvent` listener gọi (mục 4.2) thì **không** publish lại event này — tránh event chain (đúng quy tắc mục 3.5) |

## 5. So sánh trước/sau: `CommentService.create`

**Trước** (như mô tả ở `03-Class-Design.md` §13 — service gốc biết hết mọi side-effect):

```
CommentService.create
  → commentRepo.save(new Comment)
  → commentRepo.addReply(parentId, +1)        // nếu là reply
  → songRepo.addCommentCount(songId, +1)      // phải biết schema bảng songs
```

**Sau** (event design — service gốc chỉ khai báo "đã xảy ra gì"):

```
CommentService.create
  → commentRepo.save(new Comment)
  → events.publish(CommentCreatedEvent)
      ⇒ (cùng transaction, đồng bộ)
        song.SongCommentCounterListener.onCommentCreated       → songRepo.addCommentCount(+1)
        community.ParentReplyCounterListener.onCommentCreated  → commentRepo.addReply(+1)  [nếu parentId != null]
  → return mapper.toResponse(...)
```

Kết quả HTTP response **giống hệt cũ** (vẫn 201, vẫn counter đúng ngay trong response tiếp theo vì Phase A chạy xong trước khi transaction commit). Khác biệt chỉ ở chỗ `CommentService` không còn `import` `SongRepository`; thêm 1 listener mới (ví dụ sau này "tăng điểm hoạt động user") không phải sửa `CommentService`.

## 6. Quyết định thiết kế (tiếp nối D1–D15 ở `00-Backend-Overview.md`)

| # | Vấn đề | Quyết định | Lý do |
|---|---|---|---|
| D16 | Cơ chế event | Spring `ApplicationEventPublisher` trong-process, không message broker ở MVP | Monolith 1 transaction DB, không cần durability qua mạng; migrate sau không đổi event catalog (mục 7) |
| D17 | Transaction phase | Chia 2 loại: Same-tx (A) cho invariant bắt buộc, Post-commit (B) cho side-effect ngoài | Tránh rollback sai (B lỗi không được làm hỏng transaction đã xong) và tránh side-effect ngoài chạy dù A rollback |
| D18 | Lỗi ở listener Loại B | Log kèm `traceId`, **không retry** ở MVP | Đơn giản; chấp nhận rủi ro mất 1 mail hiếm; backlog: outbox pattern (mục 7) nếu cần đảm bảo gửi |
| D19 | Payload event | Chỉ id/giá trị nguyên thuỷ, không chứa entity | Entity có thể bị detached/lazy sau commit hoặc ở thread khác → `LazyInitializationException` |
| D20 | Phạm vi áp dụng | Chỉ publish event khi có fan-out thật (>1 listener hoặc cross-module) | Tránh "event-itis"; CRUD nội bộ 1:1 (setlist, rating recompute) giữ gọi trực tiếp |
| D21 | Idempotency của event | Service gốc tự đảm bảo chỉ publish khi state **thực sự** đổi (check rowCount trước khi publish), không đẩy việc dedupe xuống listener | Listener counter dùng `UPDATE x = x + 1`, nếu publish 2 lần cho 1 hành động thì counter sai gấp đôi — nhất quán với yêu cầu idempotent delete/like đã có ở `04-Implementation-Checklist.md` |

## 7. Lộ trình nâng cấp lên event thật (khi cần, không làm ngay)

Nếu sau này cần (a) nhiều instance backend cùng chạy và cần đảm bảo delivery, hoặc (b) tách 1 module thành service riêng (ví dụ `notification` tách khỏi monolith):

1. Thêm bảng `outbox_events (id, event_type, payload jsonb, created_at, processed_at)`, insert **cùng transaction** với aggregate thay đổi (thay cho publish in-process trực tiếp ở Loại B).
2. 1 scheduled job (`@Scheduled` + ShedLock, đã có sẵn pattern trong `02-Database-Design.md` §6) đọc `outbox_events` chưa `processed_at`, publish lên Kafka/RabbitMQ, đánh dấu đã xử lý (at-least-once).
3. **Event record (`XxxEvent`) không đổi** — chỉ đổi cách "giao" (transport), vì payload đã thiết kế chỉ chứa id/giá trị nguyên thuỷ (D19) nên serialize JSON thẳng vào `outbox_events.payload` được ngay.
4. Listener Loại A (invariant bắt buộc) **không** chuyển qua outbox — vẫn phải same-tx, đồng bộ, trong-process (đổi sang async sẽ phá invariant).

## 8. Checklist bổ sung cho `04-Implementation-Checklist.md`

Chèn vào cuối **Step 3 – Service Layer** (sau khi từng service đã có business rule, trước khi qua Step 4):

- [ ] `common/event/DomainEvent`, `EventPublisher` (wrapper), `config/AsyncEventConfig` (`eventTaskExecutor`, `MdcTaskDecorator`, `AsyncUncaughtExceptionHandler`)
- [ ] Event record cho từng dòng ở mục 4 (đúng tên field, đúng module sở hữu)
- [ ] Listener Loại A: `SongCommentCounterListener`, `ParentReplyCounterListener`, `CommentLikeCounterListener`, `RevokeShareLinkListener`, `RevokeAllSessionsListener`, `RevokeOtherSessionsListener`
- [ ] Listener Loại B: `PasswordResetMailListener`, `WelcomeMailListener`, `AuditLogListener`, `SecurityAlertListener`, `ReportNotificationListener`
- [ ] Service gốc thay lời gọi chéo module hiện có (`SongService.delete → ShareService`, `CommentService.create/delete → counters`, `AdminUserService.block → TokenService`) bằng `events.publish(...)`
- [ ] Rà lại import: service publish event **không còn** import trực tiếp class của module tiêu thụ (trừ khi vẫn cần gọi đồng bộ cho giá trị trả về — xem mục 2)

## 9. Checklist bổ sung cho `05-Testing-Plan.md`

| Test | Kiểm tra |
|---|---|
| `CommentServiceTest` (unit, mock `EventPublisher`) | `create` reply → publish đúng 1 `CommentCreatedEvent` với `parentId` đúng; không còn mock `SongRepository`/`CommentRepository.addReply` trực tiếp trong test service |
| `SongCommentCounterListenerTest` (unit) | Gọi trực tiếp `onCommentCreated(event)` với event dựng tay → `songRepo.addCommentCount` được gọi đúng tham số, không cần Spring context |
| `@SpringBootTest` + `@RecordApplicationEvents` | `CommentService.create` (reply) qua HTTP → assert `ApplicationEvents` chứa đúng 1 `CommentCreatedEvent`; sau request, `songs.comment_count` và `comments.reply_count` đều +1 (xác nhận listener Loại A chạy xong trước response) |
| `SongDeletedEvent` integration | Xoá bài có share link active → sau request, `shared_links.revoked_at` đã set (không cần gọi `ShareService` trực tiếp trong test `SongService`) |
| Async listener (Loại B) | `AuthService.forgotPassword` → mock mail sender, `verify(..., timeout(1000))` (vì async) thay vì gọi đồng bộ; test riêng `AsyncUncaughtExceptionHandler` log khi listener throw |
| Idempotency | Gọi `CommentService.delete` 2 lần liên tiếp cùng comment → chỉ publish `CommentDeletedEvent` **đúng 1 lần** (lần 2 không publish vì rowCount=0) |
| Không event chain | Review test: `RevokeShareLinkListener` xử lý `SongDeletedEvent` **không** publish `ShareLinkRevokedEvent` (assert `ApplicationEvents` chỉ có 1 event loại `SongDeletedEvent`, không có `ShareLinkRevokedEvent` phát sinh thêm) |

## 10. Anti-pattern cần tránh

- Dùng event để **trả dữ liệu** về cho caller (event publisher không có "return value" — nếu cần giá trị ngay, gọi method trực tiếp, không publish-rồi-chờ).
- Nhồi **entity JPA** vào payload event (D19) — chỉ id + giá trị nguyên thuỷ.
- Publish event cho **mọi** thay đổi state "để sau này tiện" — chỉ publish khi đã có ≥1 listener thật ở mục 4 (không tạo event "mồi" chưa ai dùng).
- Để listener Loại B (async, sau commit) thực hiện việc mà **tính đúng của response phụ thuộc vào nó** — nếu thấy mình cần vậy, đó là dấu hiệu phải chuyển sang Loại A.
- Listener tạo **chuỗi event** (A → publish B → publish C) mà không ghi rõ trong catalog — rất khó debug khi trace ngược "vì sao counter này tăng".
