# Danh sách API Space-X

Trạng thái: **thiết kế, chưa có mã nguồn** (cập nhật 2026-10-21). Khi làm, mỗi endpoint chuyển nhãn `Planned` → `Implemented` → `Tested`.

## Quy ước chung

- Tiền tố `/api`, dữ liệu JSON, UTF-8.
- **Quyền** luôn kiểm tra ở backend, không dựa vào việc ẩn nút ở frontend:
  - `Công khai`: ai cũng gọi được.
  - `Đăng nhập`: Học sinh hoặc Admin đã đăng nhập.
  - `ADMIN`: chỉ Admin.
- **Xác thực:** session cookie (`HttpOnly`, `Secure` khi chạy HTTPS, `SameSite=Lax`). Frontend và backend cùng một domain.
- **CSRF:** mọi yêu cầu `POST`/`PUT`/`PATCH`/`DELETE` phải gửi header `X-XSRF-TOKEN` lấy từ cookie `XSRF-TOKEN`; thiếu hoặc sai thì trả `403`. Mọi yêu cầu `GET` (kể cả `GET /api/health`) đều nhận cookie `XSRF-TOKEN`, nên giao diện chỉ cần gọi một `GET` trước khi ghi.
- **Session:** sau khi đăng nhập, trình duyệt giữ cookie `SESSION` (`HttpOnly`, `SameSite=Lax`, và `Secure` khi chạy sau proxy HTTPS như Render; chạy `http://localhost` ở máy thì không có `Secure`); phiên được lưu trong MySQL nên khởi động lại ứng dụng không làm mất đăng nhập.
- **Người dùng hiện tại** luôn lấy từ session. API không nhận `userId` gửi lên từ trình duyệt.
- **Phân trang:** `?page=0&size=20` (`size` tối đa 50). Trả `{ items, page, size, totalItems }`.
- **Lỗi:** định dạng `application/problem+json` (RFC 9457), không trả stack trace: `{ "type", "title", "status", "detail" }` (`Implemented` và `Tested` từ 2026-10-25, lớp `GlobalExceptionHandler`). Ví dụ lỗi không lường trước:

```json
{ "type": "about:blank", "title": "Internal Server Error", "status": 500, "detail": "Có lỗi xảy ra phía máy chủ. Vui lòng thử lại sau." }
```

Chi tiết kỹ thuật của lỗi 500 (tên exception, stack trace) chỉ ghi vào log ở server.

| Mã | Ý nghĩa |
|---|---|
| 400 | Dữ liệu không hợp lệ (kèm danh sách trường lỗi) |
| 401 | Chưa đăng nhập, hoặc đăng nhập thất bại (sai email/mật khẩu, tài khoản bị khóa: cùng một thông báo, D25) |
| 403 | Không đủ quyền hoặc thiếu CSRF token |
| 404 | Không tìm thấy (kể cả bài Nháp khi người gọi là Học sinh) |
| 409 | Xung đột: email/slug đã tồn tại, xóa khóa còn bài, khóa Admin cuối cùng |
| 405 | Sai method (ví dụ `POST /api/health`) |
| 413 | Ảnh vượt quá 1 MB |
| 415 | Loại file ảnh không được hỗ trợ |
| 429 | Gọi chatbot/Agent quá nhiều |
| 500 | Lỗi không lường trước ở máy chủ (không lộ chi tiết) |
| 502 / 504 | Nhà cung cấp AI lỗi / quá thời gian |

## Tài khoản

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| POST | `/api/auth/register` | Công khai | Đăng ký bằng email, vai trò `LEARNER` |
| POST | `/api/auth/login` | Công khai | Đăng nhập, tạo session |
| GET | `/oauth2/authorization/google` | Công khai | Chuyển sang Google để đăng nhập (đường dẫn mặc định của Spring Security; Google gọi lại `/login/oauth2/code/google`) |
| POST | `/api/auth/logout` | Đăng nhập | Đăng xuất, hủy session |
| GET | `/api/me` | Đăng nhập | Thông tin cá nhân: tên, email, vai trò, cách đăng nhập, ngày tham gia |
| PUT | `/api/me` | Đăng nhập | Sửa tên hiển thị |
| PUT | `/api/me/password` | Đăng nhập | Đổi mật khẩu, phải gửi kèm mật khẩu cũ. Tài khoản không có mật khẩu → 400 |

