# ToneShift

Công cụ chuyển tone cảm âm bài hát trực tuyến cho nhạc công Việt Nam.

## Cấu trúc repo

| Thư mục | Nội dung |
|---|---|
| [`frontend/`](./frontend) | Next.js 16 (App Router), Tailwind CSS v4, Zustand |
| [`backend/`](./backend) | Java 21 + Spring Boot 3 (Maven), đang ở giai đoạn thiết kế |
| [`docs/`](./docs) | Tài liệu feature, thiết kế, audit FE và plan backend |

## Chạy frontend

```bash
cd frontend
npm install
npm run dev
```

Mở http://localhost:3000.

## Backend

Xem [`backend/README.md`](./backend/README.md) và [`docs/backend/`](./docs/backend/00-Backend-Overview.md).
