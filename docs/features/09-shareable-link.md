# Feature 09 – Shareable Link (Chia sẻ Cảm âm) [FEAT009]

## 1. Feature Design

### Business Requirement
Cho phép người dùng chia sẻ bản cảm âm sau khi chuyển tone thành công dưới dạng một URL duy nhất (`toneshift.vn/s/abc123xyz`). Người nhận link có thể xem, copy, in, tải PDF nhưng không được chỉnh sửa (trừ người tạo).

### User Story
- Là một người dùng, tôi muốn bấm nút Share để lấy một đường link ngắn gửi cho ban nhạc của tôi qua Zalo.
- Là một thành viên ban nhạc, tôi muốn nhấp vào link và thấy bài hát đã được đổi sẵn sang tone của ca sĩ, với giao diện chỉ đọc sạch sẽ.

### Functional Requirement
- Sinh ra short URL ngẫu nhiên.
- Giao diện Viewer Mode (Read-only) cho link được share. Hỗ trợ Copy, Export PDF, Print.
- Chủ sở hữu (Owner) có thể chuyển sang chế độ chỉnh sửa.

### Non-Functional Requirement
- **Hiệu năng:** Tốc độ load trang `/s/[code]` phải cực nhanh (ưu tiên SSR) vì đây là landing page của người nhận link.
- **SEO:** Hỗ trợ thẻ meta (Open Graph) để khi share link qua FB/Zalo sẽ hiện tên bài hát.
- **Bảo mật:** Dùng mã Hash đủ dài/độ entropy cao để chống đoán URL.

### Permissions
- Guest (Người nhận): Chỉ xem, In ấn, Copy.
- Owner (Người tạo, nếu đã đăng nhập): Xem, In, Copy, Sửa.

---

## 2. UI Flow

```text
User trên trang Converter -> Bấm Share
  ↓
Hệ thống tự động lưu bài hát (nếu chưa lưu) 
  ↓
Gọi API Generate Share Link
  ↓
Hiển thị URL ngắn (VD: /s/abc123xyz) -> Nút "Copy Link"
  ↓
Người nhận mở URL /s/abc123xyz
  ↓
Server Component fetch dữ liệu bài hát từ DB
  ↓
Render giao diện Viewer Mode (Chỉ đọc)
```

---

## 3. Folder Structure

```text
src/
 ├── app/
 │    └── s/
 │         └── [code]/
 │              ├── page.tsx          # Server Component: Fetch data & SEO
 │              └── loading.tsx       # Skeleton Loading
 ├── features/
 │    └── share/
 │         ├── components/
 │         │    ├── ShareButton.tsx   # Nút bấm kích hoạt
 │         │    ├── ShareDialog.tsx   # Modal hiển thị link
 │         │    └── SharedViewer.tsx  # Layout cho chế độ xem
 │         └── utils/
 │              └── hash.ts           # Hàm sinh unique_code
```

---

## 4. API Design

> **Cập nhật (Backend Java/Spring Boot):** chi tiết tại [`backend/01-API-Contract.md`](../backend/01-API-Contract.md) mục 7. Tạo link **yêu cầu đăng nhập** (bài phải nằm trong Library) – xem câu hỏi mở Q1 ở `backend/00-Backend-Overview.md`.

### Tạo Link Share
- **POST** `/api/shares` – 🔒 đăng nhập, chỉ chủ bài
- **Body:** `{ "songId": "uuid-123", "settings": { "allowCopy": true } }`
- **Response:** `201 { "uniqueCode": "abc123xyz", "url": "https://toneshift.vn/s/abc123xyz", "createdAt": "..." }`. Bài đã có link đang hoạt động → `200` trả link cũ (idempotent).

### Xem Link Share
- **GET** `/api/shares/:code` – public; Server Component gọi trực tiếp (gửi kèm `Authorization` nếu user đã đăng nhập để có `canEdit`).
- **Response 200:** `{ uniqueCode, song: { title, artist, content, originalKey, tone, updatedAt }, owner: { name }, permissions: { canEdit, canCopy }, settings }`. `song.id` chỉ có khi `canEdit = true`.
- Code sai định dạng / không tồn tại / đã thu hồi / bài đã xoá → **404** `SHARE_NOT_FOUND`.
- Header `Cache-Control: private, no-cache` + `ETag`.
- **DELETE** `/api/shares/:code` – thu hồi link (chủ) → 204.

---

## 5. Database Design

> DDL: [`backend/02-Database-Design.md`](../backend/02-Database-Design.md) (V5__shared_links.sql).

**`shared_links`**:
- `id` (UUID)
- `song_id` (UUID - FK)
- `unique_code` (VARCHAR(10) - UNIQUE, INDEX) – 10 ký tự Base62 sinh bằng `SecureRandom`
- `owner_id` (UUID - FK, **NOT NULL**, trước đây Nullable)
- `settings` (JSONB, mặc định `{"allowCopy": true}`)
- `expires_at` (TIMESTAMP, Nullable – chưa expose ở MVP), `revoked_at` (TIMESTAMP, Nullable)
- `created_at` (TIMESTAMP)
- Unique index một phần `(song_id) WHERE revoked_at IS NULL`: mỗi bài chỉ có tối đa 1 link đang hoạt động.

---

## 6. Component Design

**`SharedViewer` (Client Component)**
- Bọc lại phần nội dung cảm âm (`SongContent`).
- Đi kèm với `ViewerToolBar` chứa các chức năng `Copy`, `Print` (nhưng Ẩn nút `Edit`).
- Lấy quyền (Permission) từ Server Component truyền xuống qua Props.

---

## 7. State Management

Tính năng này rất nhẹ về UI State.
- **`ShareDialog`**: Dùng `useState` nội bộ (`isOpen`, `isLoading`, `shareUrl`). Không cần Global State.
- **Trang Viewer**: Fetch Data qua **Server Component** (`fetch` trong `page.tsx`). Dữ liệu được truyền thẳng vào Client Component qua props, không cần lưu vào Zustand/Redux.

---

## 8. Development Checklist

- [ ] Phase 1: Tạo Migration DB cho bảng `shared_links`.
- [ ] Phase 2: Viết thuật toán sinh `unique_code` (Base62, NanoID).
- [ ] Phase 3: Xây dựng route `/api/shares`.
- [ ] Phase 4: Thiết kế Server Component `/s/[code]/page.tsx` và tối ưu SEO (Metadata).
- [ ] Phase 5: Xây dựng UI Component `ShareDialog` và tích hợp vào `ConverterPanel`.

---

## 9. Testing Checklist

- [ ] Thử truy cập `/s/ma-linh-tinh` -> Phải trả về trang 404 (Not Found).
- [ ] Paste link vào Zalo/Facebook -> Phải hiển thị Card Preview có tên bài hát (Open Graph tags).
- [ ] Kiểm tra phân quyền: Thử sửa bài hát ở chế độ Guest xem có báo lỗi API không.

---

## 10. Refactoring

- Cần xem xét thêm cơ chế **Rate Limit** vào API tạo link (`/api/shares`) để tránh bot spam tạo hàng tỷ link gây rác Database. Dùng thư viện `@upstash/ratelimit` kết hợp Redis.
