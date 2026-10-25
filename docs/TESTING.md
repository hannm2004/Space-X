# Danh mục test của Space-X

Cập nhật: 2026-10-25 (đếm bằng lệnh ở mục "Cách đếm lại"). Mỗi khi thêm, xóa hoặc đổi tên test, cập nhật file này trong cùng nhánh. Số liệu phải đếm bằng lệnh, không cộng nhẩm.

## Tổng quan

| | Số test | Số lớp/file | Cần Docker? |
|---|---|---|---|
| Backend (JUnit 5 + Spring Test) | 7 | 3 lớp | Không |
| Frontend (Vitest) | 10 | 2 file | Không |
| Test tích hợp với MySQL thật | 0 | – | Có |

CI chạy toàn bộ test trên mỗi Pull Request. **Không test nào gọi API AI thật**: dùng nhà cung cấp giả.

## Phân loại và quy ước

- **Unit test:** kiểm một hàm/lớp cô lập, không khởi động Spring, không mạng.
- **Slice test:** khởi động một phần Spring, ví dụ `@WebMvcTest` chỉ nạp tầng web.
- **Integration test:** Spring đầy đủ + MySQL 8.4 thật, tên lớp kết thúc bằng `IntegrationTest`. Cần MySQL thật vì tìm kiếm FULLTEXT không chạy trên H2. Thư viện Testcontainers phải được duyệt trước khi thêm.
- Viết test trước: RED → GREEN → REFACTOR.
- Tên test mô tả **hành vi** như một câu. Mỗi tính năng có ít nhất một trường hợp lỗi, không chỉ trường hợp thành công.

## Ma trận test bắt buộc

Mỗi dòng phải có test thật trước khi tuần đó được coi là xong. Trạng thái: `Planned` / `In progress` / `Implemented` / `Tested`.

| Yêu cầu | Loại | Tuần | Trạng thái |
|---|---|---|---|
| `/api/health` trả `UP`, không lộ phiên bản hay cấu hình | Slice | 2 | Tested |
| `/api/health` chỉ cho phép `GET` (`POST` bị từ chối 405) | Slice | 2 | Tested |
| Sáng/tối: ưu tiên lựa chọn đã lưu, lần đầu theo hệ điều hành, bỏ qua giá trị lưu sai | Unit (frontend) | 2 | Tested (logic); nút bấm và script chống nháy màu chỉ kiểm bằng mắt |
| Gọi `/api/health` ở frontend: thành công, lỗi HTTP, JSON sai dạng, mất kết nối | Unit (frontend) | 2 | Tested (`fetchHealth`); component `HealthStatus` chưa có test |
| Lỗi trả `problem+json`: 404, 405 và lỗi bất ngờ (500) không lộ stack trace hay nội dung exception | Slice | 3 | Tested |
| Flyway tạo đúng schema trên MySQL thật | Integration | 3 | Planned |
| Đăng ký: email trùng → 409; dữ liệu sai → 400; mật khẩu được băm, không lưu rõ | Slice + Unit | 4 | Planned |
| Đăng nhập: sai mật khẩu → 401; tài khoản bị khóa → 403 | Integration | 4 | Planned |
| Phân quyền: chưa đăng nhập → 401; Học sinh gọi API Admin → 403 | Slice | 4 | Planned |
| CSRF: thiếu token → 403 | Slice | 4 | Planned |
| Đổi mật khẩu: sai mật khẩu cũ → 400; tài khoản không có mật khẩu → 400 | Unit | 4 | Planned |
| Thông tin cá nhân: chỉ đọc/sửa được của chính mình | Slice | 4 | Planned |
| Khách xem được danh sách khóa và bài nhưng đọc nội dung bài → 401 | Slice | 5 | Planned |
| Học sinh không thấy bài Nháp (404) | Integration | 5 | Planned |
| Không xóa được khóa còn bài (409) | Unit | 5 | Planned |
| Ảnh: sai loại → 415; quá 1 MB → 413; đuôi `.png` nhưng nội dung không phải ảnh → 415 | Unit | 5 | Planned |
| Markdown của bài học không chạy được script (XSS) | Unit (frontend) | 5 | Planned |
| Tìm kiếm trả đúng bài với bộ câu hỏi mẫu | Integration | 6 | Planned |
| Tiến độ: chỉ của chính mình; `COMPLETED` không bị hạ về `IN_PROGRESS`; % chỉ tính bài Đã đăng; "Học lại" chỉ xóa đúng khóa đó | Unit + Integration | 6 | Planned |
| Quản lý người dùng: không tự khóa mình, không khóa Admin cuối cùng (409); tài khoản bị khóa không đăng nhập được | Unit | 6 | Planned |
| Dashboard đếm đúng | Integration | 7 | Planned |
| Đăng nhập Google: email chưa xác minh bị từ chối; email trùng đăng nhập vào tài khoản có sẵn; tài khoản mới có vai trò `LEARNER` | Unit | 7 | Planned |
| RAG: trích dẫn chỉ lấy từ đoạn đã truy xuất; số trích dẫn lạ bị loại | Unit | 8 | Planned |
| RAG: không đủ căn cứ → trả lời "không tìm thấy" | Unit | 8 | Planned |
| Nhà cung cấp AI lỗi / quá thời gian / hết hạn mức → 502 / 504 | Unit | 8 | Planned |
| Agent: giới hạn số vòng gọi tool, thời gian, kích thước | Unit | 9 | Planned |
| Agent: chuỗi prompt injection nằm trong nội dung bài không làm Agent đổi hành vi | Unit | 9 | Planned |
| Agent: chỉ có tool chỉ đọc; tool không trả bài Nháp | Unit | 9 | Planned |

## Cách chạy

Chạy riêng từng phần:

```powershell
# Backend (đứng ở backend/)
.\mvnw test

# Frontend (đứng ở frontend/)
npm test ; npm run lint ; npm run build
```

## Cách đếm lại

PowerShell, đứng ở thư mục gốc dự án:

```powershell
(Get-ChildItem backend\src\test -Recurse -Filter *.java | Select-String '^\s*@Test\b' | Measure-Object).Count
(Get-ChildItem frontend\src -Recurse -Include *.test.ts,*.test.tsx | Select-String '^\s*(it|test)\(' | Measure-Object).Count
```

Nếu số khác bảng "Tổng quan", sửa bảng cho khớp (không sửa test cho khớp bảng).
