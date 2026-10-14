# ToneShift Backend

Backend Java (Spring Boot) cho ToneShift. Kế hoạch triển khai đầy đủ nằm ở [`../docs/backend/`](../docs/backend/00-Backend-Overview.md).

**Trạng thái:** Foundation + module **Auth/User** đã xong ([`04-Implementation-Checklist.md`](../docs/backend/04-Implementation-Checklist.md)): register/login/logout, đăng nhập OIDC đa provider (Google trước), refresh token rotate, profile/change-password, forgot/reset password + mail, rate limit, dọn token hết hạn. Các module còn lại (song, community, setlist, share, admin) chưa có code — làm theo thứ tự ở checklist.

## Tech stack

- Java 21 (chạy test bằng `mvnw` nên không cần cài Maven), Spring Boot 3.3.4
- PostgreSQL 16 + Flyway (chưa có migration nào — thêm từ Step 1)
- Lombok + MapStruct đã có trong `pom.xml`; Spring Security (JWT), bucket4j, shedlock sẽ thêm khi đến module thực sự dùng

Hai version bị ghim trong `pom.xml` vì môi trường (có comment tại chỗ, đừng hạ xuống):

- `lombok.version` = 1.18.46 — bản Boot quản (1.18.34) không parse được JDK 26.
- `testcontainers.version` = 1.21.4 + ghim `docker-java` 3.6.0 — Docker Engine 29 từ chối API version < 1.44, docker-java mặc định của Testcontainers vẫn xin bản cũ hơn nên test báo `Could not find a valid Docker environment` (400).

## Quy ước code

- **Lombok**: `@Getter @Setter @NoArgsConstructor(access = PROTECTED)` cho `@Entity`, `@RequiredArgsConstructor` cho service/controller. **Không dùng `@Slf4j`** — xem logging bên dưới.
- **Logging**: mọi class log qua `common/logging/AppLog` (`AppLog.get(MyClass.class)`), không tự gọi `LoggerFactory`. Giá trị nhạy cảm (email, token) phải đi qua `common/logging/LogMasker`; **không bao giờ log password hay token thô**.
- Log ghi ra 3 file trong `logs/` (cấu hình ở `src/main/resources/logback-spring.xml`, đổi vị trí bằng `logging.file.path`):

  | File | Nội dung |
  |---|---|
  | `logs/app.log` | Toàn bộ log ứng dụng |
  | `logs/sql.log` | Câu SQL + tham số (`org.hibernate.SQL`, `org.hibernate.orm.jdbc.bind`, và `AppLog.sql(...)`) |
  | `logs/error.log` | Chỉ level ERROR |

  Vì SQL đi qua logger `org.hibernate.SQL` để vào đúng `sql.log`, **không bật `spring.jpa.show-sql`** (nó in qua `System.out`, không qua logback).

## Cấu trúc hiện tại

```
backend/
├── pom.xml
├── docker-compose.yml            # PostgreSQL cho môi trường dev
├── .env.example                  # copy thành .env (hoặc export tay) trước khi chạy
├── mvnw / mvnw.cmd / .mvn/       # Maven Wrapper — không cần cài Maven global
└── src/
    ├── main/
    │   ├── java/vn/toneshift/api/
    │   │   ├── ToneShiftApplication.java
    │   │   ├── config/           # JpaAuditingConfig, JacksonConfig, OpenApiConfig, CorsConfig
    │   │   └── common/
    │   │       ├── dto/          # ErrorResponse, FieldErrorItem, PageResponse, CursorResponse, UserRef
    │   │       ├── exception/    # ErrorCode, ApiException(+subclasses), GlobalExceptionHandler
    │   │       ├── util/         # CursorCodec
    │   │       └── web/          # TraceIdFilter
    │   └── resources/
    │       ├── application.yml, application-dev.yml, application-test.yml
    │       └── db/migration/     # trống, Flyway V1__... thêm từ Step 1
    └── test/java/vn/toneshift/api/
        ├── ToneShiftApplicationTests.java
        └── support/AbstractIntegrationTest.java   # Testcontainers PostgreSQL 16
```

Chi tiết từng module: [`03-Class-Design.md`](../docs/backend/03-Class-Design.md). Thứ tự triển khai: [`04-Implementation-Checklist.md`](../docs/backend/04-Implementation-Checklist.md).

## Chạy local

```bash
cp .env.example .env     # BẮT BUỘC điền DB_PASSWORD (không có giá trị mặc định, không commit .env)
docker compose up -d     # PostgreSQL 16 cho dev
export $(grep -v '^#' .env | xargs)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Test

Integration test (`AbstractIntegrationTest` và các lớp con) dùng **Testcontainers**, cần **Docker đang chạy**:

```bash
./mvnw test
```

Chỉ compile (không cần Docker):

```bash
./mvnw -DskipTests test-compile
```
