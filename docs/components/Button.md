# Button Component

**Path:** `components/ui/Button.tsx`

Component này là một nút bấm (Button) có thể tái sử dụng ở mọi nơi trong dự án. Nó hỗ trợ nhiều kiểu dáng (variant), kích thước (size), và trạng thái loading.

## Code chi tiết và giải thích

Dưới đây là toàn bộ code của component `Button` cùng với giải thích từng dòng:

```tsx
// Import các thư viện cần thiết
import React from "react";
// Import thẻ Link từ Next.js để dùng cho các nút có chức năng chuyển trang
import Link from "next/link";
// Import Spinner component (chúng ta sẽ tạo sau) để hiển thị lúc loading
import Spinner from "@/components/ui/Spinner";

// ─────────────────────────────────────────────
// 1. Định nghĩa Props (dữ liệu truyền vào)
// ─────────────────────────────────────────────
// Mở rộng từ ButtonHTMLAttributes để nhận tất cả các prop cơ bản của <button> HTML (ví dụ: onClick, disabled...)
export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  // Các kiểu dáng của nút. Dấu "?" nghĩa là không bắt buộc (có giá trị mặc định)
  variant?: "primary" | "secondary" | "outline" | "ghost";
  // Các kích thước của nút
  size?: "sm" | "md" | "lg" | "icon";
  // Trạng thái đang tải dữ liệu (sẽ hiện Spinner)
  isLoading?: boolean;
  // Icon nằm bên trái chữ
  leftIcon?: React.ReactNode;
  // Icon nằm bên phải chữ
  rightIcon?: React.ReactNode;
  // Nếu truyền href, nút này sẽ trở thành thẻ <Link> chuyển trang thay vì <button>
  href?: string;
}

// ─────────────────────────────────────────────
// 2. Component chính
// ─────────────────────────────────────────────
export default function Button({
  // Lấy các props ra và gán giá trị mặc định nếu không truyền
  children,
  variant = "primary",
  size = "md",
  isLoading = false,
  leftIcon,
  rightIcon,
  href,
  className = "",
  disabled,
  // Dấu "..." (rest parameter) gom tất cả các props còn lại (ví dụ onClick) vào biến "props"
  ...props
}: ButtonProps) {
  
  // ── Khai báo các class CSS mặc định ──
  // baseClasses: Các class dùng chung cho mọi nút (bo góc, flexbox căn giữa, hiệu ứng hover...)
  const baseClasses = "inline-flex items-center justify-center font-medium rounded-xl transition-all duration-200 active:scale-95 disabled:opacity-50 disabled:pointer-events-none";
  
  // variantClasses: Chứa CSS tương ứng cho từng kiểu dáng nút
  const variantClasses = {
    primary: "bg-blue-600 text-white hover:bg-blue-700 shadow-md",
    secondary: "bg-white/10 text-white hover:bg-white/20",
    outline: "border border-white/20 text-white hover:bg-white/10",
    ghost: "text-white/70 hover:text-white hover:bg-white/10"
  };

  // sizeClasses: Chứa CSS tương ứng cho kích thước nút
  const sizeClasses = {
    sm: "px-3 py-1.5 text-sm",
    md: "px-4 py-2 text-sm",
    lg: "px-6 py-3 text-base",
    icon: "w-10 h-10" // Nút hình vuông để chứa icon
  };

  // Ghép tất cả các class lại với nhau
  const combinedClassName = `${baseClasses} ${variantClasses[variant]} ${sizeClasses[size]} ${className}`;

  // ── Phần nội dung bên trong nút ──
  const content = (
    <>
      {/* Nếu đang loading thì hiện Spinner, nếu không thì xem có leftIcon không thì hiện ra */}
      {isLoading ? <Spinner size="sm" className="mr-2" /> : leftIcon && <span className="mr-2">{leftIcon}</span>}
      
      {/* Nội dung chính của nút (ví dụ chữ "Lưu", "Đăng nhập") */}
      {children}
      
      {/* Hiện icon bên phải nếu có và KHÔNG ĐANG loading */}
      {!isLoading && rightIcon && <span className="ml-2">{rightIcon}</span>}
    </>
  );

  // ── Điều kiện render: Thẻ <Link> hay thẻ <button>? ──
  
  // Nếu có prop "href", ta render thẻ <Link> của Next.js
  if (href) {
    return (
      <Link href={href} className={combinedClassName}>
        {content}
      </Link>
    );
  }

  // Nếu không có href, ta render thẻ <button> HTML bình thường
  return (
    <button
      // Gán class đã tính toán
      className={combinedClassName}
      // Nút bị vô hiệu hóa khi disabled = true HOẶC đang loading
      disabled={disabled || isLoading}
      // Truyền các prop còn lại (ví dụ onClick) vào thẻ button
      {...props}
    >
      {content}
    </button>
  );
}
```
