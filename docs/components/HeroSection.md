# HeroSection Component

**Path:** `components/sections/HeroSection.tsx`

Component này là phần đầu tiên đập vào mắt người dùng khi vào trang chủ (Hero), dùng để giới thiệu chức năng chính và chứa nút Bắt đầu.

## Code chi tiết và giải thích

```tsx
import React from "react";
import Button from "@/components/ui/Button"; // Import nút bấm chúng ta đã tạo
import { Music, ArrowRight } from "lucide-react"; // Import icon

export default function HeroSection() {
  return (
    // Dùng flex cột, căn giữa toàn bộ nội dung
    <section className="relative w-full flex flex-col items-center justify-center pt-32 pb-20 px-4 text-center">
      
      {/* ── Background Elements (Phần trang trí nền) ── */}
      {/* Một vòng tròn sáng mờ ảo ở giữa màn hình bằng màu xanh gradient */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[400px] bg-blue-600/20 blur-[120px] rounded-full pointer-events-none -z-10"></div>
      
      {/* ── Nội dung chính ── */}
      {/* Nút nhỏ giới thiệu phía trên tiêu đề */}
      <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-white/5 border border-white/10 text-white/80 text-sm font-medium mb-8">
        <Music className="w-4 h-4 text-blue-400" />
        <span>Công cụ chuyển Tone nhạc trực tuyến</span>
      </div>

      {/* Tiêu đề chính (h1) to, rõ ràng */}
      <h1 className="text-4xl md:text-6xl font-extrabold text-white tracking-tight max-w-4xl mb-6 leading-tight">
        Thay đổi <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 to-blue-600">cảm âm</span> bài hát<br className="hidden md:block"/> theo đúng Tone của bạn
      </h1>

      {/* Câu giới thiệu ngắn gọn */}
      <p className="text-lg text-white/60 max-w-2xl mb-10 leading-relaxed">
        Không còn chật vật với những hợp âm quá cao hay quá thấp. Dán lời bài hát vào đây, chọn Tone bạn muốn, và chúng tôi sẽ lo phần còn lại. Nhanh chóng, chính xác, và hoàn toàn miễn phí.
      </p>

      {/* Cụm nút bấm */}
      <div className="flex flex-col sm:flex-row items-center gap-4">
        {/* Nút màu chính (Primary) dùng thẻ Link để cuộn xuống form */}
        <Button href="#converter" size="lg" variant="primary" rightIcon={<ArrowRight className="w-5 h-5"/>}>
          Chuyển đổi ngay
        </Button>
        
        {/* Nút phụ (Ghost) cuộn xuống phần Cách hoạt động */}
        <Button href="#how-it-works" size="lg" variant="ghost">
          Tìm hiểu thêm
        </Button>
      </div>
    </section>
  );
}
```
