# Kế hoạch triển khai Auth (FE) — Login / Register / Google

## Bối cảnh

Backend đã xong module Auth/User ([`backend/`](../../backend), xem [`docs/backend/01-API-Contract.md`](../backend/01-API-Contract.md) mục 1–2). Frontend hiện **chưa có bất kỳ code auth nào** — không NextAuth, không trang login/register, không API client, không session.

`docs/features/02-auth-user.md` cũ có phác thảo dùng NextAuth v5, nhưng đó là thiết kế viết **trước khi** backend tự phát hành `AuthResponse` riêng (`accessToken` + `refreshToken` + `user`, tự ký JWT, tự rotate refresh token). Giờ backend đã tự lo toàn bộ phần "là ai, phát hành token gì", nên FE **không cần NextAuth** nữa — chỉ cần một lớp mỏng nhận `AuthResponse` từ BE, mã hoá lưu vào cookie HTTP-only, và đọc ra khi cần.

**Quyết định kiến trúc đã chốt với user:**
- **Không dùng NextAuth** — tự viết session layer mỏng (cookie HTTP-only mã hoá AES-256-GCM bằng `node:crypto`, không thêm dependency).
- **Phạm vi đợt này**: chỉ Login + Register + Google (OIDC). Chưa làm profile, forgot/reset password, middleware bảo vệ route (chưa có trang nào cần bảo vệ).
- **Chưa có Google Client ID thật** — làm xong UI/flow, test thật sau khi có `.env.local`.

## Nguyên tắc thiết kế

1. **Next.js server đóng vai trò BFF (backend-for-frontend)**: mọi lời gọi tới backend Spring Boot (`/api/auth/*`) đều chạy từ **Server Action** (chạy trên Node.js server của Next, không phải trình duyệt). Trình duyệt **không bao giờ** thấy `accessToken`/`refreshToken` ở dạng đọc được — chúng nằm trong cookie `httpOnly` đã mã hoá.
2. **Không CORS**: vì Next server gọi BE server-to-server (không phải fetch từ browser), không cần cấu hình CORS gì thêm ở BE cho các action này.
3. Login/Register/Logout/Google-login ở backend đều là endpoint **public 🌐** (không cần `Authorization` header) — nên core scope này **không cần** logic refresh access token. Việc đó để dành cho tính năng sau (khi FE cần gọi API cần đăng nhập, ví dụ library bài hát).
4. Giữ nguyên convention code hiện có: Tailwind v4 + token màu trong `app/globals.css` (`--background`, `--primary`, `--card`, `--border`, `--muted-foreground`...), component `Button` ở `components/ui/Button.tsx` (prop `variant`/`size`/`isLoading`/`href`), toast qua `store/toastStore.ts` + `components/ui/Toaster.tsx`, path alias `@/*`.
5. Dùng **React 19 `useActionState` + `useFormStatus`** (đã có sẵn trong React 19.2, không cần thêm `react-hook-form`/`zod`) để bind form trực tiếp vào Server Action — validate dựa vào response lỗi của BE (BE đã trả message tiếng Việt sẵn, ví dụ `"Email hoặc mật khẩu không đúng"`), không cần viết lại rule validate ở FE (tránh 2 nguồn sự thật lệch nhau).
6. `@base-ui/react` đã là dependency có sẵn (chưa dùng ở đâu) — dùng nó cho menu dropdown avatar ở Navbar thay vì tự viết hoặc thêm thư viện mới.

---

## Bước 0 — Chuẩn bị môi trường

### 0.1. Thêm biến môi trường

Tạo `frontend/.env.local` (không commit, thêm vào `.gitignore` nếu chưa có) và `frontend/.env.example` (commit được, không chứa giá trị thật):

```bash
# frontend/.env.example

# URL gốc của backend Spring Boot, Next server gọi trực tiếp (không qua browser)
API_BASE_URL=http://localhost:8080

# Khoá mã hoá cookie session — base64 của 32 byte ngẫu nhiên.
# Sinh bằng: node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"
SESSION_SECRET=

# Google OAuth Client ID (từ Google Cloud Console > APIs & Services > Credentials)
# Dùng ở client để load nút "Đăng nhập với Google" — PHẢI khớp GOOGLE_CLIENT_ID bên backend (.env của backend)
NEXT_PUBLIC_GOOGLE_CLIENT_ID=
```

Sinh thử 1 giá trị `SESSION_SECRET` hợp lệ để biết format đúng:
```bash
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"
# ví dụ: DpdFlgjY5/hehRIdv0/sSg2PbGoqQ+q1VgL0eq6Ev8o=
```

Dán giá trị sinh được vào `.env.local`.

