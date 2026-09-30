# Feature 03 – Community & Rating [COM003]

## 1. Feature Design

### Business Requirement
Tạo không gian để cộng đồng chia sẻ, tìm kiếm, đánh giá (Star Rating) và bình luận (Comment) các bài hát cảm âm. Cho phép các tương tác sâu như Reply, Like, Report, Sắp xếp và Lọc.

### User Story
- Là một người dùng, tôi muốn duyệt thư viện bài hát chung để tìm các bài hát đang hot.
- Là một người dùng, tôi muốn đánh giá 5 sao cho một bài hát chuyển tone rất chuẩn.
- Là một người dùng, tôi muốn bình luận và thả tim bình luận của người khác.

### Functional Requirement
- Danh sách bài hát cộng đồng với bộ lọc (Search, Sort by Newest/Top).
- Hệ thống Rating (1-5 sao).
- Hệ thống Comment (Hỗ trợ Reply 1 cấp, Infinite Scroll).
- Optimistic Update: Khi bấm Like/Comment, UI cập nhật ngay lập tức.

### Non-Functional Requirement
- **Performance:** Tránh N+1 query trên backend khi lấy comments. Infinite scroll mượt mà.
- **Derived State:** Điểm trung bình (Average Rating) cần được tính toán ở Backend.

### Edge Cases & Error Cases
- **Edge Case:** Comment bị xóa nhưng vẫn còn Reply con -> Giữ lại node cha, hiện chữ "Comment đã bị xóa".
- **Error Case:** Like liên tục gây spam -> Cần Throttle/Debounce. Mạng rớt khi gửi comment -> Toast lỗi và rollback UI.

### Permissions
- Xem bài, đọc comment: Public.
- Rating, Comment, Like, Share: Yêu cầu Đăng nhập (Auth).

---

## 2. UI Flow

**Luồng Gửi Bình luận (Optimistic):**
```text
User nhập comment & Đánh giá sao
  ↓ Nhấn Gửi
UI hiển thị comment ngay lập tức (Trạng thái: Pending)
  ↓ Gọi API POST ngầm
Thành công -> Xóa trạng thái Pending, hiện thời gian thực
Thất bại -> Toast: Lỗi mạng. Gỡ comment khỏi UI.
```

**Luồng Infinite Scroll:**
User cuộn xuống cuối danh sách -> Intersection Observer kích hoạt -> Fetch API Page N+1 -> Append vào danh sách hiện tại.

---

## 3. Folder Structure

```text
src/
 ├── features/
 │    └── community/
 │         ├── components/
 │         │    ├── CommunitySection.tsx
 │         │    ├── RatingSummary.tsx
 │         │    ├── CommentForm.tsx
 │         │    ├── CommentFilter.tsx
 │         │    └── CommentList.tsx
 │         │         └── CommentItem.tsx (Header, Body, Actions)
 │         └── hooks/
 │              └── useComments.ts (Bọc React Query)
```

---

## 4. API Design

> **Cập nhật (Backend Java/Spring Boot):** contract chốt bên dưới, chi tiết request/response/status: [`backend/01-API-Contract.md`](../backend/01-API-Contract.md) mục 4–5. Khác với bản cũ: phân trang comment dùng **cursor thật**, like/unlike tách `POST`/`DELETE`, rating có endpoint riêng.

### Bài hát công khai
- **GET** `/api/songs?q=&key=&sort=newest|top&page=1&limit=20` → `{ data: SongSummary[], page, limit, total, totalPages, hasNext }`
- **GET** `/api/songs/:slug` → `SongResponse` (thêm `myScore` nếu đã đăng nhập). Bài private/đã xoá → 404.
- Đưa bài lên Community: **POST** `/api/library/:id/publish` (gỡ: `/unpublish`) – yêu cầu đăng nhập, chủ bài.

### Rating (1 user – 1 rating/bài)
- **GET** `/api/songs/:id/rating-summary` → `{ average, count, distribution{1..5}, myScore }`
- **PUT** `/api/songs/:id/rating` Body `{ score: 1..5 }` (upsert) → `RatingSummary`. Tự chấm bài mình → 422 `CANNOT_RATE_OWN_SONG`.
- **DELETE** `/api/songs/:id/rating`

### Comments (Cursor pagination)
- **GET** `/api/songs/:id/comments?cursor=&limit=20&sortBy=newest|top` → `{ data, nextCursor, hasNext, total }`. `nextCursor` là chuỗi opaque hoặc `null`; FE truyền nguyên vẹn làm `pageParam` của `useInfiniteQuery`.
- **GET** `/api/comments/:id/replies?cursor=&limit=20` (reply 1 cấp, cũ → mới)

