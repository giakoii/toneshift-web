# HowItWorks Component

**Path:** `components/sections/HowItWorks.tsx`

Component này hiển thị hướng dẫn sử dụng gồm 3 bước đơn giản trên trang chủ.

## Code chi tiết và giải thích

```tsx
import React from "react";
// Import các icon từ thư viện lucide-react (có sẵn trong dự án)
import { FileText, SlidersHorizontal, CheckCircle2 } from "lucide-react";

// Dữ liệu tĩnh: Mảng chứa thông tin của 3 bước để dễ dàng render bằng vòng lặp
const STEPS = [
  {
    title: "1. Nhập bài hát",
    description: "Dán lời bài hát kèm hợp âm hoặc văn bản bạn muốn chuyển đổi vào ô nhập liệu.",
    // Lưu component icon vào biến để render động
    icon: FileText
  },
  {
    title: "2. Chọn Tone nhạc",
    description: "Xác định Tone gốc của bài hát và Tone đích mà bạn muốn chuyển sang.",
    icon: SlidersHorizontal
  },
  {
    title: "3. Nhận kết quả",
    description: "Bấm chuyển đổi và nhận ngay kết quả chính xác, giữ nguyên khoảng trắng.",
    icon: CheckCircle2
  }
];

export default function HowItWorks() {
  return (
    // section là thẻ HTML chuẩn dùng cho các phần nội dung độc lập
    <section id="how-it-works" className="w-full py-24 px-4 bg-black">
      <div className="max-w-5xl mx-auto">
        
        {/* Phần Tiêu đề của section */}
        <div className="text-center mb-16">
          <h2 className="text-3xl md:text-4xl font-bold text-white mb-4">
            Cách thức hoạt động
          </h2>
          <p className="text-white/60 max-w-xl mx-auto">
            Chỉ với 3 bước cực kì đơn giản, bạn đã có ngay một bản nhạc với Tone mới phù hợp hoàn hảo với giọng hát của mình.
          </p>
        </div>

        {/* Lưới hiển thị 3 bước */}
        {/* md:grid-cols-3: Trên màn hình máy tính (md), chia thành 3 cột đều nhau */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          
          {/* Dùng hàm map để duyệt qua mảng STEPS và tạo ra 3 thẻ div tương ứng */}
          {STEPS.map((step, index) => {
            // Lấy component icon ra từ dữ liệu để render
            const Icon = step.icon;
            
            return (
              <div 
                key={index}
                // Thiết kế thẻ hiển thị: viền mờ, nền xám trong suốt, bo góc lớn
                className="flex flex-col items-center text-center p-8 rounded-3xl border border-white/10 bg-white/5"
              >
                {/* Khung viền tròn bao quanh Icon */}
                <div className="w-14 h-14 rounded-2xl bg-blue-500/20 flex items-center justify-center text-blue-400 mb-6">
                  {/* Gọi Icon component ra */}
                  <Icon className="w-7 h-7" />
                </div>
                
                {/* Tiêu đề bước */}
                <h3 className="text-xl font-semibold text-white mb-3">
                  {step.title}
                </h3>
                
                {/* Mô tả chi tiết */}
                <p className="text-white/60 leading-relaxed text-sm">
                  {step.description}
                </p>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
```
