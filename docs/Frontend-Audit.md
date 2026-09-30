# Frontend Audit & Gap Analysis

> Rà soát toàn bộ FE (`app/`, `components/`, `hooks/`, `store/`, `lib/`, `constants/`, `public/`) trước khi tích hợp backend. Trạng thái xử lý ghi ở cột cuối: ✅ đã sửa · ⏳ còn lại.

**Phát hiện then chốt:** `components/hush/*` là giao diện đang chạy (cả `/` và `/converter` import). `components/sections`, `layout`, `effects` là bộ giao diện amber cũ, không trang nào dùng. Routes hiện có: `/`, `/converter`.

## 1. Placeholder / nội dung không thuộc app

| Mức | Vị trí | Vấn đề | Trạng thái |
|---|---|---|---|
| High | `app/layout.tsx` | Metadata "HUSH · Sleep…", `lang="en"` | ✅ |
| High | `hush/SocialProof.tsx` | Số liệu bịa (4.9/5, 100,000+) | ✅ gỡ |
| High | `hush/Reviews.tsx` | 3 testimonial giả, nhắc auto-scroll chưa có | ✅ gỡ |
| High | `hush/Workflow.tsx` | Hứa auto-scroll | ✅ |
| Med | `hush/{Pricing,SoundLibrary,NightRoutine,FeatureShowcase}.tsx` | Template app ngủ, tiếng Anh, không ai import | ✅ xoá |
| Med | `sections/CommunityShowcase.tsx` | Mock nghệ sĩ thật, số "trending" giả | ✅ xoá |
| Med | `hush/Footer.tsx` | "All rights reserved" tiếng Anh | ✅ |
| Low | `public/*.svg` | 5 asset mặc định của create-next-app | ✅ xoá |
| Low | `hush/CinematicConverter` vs `sections/ConverterShowcase` | Code nhân đôi | ✅ (bản `sections` bị xoá) |

## 2. UI chết

| Mức | Vị trí | Vấn đề | Trạng thái |
|---|---|---|---|
| High | `hush/Navbar.tsx` | CTA chính không có `onClick`/link | ✅ link `/converter` |
| High | `hush/Navbar.tsx` | Logo không link về `/` | ✅ |
| Med | `hush/Footer.tsx` | `href="#"` (Bảo mật, Điều khoản), `/#contact` không có đích | ✅ gỡ link chưa có trang |
| Med | Navbar/Footer/Hero/FinalCTA | `<a href>` thay vì `next/link` | ✅ |
| Low | `ConverterPanel.tsx` | textarea thiếu label | ✅ |
| Low | `ActionBar.tsx` | Không báo lỗi copy, timeout không cleanup, Download/Clear không phản hồi | ✅ |
| Low | `ui/Button.tsx` | Nhánh link bỏ `disabled`/`aria-*`/`id` | ⏳ |
| Low | `hush/CinematicConverter.tsx` | Demo chỉ đọc, không dẫn sang `/converter` | ⏳ |

## 3. Phím tắt & điều hướng

- Không có gợi ý phím tắt nào và không có listener `keydown` (không có gì "chết"). Space/Arrow sẽ cần khi làm auto-scroll.
- Không có breadcrumb / pagination / tab.
- `constants/navigation.ts`: `#how-it-works` sai id (đúng là `workflow`), `/blog` không có route, Navbar dùng link riêng ⇒ ✅ file được xoá cùng `layout/Header` (Navbar là nguồn duy nhất).
- Navbar ẩn link dưới `md`, không có menu mobile ⏳.
- Navbar không đọc `scrollY` lúc mount ✅.

## 4. Loading / Empty / Error / Success

| Mức | Vấn đề | Trạng thái |
|---|---|---|
| High | Thiếu `app/not-found.tsx`, `error.tsx`, `loading.tsx` | ✅ (`global-error.tsx` ⏳) |
| High | Không báo lỗi khi input không có hợp âm hoặc From = To | ✅ |
| Med | Không giới hạn 50.000 ký tự | ✅ |
| Med | Không debounce / không persist (docs feature 01, 05) | ⏳ |
| Low | Font thiếu subset `vietnamese`, thiếu weight 400/600 | ✅ |
| Low | Component tĩnh của `hush` đều `"use client"` | ⏳ |

## Tính năng đang quảng bá trên trang chủ nhưng chưa code

Theo quyết định của chủ dự án, phần Tính năng (`hush/ThreePillars.tsx`) và bước 04 của `hush/Workflow.tsx` hiện đã nêu các tính năng sau như thể đã có, không gắn nhãn "Sắp ra mắt":

| Tính năng | Doc | Trạng thái code |
|---|---|---|
| Chế độ biểu diễn (tự cuộn, giữ sáng màn hình) | 06 | Chưa có; `hooks/useAutoScroll.ts` chỉ là stub |
| Setlist, vuốt chuyển bài | 08 | Chưa có |
| Chia sẻ bằng link, in / PDF | 09, 07 | Chưa có |
| Cộng đồng, đánh giá, bình luận | 03 | Chưa có |

Đã hoạt động: chuyển tone 12 tone, ký hiệu Đô Rê Mi, giữ nguyên lời tiếng Việt.

## Còn lại (chưa làm trong lượt này)

Mobile menu, `Button` nhánh link, CinematicConverter tương tác, debounce + persist, `global-error.tsx`, `"use client"` thừa, hoàn thiện `hooks/useAutoScroll.ts` (hiện là stub, chưa ai import).
