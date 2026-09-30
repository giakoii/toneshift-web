# ResultDisplay Component

**Path:** `components/converter/ResultDisplay.tsx`

Component này hiển thị kết quả sau khi bài hát đã được chuyển Tone, kèm theo các nút tiện ích như Copy (Sao chép).

## Code chi tiết và giải thích

```tsx
"use client";

import React, { useState } from "react";
import Button from "@/components/ui/Button";
import Badge from "@/components/ui/Badge";
import { Copy, Check } from "lucide-react";

interface ResultDisplayProps {
  // Bài hát sau khi đã xử lý
  resultText: string;
  // Tone cũ
  originalKey: string;
  // Tone mới
  targetKey: string;
}

export default function ResultDisplay({ resultText, originalKey, targetKey }: ResultDisplayProps) {
  // Biến lưu trạng thái đã copy hay chưa để hiển thị icon dấu check
  const [isCopied, setIsCopied] = useState(false);

  // Hàm xử lý việc sao chép văn bản vào bộ nhớ đệm (Clipboard)
  const handleCopy = async () => {
    if (!resultText) return;
    
    try {
      // Dùng API có sẵn của trình duyệt để copy text
      await navigator.clipboard.writeText(resultText);
      // Bật trạng thái đã copy
      setIsCopied(true);
      
      // Sau 2 giây tự động tắt trạng thái đã copy đi
      setTimeout(() => setIsCopied(false), 2000);
    } catch (err) {
      console.error("Không thể copy:", err);
    }
  };

  // Nếu không có kết quả thì không render gì cả (ẩn component)
  if (!resultText) return null;

  return (
    <div className="w-full max-w-4xl mx-auto bg-blue-900/10 border border-blue-500/20 rounded-3xl p-6 md:p-8 mt-12 flex flex-col gap-6">
      
      {/* ── Phần tiêu đề & Nhãn ── */}
      <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 border-b border-blue-500/20 pb-4">
        <div>
          <h3 className="text-xl font-bold text-white mb-2">Kết quả chuyển đổi</h3>
          <div className="flex items-center gap-2">
            {/* Dùng Badge component để tạo các tag báo Tone */}
            <Badge variant="default">Gốc: {originalKey}</Badge>
            <span className="text-white/40 text-sm">→</span>
            <Badge variant="success">Mới: {targetKey}</Badge>
          </div>
        </div>
        
        {/* ── Nút Copy ── */}
        <Button 
          variant="secondary" 
          size="sm" 
          // Nếu đã copy thì icon là Check, chưa thì icon là Copy
          leftIcon={isCopied ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
          onClick={handleCopy}
        >
          {isCopied ? "Đã sao chép" : "Sao chép kết quả"}
        </Button>
      </div>

      {/* ── Vùng hiển thị kết quả ── */}
      <div className="w-full p-4 bg-black/40 border border-white/5 rounded-2xl overflow-x-auto">
        {/* 
          Thẻ <pre> giúp giữ nguyên các khoảng trắng (dấu cách, xuống dòng) của người dùng.
          Đây là thẻ bắt buộc khi làm việc với hợp âm bài hát để tránh bị lệch vị trí.
        */}
        <pre className="text-white/90 text-sm md:text-base font-mono whitespace-pre-wrap leading-loose">
          {resultText}
        </pre>
      </div>
    </div>
  );
}
```
