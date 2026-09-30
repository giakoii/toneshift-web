# ConverterForm Component

**Path:** `components/converter/ConverterForm.tsx`

Component này chứa logic chính của việc chuyển đổi tone nhạc. Nó có các ô nhập liệu, lưới chọn Tone, nút bấm và xử lý state (dữ liệu đang nhập).

## Code chi tiết và giải thích

```tsx
"use client"; // Component này có thao tác gõ phím, click chuột nên phải là Client Component

import React, { useState } from "react";
import KeySelector from "./KeySelector";
import Button from "@/components/ui/Button";

export default function ConverterForm() {
  // ── 1. Khai báo State (Dữ liệu thay đổi trên giao diện) ──
  
  // Lưu trữ nội dung người dùng nhập vào ô Textarea
  const [inputText, setInputText] = useState("");
  // Lưu trữ Tone gốc bài hát (Mặc định chọn C)
  const [sourceKey, setSourceKey] = useState("C");
  // Lưu trữ Tone đích mà người dùng muốn chuyển qua (Mặc định chọn D)
  const [targetKey, setTargetKey] = useState("D");
  // Lưu trạng thái xem hệ thống có đang xử lý không
  const [isProcessing, setIsProcessing] = useState(false);

  // ── 2. Các hàm xử lý sự kiện (Event Handlers) ──
  
  // Hàm chạy khi người dùng bấm nút "Chuyển đổi"
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault(); // Ngăn trình duyệt load lại trang khi submit form
    
    // Nếu chưa nhập gì thì không làm gì cả
    if (!inputText.trim()) return;

    // Bật trạng thái loading
    setIsProcessing(true);

    // MÔ PHỎNG: Dùng setTimeout để tạo cảm giác hệ thống đang tính toán (sau này thay bằng hàm logic thật)
    setTimeout(() => {
      alert(`Đã chuyển từ ${sourceKey} sang ${targetKey}!\n(Logic thật sẽ được code trong lib/transpose.ts)`);
      setIsProcessing(false); // Tắt loading
    }, 1000);
  };

  // ── 3. Giao diện (JSX) ──
  return (
    // Khung bọc toàn bộ form, có viền mờ và màu nền xám tối
    <form 
      id="converter"
      onSubmit={handleSubmit}
      className="w-full max-w-4xl mx-auto bg-white/5 border border-white/10 rounded-3xl p-6 md:p-8 flex flex-col gap-8"
    >
      
      {/* ── Ô nhập liệu (Textarea) ── */}
      <div className="flex flex-col gap-2">
        <label className="text-sm font-medium text-white/70">
          Nhập lời bài hát & Hợp âm
        </label>
        <textarea
          value={inputText}
          // Hàm này chạy mỗi khi người dùng gõ phím, cập nhật vào state inputText
          onChange={(e) => setInputText(e.target.value)}
          placeholder="Ví dụ: \n[C] Đừng buồn vì [G] những lời nói..."
          // Tailwind class để textarea hiển thị mượt và cuộn được
          className="w-full h-64 bg-black/40 border border-white/10 rounded-2xl p-4 text-white placeholder-white/30 focus:outline-none focus:border-blue-500/50 focus:ring-1 focus:ring-blue-500/50 resize-y"
        />
      </div>

      {/* ── Cụm 2 bộ chọn Tone (Gốc & Đích) ── */}
      {/* Chia thành 2 cột đều nhau trên màn hình lớn */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        
        {/* Component KeySelector đã viết trước đó */}
        <KeySelector
          label="Tone hiện tại (Gốc)"
          value={sourceKey}
          onChange={setSourceKey} // Khi chọn nút khác, cập nhật biến sourceKey
        />
        
        <KeySelector
          label="Tone muốn chuyển (Đích)"
          value={targetKey}
          onChange={setTargetKey} // Khi chọn nút khác, cập nhật biến targetKey
        />
      </div>

      {/* ── Nút Submit ── */}
      <div className="flex justify-end pt-4 border-t border-white/10">
        <Button 
          type="submit" 
          size="lg" 
          variant="primary"
          isLoading={isProcessing} // Hiện spinner nếu đang processing
          disabled={!inputText.trim()} // Vô hiệu hoá nút nếu chưa nhập chữ nào
        >
          Bắt đầu chuyển đổi
        </Button>
      </div>
    </form>
  );
}
```