### Đăng ký — `POST /api/auth/register` (D25)

```json
{ "displayName": "Nguyễn Văn A", "email": "a@example.com", "password": "matkhau123", "confirmPassword": "matkhau123" }
```

- `displayName`, `email`, `password`, `confirmPassword` đều bắt buộc.
- `displayName`: cắt khoảng trắng đầu/cuối, không được rỗng, tối đa 100 ký tự.
- `email`: cắt khoảng trắng đầu/cuối rồi chuyển chữ thường (`Locale.ROOT`) **trước khi** kiểm tra, lưu và tra cứu; đúng định dạng, tối đa 255 ký tự; không trùng với tài khoản đã có, kể cả tài khoản Google.
- `password`: tối thiểu **8 ký tự** và tối đa **72 byte UTF-8** (giới hạn của BCrypt; tiếng Việt có dấu chiếm 2–3 byte mỗi ký tự nên số ký tự tối đa có thể ít hơn 72). **Không cắt khoảng trắng** vì khoảng trắng có thể là một phần của mật khẩu. Lưu dạng băm BCrypt.
- `confirmPassword`: backend **bắt buộc** kiểm tra khớp `password`, không khớp → `400`. Frontend có thể kiểm tra thêm cho tiện, nhưng không thay được kiểm tra ở backend. Không được lưu.
- Vai trò luôn là `LEARNER`. Nếu thân yêu cầu có trường `role` thì bị bỏ qua, không bao giờ được dùng.
- Đăng ký **không** tự đăng nhập (D24); học sinh phải gọi tiếp `/api/auth/login`.
- Kết quả: `201` thành công; `400` dữ liệu không hợp lệ (kèm danh sách trường lỗi); `409` email đã tồn tại; `403` thiếu CSRF token.

### Đăng nhập — `POST /api/auth/login` (D25)

```json
{ "email": "a@example.com", "password": "matkhau123" }
```

- `email` được cắt khoảng trắng và chuyển chữ thường (`Locale.ROOT`) giống lúc đăng ký; `password` giữ nguyên.
- Backend xác thực bằng `AuthenticationManager` của Spring Security, sau đó tạo session (cookie `HttpOnly`). Session lưu trong MySQL bằng Spring Session JDBC (từ việc 3.6) nên khởi động lại ứng dụng không làm mất đăng nhập. Yêu cầu chưa đăng nhập tới API cần đăng nhập chỉ nhận `401`, không tạo session và không nhận cookie `SESSION`.
- **Mọi đăng nhập thất bại** trả **cùng một lỗi** `401`: email không tồn tại, sai mật khẩu, tài khoản bị Admin khóa (`enabled = false`), và tài khoản Google (không có mật khẩu) đăng nhập bằng mật khẩu. Nhờ vậy không ai dò được email nào đã đăng ký hay đang bị khóa:

```json
{ "type": "about:blank", "title": "Unauthorized", "status": 401, "detail": "Email hoặc mật khẩu không chính xác" }
```

- Thành công trả `200` kèm thông tin người dùng; session đi trong cookie, **không** trả JWT hay session ID trong thân phản hồi:

```json
{ "displayName": "Nguyễn Văn A", "email": "a@example.com", "role": "LEARNER" }
```

- Kết quả: `200` thành công; `400` thiếu trường; `401` đăng nhập thất bại (các trường hợp trên); `403` thiếu CSRF token.

### Thông tin cá nhân — `/api/me` (việc 3.7)

Người dùng hiện tại luôn lấy từ session. `userId` gửi lên (đường dẫn, query hay thân yêu cầu) bị bỏ qua. Chưa đăng nhập → `401`; thiếu CSRF → `403`.

