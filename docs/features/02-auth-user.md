# Feature 02 – Auth & User (Xác thực & Tài khoản) [AUT002]

## 1. Feature Design

### Business Requirement
Xây dựng hệ thống tài khoản để người dùng có thể lưu trữ các bài hát đã chuyển tone vào Thư viện cá nhân (Library). Cho phép đăng nhập nhanh qua Google hoặc tài khoản hệ thống. Phân quyền User thường và Admin.

### User Story
- Là một người dùng, tôi muốn đăng nhập bằng Google bằng 1 cú click chuột để không phải nhớ mật khẩu.
- Là một người dùng, tôi muốn xem profile của mình và danh sách bài hát tôi đã lưu.
- Là admin, tôi muốn quản lý quyền truy cập và kiểm duyệt nội dung cộng đồng.

### Functional Requirement
- Hỗ trợ OAuth2 (Google) và Credentials (Email/Password).
- Middleware bảo vệ các routes riêng tư (`/library`, `/settings`, `/admin`).
- Trang thông tin cá nhân (Profile, Đổi mật khẩu).
- Reset password qua email.

### Non-Functional Requirement
- **Security:** Mật khẩu phải được băm (bcrypt). Token phải an toàn (HTTP-only cookies). Chống CSRF.
- **Performance:** Session data phải nhẹ, không chứa thông tin nhạy cảm.
- **UX:** Màn hình đăng nhập tối giản, không reload trang (dùng NextAuth hooks).

### Edge Cases & Error Cases
- **Edge Case:** Email đã đăng ký qua Credentials, sau đó user lại bấm "Login with Google" với cùng email đó -> Phải tự động link account.
- **Error Case:** Sai mật khẩu, tài khoản bị khóa -> Hiển thị Toast message rõ ràng.

### Permissions
- Guest: Xem trang chủ, chuyển tone.
- User: Lưu bài hát, tạo Setlist, Rating, Comment.
- Admin: Xóa comment, block user.

---

## 2. UI Flow

```text
User 
  ↓ Bấm "Đăng nhập" (Header)
Mở Auth Dialog (hoặc chuyển hướng sang /login)
  ↓ 
Chọn "Login with Google"
  ↓ (OAuth Redirect)
Google Consent Screen
  ↓
Callback `/api/auth/callback/google`
  ↓
Thành công -> Đóng Modal, Header hiện Avatar
Thất bại -> Hiển thị lỗi "Đăng nhập thất bại"
```

---

## 3. Folder Structure

```text
src/
 ├── app/
 │    ├── (auth)/
 │    │    ├── login/page.tsx
 │    │    └── register/page.tsx
 │    └── api/
 │         └── auth/
 │              └── [...nextauth]/route.ts
 ├── components/
 │    └── auth/
 │         ├── AuthProvider.tsx      # Bọc SessionProvider
 │         ├── LoginForm.tsx
 │         ├── RegisterForm.tsx
 │         └── SocialAuth.tsx        # Nút Google/Facebook
 ├── lib/
 │    ├── auth.ts                    # Cấu hình NextAuth options
 │    └── prisma.ts                  # Database client
 ├── schemas/
 │    └── auth.schema.ts             # Zod validation (login, register)
```

---

## 4. API Design

> **Cập nhật (Backend Java/Spring Boot):** Xác thực do **Backend Java** đảm nhiệm, không còn dùng Prisma Adapter. NextAuth v5 chỉ còn làm lớp session phía Next: provider Credentials/Google gọi API BE bên dưới, lưu `accessToken` + `refreshToken` trong cookie HTTP-only đã mã hoá của NextAuth, rồi gọi BE bằng `Authorization: Bearer`. Chi tiết: [`backend/01-API-Contract.md`](../backend/01-API-Contract.md) mục 1–2, 8.

Access token JWT sống 15 phút, refresh token opaque sống 30 ngày và rotate mỗi lần dùng.