**Lưu ý:** `NEXT_PUBLIC_GOOGLE_CLIENT_ID` phải là **Client ID** (không phải Client Secret — FE không bao giờ cầm Client Secret) và phải trùng với `GOOGLE_CLIENT_ID` đã cấu hình ở backend (`backend/.env` → biến `GOOGLE_CLIENT_ID`), vì backend verify `aud` của ID token phải khớp client-id đó.

### 0.2. Kiểm tra `.gitignore` của frontend

Xác nhận `frontend/.gitignore` (hoặc `.gitignore` gốc) đã chặn `.env.local`, `.env*.local` — Next.js mặc định generate `.gitignore` có dòng này khi tạo project, kiểm tra lại cho chắc.

---

## Bước 1 — Types dùng chung

**File mới: `frontend/types/auth.ts`**

Định nghĩa type khớp **chính xác** với response backend (xem `docs/backend/01-API-Contract.md` mục 1.6, 2.1 và `backend/src/main/java/vn/toneshift/api/{auth,user}/dto/*.java`):

```ts
export type Role = "USER" | "ADMIN";
export type AuthProviderType = "GOOGLE";

export interface UserResponse {
  id: string;
  name: string;
  email: string;
  image: string | null;
  role: Role;
  hasPassword: boolean;
  providers: AuthProviderType[];
  createdAt: string; // ISO-8601
}

export interface AuthResponse {
  accessToken: string;
  tokenType: "Bearer";
  expiresIn: number; // giây, hiện tại luôn 900
  refreshToken: string;
  user: UserResponse;
}

// Khớp vn.toneshift.api.common.dto.ErrorResponse
export interface FieldErrorItem {
  field: string;
  message: string;
}

export interface ApiErrorBody {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
  traceId: string;
  errors: FieldErrorItem[];
}
```

---

## Bước 2 — API client gọi backend (server-only)

**File mới: `frontend/lib/api-client.ts`**

Một hàm `apiFetch` dùng chung cho mọi lời gọi BE, luôn chạy phía server (Server Action/Server Component), ném `ApiError` có `code` + `message` + `fieldErrors` khi BE trả lỗi.

Việc cần làm:
1. Đọc `API_BASE_URL` từ `process.env.API_BASE_URL`, fallback `http://localhost:8080` để dev thuận tiện.
2. Class `ApiError extends Error`:
   ```ts
   export class ApiError extends Error {
     constructor(
       public readonly status: number,
       public readonly code: string,
       message: string,
       public readonly fieldErrors: FieldErrorItem[] = []
     ) {
       super(message);
       this.name = "ApiError";
     }
   }
   ```
3. Hàm `apiFetch<T>(path: string, init?: RequestInit): Promise<T>`:
   - Luôn set `Content-Type: application/json` (trừ khi method là GET không có body).
   - Luôn set `cache: "no-store"` (dữ liệu auth không được cache).
   - Nếu `res.status === 204` → trả `undefined` (dùng cho `logout`).
   - Parse JSON body (bọc `try/catch` phòng trường hợp BE trả body rỗng kèm lỗi mạng).
   - Nếu `!res.ok` → ném `ApiError(res.status, body.code, body.message, body.errors ?? [])`.
   - Nếu lỗi là lỗi mạng (fetch throw, không phải HTTP status) → bắt lại, ném `ApiError(0, "NETWORK_ERROR", "Không thể kết nối máy chủ, vui lòng thử lại")`.
4. **Đánh dấu file chỉ chạy server**: thêm comment đầu file `// Server-only: không import file này vào Client Component` (không bắt buộc kỹ thuật vì Server Action tự đảm bảo điều đó, nhưng ghi chú để tránh nhầm).

Ví dụ dùng (ở bước 4):
```ts
const auth = await apiFetch<AuthResponse>("/api/auth/login", {
  method: "POST",
  body: JSON.stringify({ email, password }),
});
```

---

## Bước 3 — Session layer (cookie mã hoá)

**File mới: `frontend/lib/session.ts`**

### 3.1. Mã hoá/giải mã (AES-256-GCM, dùng `node:crypto`)

