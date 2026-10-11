# ToneShift Backend

Backend Java (Spring Boot) cho ToneShift. Kế hoạch triển khai đầy đủ nằm ở [`../docs/backend/`](../docs/backend/00-Backend-Overview.md).

**Trạng thái:** Step 0 – Foundation ([`04-Implementation-Checklist.md`](../docs/backend/04-Implementation-Checklist.md)) đã xong: cấu hình chung, exception handling, DTO dùng chung, `TraceIdFilter`, Testcontainers base test. Các module nghiệp vụ (auth, song, community, setlist, share, admin) chưa có code — làm theo thứ tự ở checklist.

## Tech stack

- Java 21 (chạy test bằng `mvnw` nên không cần cài Maven), Spring Boot 3.3.4
- PostgreSQL 16 + Flyway (chưa có migration nào — thêm từ Step 1)
- Spring Security (JWT), MapStruct, Lombok, bucket4j, shedlock — **chưa thêm vào `pom.xml`**, sẽ thêm khi đến module thực sự dùng (Step 3/5) để tránh dependency không ai dùng

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