- `GET /api/me` → `200`: `{ "displayName", "email", "role", "authProvider", "createdAt" }`. Không có mật khẩu hay mã băm.
- `PUT /api/me` nhận `{ "displayName" }` (cắt khoảng trắng đầu/cuối, không rỗng, tối đa 100 ký tự) → `200` kèm thông tin mới. Chỉ tên hiển thị được sửa; `email`, `role`, `id` nếu có trong thân yêu cầu thì bị bỏ qua. `400` kèm danh sách trường lỗi nếu dữ liệu sai.
- `PUT /api/me/password` nhận `{ "currentPassword", "newPassword", "confirmNewPassword" }` → `204`, không có thân.
  - Mật khẩu mới theo luật đăng ký (D25): tối thiểu 8 ký tự, tối đa 72 byte UTF-8, không cắt khoảng trắng; `confirmNewPassword` phải khớp; **phải khác** mật khẩu hiện tại. Sai luật → `400` kèm danh sách trường lỗi (không phản hồi lại mật khẩu).
  - `currentPassword` sai → `400` (`detail`: "Mật khẩu hiện tại không đúng"). Tài khoản không có mật khẩu (đăng nhập bằng Google) → `400`.
  - Đổi mật khẩu **không** hủy các session đang đăng nhập khác của chính người dùng (chưa làm, ghi nhận để xem lại).

## Khóa học và bài học

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| GET | `/api/courses` | Công khai | Danh sách khóa học |
| GET | `/api/courses/{slug}/lessons` | Công khai | Danh sách bài Đã đăng của khóa: chỉ tiêu đề và tóm tắt, không có nội dung |
| GET | `/api/lessons/{slug}` | Đăng nhập | Nội dung một bài Đã đăng |
| GET | `/api/lessons?q=từ khóa` | Đăng nhập | Tìm bài Đã đăng, phân trang |
| GET | `/api/images/{id}` | Đăng nhập | Ảnh thuộc bài mà người gọi được xem |

## Tiến độ học tập

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| GET | `/api/me/progress` | Đăng nhập | Tiến độ từng khóa: số bài hoàn thành / tổng, %, bài "Học tiếp" |
| PUT | `/api/me/progress/lessons/{lessonId}` | Đăng nhập | Ghi nhận `{ "status": "IN_PROGRESS" }` khi mở bài, hoặc `{ "status": "COMPLETED" }` khi bấm "Hoàn thành bài" |
| DELETE | `/api/me/progress/courses/{courseId}` | Đăng nhập | Học lại khóa: xóa tiến độ của chính mình trong khóa đó |

Ghi nhận tiến độ dùng `PUT` riêng, không ghi ngầm trong `GET /api/lessons/{slug}`, vì `GET` phải an toàn (không làm thay đổi dữ liệu). `PUT` gọi nhiều lần cho cùng một kết quả (idempotent), và không hạ bài `COMPLETED` về `IN_PROGRESS`.

## Trợ lý AI

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| POST | `/api/chat` | Đăng nhập | Chatbot RAG: truy xuất đoạn liên quan → hỏi LLM → trả lời kèm trích dẫn |
| POST | `/api/agent/ask` | Đăng nhập | Agent: LLM tự gọi công cụ chỉ đọc, tối đa vài vòng |

Ví dụ `POST /api/chat`:

```json
// Yêu cầu
{ "question": "Mạng LAN và Internet khác nhau thế nào?" }

// Phản hồi khi tìm thấy căn cứ
{
  "answer": "Mạng LAN kết nối các máy trong phạm vi nhỏ ... [1]",
  "grounded": true,
  "citations": [
    { "n": 1, "chunkId": 1, "lessonSlug": "mang-may-tinh", "lessonTitle": "Mạng máy tính và Internet", "heading": "Phân loại mạng" }
  ]
}

// Phản hồi khi không đủ căn cứ
{ "answer": "Mình không tìm thấy thông tin phù hợp trong kho bài học.", "grounded": false, "citations": [] }
```