```ts
import { randomBytes, createCipheriv, createDecipheriv } from "node:crypto";

const ALGORITHM = "aes-256-gcm";
const IV_LENGTH = 12; // khuyến nghị cho GCM

interface SessionPayload {
  user: UserResponse;
  accessToken: string;
  accessTokenExpiresAt: number; // epoch ms
  refreshToken: string;
}

function getSessionKey(): Buffer {
  const secret = process.env.SESSION_SECRET;
  if (!secret) {
    throw new Error("Thiếu biến môi trường SESSION_SECRET");
  }
  const key = Buffer.from(secret, "base64");
  if (key.length !== 32) {
    throw new Error("SESSION_SECRET phải là base64 của đúng 32 byte");
  }
  return key;
}

function encryptSession(payload: SessionPayload): string {
  const key = getSessionKey();
  const iv = randomBytes(IV_LENGTH);
  const cipher = createCipheriv(ALGORITHM, key, iv);
  const plaintext = Buffer.from(JSON.stringify(payload), "utf8");
  const encrypted = Buffer.concat([cipher.update(plaintext), cipher.final()]);
  const authTag = cipher.getAuthTag();
  // base64url từng phần, nối bằng dấu chấm — giống cấu trúc JWT cho dễ debug bằng mắt
  return [iv, authTag, encrypted].map((b) => b.toString("base64url")).join(".");
}

function decryptSession(token: string): SessionPayload | null {
  try {
    const [ivB64, tagB64, dataB64] = token.split(".");
    if (!ivB64 || !tagB64 || !dataB64) return null;
    const key = getSessionKey();
    const decipher = createDecipheriv(ALGORITHM, key, Buffer.from(ivB64, "base64url"));
    decipher.setAuthTag(Buffer.from(tagB64, "base64url"));
    const decrypted = Buffer.concat([
      decipher.update(Buffer.from(dataB64, "base64url")),
      decipher.final(),
    ]);
    return JSON.parse(decrypted.toString("utf8")) as SessionPayload;
  } catch {
    // Token hỏng/sai key/hết hạn định dạng → coi như chưa đăng nhập, không throw
    return null;
  }
}
```

**Vì sao AES-256-GCM tự viết thay vì thêm `iron-session`/`jose`:** đây là mã hoá đối xứng cơ bản, Node có sẵn trong `node:crypto`, không cần thêm dependency. GCM tự kèm authentication tag nên chống luôn việc cookie bị sửa tay (tamper) — giải mã sai tag sẽ throw, bắt được ở `catch` và coi như chưa đăng nhập.

### 3.2. Đọc/ghi cookie

Cookie tên `ts_session`, `httpOnly`, `sameSite: "lax"`, `secure` chỉ bật ở production (để dev trên `http://localhost` vẫn hoạt động), `maxAge` = 30 ngày (khớp đúng hạn refresh token ở BE, xem `docs/backend/02-Database-Design.md` — `refresh_tokens.expires_at` = 30 ngày).

```ts
import { cookies } from "next/headers";
import type { AuthResponse } from "@/types/auth";

const COOKIE_NAME = "ts_session";
const COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 30; // 30 ngày

export async function createSession(auth: AuthResponse): Promise<void> {
  const payload: SessionPayload = {
    user: auth.user,
    accessToken: auth.accessToken,
    accessTokenExpiresAt: Date.now() + auth.expiresIn * 1000,
    refreshToken: auth.refreshToken,
  };
  const token = encryptSession(payload);
  const store = await cookies();
  store.set(COOKIE_NAME, token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: COOKIE_MAX_AGE_SECONDS,
  });
}

export async function getSession(): Promise<SessionPayload | null> {
  const store = await cookies();
  const cookie = store.get(COOKIE_NAME);
  if (!cookie) return null;
  return decryptSession(cookie.value);
}

export async function destroySession(): Promise<void> {
  const store = await cookies();
  store.delete(COOKIE_NAME);
}
```

**Lưu ý Next.js 16**: `cookies()` là **async** (trả `Promise`), phải `await` — khác với Next 13/14 cũ. `createSession`/`getSession`/`destroySession` chỉ gọi được từ Server Action, Route Handler, hoặc Server Component — không gọi được từ Client Component.

---

## Bước 4 — Server Actions (nơi thực sự gọi backend)

**File mới: `frontend/app/(auth)/actions.ts`**

