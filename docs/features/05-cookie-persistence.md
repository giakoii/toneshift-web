# Feature 05 – Cookie Persistence (Guest Mode) [COK005]

## 1. Feature Design

### Business Requirement
Khách hàng không cần đăng nhập vẫn có thể sử dụng mượt mà tính năng của hệ thống. Những tuỳ chỉnh cá nhân (Tone mặc định, lịch sử chuyển tone gần đây) phải được ghi nhớ và giữ nguyên trong những lần truy cập sau thông qua LocalStorage hoặc Cookie.

### User Story
- Là một người dùng, tôi muốn khi mở lại web, tone đích mặc định vẫn giữ nguyên là D (tone giọng của tôi).
- Là một người dùng, tôi muốn xem lại các đoạn cảm âm mình đã dán tuần trước mà không cần tạo tài khoản.

### Functional Requirement
- Tự động lưu `fromKey` và `toKey` đang chọn vào bộ nhớ trình duyệt.
- Tự động lưu một mảng các nội dung `inputText` gần nhất (Recent History) vào bộ nhớ, giới hạn tối đa 10 bài.

### Non-Functional Requirement
- Dung lượng lưu trữ: LocalStorage giới hạn 5MB, nên chỉ lưu lịch sử ngắn, xóa nội dung cũ nhất nếu đầy.
- Hydration Issue: Cần xử lý khác biệt giữa Server Render (chưa có LocalStorage) và Client Render để không bị lỗi UI Mismatch của Next.js.

### Permissions
- Guest (Không cần đăng nhập).

---

## 2. UI Flow

```text
User thay đổi To Key
  ↓
Zustand Store cập nhật State
  ↓
Middleware của Zustand (Persist) tự động stringify và ghi vào LocalStorage
  ↓
Lần sau user tải lại trang
  ↓
Zustand đọc từ LocalStorage và hydrate (khôi phục) State trước khi render UI.
```

---

## 3. Folder Structure

```text
src/
 ├── store/
 │    └── converterStore.ts     # Nơi khai báo Persist Middleware
 ├── hooks/
 │    └── useHasMounted.ts      # Hook giải quyết lỗi Hydration Mismatch
```

---

## 4. API Design
(Xử lý hoàn toàn Client-side)

> **Ngoại lệ – đồng bộ sau đăng nhập:** khi guest đăng nhập, FE gửi tối đa 10 bài trong history lên **POST** `/api/library/sync` (Body `{ items: SongRequest[] }`, header `Idempotency-Key: <uuid lưu ở localStorage>`), BE lưu toàn bộ trong 1 transaction (lỗi 1 item → 400 cả lô) và không tạo trùng nếu gửi lại cùng key. Sau khi `200`, FE xoá `recentTexts`. Chi tiết: [`../backend/01-API-Contract.md`](../backend/01-API-Contract.md) mục 3.

---

## 5. Database Design
- Sử dụng **LocalStorage** của trình duyệt. 
- Key name: `toneshift-converter-storage`
- Format: JSON.

---

## 6. Component Design

Không có UI riêng cho tính năng này. Nó chỉ là lớp tích hợp phía dưới của `ConverterPanel`.

Tuy nhiên, cần phải cẩn thận khi render các giá trị lấy từ LocalStorage để không gặp lỗi SSR của Next.js.

---

## 7. State Management

Zustand hỗ trợ middleware `persist` rất mạnh mẽ.

```typescript
import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export const useConverterStore = create(
  persist(
    (set) => ({
      fromKey: 'C',
      toKey: 'C',
      recentTexts: [],
      setFromKey: (key) => set({ fromKey: key }),
      // ...
    }),
    {
      name: 'toneshift-converter-storage', // Tên key trong LocalStorage
    }
  )
)
```

---

## 8. Development Checklist

- [ ] Tích hợp `persist` middleware vào `converterStore`.
- [ ] Viết hook `useHasMounted` (trả về true nếu app đã mount ở client) để bọc những component gọi state từ persisted store, tránh lỗi Hydration.
- [ ] Xây dựng tính năng Recent History (mỗi khi bấm nút chuyển tone, push vào mảng `recentTexts`, giới hạn độ dài 10).

---

## 9. Testing Checklist

- [ ] Mở devtools, chỉnh sửa `fromKey`, F5 lại trang xem state có bị mất không.
- [ ] Dán nhiều text dài, kiểm tra xem LocalStorage có bị vượt quá 5MB gây lỗi không (QuotaExceededError).
- [ ] Bật tắt incognito mode để test trải nghiệm mới hoàn toàn.

---

## 10. Refactoring

Nếu dữ liệu lịch sử vượt quá mức LocalStorage, có thể cân nhắc chuyển sang IndexedDB (thông qua localForage) thay vì LocalStorage đơn thuần. Tuy nhiên với app này, text cảm âm rất nhẹ nên LocalStorage là quá đủ.
