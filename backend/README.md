# ToneShift Backend

Backend Java (Spring Boot) cho ToneShift. Thư mục này chưa có source code. Kế hoạch triển khai nằm ở [`../docs/backend/`](../docs/backend/00-Backend-Overview.md).

## Tech stack dự kiến

- Java 21, Spring Boot 3, Maven
- PostgreSQL 16 + Flyway
- Spring Security (JWT), Spring Data JPA, MapStruct

## Cấu trúc dự kiến

```
backend/
├── pom.xml
├── docker-compose.yml            # PostgreSQL cho môi trường dev
└── src/
    ├── main/
    │   ├── java/vn/toneshift/api/  # config, common, security, auth, user, song, community, setlist, share, admin
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/        # Flyway V1__... V6__...
    └── test/java/vn/toneshift/api/
```

Chi tiết từng module: [`03-Class-Design.md`](../docs/backend/03-Class-Design.md). Thứ tự triển khai: [`04-Implementation-Checklist.md`](../docs/backend/04-Implementation-Checklist.md).
