# Feature 07 – Xuất PDF / In ấn (Export & Print) [FEAT007]

## 1. Feature Design

### Business Requirement
Rất nhiều nhạc công có thói quen in sheet nhạc ra giấy (A4) hoặc lưu thành file PDF để lưu trữ ngoại tuyến và xem trên iPad/Máy tính bảng khi đi biểu diễn (nơi không có mạng).

### User Story
- Là một nhạc công chơi ở quán cà phê, tôi muốn in bài hát vừa chuyển tone ra khổ A4 với định dạng gọn gàng, chữ đen nền trắng để kẹp vào binder biểu diễn.
- Là một nhạc công, tôi muốn lưu thành file PDF để AirDrop sang iPad.

### Functional Requirement
- Sinh ra Print View chuẩn chỉ: Xóa bỏ mọi thành phần UI rườm rà (Header, Footer, Buttons).
- Chuyển toàn bộ màu sắc sang Black & White (Chữ đen, Nền trắng).
- In kèm theo thông tin bài hát: Tiêu đề, Tên Tác giả, Tone đang chọn.
- Hỗ trợ cắt trang (Page break) hợp lý để không bị đứt đôi dòng khi sang trang mới.

### Non-Functional Requirement
- **Performance:** Không cần viện tới thư viện backend (như Puppeteer) làm nặng máy chủ. Sử dụng CSS `@media print` và hàm `window.print()` của trình duyệt là tối ưu nhất.

### Permissions
- Guest (Không cần đăng nhập).

---

## 2. UI Flow

```text
User nhấn nút "In ấn / Xuất PDF"
  ↓
Ứng dụng gọi hàm `window.print()` của trình duyệt
  ↓
Browser Print Dialog hiện lên
(CSS `@media print` đã tự động giấu hết Header/Footer, đổi màu text sang màu Đen)
  ↓
User chọn máy in hoặc chọn "Save as PDF"
  ↓
Hoàn thành
```

---

## 3. Folder Structure

```text
src/
 ├── app/
 │    └── globals.css              # Thêm block `@media print` vào đây
 ├── components/
 │    └── print/
 │         └── PrintHeader.tsx     # Chỉ hiện ra khi in (dùng display: none trên màn hình, display: block lúc in)
```

---

## 4. API Design
(Không yêu cầu API)

---

## 5. Database Design
(Không yêu cầu DB)

---

## 6. Component Design

**`PrintHeader.tsx`**
- Component này sẽ luôn được render trên DOM nhưng bị ẩn đi (`hidden print:block`).
- Chứa Tiêu đề bài hát, Tên người đóng góp, Tone hiện tại.

**Tailwind CSS (Print Utilities)**
Sử dụng các class `print:*` của Tailwind:
- `print:hidden`: Ẩn đi khi in (dùng cho Navigation, Button, Form).
- `print:block`: Hiện ra khi in (dùng cho PrintHeader).
- `print:text-black`, `print:bg-white`: Ghi đè màu sắc (vì theme web đang là Dark Mode).
- `break-inside-avoid`: Đảm bảo một đoạn văn bản hoặc khổ thơ không bị chia làm đôi giữa hai trang A4.

---

## 7. State Management
Không yêu cầu state đặc biệt. Sử dụng `useConverterStore` để lấy `outputText` và `toKey` hiển thị trên trang in.

---

## 8. Development Checklist

- [ ] Phase 1: Tạo Component `PrintHeader` và nhúng vào `ConverterPanel`.
- [ ] Phase 2: Đi qua tất cả các layout components (Header, Footer, Sidebar, Floating Buttons) và gắn class `print:hidden`.
- [ ] Phase 3: Bổ sung CSS fix cho Dark Mode khi in (bắt buộc nền trắng chữ đen).
- [ ] Phase 4: Gắn sự kiện `onClick={() => window.print()}` cho nút Export.

---

## 9. Testing Checklist

- [ ] Nhấn Ctrl+P / Cmd+P xem trang in Preview có bị dính UI rác không.
- [ ] Kiểm tra màu sắc (Chữ có đủ đen không, nền có trắng không).
- [ ] In bài hát quá dài -> Xem chỗ ngắt trang (Page Break) có bị cắt ngang dòng chữ không.

---

## 10. Refactoring
Nếu sau này có nhu cầu xuất PDF với custom font chữ đẹp hơn hoặc chèn watermark logo ToneShift, ta có thể cài đặt thư viện `jspdf` hoặc `html2canvas` ở client-side thay vì phụ thuộc hoàn toàn vào trình duyệt, dù nó có thể làm bundle size to hơn. Lựa chọn dùng `@media print` hiện tại là cân bằng tốt nhất giữa Performance và UX.