```ts
"use server";

import { redirect } from "next/navigation";
import { apiFetch, ApiError } from "@/lib/api-client";
import { createSession, destroySession, getSession } from "@/lib/session";
import type { AuthResponse, FieldErrorItem } from "@/types/auth";

export interface AuthFormState {
  error?: string;
  fieldErrors?: FieldErrorItem[];
}

const INITIAL_STATE: AuthFormState = {};

export async function registerAction(
  _prevState: AuthFormState,
  formData: FormData
): Promise<AuthFormState> {
  const name = String(formData.get("name") ?? "").trim();
  const email = String(formData.get("email") ?? "").trim();
  const password = String(formData.get("password") ?? "");

  try {
    const auth = await apiFetch<AuthResponse>("/api/auth/register", {
      method: "POST",
      body: JSON.stringify({ name, email, password }),
    });
    await createSession(auth);
  } catch (e) {
    return toFormState(e);
  }
  redirect("/"); // hoặc trang đích sau đăng ký, ví dụ /converter
}

export async function loginAction(
  _prevState: AuthFormState,
  formData: FormData
): Promise<AuthFormState> {
  const email = String(formData.get("email") ?? "").trim();
  const password = String(formData.get("password") ?? "");

  try {
    const auth = await apiFetch<AuthResponse>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    });
    await createSession(auth);
  } catch (e) {
    return toFormState(e);
  }
  redirect("/");
}

// Gọi trực tiếp từ GoogleButton (không phải form submit, nên không dùng useActionState ở đây)
export async function googleLoginAction(idToken: string): Promise<AuthFormState> {
  try {
    const auth = await apiFetch<AuthResponse>("/api/auth/oauth/google", {
      method: "POST",
      body: JSON.stringify({ idToken }),
    });
    await createSession(auth);
  } catch (e) {
    return toFormState(e);
  }
  redirect("/");
}

export async function logoutAction(): Promise<void> {
  const session = await getSession();
  if (session) {
    // Endpoint public, idempotent — không quan tâm thành công hay thất bại, vẫn xoá cookie local
    await apiFetch("/api/auth/logout", {
      method: "POST",
      body: JSON.stringify({ refreshToken: session.refreshToken }),
    }).catch(() => {});
  }
  await destroySession();
  redirect("/");
}

// Gộp xử lý lỗi dùng chung cho 3 action phía trên
function toFormState(e: unknown): AuthFormState {
  if (e instanceof ApiError) {
    return { error: e.message, fieldErrors: e.fieldErrors };
  }
  return { error: "Không thể kết nối máy chủ, vui lòng thử lại" };
}
```

**Vì sao `redirect()` nằm ngoài `try/catch`**: `redirect()` của Next.js hoạt động bằng cách throw một lỗi đặc biệt (`NEXT_REDIRECT`) để Next chặn lại và điều hướng. Nếu đặt trong `try` sẽ vô tình bị `catch (e)` nuốt mất, trông như request thất bại dù thực ra đã thành công. **Luôn gọi `redirect()` sau khối `try/catch`, không bao giờ bên trong `try`.**

---

## Bước 5 — Component UI dùng chung

### 5.1. `frontend/components/ui/Input.tsx` (component mới)

Theo đúng convention của `Button.tsx` (forward props, `variant` không cần vì input chỉ có 1 style), dùng token màu có sẵn:

```tsx
import React from "react";

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
}

const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, id, className = "", ...props }, ref) => {
    const inputId = id ?? props.name;
    return (
      <div className="flex flex-col gap-1.5">
        <label htmlFor={inputId} className="text-sm font-medium text-foreground">
          {label}
        </label>
        <input
          ref={ref}
          id={inputId}
          className={`h-11 px-4 rounded-xl bg-card border text-foreground placeholder:text-muted-foreground
            focus:outline-none focus:ring-2 focus:ring-primary/50 transition-colors
            ${error ? "border-red-500/60" : "border-border"} ${className}`}
          aria-invalid={!!error}
          aria-describedby={error ? `${inputId}-error` : undefined}
          {...props}
        />
        {error && (
          <p id={`${inputId}-error`} className="text-sm text-red-400">
            {error}
          </p>
        )}
      </div>
    );
  }
);
Input.displayName = "Input";

export default Input;
```

### 5.2. `frontend/lib/form-errors.ts` (helper nhỏ)

Dùng chung giữa `LoginForm`/`RegisterForm` để lấy lỗi theo field từ `AuthFormState.fieldErrors`:

```ts
import type { FieldErrorItem } from "@/types/auth";

export function fieldError(errors: FieldErrorItem[] | undefined, field: string): string | undefined {
  return errors?.find((e) => e.field === field)?.message;
}
```

---

## Bước 6 — Form Login / Register

### 6.1. `frontend/components/auth/LoginForm.tsx`

Dùng `useActionState` (React 19) để bind thẳng vào `loginAction`, `useFormStatus` để hiện loading trên nút submit (phải nằm trong 1 component con vì `useFormStatus` chỉ đọc được state của `<form>` cha):

```tsx
"use client";

import { useActionState } from "react";
import { useFormStatus } from "react-dom";
import Link from "next/link";
import Button from "@/components/ui/Button";
import Input from "@/components/ui/Input";
import GoogleButton from "@/components/auth/GoogleButton";
import { loginAction, type AuthFormState } from "@/app/(auth)/actions";
import { fieldError } from "@/lib/form-errors";

const INITIAL_STATE: AuthFormState = {};

function SubmitButton() {
  const { pending } = useFormStatus();
  return (
    <Button type="submit" className="w-full" isLoading={pending}>
      Đăng nhập
    </Button>
  );
}

export default function LoginForm() {
  const [state, formAction] = useActionState(loginAction, INITIAL_STATE);

  return (
    <form action={formAction} className="flex flex-col gap-4">
      <Input
        label="Email"
        name="email"
        type="email"
        autoComplete="email"
        required
        error={fieldError(state.fieldErrors, "email")}
      />
      <Input
        label="Mật khẩu"
        name="password"
        type="password"
        autoComplete="current-password"
        required
        error={fieldError(state.fieldErrors, "password")}
      />

      {state.error && <p className="text-sm text-red-400" role="alert">{state.error}</p>}

      <SubmitButton />

      <div className="relative text-center text-sm text-muted-foreground my-2">
        <span className="bg-background px-2 relative z-10">hoặc</span>
        <div className="absolute inset-x-0 top-1/2 h-px bg-border -z-0" />
      </div>

      <GoogleButton />

      <p className="text-sm text-muted-foreground text-center mt-2">
        Chưa có tài khoản?{" "}
        <Link href="/register" className="text-primary hover:underline">
          Đăng ký
        </Link>
      </p>
    </form>
  );
}
```