| Method | Endpoint | Auth | Mô tả | Status |
|---|---|---|---|---|
| POST | `/api/auth/register` | Public | `{name,email,password}` → `AuthResponse` | 201 / 400 / 409 |
| POST | `/api/auth/login` | Public | `{email,password}` → `AuthResponse` | 200 / 401 / 403 |
| POST | `/api/auth/oauth/google` | Public | `{idToken}`; email trùng → tự link account | 200 / 401 / 403 |
| POST | `/api/auth/refresh` | Public | `{refreshToken}` → cặp token mới | 200 / 401 |
| POST | `/api/auth/logout` | Public | `{refreshToken}` | 204 |
| POST | `/api/auth/forgot-password` | Public | `{email}` (luôn 202, không lộ email tồn tại) | 202 |
| POST | `/api/auth/reset-password` | Public | `{token,newPassword}` | 204 / 422 |
| GET | `/api/user/me` | User | Thông tin user hiện tại | 200 / 401 |
| PUT | `/api/user/profile` | User | `{name,image}` | 200 / 400 |
| POST | `/api/user/change-password` | User | `{currentPassword,newPassword}` | 204 / 401 |
| POST | `/api/admin/users/:id/block` · `/unblock` | Admin | Khoá / mở khoá user | 204 |

Lưu ý: BE lưu hash bcrypt (strength 12); mật khẩu 8–72 ký tự, có chữ và số. Sai email và sai mật khẩu trả cùng một lỗi `INVALID_CREDENTIALS`.

---

## 5. Database Design

> **Cập nhật:** Schema do BE sở hữu (PostgreSQL + Flyway). Không dùng bảng `Session`/`VerificationToken` của NextAuth. DDL đầy đủ: [`backend/02-Database-Design.md`](../backend/02-Database-Design.md).

- **`users`**: `id` (UUID), `email` (citext, unique), `name`, `password_hash` (NULL nếu chỉ đăng nhập Google), `image_url`, `role` (`USER`/`ADMIN`), `status` (`ACTIVE`/`BLOCKED`), `email_verified_at`, `created_at`, `updated_at`.
- **`auth_providers`**: `user_id`, `provider` (`GOOGLE`), `provider_account_id`; unique `(provider, provider_account_id)` – phục vụ link account.
- **`refresh_tokens`**: `user_id`, `token_hash` (SHA-256), `family_id`, `expires_at`, `revoked_at` – phát hiện dùng lại token đã revoke.
- **`password_reset_tokens`**: `user_id`, `token_hash`, `expires_at` (30 phút), `used_at`.

---

## 6. Component Design

**`LoginForm`**
- **Props:** `callbackUrl` (Chuyển hướng sau khi thành công)
- **State:** `email`, `password`, `isLoading`, `error` (quản lý bởi React Hook Form).
- **Responsibility:** Validate client-side bằng Zod, gọi `signIn('credentials', ...)` của NextAuth.

**`SocialAuth`**
- **Responsibility:** Hiển thị nút "Continue with Google", gọi `signIn('google')`.

---

## 7. State Management

- **Global State:** Sử dụng `useSession()` của NextAuth để lấy thông tin user hiện tại trên toàn bộ ứng dụng.
- **Server State:** Dùng `auth()` trong các Server Component/Actions để lấy thông tin an toàn.
- **Local State:** Trạng thái form (loading, error messages) quản lý bằng React Hook Form.

---

## 8. Development Checklist

- [ ] Phase 1: Setup Prisma Schema cho NextAuth.
- [ ] Phase 2: Cấu hình `auth.ts` (Google Provider & Credentials Provider).
- [ ] Phase 3: Viết Middleware bảo vệ các route `/library`.
- [ ] Phase 4: Xây dựng UI Login/Register pages.
- [ ] Phase 5: Xây dựng chức năng Logout & UI Header hiển thị User Menu.

---

## 9. Testing Checklist

- [ ] **Integration Test:** Đăng nhập sai mật khẩu phải trả về lỗi đúng.
- [ ] **Security:** Cố gắng truy cập `/library` khi chưa đăng nhập phải bị redirect về `/login`.
- [ ] **UI Test:** Avatar user phải hiện đúng sau khi Login with Google thành công.

---

## 10. Refactoring

- Đảm bảo việc query Session trên Server Layout không làm chậm thời gian render trang (TTFB). Dùng tính năng Session Caching nếu cần thiết.
- Tách các hàm thao tác DB (Prisma) ra các file Service thay vì viết thẳng vào API Route để tái sử dụng.
