# Space-X

Website học **Tin học lớp 10, 11, 12** (Chương trình GDPT 2018) bằng tiếng Việt cho học sinh THPT: học theo khóa và bài, lưu tiến độ học, chatbot trả lời có trích dẫn bài học, AI Agent gợi ý bài nên đọc.

> **Trạng thái (2026-10-22):** đã xong khung ứng dụng (Spring Boot + React đóng chung một ứng dụng), CI và deploy lần đầu lên Render: https://space-x-47f1.onrender.com (kiểm tra ngày 2026-10-22: `GET /api/health` trả `{"status":"UP"}`, HTTP 200). Gói Free tự ngủ khi lâu không có truy cập nên lần mở đầu có thể chậm 1–2 phút. **Chưa có** đăng nhập, bài học, chatbot, AI Agent. Tiến trình từng buổi làm việc: [current-state.md](current-state.md).

## Tính năng

Nhãn trạng thái: `Planned` / `In progress` / `Implemented` / `Tested` / `Deployed`.

| Nhóm | Tính năng | Người dùng | Trạng thái |
|---|---|---|---|
| Tài khoản | Đăng ký, đăng nhập, đăng xuất | Khách, Học sinh | Planned |
| | Đăng nhập bằng Google | Khách | Planned |
| | Xem/sửa thông tin cá nhân, đổi mật khẩu | Học sinh, Admin | Planned |
| | Giao diện sáng/tối (lần đầu theo hệ điều hành, nhớ lựa chọn) | Mọi người | Implemented (logic có 6 test; nút bấm kiểm bằng mắt) |
| Học tập | Xem danh sách khóa học và bài | Khách, Học sinh | Planned |
| | Đọc bài học, tìm kiếm bài | Học sinh | Planned |
| | Tiến độ học: hoàn thành bài, % theo khóa, học tiếp, học lại | Học sinh | Planned |
| | Chatbot RAG trả lời kèm trích dẫn bài học | Học sinh | Planned |
| | AI Agent gọi công cụ chỉ đọc để gợi ý bài | Học sinh | Planned |
| Quản trị | Dashboard thống kê | Admin | Planned |
| | Quản lý khóa học, bài học, ảnh bài học | Admin | Planned |
| | Quản lý người dùng: khóa/mở, đặt lại mật khẩu | Admin | Planned |
| | Dựng lại chỉ mục tìm kiếm | Admin | Planned |
| Hệ thống | Kiểm tra sống `GET /api/health`, hiện trạng thái trên trang chủ | Mọi người | Deployed (Render) |

Ngoài phạm vi bản đầu: đăng nhập Facebook (làm sau nếu còn thời gian), quiz, lưu lịch sử chat, quên mật khẩu qua email (Render Free chặn cổng gửi mail; thay bằng Admin đặt lại mật khẩu).

## Công nghệ

| Phần | Công nghệ |
|---|---|
| Backend | Java 21, Spring Boot, Spring Security, Spring Data JPA, Flyway, Maven |
| Frontend | React, TypeScript, Vite, Tailwind CSS — build rồi đóng chung vào Spring Boot (1 ứng dụng, 1 domain) |
| Database | MySQL 8.4 (Docker ở máy, Aiven ở production) |
| AI | Gemini API (tool calling); tìm kiếm bằng MySQL FULLTEXT |
| Triển khai | Docker, Render, GitHub Actions (build/test, gitleaks, CodeQL) |

## Lộ trình 12 tuần

| Tuần | Nội dung | Trạng thái |
|---|---|---|
| 1 | Use case, ERD, danh sách API; MySQL bằng Docker | Done |
| 2 | Khung Spring Boot + React, nút sáng/tối, Dockerfile, CI, deploy `/api/health` | Done |
| 3 | Database + Flyway, xử lý lỗi thống nhất, test tích hợp | Planned |
| 4 | Tài khoản: đăng ký, đăng nhập, đăng xuất, phân quyền, thông tin cá nhân, đổi mật khẩu | Planned |
| 5 | Khóa học, bài học, Admin quản lý bài và ảnh | Planned |
| 6 | Tìm kiếm, tiến độ học tập, quản lý người dùng | Planned |
| 7 | Admin Dashboard, đăng nhập Google, hoàn thiện giao diện; deploy bản chưa có AI | Planned |
| 8 | Chia đoạn, truy xuất, chatbot RAG | Planned |
| 9 | AI Agent, giới hạn vòng gọi, test prompt injection | Planned |
| 10 | Test toàn luồng, rà soát bảo mật, sửa lỗi; ngừng thêm tính năng | Planned |
| 11–12 | Hoàn thiện tài liệu, deploy bản cuối, tổng duyệt demo | Planned |

## Quy trình phát triển

- `main` luôn chạy được. Mỗi việc làm trên một nhánh riêng (`feat/...`, `fix/...`, `docs/...`, `ci/...`) và gộp vào `main` qua Pull Request khi CI xanh.
- Mỗi buổi làm việc được ghi lại trong [current-state.md](current-state.md).
- Quy tắc chi tiết: [CLAUDE.md](CLAUDE.md).

## Chạy ở máy

Yêu cầu: Docker Desktop, Java 21, Node 24. Dùng PowerShell, đứng ở thư mục gốc dự án.

```powershell
# Backend: chạy test, rồi chạy ứng dụng (cổng 8080)
cd backend
.\mvnw test
.\mvnw spring-boot:run          # kiểm tra: http://localhost:8080/api/health -> {"status":"UP"}

# Frontend (terminal khác): tự tải lại khi sửa; /api được chuyển sang backend cổng 8080
cd frontend
npm install
npm run dev
npm test ; npm run lint ; npm run build

# Chạy bản đóng gói như trên Render (giao diện + API cùng cổng 8080)
docker build -t spacex:local .
docker run --rm -p 8080:8080 --memory=512m spacex:local

# MySQL ở máy (chưa dùng trong ứng dụng; sẽ dùng từ Tuần 3)
Copy-Item .env.example .env     # rồi mở .env, đặt mật khẩu của bạn
docker compose up -d
docker compose ps               # chờ cột STATUS có "(healthy)"
```

## Tài liệu

- [current-state.md](current-state.md) — nhật ký phát triển theo ngày
- [docs/DATABASE_DESIGN.md](docs/DATABASE_DESIGN.md) — ERD, ràng buộc, quy tắc nghiệp vụ gắn với dữ liệu
- [docs/API.md](docs/API.md) — danh sách API và quyền
- [docs/TESTING.md](docs/TESTING.md) — danh mục test và ma trận test bắt buộc
- [CLAUDE.md](CLAUDE.md) — quy tắc làm việc (kể cả cách dùng AI hỗ trợ)
