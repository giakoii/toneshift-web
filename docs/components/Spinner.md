# Spinner Component

**Path:** `components/ui/Spinner.tsx`

Component này là một vòng tròn xoay vòng để báo hiệu hệ thống đang xử lý (loading).

## Code chi tiết và giải thích

```tsx
import React from "react";

// Định nghĩa props: chỉ cần kích thước và class tuỳ chỉnh
interface SpinnerProps {
  size?: "sm" | "md" | "lg";
  className?: string;
}

export default function Spinner({ size = "md", className = "" }: SpinnerProps) {
  // Quy định kích thước tương ứng bằng class của Tailwind (w-4 h-4 là 16x16 px)
  const sizeClasses = {
    sm: "w-4 h-4",
    md: "w-6 h-6",
    lg: "w-8 h-8"
  };

  return (
    // Thẻ SVG vẽ hình tròn xoay
    // "animate-spin" là class của Tailwind tạo hiệu ứng xoay 360 độ liên tục
    <svg
      className={`animate-spin text-current ${sizeClasses[size]} ${className}`}
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
    >
      {/* Vẽ một vòng tròn mờ làm viền nền */}
      <circle
        className="opacity-25"
        cx="12"
        cy="12"
        r="10"
        stroke="currentColor"
        strokeWidth="4"
      ></circle>
      
      {/* Vẽ một cung tròn đậm màu để tạo cảm giác vòng xoay */}
      <path
        className="opacity-75"
        fill="currentColor"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
      ></path>
    </svg>
  );
}
```