Quy tắc: chỉ những `n` thật sự nằm trong tập đoạn đã truy xuất mới được giữ lại trong `citations`; số trích dẫn lạ do LLM sinh ra bị loại.

`POST /api/agent/ask` trả thêm `toolCalls` (tên công cụ + tham số đã dùng) để người dùng thấy Agent đã làm gì.

### Công cụ của Agent (chỉ đọc)

| Công cụ | Tham số (giới hạn) | Kết quả |
|---|---|---|
| `searchKnowledge` | `query` (chuỗi, 1–200 ký tự), `limit` (1–5, mặc định 3) | Danh sách đoạn: `chunkId`, `lessonSlug`, `heading`, nội dung rút gọn |
| `findLessons` | `query` (1–200 ký tự), `courseSlug` (tùy chọn), `limit` (1–5) | Danh sách bài Đã đăng: `slug`, `title`, `summary` |

An toàn: không có công cụ ghi/xóa; không để LLM sinh SQL; công cụ chỉ thấy bài `PUBLISHED`; không gửi email hay tiến độ của học sinh cho LLM; giới hạn số vòng gọi và thời gian; nội dung đoạn truy xuất là dữ liệu không đáng tin, không phải chỉ dẫn.

## Quản trị (chỉ `ADMIN`)

| Method | Đường dẫn | Mô tả |
|---|---|---|
| GET | `/api/admin/dashboard` | Số người dùng (tổng, mới trong 7 ngày), số bài theo trạng thái và theo khóa, số học sinh đang học, số lượt hoàn thành bài |
| POST | `/api/admin/courses` | Tạo khóa |
| PUT | `/api/admin/courses/{id}` | Sửa khóa |
| DELETE | `/api/admin/courses/{id}` | Xóa khóa; còn bài thì 409 |
| GET | `/api/admin/lessons` | Mọi bài kể cả Nháp; lọc `?courseId=&status=`, phân trang |
| GET | `/api/admin/lessons/{id}` | Chi tiết một bài để sửa (kể cả Nháp) |
| POST | `/api/admin/lessons` | Tạo bài, mặc định `DRAFT` |
| PUT | `/api/admin/lessons/{id}` | Sửa bài |
| PATCH | `/api/admin/lessons/{id}/status` | Đăng (`PUBLISHED`) hoặc ẩn (`DRAFT`) |
| DELETE | `/api/admin/lessons/{id}` | Xóa bài (kèm ảnh, tiến độ, đoạn tri thức của bài) |
| GET | `/api/admin/lessons/{id}/images` | Danh sách ảnh của bài |
| POST | `/api/admin/lessons/{id}/images` | Tải ảnh lên (`multipart/form-data`; PNG/JPEG/WebP; tối đa 1 MB); trả `{ "id", "url" }` để chèn vào Markdown |
| DELETE | `/api/admin/images/{id}` | Xóa ảnh |
| GET | `/api/admin/users` | Danh sách người dùng, tìm theo email/tên, phân trang |
| PATCH | `/api/admin/users/{id}/status` | Khóa/mở: `{ "enabled": false }`. Không tự khóa mình, không khóa Admin cuối cùng (409) |
| POST | `/api/admin/users/{id}/reset-password` | Đặt mật khẩu tạm và trả về **một lần** để Admin báo cho học sinh; không ghi mật khẩu vào log |
| POST | `/api/admin/knowledge/reindex` | Dựng lại toàn bộ đoạn tri thức từ các bài Đã đăng |

## Hệ thống

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| GET | `/api/health` | Công khai | Kiểm tra sống (cho Render). Chỉ trả `{"status":"UP"}`, không lộ thông tin cấu hình |

## Ghi chú

- Giới hạn tần suất gọi chatbot/Agent (số yêu cầu mỗi phút mỗi người dùng) sẽ chốt ở Tuần 7 sau khi biết hạn mức thật của nhà cung cấp AI.
- Danh sách này cập nhật cùng nhánh khi cài đặt từng endpoint.
