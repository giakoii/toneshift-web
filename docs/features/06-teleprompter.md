# Feature 06 – Teleprompter / Auto-Scroll (Chế độ biểu diễn) [FEAT006]

## 1. Feature Design

### Business Requirement
Cung cấp trải nghiệm cuộn tự động rảnh tay cho nhạc công khi biểu diễn. Do nhạc công phải cầm nhạc cụ nên không thể dùng tay lướt màn hình để đọc lời.

### User Story
- Là một nhạc công đang chơi guitar, tôi muốn màn hình tự động cuộn xuống theo tốc độ bài hát để tôi có thể vừa đàn vừa hát mà không bị lỡ nhịp.
- Tôi muốn màn hình không bao giờ tự động tắt khi tôi đang dùng chế độ này.

### Functional Requirement
- Nút "Vào chế độ biểu diễn" (Performance Mode).
- Mở toàn màn hình, ẩn các menu không cần thiết.
- Điều khiển cuộn tự động (Play/Pause, Slider chỉnh Tốc độ cuộn, +/- size chữ).
- Background đen thui (AMOLED dark) giảm chói và tiết kiệm pin.

### Non-Functional Requirement
- **Performance:** Bắt buộc phải dùng `requestAnimationFrame` thay vì `setInterval` để cuộn mượt mà 60FPS.
- **Hardware Integration:** Sử dụng **Screen Wake Lock API** để giữ màn hình không tự tắt (Keep Awake).

### Permissions
- Guest (Không cần đăng nhập).

---

## 2. UI Flow

```text
User nhấn nút "Auto Scroll / Performance"
  ↓
UI chuyển sang Full-screen Mode (Màu tối)
Floating Controller xuất hiện góc dưới (Play/Pause, Speed)
Trình duyệt yêu cầu Wake Lock API
  ↓
User bấm Play
  ↓
Trang tự cuộn xuống (Dùng rAF). 
Nếu user cuộn chuột/tay ngược lại -> Tạm dừng (Pause).
  ↓
Khi cuộn chạm đáy màn hình -> Tự động Stop.
```

---

## 3. Folder Structure

```text
src/
 ├── features/
 │    └── teleprompter/
 │         ├── components/
 │         │    ├── AutoScrollFloatingPanel.tsx (Floating Controller)
 │         │    ├── PlayPauseButton.tsx
 │         │    └── SpeedSlider.tsx
 │         └── hooks/
 │              ├── useAutoScroll.ts       # Core hook cuộn màn hình
 │              ├── useKeepAwake.ts        # Handle Wake Lock API
 │              └── useKeyboardShortcut.ts # Bắt sự kiện phím Space/Arrow
```

---

## 4. API Design
(Xử lý hoàn toàn ở Client)

---

## 5. Database Design
- Lưu tốc độ cuộn gần nhất của user (`speed`) vào LocalStorage để có tính năng "Remember Last Speed".

---

## 6. Component Design

**`AutoScrollFloatingPanel`**
- Render bằng thẻ Portal hoặc absolute bottom để luôn nổi lên trên các component khác.
- Lắng nghe keyboard events (Space, Arrows).
- Tự động giảm độ mờ (opacity) khi chuột không hover vào để tránh che chữ.

---

## 7. State Management

Sử dụng **Local State** (`useState`, `useRef`) kết hợp **LocalStorage**.
- `speed` (number): Lưu LocalStorage.
- `isScrolling` (boolean): Dành cho Play/Pause.
- `scrollRef` (Mutable Ref): Lưu `animationId` của hàm `requestAnimationFrame` để có thể `cancelAnimationFrame`. **Tuyệt đối không lưu vào State vì sẽ gây re-render vô ích mỗi frame.**

---

## 8. Development Checklist

- [ ] Phase 1: Code custom hook `useAutoScroll.ts` dùng rAF.
- [ ] Phase 2: Code custom hook `useKeepAwake.ts` gọi Wake Lock API.
- [ ] Phase 3: Code `useKeyboardShortcut.ts` bắt sự kiện Spacebar (Pause/Resume) và Arrow Up/Down (Đổi tốc độ).
- [ ] Phase 4: Lắp ráp UI cho `AutoScrollFloatingPanel`.
- [ ] Phase 5: Xử lý Edge Case: Khi cuộn tới đáy, tự động Pause. Khi User dùng tay vuốt lên, tự động Pause.

---

## 9. Testing Checklist

- [ ] Test Mobile: Khi chạm tay vuốt ngược lên, có tự ngưng cuộn không?
- [ ] Test Performance: Có bị giật lag khi cuộn ở tốc độ cực thấp không? (Kiểm tra Profiler để xem có bị thừa re-render không).
- [ ] Test Wake Lock: Chờ 2 phút xem màn hình điện thoại có tự động khoá (sleep) không. Nếu không khoá -> Thành công.

---

## 10. Refactoring

- Đóng gói các logic cuộn thành hook tách biệt để sau này nếu làm tính năng "Thuyết trình" thì có thể mang hook đó đi tái sử dụng nguyên vẹn.
- Để ý memory leak ở `useKeyboardShortcut` -> phải `window.removeEventListener` trong return của `useEffect`.

---

## 11. Reference Implementation (Code mẫu & Giải thích)

