# Thiết kế tính năng: Auto Scroll Mode

## 1. Phân tích UI Flow & UX

Tính năng cuộn tự động (như khi đánh đàn, tay không rảnh để cuộn chuột) là một tính năng UX "ăn tiền" của ứng dụng âm nhạc.

### 1.1. Luồng hoạt động (User Journey)
1. **Kích hoạt:** User nhấn nút **"Auto Scroll"** trên thanh công cụ.
2. **Trạng thái:** Trang bắt đầu tự động cuộn xuống với vận tốc (Speed x1). Màn hình bật chế độ Keep Awake (chống tắt màn hình).
3. **Điều khiển (Floating Controller):** 
   - Xuất hiện một Floating Box nhỏ ở góc dưới màn hình (Luôn visible).
   - Có nút Play/Pause.
   - Có Slider hoặc các nút (-/+) để tăng giảm tốc độ: x0.5, x1, x1.5, x2.
4. **Hành vi ngắt quãng:**
   - Nếu user cuộn chuột ngược lại -> Tạm dừng (Pause) auto scroll.
   - Nếu cuộn hết trang -> Tự động Stop.
5. **Shortcuts:** Nhấn Spacebar để Pause/Resume, Arrow Up/Down để đổi tốc độ.

---

## 2. Phân tích Component

Tính năng này không cần API, hoàn toàn chạy ở Client.

```text
(Layout/Page)
 ├── AutoScrollButton (Nút trên toolbar để kích hoạt)
 └── AutoScrollFloatingPanel (Render bằng Portal để đè lên mọi UI)
      ├── PlayPauseButton
      ├── SpeedSlider (Thanh trượt)
      └── CloseButton
```

**Trách nhiệm của `AutoScrollFloatingPanel`:**
- Lắng nghe keyboard events (Space, Arrows).
- Chứa logic UI (Dark Mode support) với độ mờ (opacity) giảm khi không hover để tránh che khuất nốt nhạc.

---

## 3. Phân tích Hook (Trái tim của tính năng)

Thay vì viết hàm cuộn rải rác, toàn bộ logic được đóng gói thành các **Custom Hooks** đạt chuẩn Senior:

### 3.1. `useAutoScroll.ts`
Đây là core hook. KHÔNG dùng `setInterval` (gây giật lag). **BẮT BUỘC** dùng `requestAnimationFrame` để cuộn mượt (Smooth Scroll) 60FPS.

```typescript
// Ý tưởng interface
interface UseAutoScrollProps {
  initialSpeed?: number;
}
interface UseAutoScrollReturn {
  isScrolling: boolean;
  speed: number;
  start: () => void;
  pause: () => void;
  resume: () => void;
  stop: () => void;
  setSpeed: (val: number) => void;
}
```

### 3.2. `useKeepAwake.ts`
Dùng **Screen Wake Lock API** của trình duyệt web.
- Mục đích: Không để điện thoại/tablet tự tắt màn hình khi user đang đánh đàn.
- Logic: Khi `isScrolling === true`, request WakeLock. Khi pause/stop, release WakeLock.

### 3.3. `useKeyboardShortcut.ts`
Lắng nghe sự kiện `keydown`. Nhớ `return () => window.removeEventListener` trong cleanup function của `useEffect` để tránh memory leak.

---

## 4. Phân tích State Management

Quản lý State hoàn toàn bằng **Local State** (`useState`, `useRef`) kết hợp **LocalStorage**. Không cần Global Store (Zustand/Redux).

1. **`speed` (number):** Lưu trong LocalStorage (`useLocalStorage` hook) để có tính năng **Remember Last Speed** (Lần sau user bật, vẫn giữ tốc độ cũ).
2. **`isScrolling` (boolean):** Xác định trạng thái Play/Pause.
3. **`scrollRef` (Mutable Ref):** 
   - Cần dùng `useRef` lưu tham chiếu của hàm `requestAnimationFrame` ID để có thể `cancelAnimationFrame` khi pause.
   - Không lưu `animationId` vào `useState` vì nó gây re-render liên tục không cần thiết.

### Edge Cases cần xử lý:
- Tắt tính năng khi cuộn chạm đáy (`window.innerHeight + window.scrollY >= document.body.offsetHeight`).
- Mobile Support: Khi chạm vào màn hình cảm ứng, có nên pause không? (Có, chạm 1 lần để Pause, chạm lại để Resume).
