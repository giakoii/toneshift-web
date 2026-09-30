# Feature 04 – UI/UX & Design System [UIX004]

## 1. Feature Design

### Business Requirement
Xây dựng một hệ thống Design System chuẩn mực, đẹp mắt, có hiệu ứng Awwwards-style nhưng không rơi vào bẫy "AI-slop". Hệ thống cần đảm bảo tính nhất quán (Consistency) và dễ dàng thay đổi theme (Dark Mode ưu tiên).

### User Story
- Là một developer, tôi muốn có sẵn các component cơ bản (Button, Input, Modal) được thiết kế sẵn để không phải code lại từ đầu.
- Là một người dùng, tôi muốn giao diện mượt mà, phản hồi tốt với các vi-tương tác (micro-interactions) khi cuộn chuột hoặc hover.

### Functional Requirement
- Tích hợp Framer Motion cho các Animation (Slide, Fade, Layout morphing).
- Tích hợp Tailwind CSS version 4 với OKLCH colors.
- Component library dựa trên Radix UI / Shadcn UI nhưng được custom lại theo phong cách riêng (Acoustic & Sunset).

### Non-Functional Requirement
- **Accessibility:** Phải tương thích với màn hình đọc (Screen Readers), có viền focus rõ ràng khi dùng phím Tab.
- **Performance:** Hạn chế re-render quá nhiều khi chạy Animation. Cần có cơ chế tắt hiệu ứng cho người dùng bật tính năng "Reduced Motion".

### Permissions
- Không liên quan đến quyền truy cập.

---

## 2. UI Flow

UI Flow của Design System xoay quanh việc sử dụng các Component ở các trạng thái khác nhau (Default, Hover, Active, Disabled).

Ví dụ với một Button:
`Default` -> Hover -> `Glow/Lift` -> Click -> `Scale down` -> Loading -> `Spinner` -> Disable -> `Gray out`

---

## 3. Folder Structure

```text
src/
 ├── components/
 │    ├── ui/                  # Các component nguyên thủy (Cơ bản)
 │    │    ├── Button.tsx
 │    │    ├── Input.tsx
 │    │    ├── Modal.tsx
 │    │    └── Toast.tsx
 │    ├── effects/             # Các component hiệu ứng (Animation)
 │    │    ├── AuroraBackground.tsx
 │    │    ├── MouseSpotlight.tsx
 │    │    └── TextReveal.tsx
 ├── app/
 │    └── globals.css          # Nơi chứa các CSS Variables cho Theme (oklch)
```

---

## 4. API Design
(Không áp dụng cho Design System)

---

## 5. Database Design
(Không áp dụng cho Design System)

---

## 6. Component Design

Mỗi Component trong thư mục `ui/` phải tuân thủ triết lý:
- Dùng `cva` (class-variance-authority) để quản lý các biến thể (Variants) như: `primary`, `secondary`, `outline`, `ghost`.
- Dùng `cn` (clsx + tailwind-merge) để cho phép component cha ghi đè class an toàn mà không bị conflict.

**Ví dụ `Button.tsx`:**
- **Props:** `variant`, `size`, `isLoading`, `leftIcon`, `rightIcon`
- **Responsibility:** Nút bấm đa năng, nếu có `href` truyền vào thì tự động render thành `<Link>` của Next.js thay vì `<button>`.

---

## 7. State Management

- Local State nội bộ của từng Component (ví dụ: `isOpen` cho Modal, `isHovered` cho một số hiệu ứng phức tạp).
- Không cần Global Store.

---

## 8. Development Checklist

- [ ] Phase 1: Khai báo Color Palette trong `globals.css` bằng `oklch`.
- [ ] Phase 2: Xây dựng các UI Components cơ bản (Button, Input).
- [ ] Phase 3: Xây dựng các Effect Components bằng Framer Motion (AuroraBackground, MouseSpotlight).
- [ ] Phase 4: Thiết lập Radix UI cho các component phức tạp (Dialog, Select, Dropdown).

---

## 9. Testing Checklist

- [ ] Accessibility: Kiểm tra bằng trình duyệt (Tab key) xem focus ring có hoạt động đúng không.
- [ ] Reduced Motion: Bật tính năng giảm hiệu ứng của hệ điều hành, kiểm tra xem Framer Motion có tuân thủ (tắt bớt animation) không.
- [ ] Responsive: Test trên màn hình nhỏ.

---

## 10. Refactoring

- Hạn chế sử dụng `transition-all` ở Tailwind vì có thể gây giật lag khi kết hợp nhiều hiệu ứng, thay vào đó dùng `transition-colors`, `transition-transform`.
- Dọn dẹp các class thừa do AI sinh ra (AI-slop) như gradient text lộn xộn hoặc viền sáng bóng (glow-shadow) không cần thiết. Đảm bảo tuân thủ nguyên tắc thiết kế "Hallmark".