**Lưu ý quan trọng — tên biến import**: `loginAction` export từ file Server Action `app/(auth)/actions.ts`, import vào Client Component vẫn hợp lệ (Next tự tạo "reference" tới server function, không bundle code server vào client). Đừng export thêm biến non-function/non-`"use server"` nào khác trong cùng file (file `"use server"` chỉ được export async function).

### 6.2. `frontend/components/auth/RegisterForm.tsx`

Giống hệt cấu trúc trên nhưng thêm field `name`, bind vào `registerAction`, và các ràng buộc HTML5 tương ứng với rule backend (chỉ để UX phản hồi nhanh, **không thay thế** validate của BE):

- `name`: `maxLength={100}`, `required`
- `email`: `type="email"`, `required`
- `password`: `minLength={8}`, `maxLength={72}`, `required`, thêm dòng hint nhỏ dưới input: `"Ít nhất 8 ký tự, có cả chữ và số"` (khớp `RegisterRequest.password` pattern `^(?=.*[A-Za-z])(?=.*\d).+$` ở BE)

Link cuối form trỏ `/login` thay vì `/register`.

---

## Bước 7 — Đăng nhập Google (Google Identity Services, không qua NextAuth)

### 7.1. Vì sao cần Google Identity Services (GIS) ở client

Backend chỉ nhận **ID token đã ký sẵn** từ Google (`POST /api/auth/oauth/google` body `{"idToken": "..."}"`) — BE không tự đi lấy token hộ FE. Việc lấy ID token từ người dùng (hiển thị nút "Đăng nhập với Google", mở popup chọn tài khoản, nhận token) phải làm ở trình duyệt bằng SDK chính thức của Google: **Google Identity Services (GIS)**, load qua thẻ `<script src="https://accounts.google.com/gsi/client">`.

### 7.2. `frontend/components/auth/GoogleButton.tsx`

```tsx
"use client";

import { useEffect, useId, useRef, useTransition } from "react";
import Script from "next/script";
import { googleLoginAction } from "@/app/(auth)/actions";
import { toast } from "@/store/toastStore";

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: {
            client_id: string;
            callback: (response: { credential: string }) => void;
          }) => void;
          renderButton: (parent: HTMLElement, options: Record<string, unknown>) => void;
        };
      };
    };
  }
}

export default function GoogleButton() {
  const containerId = useId();
  const containerRef = useRef<HTMLDivElement>(null);
  const [isPending, startTransition] = useTransition();

  useEffect(() => {
    const clientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;
    if (!clientId || !window.google || !containerRef.current) return;

    window.google.accounts.id.initialize({
      client_id: clientId,
      callback: (response) => {
        startTransition(async () => {
          const result = await googleLoginAction(response.credential);
          if (result?.error) {
            toast.error(result.error);
          }
        });
      },
    });

    window.google.accounts.id.renderButton(containerRef.current, {
      type: "standard",
      theme: "outline",
      size: "large",
      width: 320,
      text: "continue_with",
      locale: "vi",
    });
  }, []);

  return (
    <>
      <Script src="https://accounts.google.com/gsi/client" strategy="afterInteractive" />
      <div
        id={containerId}
        ref={containerRef}
        className="flex justify-center min-h-[44px]"
        aria-busy={isPending}
      />
      {!process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID && (
        <p className="text-xs text-muted-foreground text-center">
          Chưa cấu hình NEXT_PUBLIC_GOOGLE_CLIENT_ID — nút Google sẽ không hiển thị.
        </p>
      )}
    </>
  );
}
```

**Lý do dùng Google render sẵn nút (`renderButton`) thay vì tự vẽ nút rồi gọi `prompt()`:** nút do Google render tự động xử lý toàn bộ accessibility, theo dõi thay đổi style theo `theme`, và là cách Google khuyến nghị chính thức — tự vẽ nút custom rồi gọi flow riêng phức tạp hơn nhiều mà không cần thiết ở core scope này.

