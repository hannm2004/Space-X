# Space-X

[![CI](https://github.com/hannm2004/Space-X/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/hannm2004/Space-X/actions/workflows/ci.yml)

Website của **Trung tâm Tin học SPACE-X** (trung tâm hư cấu): nền tảng **tự học Tin học trực tuyến** bằng tiếng Việt. Đồ án chuyên ngành 11 tuần: danh mục khóa học, đọc và tìm bài, theo dõi tiến độ, trang quản trị; mua khóa, chatbot RAG và AI Agent chưa có, đang trong kế hoạch. Môi trường thử nghiệm, không thu tiền thật.

**Bản chạy thật:** https://space-x-47f1.onrender.com — gói Free tự ngủ, lần mở đầu có thể chậm 30–60 giây.

**Hiện tại (2026-11-27):** bản đã deploy có danh mục khóa theo nhóm chương trình (7 khóa, 31 bài), chưa có mua khóa, chatbot và Agent. Mã nguồn có 680 test backend và 607 test frontend.

## Tính năng

| Nhóm | Tính năng | Trạng thái |
|---|---|---|
| Tài khoản | Đăng ký, đăng nhập, đăng xuất; đăng nhập bằng Google | Deployed |
| | Hồ sơ cá nhân, đổi mật khẩu | Deployed |
| Khóa học | Danh mục theo nhóm chương trình; lọc theo trình độ, thời lượng, học phí | Deployed |
| Học tập | Danh sách bài của khóa, ảnh bìa khóa | Deployed |
| | Đọc bài (Markdown, bảng, ảnh), tìm kiếm bài | Deployed |
| | Bài trước/sau, đường dẫn, thời gian đọc, số bài của khóa | Tested |
| | Tiến độ học: hoàn thành bài, phần trăm theo khóa, học tiếp, học lại | Deployed |
| | Chatbot RAG trả lời kèm trích dẫn | Planned |
| | AI Agent gọi công cụ chỉ đọc để gợi ý khóa và bài | Planned |
| Mua khóa | Khóa miễn phí và trả phí, ghi danh, "Khóa học của tôi" | Planned |
| | Đơn hàng, chuyển khoản VietQR do Admin xác nhận (thử nghiệm) | Planned |
| | VNPay sandbox | Planned |
| Quản trị | Dashboard thống kê | Deployed |
| | Quản lý khóa học (trạng thái, nhóm, trình độ, số giờ, học phí), bài học, ảnh và ảnh bìa | Deployed |
| | Quản lý người dùng: tìm, khóa/mở, đặt lại mật khẩu | Deployed |
| | Dựng lại chỉ mục tìm kiếm | Planned |
| Hệ thống | Giao diện sáng/tối, kiểm tra sống `GET /api/health` | Deployed |

Nhãn: `Planned` → `In progress` → `Implemented` → `Tested` → `Deployed`. Ngoài phạm vi: lớp học và lịch khai giảng, giữ chỗ, chứng nhận, mã giảm giá, blog, MoMo, ZaloPay, đăng nhập Facebook. Thanh toán chỉ chạy thử nghiệm, không thu tiền thật.

## Công nghệ

| Phần | Công nghệ |
|---|---|
| Backend | Java 21, Spring Boot, Spring Security, Spring Data JPA, Flyway, Maven |
| Frontend | React, TypeScript, Vite, Tailwind CSS (build rồi đóng chung vào Spring Boot: 1 ứng dụng, 1 địa chỉ) |
| Database | MySQL 8.4 (Docker ở máy, Aiven ở production); tìm kiếm bằng FULLTEXT |
| Xác thực | Session cookie + CSRF; email và mật khẩu hoặc Google (OAuth2); vai trò `LEARNER`, `ADMIN` |
| AI | Gemini API (tool calling) |
| Triển khai | Docker, Render, GitHub Actions (build/test, gitleaks, CodeQL) |

## Lộ trình

| Tuần | Nội dung | Trạng thái |
|---|---|---|
| 1–2 | Use case, ERD, API; khung Spring Boot + React, Dockerfile, CI, deploy | Done |
| 3 | Database + Flyway, xử lý lỗi, test tích hợp; tài khoản và phân quyền | Done |
| 4 | API khóa học, bài học; Admin quản lý bài và ảnh; dữ liệu mẫu | Done |
| 5 | Giao diện đọc bài và tài khoản, tìm kiếm, tiến độ học, quản lý người dùng | Done |
| 6 | Dashboard, đăng nhập Google, giao diện quản trị, thiết kế lại giao diện, nội dung lớp 6–12, ảnh bìa | Done |
| 7 | Chuyển sang Trung tâm Tin học SPACE-X, danh mục khóa; bản chạy được tối thiểu của mua khóa và chatbot (đến 29/11) | In progress |
| 8 | Làm kỹ ghi danh, đơn hàng, thanh toán thử nghiệm, hoàn tiền, email | Planned |
| 9 | Module bài học, danh mục và trang khóa, chatbot RAG | Planned |
| 10 | AI Agent, VNPay sandbox, doanh thu trên dashboard | Planned |
| 11 | Dừng thêm tính năng: kiểm thử, rà soát bảo mật, báo cáo | Planned |

## Chạy ở máy

Yêu cầu: Docker Desktop, Java 21, Node 24. Dùng PowerShell, đứng ở thư mục gốc dự án.

```powershell
# 1. Cấu hình và bật MySQL
Copy-Item .env.example .env      # mở .env, đặt mật khẩu; DB_USERNAME, DB_PASSWORD phải trùng MYSQL_USER, MYSQL_PASSWORD
docker compose up -d             # chờ cột STATUS của "docker compose ps" có "(healthy)"

# 2. Backend (cổng 8080)
cd src\backend
.\mvnw spring-boot:run           # kiểm tra: http://localhost:8080/api/health -> {"status":"UP"}

# 3. Frontend (terminal khác; /api được chuyển sang cổng 8080)
cd src\frontend
npm install
npm run dev                      # http://localhost:5173

# 4. Nạp dữ liệu mẫu (7 khóa, 31 bài) khi backend đang chạy
.\scripts\seed-sample-data.ps1 -FromEnv -DryRun   # xem trước, chưa tạo gì
.\scripts\seed-sample-data.ps1 -FromEnv           # nạp thật
```

Đăng nhập Google chỉ bật khi `.env` có `GOOGLE_CLIENT_ID` và `GOOGLE_CLIENT_SECRET`. Script nạp chạy lại an toàn (khóa và bài đã có theo `slug` thì bỏ qua); muốn nạp lên bản deploy thì thêm `-BaseUrl https://<địa-chỉ>` và gõ `yes` khi được hỏi.

**Kiểm thử**

```powershell
cd src\backend ; .\mvnw test                      # test tích hợp tự bật MySQL bằng Testcontainers, cần Docker Desktop
cd src\frontend ; npm test ; npm run lint ; npm run build
```

Máy ít RAM nên chạy hai đợt: `.\mvnw test "-Dtest=!*IntegrationTest"` rồi `.\mvnw test "-Dtest=*IntegrationTest"`.

## Quy trình phát triển

- `main` luôn chạy được; mỗi việc làm trên một nhánh `feat/...` hoặc `fix/...`, mở Pull Request, chờ CI xanh rồi gộp.
- Commit nhỏ, theo Conventional Commits. Không đưa mật khẩu, khóa API hay dữ liệu cá nhân lên repo công khai.
- Mỗi buổi làm việc ghi trong [current-state.md](current-state.md). Quy tắc chi tiết: [CLAUDE.md](CLAUDE.md).

## Tài liệu

- [docs/API.md](docs/API.md) — danh sách API và quyền
- [docs/DATABASE_DESIGN.md](docs/DATABASE_DESIGN.md) — ERD, ràng buộc, quy tắc nghiệp vụ
- [docs/TESTING.md](docs/TESTING.md) — danh mục test và số liệu kiểm thử
- [current-state.md](current-state.md) — nhật ký phát triển theo ngày
