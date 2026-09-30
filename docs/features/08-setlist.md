# Feature 08 – Setlist & Player Mode (Sổ tay đi diễn) [FEAT008]

## 1. Feature Design

### Business Requirement
Cho phép nhạc công tạo các danh sách bài hát (Setlist) cho từng buổi biểu diễn cụ thể. Khi đang trên sân khấu, người dùng có thể mở Setlist và lướt qua lại giữa các bài hát một cách liền mạch giống như đang xài một ứng dụng nghe nhạc.

### User Story
- Là một nhạc công đi show cuối tuần, tôi muốn tạo một Setlist "Show Acoustic Tối T7" gồm 10 bài hát đã chuyển sẵn tone.
- Khi đang biểu diễn, tôi muốn chỉ cần vuốt tay (Swipe) trên iPad để chuyển ngay sang bài tiếp theo mà không cần phải quay lại màn hình tìm kiếm.

### Functional Requirement
- CRUD Setlist (Tạo, Sửa, Xóa, Thêm bài vào Setlist).
- **Player Mode:** Giao diện toàn màn hình, hỗ trợ Swipe Left / Right (Vuốt trái phải) để chuyển bài trong Setlist.
- Hỗ trợ sắp xếp lại thứ tự bài hát (Drag & Drop reordering).

### Non-Functional Requirement
- Tốc độ vuốt (Swipe) phải mượt mà (60FPS).
- Dữ liệu phải được Prefetch (tải trước bài hát tiếp theo) để khi vuốt sang không có độ trễ loading.

### Edge Cases & Error Cases
- **Edge Case:** Mở Setlist trống -> Hiện thông báo mời thêm bài hát.
- **Edge Case:** Bài hát gốc đã bị tác giả xoá, nhưng vẫn nằm trong Setlist -> Hiện thông báo "Bài hát không còn khả dụng".

### Permissions
- Yêu cầu Đăng nhập (Auth).

---

## 2. UI Flow

```text
User vào My Library -> Tạo Setlist mới (VD: "Show T7")
  ↓
Thêm các bài hát từ kho vào Setlist. Sắp xếp thứ tự (Drag & Drop).
  ↓
User bấm nút "Biểu diễn" (Play Setlist)
  ↓
Màn hình chuyển sang Player Mode (Toàn màn hình).
Hiện bài số 1.
  ↓
User lướt sang trái (Swipe Left)
  ↓
Hiệu ứng Slide (Framer Motion)
Trang chuyển sang Bài số 2 (Đã được prefetch sẵn)
```

---

## 3. Folder Structure

```text
src/
 ├── features/
 │    └── setlist/
 │         ├── components/
 │         │    ├── SetlistManager.tsx    # Giao diện thêm xóa sửa danh sách
 │         │    ├── DraggableList.tsx     # Logic kéo thả sắp xếp (dnd-kit)
 │         │    └── PlayerMode/
 │         │         ├── PlayerOverlay.tsx
 │         │         ├── SwipeableSong.tsx
 │         │         └── PlayerControls.tsx # Nút Prev/Next
 │         └── store/
 │              └── playerStore.ts        # Zustand store lưu queue bài hát
```

---

## 4. API Design

> **Cập nhật (Backend Java/Spring Boot):** tất cả endpoint yêu cầu đăng nhập và chỉ thao tác trên setlist của chính user (setlist người khác → 404). Chi tiết: [`backend/01-API-Contract.md`](../backend/01-API-Contract.md) mục 6.

| Method | Endpoint | Body / Ghi chú | Status |
|---|---|---|---|
| POST | `/api/setlists` | `{name, description}` (Tạo Setlist, tối đa 100/user) | 201 / 400 / 422 |
| GET | `/api/setlists?page=1&limit=20` | Danh sách `SetlistSummary` | 200 |
| GET | `/api/setlists/:id` | Setlist + `items[]` (`songId`, `orderIndex`, `customKey`, `available`, `song` meta – **không** có `content`) | 200 / 404 |
| PUT | `/api/setlists/:id` | `{name, description, version}` | 200 / 409 |
| DELETE | `/api/setlists/:id` | | 204 |
| POST | `/api/setlists/:id/add-song` | `{songId, customKey}` – thêm vào cuối; trùng → 409 `SONG_ALREADY_IN_SETLIST` | 201 |
| PATCH | `/api/setlists/:id/songs/:songId` | `{customKey}` | 200 |
| DELETE | `/api/setlists/:id/songs/:songId` | Bỏ bài, `orderIndex` được đánh lại liên tục | 200 |
| PUT | `/api/setlists/:id/reorder` | `{songIds: [...]}` – phải là hoán vị **đầy đủ**, sai → 422 `REORDER_MISMATCH` | 200 |
| GET | `/api/setlists/:id/songs/:songId` | Nội dung đầy đủ 1 bài (Player Mode / prefetch bài `N+1`) | 200 |