**Vì sao không redirect() được trong callback của Google**: `window.google...callback` là hàm JS thuần chạy ở client, không phải Server Action, nên không thể gọi `redirect()` trực tiếp. Phải gọi `googleLoginAction` (Server Action) bên trong `startTransition`, và vì `googleLoginAction` tự `redirect("/")`, Next sẽ tự điều hướng — `startTransition` chỉ cần để tránh warning "missing transition" khi gọi Server Action ngoài `<form action>`.

### 7.3. Thêm Google button vào cả `RegisterForm`

Y hệt cách dùng trong `LoginForm`, đặt sau đường kẻ "hoặc".

---

## Bước 8 — Trang Login / Register

### 8.1. Route group `(auth)`

Tạo route group không ảnh hưởng URL (`(auth)` không xuất hiện trong path):

```
frontend/app/(auth)/
├── actions.ts          # đã làm ở Bước 4
├── layout.tsx           # layout riêng cho login/register (căn giữa màn hình, không có Navbar/Footer)
├── login/
│   └── page.tsx
└── register/
    └── page.tsx
```

### 8.2. `frontend/app/(auth)/layout.tsx`

```tsx
import Link from "next/link";
import { Music } from "lucide-react";

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen flex items-center justify-center bg-background px-4">
      <div className="w-full max-w-sm">
        <Link href="/" className="flex items-center justify-center gap-2 mb-8 text-foreground">
          <Music className="w-6 h-6 text-primary" />
          <span className="font-bold text-2xl tracking-tight">ToneShift</span>
        </Link>
        <div className="bg-card border border-border rounded-2xl p-6 shadow-2xl">
          {children}
        </div>
      </div>
    </div>
  );
}
```

### 8.3. `frontend/app/(auth)/login/page.tsx`

```tsx
import type { Metadata } from "next";
import LoginForm from "@/components/auth/LoginForm";

export const metadata: Metadata = { title: "Đăng nhập · ToneShift" };

export default function LoginPage() {
  return (
    <>
      <h1 className="text-xl font-bold text-center mb-6">Đăng nhập</h1>
      <LoginForm />
    </>
  );
}
```

### 8.4. `frontend/app/(auth)/register/page.tsx`

Tương tự, tiêu đề `"Tạo tài khoản"`, render `RegisterForm`.

### 8.5. Nếu người dùng đã đăng nhập mà vào `/login` hoặc `/register`

Thêm vào đầu mỗi `page.tsx`:
```ts
import { redirect } from "next/navigation";
import { getSession } from "@/lib/session";
// trong component (server component, không "use client"):
const session = await getSession();
if (session) redirect("/");
```

---

## Bước 9 — Auth context + cập nhật Navbar

### 9.1. `frontend/components/auth/AuthProvider.tsx` (Client Context)

```tsx
"use client";

import { createContext, useContext, useState, type ReactNode } from "react";
import type { UserResponse } from "@/types/auth";

interface AuthContextValue {
  user: UserResponse | null;
}

const AuthContext = createContext<AuthContextValue>({ user: null });

export function AuthProvider({
  initialUser,
  children,
}: {
  initialUser: UserResponse | null;
  children: ReactNode;
}) {
  // Không cần setState phức tạp ở core scope: logout/login đều redirect + full reload
  // dữ liệu session qua Server Component cha, nên initialUser luôn đúng sau mỗi điều hướng.
  const [user] = useState(initialUser);
  return <AuthContext.Provider value={{ user }}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
```

### 9.2. Gắn `AuthProvider` vào `app/layout.tsx`

`layout.tsx` hiện là Server Component thuần (không `async`). Sửa thành:

```tsx
import type { Metadata } from "next";
import { Plus_Jakarta_Sans } from "next/font/google";
import "./globals.css";
import Toaster from "@/components/ui/Toaster";
import { AuthProvider } from "@/components/auth/AuthProvider";
import { getSession } from "@/lib/session";

// ... giữ nguyên phần font + metadata ...

export default async function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  const session = await getSession();

  return (
    <html lang="vi" className={`${plusJakartaSans.variable} h-full antialiased dark`}>
      <body className="bg-background text-foreground overflow-x-hidden font-sans relative selection:bg-primary/30">
        <AuthProvider initialUser={session?.user ?? null}>
          {children}
        </AuthProvider>
        <Toaster />
      </body>
    </html>
  );
}
```

**Lưu ý**: thêm `async` vào `RootLayout` là hợp lệ trong App Router (Server Component có thể async).

### 9.3. Sửa `components/hush/Navbar.tsx`

Thêm nhánh hiển thị theo `useAuth()`. Navbar hiện là Client Component (`"use client"`) — giữ nguyên, chỉ thêm:

