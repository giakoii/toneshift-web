# KeySelector Component

**Path:** `components/converter/KeySelector.tsx`

Component này tạo ra một lưới các nút (ví dụ: C, D, E...) để người dùng chọn Tone gốc và Tone đích của bài hát.

## Code chi tiết và giải thích

```tsx
// Bắt buộc khai báo "use client" vì có tương tác onClick (chỉ chạy ở phía trình duyệt)
"use client";

import React from "react";

// Danh sách 12 nốt nhạc cơ bản (hằng số)
const MUSICAL_KEYS = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"];

interface KeySelectorProps {
  // Tên hiển thị phía trên danh sách chọn (Ví dụ: "Tone gốc")
  label: string;
  // Nốt nhạc đang được chọn hiện tại (Ví dụ: "C")
  value: string;
  // Hàm sẽ chạy khi người dùng bấm chọn nốt khác
  onChange: (key: string) => void;
}

export default function KeySelector({ label, value, onChange }: KeySelectorProps) {
  return (
    <div className="flex flex-col gap-3">
      {/* Hiển thị dòng tiêu đề */}
      <label className="text-sm font-medium text-white/70">
        {label}
      </label>
      
      {/* Tạo một lưới (grid) gồm 4 cột (mỗi dòng 4 nốt) */}
      <div className="grid grid-cols-4 gap-2">
        {/* Lặp qua mảng 12 nốt nhạc để tạo ra 12 cái nút tương ứng */}
        {MUSICAL_KEYS.map((key) => {
          // Kiểm tra xem nút này có trùng với nút đang được chọn hay không
          const isSelected = key === value;
          
          return (
            <button
              // Mỗi phần tử trong list cần 1 key duy nhất để React quản lý
              key={key}
              // Gọi hàm onChange và truyền nốt nhạc tương ứng khi user click
              onClick={() => onChange(key)}
              // Class linh hoạt: Nếu được chọn thì tô nền màu xanh, nếu không thì xám nhạt
              className={`
                py-2 rounded-lg text-sm font-semibold transition-all duration-200
                ${isSelected 
                  ? "bg-blue-600 text-white shadow-md shadow-blue-500/20" 
                  : "bg-white/5 text-white/50 hover:bg-white/10 hover:text-white"
                }
              `}
            >
              {/* Chữ hiển thị trên nút (VD: "C#") */}
              {key}
            </button>
          );
        })}
      </div>
    </div>
  );
}
```