### Action APIs
- **POST** `/api/comments` (Body: `songId`, `content` ≤ 2000, `parentId`) → 201 `CommentResponse`. Reply vào reply → 422 `COMMENT_REPLY_DEPTH_EXCEEDED`.
- **PUT** `/api/comments/:id` (Body: `content`) – chỉ chủ comment.
- **DELETE** `/api/comments/:id` – chủ hoặc Admin, xoá mềm → 204.
- **POST** `/api/comments/:id/like` → `{ likes, isLiked: true }` (idempotent)
- **DELETE** `/api/comments/:id/like` → `{ likes, isLiked: false }` (idempotent)
- **POST** `/api/comments/:id/report` (Body: `reason`, `detail`) → 201; báo lặp → 409 `ALREADY_REPORTED`.

Comment đã xoá nhưng còn reply trả `deleted: true`, `content: null` để FE hiện "Comment đã bị xóa".

---

## 5. Database Design

> DDL, index và query tránh N+1: [`backend/02-Database-Design.md`](../backend/02-Database-Design.md) (V3__community.sql).

- **`ratings`** (1 user - 1 bài hát chỉ có 1 rating):
  - `id`, `user_id`, `song_id`, `score` (CHECK 1–5), `created_at`, `updated_at`; unique `(user_id, song_id)`
- **`comments`** (Tree structure, tối đa 1 cấp reply):
  - `id`, `user_id`, `song_id`, `parent_id` (Nullable), `content`, `like_count`, `reply_count` (denormalized, cập nhật atomic), `edited_at`, `created_at`, `updated_at`, `deleted_at` (soft delete)
- **`comment_likes`**:
  - `user_id`, `comment_id`, `created_at`; PK `(user_id, comment_id)`
- **`reports`**:
  - `id`, `reporter_id`, `comment_id`, `reason` (`SPAM`/`OFFENSIVE`/`WRONG_CONTENT`/`OTHER`), `detail`, `status` (`OPEN`/`RESOLVED`/`DISMISSED`), `resolved_by`, `resolved_at`; unique `(reporter_id, comment_id)`
- **`songs`** (dùng chung Library + Community): thêm `rating_avg`, `rating_count`, `comment_count` (denormalized) để sort `top` mà không phải `AVG()` mỗi request.

---

## 6. Component Design

**`CommentList`**
- Chứa logic `IntersectionObserver` cho Infinite Scroll. Map qua danh sách `CommentItem`.

**`CommentItem`**
- **Props:** `comment`, `onReply`, `onLike`
- **Responsibility:** Hiển thị 1 comment. Gồm `CommentHeader`, `CommentBody`, và `CommentActions`. Nếu có `replies`, render đệ quy hoặc hiển thị danh sách tĩnh bên dưới.

---

## 7. State Management

Sử dụng **TanStack React Query** để quản lý Server State:
- Dùng `useInfiniteQuery` cho việc gọi API phân trang (tự ghép data khi cuộn).
- Optimistic Updates: Khi Like bình luận, dùng `queryClient.setQueryData` sửa bộ đệm ngay lập tức.
- **Local State:** Trạng thái input form (`useState`), đóng mở modal báo cáo.
- **URL State:** Filter/Sort (vd `?sort=top`) để dễ chia sẻ URL.

---

## 8. Development Checklist

- [ ] Phase 1: Tạo schema DB cho Comments và Ratings.
- [ ] Phase 2: Viết các Endpoint API.
- [ ] Phase 3: Setup React Query.
- [ ] Phase 4: Code UI Components tĩnh (Storybook hoặc trực tiếp).
- [ ] Phase 5: Tích hợp Infinite Scroll bằng `react-intersection-observer`.
- [ ] Phase 6: Code logic Optimistic Update cho chức năng Like và Post Comment.

---

## 9. Testing Checklist

- [ ] **Data Fetching:** Cuộn kịch kim xem Infinite scroll có lấy đúng trang tiếp theo không.
- [ ] **Optimistic Update:** Tắt WiFi, bấm Like xem UI phản hồi ra sao (phải rollback lại trạng thái cũ).
- [ ] **Edge Cases:** Test hiển thị comment gốc bị xóa nhưng vẫn còn comment con.

---

## 10. Refactoring

- Xử lý bài toán N+1 ở backend khi lấy danh sách user tương ứng với comments. (Nên dùng JOIN hoặc Prisma `include`).
- Hạn chế độ sâu của Reply (chỉ 1 cấp, giống YouTube) để tránh vỡ layout Mobile.
- Dùng Debounce/Throttle phía client để chống Spam click Like.

---

## 11. Reference Implementation (Code mẫu & Giải thích)

### A. Next.js API Route (Backend)
Đây là cách thiết kế một API Route GET trả về danh sách comment hỗ trợ phân trang (Pagination) bằng `cursor`.

