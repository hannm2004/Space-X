# Space-X

Website học **Tin học lớp 10, 11, 12** (Chương trình GDPT 2018) bằng tiếng Việt cho học sinh THPT: học theo khóa và bài, lưu tiến độ học, chatbot trả lời có trích dẫn bài học, AI Agent gợi ý bài nên đọc.

> **Trạng thái (2026-11-09):** xong Tuần 4 về mặt mã (các việc 4.1 đến 4.6 đã gộp qua Pull Request #19 đến #24, CI xanh). **Backend** đã có tài khoản (đăng ký, đăng nhập, đăng xuất, thông tin cá nhân, đổi mật khẩu), phân quyền `LEARNER`/`ADMIN`, CSRF, API đọc khóa học và bài học, API Admin quản lý khóa học, bài học và ảnh bài học, cùng dữ liệu mẫu 3 khóa / 6 bài và script nạp. Test: 399 backend (273 không cần Docker + 126 tích hợp trên MySQL thật) và 210 frontend. Đã deploy lên Render: https://space-x-47f1.onrender.com (kiểm tra ngày 2026-11-09: `GET /api/courses` trả 200 với 3 khóa mẫu, mỗi khóa 2 bài đã đăng, sau khi nạp dữ liệu mẫu bằng script). Gói Free tự ngủ khi lâu không có truy cập nên lần mở đầu có thể chậm 1–2 phút. **Chưa có:** giao diện đăng nhập và quản trị (frontend có routing, API client tự gắn CSRF, trang chủ kèm danh sách khóa, trang khóa, trang đọc bài và trang 404), đăng nhập Google, tìm kiếm, tiến độ học, chatbot, AI Agent. Tiến trình từng buổi làm việc: [current-state.md](current-state.md).

## Tính năng

Nhãn trạng thái: `Planned` / `In progress` / `Implemented` / `Tested` / `Deployed`.

| Nhóm | Tính năng | Người dùng | Trạng thái |
|---|---|---|---|
| Tài khoản | Đăng ký, đăng nhập, đăng xuất | Khách, Học sinh | Tested (API và giao diện; đã thử thật ở máy bằng Chrome và `curl`; chưa thử trên Render với cookie `Secure`) |
| | Đăng nhập bằng Google | Khách | Planned |
| | Xem/sửa thông tin cá nhân, đổi mật khẩu | Học sinh, Admin | Tested (API đủ; giao diện mới có đổi mật khẩu, chưa có xem/sửa thông tin cá nhân). Đổi mật khẩu hủy các session khác (D28) |
| | Giao diện sáng/tối (lần đầu theo hệ điều hành, nhớ lựa chọn) | Mọi người | Implemented (logic có 6 test; nút bấm kiểm bằng mắt) |
| Học tập | Xem danh sách khóa học và bài | Khách, Học sinh | Tested (API và giao diện; giao diện đã xem thật trên trình duyệt ở máy, chưa deploy) |
| | Đọc bài học (Markdown, bảng, ảnh của hệ thống) | Học sinh | Tested (API và giao diện; đọc bài cần đăng nhập, có trang đăng nhập; ảnh trong bài mới có test, chưa xem thật) |
| | Tìm kiếm bài | Học sinh | Tested (API FULLTEXT và giao diện; ô tìm ở thanh đầu trang đã thử tay trên Chrome ngày 2026-11-12, chưa thử điện thoại thật, chưa deploy). Từ chỉ khác dấu được coi là một từ; từ ngắn hơn 3 ký tự bị bỏ qua |
| | Tiến độ học: hoàn thành bài, % theo khóa, học tiếp, học lại | Học sinh | Planned |
| | Chatbot RAG trả lời kèm trích dẫn bài học | Học sinh | Planned |
| | AI Agent gọi công cụ chỉ đọc để gợi ý bài | Học sinh | Planned |
| Quản trị | Dashboard thống kê | Admin | Planned |
| | Quản lý khóa học, bài học, ảnh bài học | Admin | Tested (chỉ API; chưa có giao diện; tạo khóa, tạo bài và đăng bài đã chạy thật trên bản deploy qua script nạp; ảnh chưa kiểm trên bản deploy) |
| | Dữ liệu mẫu 3 khóa / 6 bài và script nạp qua API Admin | Admin | Deployed (đã nạp lên bản deploy ngày 2026-11-09; chạy lại lần hai không tạo trùng) |
| | Quản lý người dùng: khóa/mở, đặt lại mật khẩu | Admin | Planned |
| | Dựng lại chỉ mục tìm kiếm | Admin | Planned |
| Hệ thống | Kiểm tra sống `GET /api/health`, hiện trạng thái trên trang chủ | Mọi người | Deployed (Render) |

Ngoài phạm vi bản đầu: đăng nhập Facebook và thanh toán gói Premium mở thêm lượt hỏi AI (làm sau nếu còn thời gian; thanh toán chỉ ở môi trường thử, không thu tiền thật), quiz, lưu lịch sử chat, quên mật khẩu qua email (Render Free chặn cổng gửi mail; thay bằng Admin đặt lại mật khẩu).

## Công nghệ

| Phần | Công nghệ |
|---|---|
| Backend | Java 21, Spring Boot, Spring Security, Spring Data JPA, Flyway, Maven |
| Frontend | React, TypeScript, Vite, Tailwind CSS — build rồi đóng chung vào Spring Boot (1 ứng dụng, 1 domain) |
| Database | MySQL 8.4 (Docker ở máy, Aiven ở production) |
| AI | Gemini API (tool calling); tìm kiếm bằng MySQL FULLTEXT |
| Triển khai | Docker, Render, GitHub Actions (build/test, gitleaks, CodeQL) |

## Lộ trình 11 tuần

| Tuần | Nội dung | Trạng thái |
|---|---|---|
| 1 | Use case, ERD, danh sách API; MySQL bằng Docker | Done |
| 2 | Khung Spring Boot + React, nút sáng/tối, Dockerfile, CI, deploy `/api/health` | Done |
| 3 | Database + Flyway, xử lý lỗi thống nhất, test tích hợp; tài khoản (đăng ký, đăng nhập, đăng xuất), phân quyền, thông tin cá nhân, đổi mật khẩu | Done |
| 4 | API khóa học và bài học, Admin quản lý khóa, bài và ảnh, dữ liệu mẫu | Done |
| 5 | Giao diện đọc bài và đăng ký/đăng nhập (React), tìm kiếm, tiến độ học tập, quản lý người dùng | In progress (CP5.0 đến CP5.4 xong) |
| 6 | Admin Dashboard, đăng nhập Google, hoàn thiện giao diện; deploy bản chưa có AI | Planned |
| 7 | Chia đoạn, truy xuất, chatbot RAG | Planned |
| 8 | AI Agent, giới hạn vòng gọi, test prompt injection | Planned |
| 9 | Test toàn luồng, rà soát bảo mật, sửa lỗi; ngừng thêm tính năng | Planned |
| 10–11 | Hoàn thiện tài liệu, deploy bản cuối, tổng duyệt demo | Planned |

## Quy trình phát triển

- `main` luôn chạy được. Mỗi việc làm trên một nhánh riêng (`feat/...`, `fix/...`, `docs/...`, `ci/...`) và gộp vào `main` qua Pull Request khi CI xanh.
- Mỗi buổi làm việc được ghi lại trong [current-state.md](current-state.md).
- Quy tắc chi tiết: [CLAUDE.md](CLAUDE.md).

## Chạy ở máy

Yêu cầu: Docker Desktop, Java 21, Node 24. Dùng PowerShell, đứng ở thư mục gốc dự án.

```powershell
# Backend: chạy test, rồi chạy ứng dụng (cổng 8080)
# Từ Tuần 3 ứng dụng cần MySQL đang chạy và file .env có DB_URL, DB_USERNAME, DB_PASSWORD (xem mục MySQL bên dưới).
# Test tích hợp tự bật MySQL bằng Testcontainers, nên chỉ cần Docker Desktop đang chạy.
cd src\backend
.\mvnw test
.\mvnw spring-boot:run          # kiểm tra: http://localhost:8080/api/health -> {"status":"UP"}

# Frontend (terminal khác): tự tải lại khi sửa; /api được chuyển sang backend cổng 8080
cd src\frontend
npm install
npm run dev
npm test ; npm run lint ; npm run build

# Chạy bản đóng gói như trên Render (giao diện + API cùng cổng 8080)
# Từ Tuần 3 container cũng cần các biến DB_*; cách trỏ container tới MySQL ở máy chưa được kiểm lại.
docker build -t spacex:local .
docker run --rm -p 8080:8080 --memory=512m spacex:local

# MySQL ở máy (backend dùng từ Tuần 3)
Copy-Item .env.example .env     # rồi mở .env, đặt mật khẩu của bạn; DB_USERNAME, DB_PASSWORD phải trùng MYSQL_USER, MYSQL_PASSWORD
docker compose up -d
docker compose ps               # chờ cột STATUS có "(healthy)"

# Dữ liệu mẫu: 3 khóa Tin học 10/11/12 và 6 bài mẫu (thư mục seed/), nạp qua API Admin khi ứng dụng đang chạy.
# Chạy lại nhiều lần an toàn: khóa/bài đã có (theo slug) bị bỏ qua, không ghi đè bài đã sửa.
.\scripts\seed-sample-data.ps1 -FromEnv -DryRun   # xem trước, chưa tạo gì (-FromEnv: lấy ADMIN_EMAIL/ADMIN_PASSWORD từ .env)
.\scripts\seed-sample-data.ps1 -FromEnv           # nạp vào localhost:8080
.\scripts\seed-sample-data.ps1 -BaseUrl https://<địa-chỉ-bản-deploy>   # nạp vào bản deploy: hỏi email/mật khẩu và đòi gõ "yes"
# Sau khi nạp, kiểm tra: curl.exe https://<địa-chỉ-bản-deploy>/api/courses  -> phải ra 3 khóa, không phải []
```

## Tài liệu

- [current-state.md](current-state.md) — nhật ký phát triển theo ngày
- [docs/DATABASE_DESIGN.md](docs/DATABASE_DESIGN.md) — ERD, ràng buộc, quy tắc nghiệp vụ gắn với dữ liệu
- [docs/API.md](docs/API.md) — danh sách API và quyền
- [docs/TESTING.md](docs/TESTING.md) — danh mục test và ma trận test bắt buộc
- [CLAUDE.md](CLAUDE.md) — quy tắc làm việc (kể cả cách dùng AI hỗ trợ)