```tsx
"use client";
import { useState, useEffect } from "react";
import Link from "next/link";
import { Music, LogOut, User as UserIcon } from "lucide-react";
import { clsx } from "clsx";
import { useAuth } from "@/components/auth/AuthProvider";
import { logoutAction } from "@/app/(auth)/actions";

export function Navbar() {
  const { user } = useAuth();
  // ... giữ nguyên state scrolled + useEffect ...

  return (
    <nav className={/* giữ nguyên */}>
      <div className="mx-auto max-w-7xl h-full flex items-center justify-between">
        {/* logo + nav links giữ nguyên */}

        {user ? (
          <UserMenu name={user.name} image={user.image} />
        ) : (
          <div className="flex items-center gap-3">
            <Link href="/login" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">
              Đăng nhập
            </Link>
            <Link
              href="/register"
              className="inline-flex items-center h-10 px-5 rounded-full bg-primary text-primary-foreground font-semibold text-sm hover:brightness-110 transition-all"
            >
              Đăng ký
            </Link>
          </div>
        )}
      </div>
    </nav>
  );
}

function UserMenu({ name, image }: { name: string; image: string | null }) {
  const [open, setOpen] = useState(false);
  return (
    <div className="relative">
      <button
        onClick={() => setOpen((v) => !v)}
        className="flex items-center gap-2 h-10 pl-1 pr-3 rounded-full border border-border hover:bg-card transition-colors"
      >
        {image ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={image} alt={name} className="w-8 h-8 rounded-full object-cover" />
        ) : (
          <div className="w-8 h-8 rounded-full bg-primary/20 flex items-center justify-center">
            <UserIcon className="w-4 h-4 text-primary" />
          </div>
        )}
        <span className="hidden md:inline text-sm font-medium text-foreground max-w-[120px] truncate">
          {name}
        </span>
      </button>

      {open && (
        <>
          <div className="fixed inset-0 z-40" onClick={() => setOpen(false)} />
          <div className="absolute right-0 mt-2 w-48 rounded-xl border border-border bg-card shadow-2xl z-50 py-1">
            <form action={logoutAction}>
              <button
                type="submit"
                className="w-full flex items-center gap-2 px-4 py-2.5 text-sm text-foreground hover:bg-background/60 transition-colors"
              >
                <LogOut className="w-4 h-4" />
                Đăng xuất
              </button>
            </form>
          </div>
        </>
      )}
    </div>
  );
}
```