### A. Custom Hook `useAutoScroll` (Tối ưu 60FPS)
Khi viết tính năng cuộn tự động, người mới thường hay dùng `setInterval`. Tuy nhiên `setInterval` không đồng bộ với tần số quét của màn hình, dẫn đến hiện tượng giật lag (stuttering). 
Dưới đây là cách Senior Frontend dùng `requestAnimationFrame`:

```tsx
// hooks/useAutoScroll.ts
import { useEffect, useRef, useState, useCallback } from 'react';

export function useAutoScroll(containerRef: React.RefObject<HTMLElement>) {
  const [isPlaying, setIsPlaying] = useState(false);
  const [speed, setSpeed] = useState(1);
  
  // Dùng useRef để lưu animation ID (Không dùng useState vì sẽ gây re-render vô ích)
  const animationRef = useRef<number | null>(null);
  const lastTimeRef = useRef<number>(0);

  const scrollStep = useCallback((time: number) => {
    if (!containerRef.current) return;

    if (lastTimeRef.current !== 0) {
      const deltaTime = time - lastTimeRef.current;
      // Công thức: quãng đường = vận tốc * thời gian.
      // Speed 1x tương đương cuộn 0.05px mỗi ms
      containerRef.current.scrollTop += 0.05 * speed * deltaTime;
    }
    
    lastTimeRef.current = time;
    
    // Tiếp tục loop frame tiếp theo nếu đang play
    if (isPlaying) {
      animationRef.current = requestAnimationFrame(scrollStep);
    }
  }, [isPlaying, speed, containerRef]);

  useEffect(() => {
    if (isPlaying) {
      lastTimeRef.current = performance.now(); // Khởi tạo mốc thời gian
      animationRef.current = requestAnimationFrame(scrollStep);
    } else {
      // Cleanup: Ngừng cuộn khi pause
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
      lastTimeRef.current = 0;
    }

    // Cleanup khi component unmount
    return () => {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
    };
  }, [isPlaying, scrollStep]);

  return { isPlaying, setIsPlaying, speed, setSpeed };
}
```

> **Bài học rút ra:** 
> 1. Trạng thái cuộn thay đổi từng mili-giây, nếu dùng `useState` lưu giá trị `animationId`, component sẽ bị render lại hàng trăm lần mỗi giây khiến app treo cứng. `useRef` là cứu cánh hoàn hảo cho các biến thay đổi liên tục nhưng không cần vẽ lại UI.
> 2. `cleanup function` trong `useEffect` cực kỳ quan trọng, thiếu nó thì khi thoát trang, hàm cuộn vẫn chạy ngầm gây tốn CPU (Memory Leak).

### B. Màn hình Controller (Floating UI)
Đây là Component Controller nổi lên để điều khiển tốc độ.

```tsx
// components/teleprompter/AutoScrollFloatingPanel.tsx
import { Play, Pause } from 'lucide-react';
import { useAutoScroll } from '@/hooks/useAutoScroll';
import { useRef } from 'react';

export default function AutoScrollFloatingPanel() {
  const scrollContainerRef = useRef<HTMLDivElement>(null);
  
  // Nhúng custom hook vừa tạo ở trên
  const { isPlaying, setIsPlaying, speed, setSpeed } = useAutoScroll(scrollContainerRef);

  return (
    <>
      {/* Vùng nội dung (cần cuộn) */}
      <div 
        ref={scrollContainerRef} 
        className="h-screen w-full overflow-y-auto bg-black text-white p-8"
      >
        <p className="text-2xl leading-loose">
          {/* Nội dung bài hát siêu dài ở đây... */}
        </p>
      </div>

      {/* Controller nổi ở đáy màn hình */}
      <div className="fixed bottom-8 left-1/2 -translate-x-1/2 flex items-center gap-4 bg-[#111] px-6 py-3 rounded-full border border-white/10 shadow-2xl transition-opacity hover:opacity-100 opacity-60">
        
        {/* Nút Giảm tốc độ */}
        <button onClick={() => setSpeed(s => Math.max(0.5, s - 0.5))} className="text-white/50 hover:text-white">
          -
        </button>
        
        <span className="text-xs text-amber-500 font-mono w-8 text-center">{speed}x</span>
        
        {/* Nút Play / Pause */}
        <button 
          onClick={() => setIsPlaying(!isPlaying)}
          className="bg-amber-600 rounded-full p-3 text-black hover:scale-105 active:scale-95 transition-transform"
        >
          {isPlaying ? <Pause size={18} fill="currentColor" /> : <Play size={18} fill="currentColor" className="ml-0.5" />}
        </button>

        {/* Nút Tăng tốc độ */}
        <button onClick={() => setSpeed(s => Math.min(3, s + 0.5))} className="text-white/50 hover:text-white">
          +
        </button>
        
      </div>
    </>
  );
}
```

> **Bài học rút ra:** Controller sử dụng class `fixed bottom-8 left-1/2 -translate-x-1/2` để nổi ngay giữa đáy màn hình bất chấp phần nội dung bên dưới cuộn đi đâu. Lớp `opacity-60 hover:opacity-100` giúp nó mờ đi khi không đụng tới để không che khuất chữ trên màn hình.
