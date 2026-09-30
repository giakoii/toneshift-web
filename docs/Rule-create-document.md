Đọc tài liệu:

docs/[React-NextJS-Feature-Development-Guide.md](file;file:///c%3A/Users/KhoiPG/Documents/toneshift-web/docs/React-NextJS-Feature-Development-Guide.md)

và áp dụng toàn bộ các nguyên tắc trong tài liệu đó.

Tôi muốn phát triển một feature mới cho project NextJS.

Không được viết code ngay.

Hãy đóng vai một Senior Frontend Architect và tạo toàn bộ tài liệu thiết kế trước khi code.

Mục tiêu là giúp tôi học cách phân tích và tổ chức source code.

Hãy tạo các file Markdown sau.

# 1. Feature-Design.md

Bao gồm

- Business Requirement
- User Story
- Functional Requirement
- Non Functional Requirement
- Edge Cases
- Error Cases
- Permission
- Loading State
- Empty State
- Error State
- Responsive
- Accessibility
- Security
- SEO (nếu cần)

Sau đó phân tích

Feature này cần

- bao nhiêu component
- bao nhiêu page
- bao nhiêu API
- bao nhiêu custom hook
- bao nhiêu service
- bao nhiêu type
- bao nhiêu schema
- bao nhiêu utility

Giải thích vì sao.

----------------------------------------

# 2. UI-Flow.md

Phân tích toàn bộ UI.

Ví dụ

User

↓

Click button

↓

Open Modal

↓

Validate

↓

Loading

↓

Success

↓

Toast

↓

Refresh

↓

Done

Có Flowchart ASCII.

Có User Journey.

Có Screen Transition.

----------------------------------------

# 3. Folder-Structure.md

Chỉ rõ cần tạo những folder nào.

Ví dụ

features/

components/

hooks/

services/

types/

schemas/

store/

constants/

utils/

Giải thích từng folder.

Liệt kê chính xác các file sẽ tạo.

Ví dụ

components

ShareButton.tsx

ShareDialog.tsx

CopyButton.tsx

...

Giải thích trách nhiệm của từng file.

----------------------------------------

# 4. API-Design.md

Không code.

Chỉ thiết kế.

Bao gồm

Request

Response

Status Code

Authentication

Authorization

Validation

Pagination

Sorting

Filtering

Error Response

Sequence Diagram

----------------------------------------

# 5. Database-Design.md

Nếu cần backend.

Thiết kế

Table

Column

Relation

Index

Migration

Soft Delete

Audit

Sequence

----------------------------------------

# 6. Component-Design.md

Liệt kê từng Component.

Ví dụ

ShareButton

Props

State

Event

Responsibility

Child Component

Parent Component

Có sơ đồ Component Tree.

----------------------------------------

# 7. State-Management.md

Phân tích

State nào dùng useState

State nào dùng Context

State nào dùng Zustand

State nào nên dùng React Query

State nào là derived state

State nào cần memo

----------------------------------------

# 8. Development-Checklist.md

Theo từng bước.

Ví dụ

Phase 1

Tạo folder

Phase 2

Tạo types

Phase 3

Tạo API

Phase 4

Tạo Hook

Phase 5

Tạo Components

Phase 6

Testing

Mỗi bước có checklist.

----------------------------------------

# 9. Testing-Checklist.md

Unit Test

Integration Test

UI Test

Manual Test

Edge Cases

Performance

Accessibility

Responsive

Cross Browser

----------------------------------------

# 10. Refactoring.md

Sau khi feature hoàn thành.

Có thể refactor gì.

Có thể reusable gì.

Có thể optimize gì.

Những phần nào dễ technical debt.

----------------------------------------

Không viết code.

Tập trung vào cách suy nghĩ của Senior Frontend.

Giải thích cực kỳ chi tiết.