> **Ghi chú về `@base-ui/react`**: ban đầu định dùng `@base-ui/react` (Menu primitive) cho dropdown này để có sẵn accessibility (focus trap, đóng bằng phím Esc, `role="menu"`). Nếu muốn làm chuẩn hơn bản tự viết ở trên, thay `UserMenu` bằng `Menu.Root` / `Menu.Trigger` / `Menu.Portal` / `Menu.Positioner` / `Menu.Popup` / `Menu.Item` của `@base-ui/react` (xem doc tại https://base-ui.com/react/components/menu — kiểm tra đúng API theo version `^1.6.0` đang cài vì package này còn đang thay đổi API khá nhanh giữa các bản). Bản tự viết bằng `useState` + click-outside ở trên **đã đủ dùng cho core scope**, không bắt buộc phải đổi.

**Ảnh đại diện Google**: domain ảnh là `lh3.googleusercontent.com` — nếu sau này đổi `<img>` thường sang `next/image` để tối ưu, nhớ thêm domain đó vào `images.remotePatterns` trong `next.config.ts`. Ở bước này dùng `<img>` thường cho đơn giản (đã có eslint-disable sẵn).

---

## Bước 10 — Kiểm thử thủ công (checklist)

Chạy song song backend (`cd backend && docker compose up -d && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`, cổng `8080`) và frontend (`cd frontend && npm run dev`, cổng mặc định `3000`).

- [ ] `.env.local` có đủ `API_BASE_URL`, `SESSION_SECRET` (32 byte base64), `NEXT_PUBLIC_GOOGLE_CLIENT_ID` (để trống cũng được — nút Google ẩn, không lỗi).
- [ ] Vào `/register`, điền tên/email/password hợp lệ → submit → redirect về `/`, Navbar hiện tên + avatar mặc định (icon).
- [ ] Vào lại `/register` lúc đã đăng nhập → tự redirect về `/`.
- [ ] Đăng ký lại đúng email vừa dùng → báo lỗi `"Email đã được sử dụng"` ngay trên form, không redirect.
- [ ] Để trống password, hoặc gõ password không có số (`"abcdefgh"`) → submit → BE trả `VALIDATION_FAILED`, `fieldErrors` hiện đúng dưới ô `password`.
- [ ] Đăng xuất (click avatar → "Đăng xuất") → Navbar quay về "Đăng nhập/Đăng ký", vào lại trang bất kỳ không còn thấy tên cũ.
- [ ] Vào `/login`, gõ sai mật khẩu → báo `"Email hoặc mật khẩu không đúng"`.
- [ ] Gõ email không tồn tại → **cùng message y hệt** như sai mật khẩu (đây là hành vi **đúng theo thiết kế** chống dò email của BE, không phải bug).
- [ ] Mở DevTools → Application → Cookies → thấy cookie `ts_session`, `HttpOnly` = true, giá trị là chuỗi base64 vô nghĩa (không đọc được access/refresh token bằng mắt thường).
- [ ] (Sau khi điền `GOOGLE_CLIENT_ID` thật cả 2 phía FE/BE) bấm nút Google trên `/login` → chọn tài khoản → redirect về `/`, Navbar hiện đúng tên + avatar Google thật.
- [ ] Đăng ký bằng email/password trước, sau đó "Đăng nhập với Google" bằng **cùng email Google** → không tạo user mới, vẫn vào được tài khoản cũ (test hành vi *link account* của BE, xem `AuthService.resolveUser`).

---

## Việc cố tình KHÔNG làm trong đợt này (đã thống nhất với user)

- Trang `/profile` (đổi tên, avatar, đổi mật khẩu) — BE đã có `PUT /api/user/profile`, `POST /api/user/change-password`, chưa làm UI.
- Trang `/forgot-password`, `/reset-password` — BE đã có `POST /api/auth/forgot-password`, `/reset-password`, chưa làm UI.
- `middleware.ts` bảo vệ route — chưa có trang nào cần đăng nhập mới xem được (converter hiện tại là public), nên chưa cần. Khi có trang kiểu `/library` cần đăng nhập, thêm `middleware.ts` chỉ cần check **sự tồn tại** của cookie `ts_session` (không cần giải mã ở middleware vì Edge Runtime không có `node:crypto` AES — xem ghi chú dưới).
- Refresh access token tự động — core scope chỉ dùng endpoint public (không cần `Authorization` header), nên chưa cần. Khi có tính năng cần gọi API yêu cầu đăng nhập (ví dụ library), thêm hàm `getValidAccessToken()` trong `lib/session.ts`: đọc `accessTokenExpiresAt`, nếu gần hết hạn thì gọi `POST /api/auth/refresh` bằng `refreshToken` đang có, `createSession()` lại với response mới.

### Ghi chú kỹ thuật cho việc bảo vệ route sau này

Next.js Middleware mặc định chạy ở **Edge Runtime**, vốn không có `node:crypto` (API `createCipheriv`/`createDecipheriv` dùng ở Bước 3 **không chạy được** trong middleware theo mặc định). Hai lựa chọn khi cần `middleware.ts`:
1. **Đơn giản (khuyến nghị trước)**: middleware chỉ kiểm tra cookie `ts_session` có tồn tại hay không (không giải mã), redirect `/login` nếu thiếu. Việc đọc *nội dung* session (ai đang đăng nhập) vẫn làm ở Server Component bằng `getSession()` như bình thường.
2. **Đầy đủ**: khai báo `export const runtime = "nodejs"` trong `middleware.ts` (Next.js các bản gần đây hỗ trợ Node.js runtime cho middleware ở dạng thử nghiệm) để dùng được `node:crypto` trực tiếp trong middleware — cần tự kiểm tra lại tài liệu Next 16 vì đây là API đang thay đổi.

---

## File sẽ tạo/sửa — tóm tắt

| File | Trạng thái |
|---|---|
| `frontend/.env.example` | Tạo mới |
| `frontend/.env.local` | Tạo mới (không commit) |
| `frontend/types/auth.ts` | Tạo mới |
| `frontend/lib/api-client.ts` | Tạo mới |
| `frontend/lib/session.ts` | Tạo mới |
| `frontend/lib/form-errors.ts` | Tạo mới |
| `frontend/app/(auth)/actions.ts` | Tạo mới |
| `frontend/app/(auth)/layout.tsx` | Tạo mới |
| `frontend/app/(auth)/login/page.tsx` | Tạo mới |
| `frontend/app/(auth)/register/page.tsx` | Tạo mới |
| `frontend/components/ui/Input.tsx` | Tạo mới |
| `frontend/components/auth/AuthProvider.tsx` | Tạo mới |
| `frontend/components/auth/LoginForm.tsx` | Tạo mới |
| `frontend/components/auth/RegisterForm.tsx` | Tạo mới |
| `frontend/components/auth/GoogleButton.tsx` | Tạo mới |
| `frontend/app/layout.tsx` | Sửa — bọc `AuthProvider`, đọc `getSession()` |
| `frontend/components/hush/Navbar.tsx` | Sửa — thêm nhánh đăng nhập/avatar |

Thứ tự làm khuyến nghị: **0 → 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9**, mỗi bước build thử (`npm run build` hoặc `npm run dev` + mở trang) trước khi qua bước kế — bước 2/3/4 là lõi logic, sai ở đây sẽ kéo lỗi khó hiểu ở các bước UI phía sau.
