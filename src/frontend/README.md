# Frontend Space-X

Giao diện web của Space-X: React 19 + TypeScript + Vite + Tailwind CSS 4. Khi đóng gói, `Dockerfile` ở thư mục gốc build thư mục này ra `dist/` rồi chép vào Spring Boot, nên giao diện và API chạy chung một cổng, một domain.

## Lệnh

Dùng PowerShell, đứng ở `src\frontend`:

```powershell
npm install        # lần đầu
npm run dev        # chạy ở máy, tự tải lại khi sửa; /api và /oauth2 (đăng nhập Google) được chuyển sang backend cổng 8080
npm test           # Vitest + Testing Library (jsdom), không cần backend hay Docker
npm run lint       # Oxlint
npm run build      # tsc -b rồi vite build, kết quả ở dist/
```

`npm run dev` chỉ có dữ liệu thật khi backend và MySQL đang chạy (xem `README.md` ở thư mục gốc).

## Cấu trúc `src/`

| Nhóm | File chính |
|---|---|
| Khung và định tuyến | `main.tsx`, `App.tsx` (các route), `Layout.tsx` (đầu trang: thương hiệu, điều hướng chính gồm Khóa học và Hỏi đáp, tìm kiếm, đổi giao diện, tài khoản; chân trang nhiều cột: khóa theo cấp học, hỗ trợ, pháp lý, trạng thái hệ thống), `pages/PolicyPages.tsx` (Chính sách bảo mật, Điều khoản sử dụng, Bản quyền), `courseGroups.ts` (chia khóa theo cấp THCS/THPT từ slug `tin-hoc-<lớp>`), `links.ts` (địa chỉ nhận góp ý), `AdminLayout.tsx` (thanh mục riêng của khu quản trị, chỉ hiện với Admin), `pages/` (mỗi trang một file) |
| Gọi API | `client.ts` (tự gắn `X-XSRF-TOKEN` cho `POST/PUT/PATCH/DELETE`, đổi lỗi `problem+json` thành `ApiError`), `authApi.ts`, `learningApi.ts`, `adminApi.ts`, `parse.ts` (kiểm dạng dữ liệu nhận từ máy chủ) |
| Đăng nhập | `AuthContext.tsx`, `useAuth.ts`, `authRedirect.ts` (chỉ nhận đường dẫn nội bộ), `formErrors.ts` |
| Đọc bài | `MarkdownContent.tsx` (không cho HTML thô, chỉ ảnh `/api/images/{id}`), `HeaderSearch.tsx`, `SearchForm.tsx` |
| Tiến độ học | `ProgressBar.tsx`, `LessonProgressControl.tsx`, `CourseProgressPanel.tsx`, `useMyProgress.ts` |
| Giao diện sáng/tối | `theme.ts` (hàm thuần), `ThemeToggle.tsx`, script chống nháy màu trong `index.html` |
| Hệ thiết kế | `index.css`: màu theo vai trò (`bg-paper`, `bg-surface`, `bg-sunken`, `text-ink`, `text-muted`, `border-line`, `bg-accent`, `text-danger`, `text-success`...) khai báo bằng `@theme`, giá trị chế độ tối đổi ở khối `.dark` nên thành phần không cần `dark:`; lớp dùng lại `btn btn-primary/secondary/danger/light`, `btn-sm`, `btn-icon`, `field`, `link`, `eyebrow`. Phong cách: nền xám xanh nhạt, thẻ trắng bo góc 12 px có bóng nhẹ (`shadow-card`, `shadow-lift`), một màu nhấn xanh lam, chữ không chân cho cả tiêu đề. Không dùng màu mặc định của Tailwind (`slate-*`, `blue-*`...) trong thành phần. `designSystem.test.ts` tính độ tương phản của bảng màu (WCAG AA) ở cả hai chế độ và chặn quay lại chữ có chân. |

Phần lớn file `X.ts(x)` có `X.test.ts(x)` cạnh nó.

## Quy ước

- Viết test trước (RED → GREEN). Tên test mô tả hành vi bằng một câu tiếng Việt. Số test hiện tại và cách đếm lại: `docs/TESTING.md`.
- Dữ liệu nhận từ mạng là `unknown`, phải qua hàm kiểm dạng trong `parse.ts` trước khi dùng; lỗi hiển thị cho người dùng là câu cố định của frontend, không in nội dung lỗi thô của máy chủ.
- Giao diện chỉ ẩn trang theo vai trò cho tiện; quyền thật do backend kiểm (`/api/admin/**` chỉ `ADMIN`).
- Không lưu mật khẩu, mật khẩu tạm hay dữ liệu nhạy cảm vào `localStorage` hay địa chỉ trang; `localStorage` chỉ dùng cho lựa chọn sáng/tối.
- Dependency mới phải được duyệt trước (`CLAUDE.md`); những thư viện đã duyệt cho giao diện gồm `react-router`, `react-markdown`, `remark-gfm`, `@testing-library/react`, `jsdom`.
