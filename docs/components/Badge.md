# Badge Component

**Path:** `components/ui/Badge.tsx`

Component này hiển thị một nhãn nhỏ, dùng để báo trạng thái, làm thẻ tag hoặc ghi chú.

## Code chi tiết và giải thích

```tsx
import React from "react";

interface BadgeProps {
  // Dữ liệu chữ bên trong (ví dụ: "Mới")
  children: React.ReactNode;
  // Các màu sắc tương ứng với trạng thái
  variant?: "default" | "success" | "warning" | "destructive";
  className?: string;
}

export default function Badge({
  children,
  variant = "default",
  className = "",
}: BadgeProps) {
  // CSS chung cho mọi badge: chữ nhỏ, in đậm, bo góc tròn
  const baseClasses = "inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold";
  
  // CSS riêng cho từng trạng thái màu
  const variantClasses = {
    // Màu xanh dương nhạt (mặc định)
    default: "bg-blue-500/10 text-blue-400",
    // Màu xanh lá (thành công)
    success: "bg-emerald-500/10 text-emerald-400",
    // Màu vàng (cảnh báo)
    warning: "bg-amber-500/10 text-amber-400",
    // Màu đỏ (lỗi, nguy hiểm)
    destructive: "bg-red-500/10 text-red-400"
  };

  return (
    // Thẻ <span> bao bọc nội dung chữ
    <span className={`${baseClasses} ${variantClasses[variant]} ${className}`}>
      {children}
    </span>
  );
}
```