```ts
// app/api/songs/[id]/comments/route.ts
import { NextResponse } from 'next/server';
import prisma from '@/lib/prisma'; // Database client

export async function GET(
  request: Request,
  { params }: { params: { id: string } }
) {
  // Lấy các Query Parameters từ URL (vd: ?cursor=10&limit=20)
  const { searchParams } = new URL(request.url);
  const cursor = searchParams.get('cursor'); // ID của comment cuối cùng ở trang trước
  const limit = parseInt(searchParams.get('limit') || '20', 10);
  const songId = params.id;

  try {
    const comments = await prisma.comment.findMany({
      where: { songId },
      take: limit + 1, // Lấy dư 1 item để kiểm tra xem còn trang tiếp theo không
      cursor: cursor ? { id: cursor } : undefined,
      orderBy: { createdAt: 'desc' },
      include: { user: true }, // Tránh N+1 query bằng cách include luôn thông tin user
    });

    // Nếu số item trả về lớn hơn limit, tức là còn trang tiếp theo
    let nextCursor: string | null = null;
    if (comments.length > limit) {
      const nextItem = comments.pop(); // Bỏ đi phần tử dư ra
      nextCursor = nextItem!.id;
    }

    return NextResponse.json({
      data: comments,
      nextCursor,
    });
  } catch (error) {
    return NextResponse.json({ error: 'Failed to fetch comments' }, { status: 500 });
  }
}
```

> **Bài học rút ra:** 
> Cursor-based Pagination tối ưu hiệu năng Database hơn rất nhiều so với Offset-based Pagination (dùng `skip`, `take`), đặc biệt khi bảng có hàng triệu dòng. Query Database `include: { user: true }` giúp lấy luôn thông tin người dùng trong 1 lần gọi (tránh lỗi N+1 Query).

### B. React Query: Infinite Scroll & Optimistic Update (Frontend)
Thay vì dùng `useEffect` gọi API thủ công, React Query giúp quản lý Caching, Tự động gọi lại khi mạng rớt, và Optimistic Update.

```tsx
// hooks/useComments.ts
import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query';

export function useComments(songId: string) {
  const queryClient = useQueryClient();

  // Lấy danh sách comments hỗ trợ cuộn vô tận
  const query = useInfiniteQuery({
    queryKey: ['comments', songId],
    queryFn: async ({ pageParam = '' }) => {
      const res = await fetch(`/api/songs/${songId}/comments?cursor=${pageParam}&limit=20`);
      return res.json();
    },
    getNextPageParam: (lastPage) => lastPage.nextCursor || undefined,
  });

  // Gửi comment mới (Sử dụng Optimistic Update)
  const addCommentMutation = useMutation({
    mutationFn: async (content: string) => {
      // Gọi API thực tế
      await fetch(`/api/comments`, { method: 'POST', body: JSON.stringify({ songId, content }) });
    },
    onMutate: async (newContent) => {
      // 1. Dừng các query đang chạy để tránh đè dữ liệu
      await queryClient.cancelQueries({ queryKey: ['comments', songId] });

      // 2. Lấy dữ liệu cũ lưu lại phòng trường hợp gọi API thất bại
      const previousComments = queryClient.getQueryData(['comments', songId]);

      // 3. Optimistic Update: Thêm ngay comment giả vào UI (như Facebook)
      queryClient.setQueryData(['comments', songId], (oldData: any) => {
        // Tạo một comment giả (Fake) để hiển thị trước
        const fakeComment = { id: Math.random().toString(), content: newContent, isPending: true };
        
        // Thêm vào đầu trang số 1
        oldData.pages[0].data.unshift(fakeComment);
        return oldData;
      });

      return { previousComments };
    },
    onError: (err, newContent, context) => {
      // Nếu gọi API báo lỗi -> Rollback (Hủy) trả lại dữ liệu cũ như chưa có gì xảy ra
      queryClient.setQueryData(['comments', songId], context?.previousComments);
    },
    onSettled: () => {
      // Bất kể thành công hay thất bại, gọi API load lại danh sách chuẩn xác từ DB
      queryClient.invalidateQueries({ queryKey: ['comments', songId] });
    }
  });

  return { query, addCommentMutation };
}
```

> **Bài học rút ra:** 
> Optimistic Update (Cập nhật lạc quan) là kĩ thuật đỉnh cao của UI/UX. Khi người dùng bấm "Gửi", bình luận ngay lập tức hiện lên màn hình dù chưa được lưu vào Server, tạo cảm giác app chạy cực kỳ nhanh và không có độ trễ. Nếu Server báo lỗi, UI sẽ tự động thu hồi lại bình luận đó (Rollback).
