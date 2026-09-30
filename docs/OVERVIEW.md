# ToneShift – Project Overview & Architecture

> Công cụ chuyển đổi cảm âm bài hát trực tuyến, hỗ trợ cá nhân hóa và chia sẻ cộng đồng.

---

## Tech Stack

| Layer | Công nghệ |
|---|---|
| Framework | Next.js 16 (App Router) |
| UI | Tailwind CSS v4 + shadcn/ui |
| Auth | NextAuth.js v5 (chỉ giữ session) + JWT do Backend Java cấp (Email/Password + Google) |
| Backend | Java 21 + Spring Boot 3 (Maven), Spring Security, Spring Data JPA – xem [`backend/00-Backend-Overview.md`](./backend/00-Backend-Overview.md) |
| Database | PostgreSQL 16 + Flyway (do Backend sở hữu, **không** dùng Prisma) |
| State | Zustand (global) + React Context (local) |
| Cookie | `js-cookie` + Next.js middleware |
| Theme | `next-themes` |
| Hosting | Vercel |

---

## Cấu trúc thư mục dự án

```
toneshift-web/
├── app/
│   ├── (auth)/               # Route group: login, register
│   │   ├── login/page.tsx
│   │   └── register/page.tsx
│   ├── (main)/               # Route group: app pages
│   │   ├── converter/page.tsx
│   │   ├── library/page.tsx
│   │   └── community/
│   │       ├── page.tsx
│   │       └── [slug]/page.tsx
│   ├── api/
│   │   ├── auth/[...nextauth]/route.ts
│   │   ├── songs/route.ts
│   │   └── library/route.ts
│   ├── layout.tsx
│   ├── page.tsx              # Landing page
│   └── globals.css
├── components/
│   ├── layout/               # Header, Footer, Sidebar
│   ├── sections/             # Landing page sections
│   ├── converter/            # Converter-specific components
│   ├── community/            # Community-specific components
│   └── ui/                   # shadcn/ui primitives
├── lib/
│   ├── auth.ts               # NextAuth config
│   ├── prisma.ts             # Prisma client
│   ├── transpose.ts          # Core transpose algorithm
│   └── utils.ts
├── hooks/
│   ├── useScrolled.ts
│   ├── useConverterHistory.ts
│   └── useTheme.ts
├── store/
│   └── converterStore.ts     # Zustand store
├── constants/
│   ├── navigation.ts
│   ├── musical-keys.ts       # 12 tones definition
│   └── cookie-keys.ts
├── types/
│   └── index.ts
└── docs/
    ├── OVERVIEW.md
    ├── MVP_ROADMAP.md
    └── features/
        ├── 01-core-converter.md
        ├── 02-auth-user.md
        ├── 03-community.md
        ├── 04-ui-ux.md
        └── 05-cookie-persistence.md
```

---

## MVP Phases

| Phase | Tên | Nội dung |
|---|---|---|
| **Phase 1** | Core | Converter + Cookie persistence |
| **Phase 2** | Auth | Đăng ký / Đăng nhập + My Library |
| **Phase 3** | Community | Danh sách, chi tiết, tìm kiếm |
| **Phase 4** | Polish | Dark/Light mode, responsive, SEO |

---

## Nguyên tắc thiết kế

- **Dark-first**: UI mặc định dark mode (`#09090B` background, `#2563EB` accent)
- **No layout shift**: Header float, nội dung có `pt` bù trừ
- **Cookie before Auth**: Người dùng chưa đăng nhập vẫn có trải nghiệm đầy đủ qua cookie
- **Atomic commits**: Mỗi tính năng = 1 nhánh git riêng

---

## Đọc thêm

- [MVP Roadmap](./MVP_ROADMAP.md)
- [01 – Core Converter [COR001]](./features/01-core-converter.md)
- [02 – Auth & User [AUT002]](./features/02-auth-user.md)
- [03 – Community [COM003]](./features/03-community.md)
- [04 – UI/UX [UIX004]](./features/04-ui-ux.md)
- [05 – Cookie Persistence [COK005]](./features/05-cookie-persistence.md)
- [Backend Technical Plan (Java/Spring Boot)](./backend/00-Backend-Overview.md) → [API Contract](./backend/01-API-Contract.md), [Database](./backend/02-Database-Design.md), [Class Design](./backend/03-Class-Design.md), [Checklist](./backend/04-Implementation-Checklist.md), [Testing](./backend/05-Testing-Plan.md)
