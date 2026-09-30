# MVP Roadmap – ToneShift

> Lộ trình triển khai chia theo Phase, từ core đến community. Mỗi Phase độc lập và có thể ship riêng.

---

## Phase 1 – Core Converter ✦ Priority: CRITICAL [COR001]

**Mục tiêu:** Người dùng có thể chuyển đổi cảm âm ngay lập tức, không cần đăng nhập.

| Task | File cần tạo | Độ phức tạp |
|---|---|---|
| Định nghĩa 12 tone + mapping | `constants/musical-keys.ts` | Thấp |
| Thuật toán transpose | `lib/transpose.ts` | Trung bình |
| Zustand store cho converter | `store/converterStore.ts` | Thấp |
| UI: Input/Output panels | `components/converter/ConverterPanel.tsx` | Trung bình |
| UI: Key selector dropdowns | `components/converter/KeySelector.tsx` | Thấp |
| UI: Syntax highlighter | `components/converter/SyntaxHighlighter.tsx` | Trung bình |
| UI: Action bar (Copy/Clear/Download) | `components/converter/ActionBar.tsx` | Thấp |
| Trang converter | `app/(main)/converter/page.tsx` | Thấp |
| **Cookie persistence** | `lib/cookie.ts` + `hooks/useConverterHistory.ts` | Trung bình |

**Definition of Done:**

- [ ] Chuyển đổi đúng tất cả 12 tone
- [ ] Nốt nhạc được tô màu, lời ca không đổi
- [ ] Nút Copy hoạt động
- [ ] Lịch sử lưu trong cookie (tối đa 5 bản gần nhất)

---

## Phase 2 – Auth & My Library ✦ Priority: HIGH [AUT002]

**Mục tiêu:** Người dùng đăng nhập và lưu bản cảm âm vào thư viện cá nhân.

| Task | File cần tạo | Độ phức tạp |
|---|---|---|
| Prisma schema | `prisma/schema.prisma` | Trung bình |
| NextAuth config | `lib/auth.ts` | Cao |
| API: auth routes | `app/api/auth/[...nextauth]/route.ts` | Cao |
| Trang Login | `app/(auth)/login/page.tsx` | Thấp |
| Trang Register | `app/(auth)/register/page.tsx` | Thấp |
| API: lưu/xoá bản cảm âm | `app/api/library/route.ts` | Trung bình |
| Trang My Library | `app/(main)/library/page.tsx` | Trung bình |
| Component: LibraryCard | `components/library/LibraryCard.tsx` | Thấp |
| **Sync cookie → DB khi login** | `lib/cookie.ts` | Trung bình |

**Definition of Done:**

- [ ] Đăng ký/đăng nhập bằng Email hoặc Google
- [ ] Lưu bản cảm âm thành công
- [ ] My Library hiển thị đúng danh sách
- [ ] Cookie history sync lên DB sau khi đăng nhập

---

## Phase 3 – Community ✦ Priority: MEDIUM [COM003]

**Mục tiêu:** Người dùng có thể xem và tìm kiếm bản cảm âm cộng đồng.

| Task | File cần tạo | Độ phức tạp |
|---|---|---|
| API: danh sách bài hát công khai | `app/api/songs/route.ts` | Trung bình |
| Trang Community list | `app/(main)/community/page.tsx` | Trung bình |
| Trang chi tiết bài hát | `app/(main)/community/[slug]/page.tsx` | Trung bình |
| Component: SongCard | `components/community/SongCard.tsx` | Thấp |
| Component: SearchBar + Filter | `components/community/SearchFilter.tsx` | Trung bình |
| Nút "Chia sẻ bản cảm âm" | `components/converter/ShareButton.tsx` | Thấp |

**Definition of Done:**

- [ ] Hiển thị danh sách bài hát công khai
- [ ] Tìm kiếm theo tên hoạt động
- [ ] Lọc theo Tone hoạt động
- [ ] Trang chi tiết hiển thị đúng nội dung

---

## Phase 4 – UI/UX Polish ✦ Priority: MEDIUM [UIX004]

**Mục tiêu:** Trải nghiệm hoàn chỉnh, responsive, accessible.

| Task | File cần tạo/sửa | Độ phức tạp |
|---|---|---|
| Dark/Light mode toggle | `components/layout/ThemeToggle.tsx` | Thấp |
| Landing page hoàn chỉnh | `app/page.tsx` + `components/sections/` | Cao |
| Responsive mobile converter | `components/converter/ConverterPanel.tsx` | Trung bình |
| SEO metadata | `app/layout.tsx` + từng page | Thấp |
| Loading states + Skeleton | các component | Thấp |
| Error boundaries | `app/error.tsx`, `app/not-found.tsx` | Thấp |

**Definition of Done:**

- [ ] Dark/Light mode chuyển mượt
- [ ] Mobile converter dùng được 1 tay
- [ ] Lighthouse score ≥ 90

---

## Thứ tự thực hiện khuyến nghị

```
Phase 1 (Core) → Phase 4 (Polish) → Phase 2 (Auth) → Phase 3 (Community)
```

> Lý do: Đưa giá trị cốt lõi (converter) ra người dùng sớm nhất, sau đó polish UI, rồi mới thêm auth + community để giảm rủi ro kỹ thuật.