Bài đã bị chủ xoá hoặc gỡ public khỏi Community → item trả `available: false` (FE hiện "Bài hát không còn khả dụng"), không tự xoá khỏi setlist.

---

## 5. Database Design

> DDL: [`backend/02-Database-Design.md`](../backend/02-Database-Design.md) (V4__setlists.sql).

- **`setlists`**: `id`, `owner_id` (trước đây `user_id`), `name`, `description`, `version` (optimistic lock), `created_at`, `updated_at`.
- **`setlist_songs`** (Pivot Table): PK `(setlist_id, song_id)`, `order_index` (Số nguyên xác định vị trí; unique `(setlist_id, order_index)` kiểu **DEFERRABLE INITIALLY DEFERRED** để đổi chỗ bài trong 1 transaction), `custom_key` (Tone riêng biệt do user set, khác tone gốc của bài hát), `added_at`.
- Mọi thao tác ghi khoá dòng `setlists` (`PESSIMISTIC_WRITE`) để add/reorder đồng thời không tạo trùng vị trí.

---

## 6. Component Design

**`SwipeableSong`**
- Tích hợp **Framer Motion** (`<motion.div drag="x">`).
- **Logic:** Khi `dragEnd` vượt quá một ngưỡng (threshold) nhất định -> Trigger đổi Index của bài hiện tại +1 (Next) hoặc -1 (Prev).
- Nếu đổi bài, kích hoạt hàm lấy dữ liệu của bài hát tương ứng.

**`DraggableList`**
- Sử dụng `@dnd-kit/core` để tối ưu hóa tính năng kéo thả mượt mà thay vì tự code bằng HTML5 Drag & Drop API (vốn rất tệ trên mobile).

---

## 7. State Management

Dùng Zustand `playerStore` để quản lý Player Queue (Hàng đợi nhạc) giống hệt Spotify:
- `queue`: `Song[]`
- `currentIndex`: `number`
- Hàm `next()` và `prev()`.

Khi user lướt, gọi `next()` -> `currentIndex` tăng -> UI render bài hát từ mảng `queue[currentIndex]`.

---

## 8. Development Checklist

- [ ] Phase 1: Tạo DB Models cho Setlist.
- [ ] Phase 2: Viết CRUD APIs cho Setlist.
- [ ] Phase 3: Setup thư viện kéo thả `@dnd-kit/core` ở giao diện quản lý.
- [ ] Phase 4: Thiết kế Zustand `playerStore`.
- [ ] Phase 5: Xây dựng UI Player Mode với Animation Swipe (Framer Motion).

---

## 9. Testing Checklist

- [ ] **Mobile Touch Test:** Dùng ngón tay vuốt ngang (Swipe) trên điện thoại xem nhạy không, có bị conflict với chức năng cuộn dọc (Scroll) không.
- [ ] **Data Integrity:** Kéo thả đổi vị trí bài hát, reload lại trang xem thứ tự có được lưu đúng trên Backend không.
- [ ] **Prefetching:** Vuốt sang bài thứ 2, ngắt mạng, vuốt sang bài thứ 3 xem nó có load được vì đã prefetch không.

---

## 10. Refactoring

- Tính năng Drag của Framer Motion và Scroll dọc của trình duyệt dễ cắn nhau trên Safari iOS. Cần cấu hình `touch-action: pan-y` cẩn thận ở CSS.
- Mảng `queue` trong Store nếu chứa toàn bộ văn bản của 100 bài hát có thể làm phình State Memory. Giải pháp: Chỉ load Meta (ID, Title) vào Queue, tới bài nào fetch text bài đó, prefetch sẵn bài `N+1` thay vì fetch tất cả lúc đầu.
