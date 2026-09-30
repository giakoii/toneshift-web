# Feature 01 – Core Converter [COR001]

## 1. Feature Design

### Business Requirement
Cho phép người dùng nhập đoạn cảm âm/lyric có chứa hợp âm/chữ cái nốt nhạc, chọn Tone gốc (From Key) và Tone đích (To Key) để chuyển đổi toàn bộ hợp âm/nốt nhạc trong đoạn văn bản đó một cách chính xác mà không làm thay đổi lời bài hát.

### User Story
- Là một nhạc công, tôi muốn dán đoạn cảm âm vào khung nhập liệu, chọn chuyển từ tone C sang tone D, để hệ thống tự động đổi tất cả nốt C thành D, G thành A... ngay lập tức.

### Functional Requirement
- Hỗ trợ nhập liệu văn bản lớn.
- Hai dropdown chọn Tone Gốc và Tone Đích (đủ 12 tone).
- Engine xử lý regex thông minh: chỉ đổi các ký tự được xác định là nốt nhạc/hợp âm (như C, Dm, F#) mà không đổi các chữ cái trong lời bài hát (như chữ "Cho", "Đường").
- Hỗ trợ đổi format hiển thị (chỉ hợp âm, chỉ lời, hoặc cả hai).

### Non-Functional Requirement
- **Performance:** Việc parse văn bản lớn phải hoàn tất dưới 50ms, không gây block UI thread.
- **Resilience:** Không được sụp đổ (crash) khi text có ký tự lạ.

### Edge Cases & Error Cases
- **Edge Case:** Chữ cái viết hoa đầu dòng dễ bị nhầm là hợp âm (VD: "Cho em" -> "C" bị hiểu là nốt C).
- **Edge Case:** Hợp âm dính liền (C/G).
- **Error Case:** Người dùng paste text quá 50,000 ký tự (Cần limit độ dài).

### Permissions
- Không yêu cầu đăng nhập (Public access).

---

## 2. UI Flow

```text
User 
  ↓ Mở trang Home / Converter
Input Text (Dán văn bản)
  ↓ 
Select From Key & To Key
  ↓ (Trigger Zustand Store change)
Converter Engine xử lý
  ↓
Hiển thị Output Result (Real-time)
  ↓
User có thể Copy / Share / Save
```

**Sơ đồ trạng thái:**
- **Loading State:** Do xử lý ở client nên gần như không có loading, nhưng khi paste text rất dài, có thể hiện spinner nhẹ.
- **Empty State:** Textbox mờ gợi ý "Dán cảm âm của bạn vào đây...".
- **Error State:** Cảnh báo đỏ nếu vượt quá giới hạn ký tự.

---

## 3. Folder Structure

```text
src/
 ├── components/
 │    └── converter/
 │         ├── ConverterPanel.tsx    # Layout chính chia 2 cột Input/Output
 │         ├── TextInput.tsx         # Khung nhập liệu
 │         ├── OutputDisplay.tsx     # Khung hiển thị kết quả
 │         ├── KeySelector.tsx       # Dropdown chọn Tone
 │         └── Toolbar.tsx           # Copy, Clear buttons
 ├── store/
 │    └── converterStore.ts          # Zustand store
 ├── utils/
 │    ├── engine.ts                  # Core logic Regex xử lý đổi tone
 │    └── musicTheory.ts             # Các hàm liên quan tới 12 tone
 ├── constants/
 │    └── musical-keys.ts            # Mảng CHROMATIC_SCALE
```

---

## 4. API Design

Tính năng này xử lý hoàn toàn ở **Client-Side**, không có API request nào được thực hiện khi chuyển tone.

---

## 5. Database Design

Không yêu cầu lưu trữ database cho logic chuyển tone cốt lõi. (Việc lưu bài hát sẽ nằm ở Feature Setlist/Library).

---

## 6. Component Design

**`ConverterPanel`**
- **Responsibility:** Bọc các component con, lấy state từ Store.
- **Child Components:** `TextInput`, `OutputDisplay`, `KeySelector`.

**`TextInput`**
- **Props:** `value`, `onChange`
- **State:** Local state cho input đang nhập (tránh lag nếu đưa thẳng lên global store liên tục). Chỉ push lên store khi debounce 300ms.

**`OutputDisplay`**
- **Props:** `content` (kết quả đã xử lý)
- **Responsibility:** Hiển thị format `<pre>`, hỗ trợ highlight hợp âm để dễ nhìn.

---

## 7. State Management

Sử dụng **Zustand** để quản lý trạng thái chuyển tone vì nó cần được truy cập từ nhiều component khác nhau (Header muốn biết tone, Floating Toolbar muốn lấy text để in).

**Zustand Store (`converterStore`)**:
- `inputText` (string)
- `fromKey` (string)
- `toKey` (string)
- `outputText` (derived state - được tính toán tự động dựa trên 3 state trên, không lưu cứng).

**Debounce State**: Sử dụng local state `useState` cho khung nhập liệu, chỉ đẩy lên Zustand sau 300ms để tránh việc engine tính toán lại trên từng keystroke.

---

## 8. Development Checklist

- [x] Phase 1: Xây dựng thuật toán đổi tone (Engine regex).
- [x] Phase 2: Khởi tạo Zustand Store.
- [x] Phase 3: Xây dựng UI Components (Input, Output, Selectors).
- [ ] Phase 4: Tích hợp Debounce cho Text Input.
- [ ] Phase 5: Cải thiện regex để phân biệt chính xác chữ cái tiếng Việt và hợp âm.

---

## 9. Testing Checklist

- [ ] **Unit Test:** Engine đổi tone (Đổi `C` sang `D` có ra đúng `D` không? Đổi `F#` sang `G`?).
- [ ] **Unit Test:** Đảm bảo từ "Cho" không bị đổi thành "Dho" khi chuyển tone C -> D.
- [ ] **Performance:** Paste văn bản 10,000 chữ, đo thời gian xử lý < 50ms.
- [ ] **Responsive:** Đảm bảo layout 2 cột ở Desktop chuyển thành 1 cột (stack) trên Mobile.
- [ ] **Accessibility:** Các dropdown và textarea phải có `aria-label`.

---

## 10. Refactoring

- **Optimize:** Engine regex hiện tại có thể bị chậm nếu nội dung lớn. Có thể chuyển engine sang dùng Web Worker nếu nhận thấy drop FPS.
- **Reusable:** Hàm `transposeChord(chord, steps)` có thể tái sử dụng cho các module khác.
- **Technical Debt:** Việc bóc tách hợp âm và lời hiện đang dùng regex đơn giản, tương lai có thể cần một AST parser nhỏ để hiểu cú pháp bài hát chuẩn hơn.

---

## 11. Reference Implementation (Code mẫu & Giải thích)

### A. Zustand Store (`converterStore.ts`)
Việc dùng Zustand giúp chia sẻ state (ví dụ: Tone gốc, Tone đích) cho rất nhiều Component khác nhau (như Header, Footer, Toolbar) mà không phải truyền Props (Props Drilling) cồng kềnh.

```ts
// store/converterStore.ts
import { create } from 'zustand';

interface ConverterState {
  fromKey: string;
  toKey: string;
  inputText: string;
  // Các hàm thay đổi state (Actions)
  setFromKey: (key: string) => void;
  setToKey: (key: string) => void;
  setInputText: (text: string) => void;
}

export const useConverterStore = create<ConverterState>((set) => ({
  fromKey: 'C',
  toKey: 'C',
  inputText: '',
  setFromKey: (key) => set({ fromKey: key }),
  setToKey: (key) => set({ toKey: key }),
  setInputText: (text) => set({ inputText: text }),
}));
```

> **Bài học rút ra:** 
> Zustand có cú pháp ngắn gọn hơn Redux rất nhiều. Hàm `set` sẽ tự động merge (trộn) state mới với state cũ, không cần phải viết `...state` như React `useState`.

### B. Tối ưu Re-render bằng Debounce trong Khung nhập liệu
Nếu bạn đưa trực tiếp sự kiện gõ phím `onChange` lên Zustand Store, toàn bộ ứng dụng sẽ bị re-render với mỗi ký tự bạn gõ (chữ A, chữ B...). Điều này làm ứng dụng cực kỳ giật lag nếu bài hát dài. Giải pháp là dùng **Local State** và **Debounce**.

```tsx
// components/converter/TextInput.tsx
import React, { useState, useEffect } from 'react';
import { useConverterStore } from '@/store/converterStore';

export default function TextInput() {
  const setGlobalInputText = useConverterStore(state => state.setInputText);
  
  // Local state: Lấy giá trị tức thời khi người dùng gõ
  const [localText, setLocalText] = useState('');

  useEffect(() => {
    // Đặt 1 bộ đếm giờ (Timer) 300ms
    const timer = setTimeout(() => {
      // Chỉ đẩy lên Store tổng (Zustand) sau khi người dùng NGỪNG GÕ 300ms
      setGlobalInputText(localText);
    }, 300);

    // Hàm Cleanup: Nếu người dùng gõ tiếp trước khi hết 300ms, hủy timer cũ đi
    return () => clearTimeout(timer);
  }, [localText, setGlobalInputText]);

  return (
    <textarea
      className="w-full h-96 p-4 bg-zinc-900 text-white rounded-xl"
      placeholder="Dán cảm âm vào đây..."
      value={localText}
      onChange={(e) => setLocalText(e.target.value)}
    />
  );
}
```

> **Bài học rút ra:** 
> Debounce (Trì hoãn) bằng `setTimeout` và `clearTimeout` trong `useEffect` là kỹ năng bắt buộc phải biết của Senior React. Nó giúp giảm thiểu 90% số lần re-render vô nghĩa và làm mượt UI ngay lập tức.
