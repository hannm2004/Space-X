# Space-X

Website học **Tin học từ lớp 6 đến lớp 12** (Chương trình GDPT 2018) bằng tiếng Việt cho học sinh THCS và THPT: học theo khóa và bài, lưu tiến độ học, chatbot trả lời có trích dẫn bài học, AI Agent gợi ý bài nên đọc.

> **Trạng thái (2026-11-19):** Tuần 6 đã xong phần chức năng: Admin Dashboard, đăng nhập Google, trang Hồ sơ và giao diện Admin quản lý khóa học, bài học, ảnh (PR #39 đến #44; CI xanh và deploy thành công theo người dùng báo). **Đăng nhập Google đã thử thật trên production ngày 2026-11-19:** đăng nhập thành công và về trang chủ; `/ho-so` hiện "Đăng nhập bằng Google" và không có Đổi mật khẩu; F5 vẫn giữ phiên; cookie `SESSION` và `XSRF-TOKEN` đều có cờ `Secure`. Test hiện tại (chạy đủ ngày 2026-11-20): 608 backend (430 không cần Docker + 178 tích hợp trên MySQL thật) và 530 frontend. **Đã gộp vào `main` và deploy (PR #46; CI xanh và deploy thành công theo người dùng báo):** giao diện làm lại (hệ màu, trang chủ chia khóa theo cấp THCS/THPT, câu hỏi thường gặp, chân trang, ba trang chính sách), nội dung Tin học lớp 6 đến 12 (7 khóa, 31 bài trong `seed/`), ảnh bìa bài học. Người dùng đã nạp dữ liệu mẫu lên production; kiểm bằng API ngày 2026-11-19: đủ 7 khóa và 31 bài (lớp 6 đến 9 mỗi khóa 4 bài, lớp 10 đến 12 mỗi khóa 5 bài). **Mới trên nhánh `feat/ui-nut-bam` (chưa deploy, chưa mở PR):** **ảnh bìa khóa học** (bảng mới `course_covers`, migration `V5`; ảnh bìa khóa công khai nên khách thấy ở trang chủ; khóa chưa có ảnh thì hiện hình mặc định theo lớp; Admin tải, đổi, bỏ ảnh ở trang quản lý khóa học), nút **Chọn ảnh từ máy** thay ô chọn tệp mặc định của trình duyệt, chuẩn hóa nút bấm (một nguồn kiểu nút `buttons.ts`, cỡ chạm tối thiểu trên điện thoại, nút Google dùng `btn-neutral`, liên kết điều hướng đứng riêng thành nút) và liên kết "Ảnh" ở danh sách bài học Admin trỏ thẳng tới mục tải ảnh. Mới kiểm bằng trình duyệt ở máy, chưa kiểm trên production. Chi tiết dưới đây là phần đã kiểm đến cuối Tuần 5 (2026-11-15). Tuần 5 đã gộp vào `main` (PR #34 đến #37; CI xanh và deploy thành công theo người dùng báo cho PR #34, #35). **Backend** có tài khoản (đăng ký, đăng nhập, đăng xuất, thông tin cá nhân, đổi mật khẩu), phân quyền `LEARNER`/`ADMIN`, CSRF, API đọc khóa học và bài học, tìm kiếm bài, tiến độ học, API Admin quản lý khóa học, bài học, ảnh bài học và người dùng (khóa/mở khóa, đặt lại mật khẩu), cùng dữ liệu mẫu 3 khóa / 6 bài và script nạp. **Frontend** có trang chủ, khóa học, đọc bài, tìm kiếm, đăng ký/đăng nhập, đổi mật khẩu, tiến độ học và trang Admin quản lý người dùng. Test: 495 backend (345 không cần Docker + 150 tích hợp trên MySQL thật) và 295 frontend. Đã deploy lên Render: https://space-x-47f1.onrender.com. Kiểm ngày 2026-11-13 bằng script gọi API thật trên production: đăng ký, đăng nhập (cookie `SESSION` và `XSRF-TOKEN` có cờ `Secure`), đọc bài, tìm kiếm, tiến độ học và toàn bộ API Admin người dùng (48 kiểm tra đạt, tài khoản thử đã khóa); bản build chứa trang Admin đã lên (file JS có route `quan-tri/nguoi-dung`). **Chưa kiểm trên production:** thao tác bằng trình duyệt (giao diện), điện thoại thật, học sinh đọc bài nháp trên production (đã kiểm ở máy). API Admin khóa học/bài học/ảnh đã kiểm bằng script trên production, 38/38 đạt, dữ liệu thử đã xóa. Gói Free tự ngủ khi lâu không có truy cập nên lần mở đầu có thể chậm 1–2 phút. **Chưa có:** chatbot, AI Agent. Tiến trình từng buổi làm việc: [current-state.md](current-state.md).

## Tính năng

Nhãn trạng thái: `Planned` / `In progress` / `Implemented` / `Tested` / `Deployed`.

| Nhóm | Tính năng | Người dùng | Trạng thái |
|---|---|---|---|
| Tài khoản | Đăng ký, đăng nhập, đăng xuất | Khách, Học sinh | Tested (API và giao diện; đã thử thật ở máy bằng Chrome và `curl`; trên production ngày 2026-11-13 qua script API: đăng ký, đăng ký trùng → 409, đăng nhập, cookie `SESSION` và `XSRF-TOKEN` có cờ `Secure`; chưa bấm giao diện trên production) |
| | Đăng nhập bằng Google | Khách | Deployed (đã thử thật trên production ngày 2026-11-19: đăng nhập thành công, về trang chủ, F5 vẫn giữ phiên, cookie `SESSION` và `XSRF-TOKEN` có cờ `Secure`; chỉ nhận email đã xác minh; email thuộc tài khoản mật khẩu bị từ chối và tài khoản Google bị khóa không vào được, cả hai đã thử tay trên production cùng ngày) |
| | Xem/sửa thông tin cá nhân, đổi mật khẩu | Học sinh, Admin | Deployed (trang `/ho-so` xem và sửa tên hiển thị, tài khoản Google không có Đổi mật khẩu; đã xem trên production ngày 2026-11-19 với tài khoản Google). Đổi mật khẩu hủy các session khác (D28) |
| | Giao diện sáng/tối (lần đầu theo hệ điều hành, nhớ lựa chọn) | Mọi người | Implemented (logic có 6 test; nút bấm kiểm bằng mắt) |
| Học tập | Xem danh sách khóa học và bài | Khách, Học sinh | Tested (API và giao diện; giao diện đã xem thật trên trình duyệt ở máy; API đã kiểm trên production ngày 2026-11-13: `/api/courses` ra 3 khóa; chưa bấm giao diện trên production) |
| | Đọc bài học (Markdown, bảng, ảnh của hệ thống) | Học sinh | Tested (API và giao diện; đọc bài cần đăng nhập, có trang đăng nhập; ảnh trong bài đã xem thật ở máy ngày 2026-11-13: ảnh lưu trong MySQL hiển thị đúng, kèm bảng, ở cỡ điện thoại giả lập 375×667; API đọc bài và API ảnh đã kiểm trên production ngày 2026-11-13; chưa xem ảnh hiển thị trong giao diện trên production) |
| | Tìm kiếm bài | Học sinh | Tested (API FULLTEXT và giao diện; ô tìm ở thanh đầu trang đã thử tay trên Chrome ngày 2026-11-12; API đã kiểm trên production ngày 2026-11-13; chưa thử điện thoại thật). Từ chỉ khác dấu được coi là một từ; từ ngắn hơn 3 ký tự bị bỏ qua |
| | Tiến độ học: hoàn thành bài, % theo khóa, học tiếp, học lại | Học sinh | Tested (API đã gộp qua PR #32; giao diện đã thử tay trên Chrome ngày 2026-11-12; API đã kiểm trên production ngày 2026-11-13: hoàn thành bài → 50%, học lại → 0%; chưa thử điện thoại thật) |
| | Chatbot RAG trả lời kèm trích dẫn bài học | Học sinh | Planned |
| | AI Agent gọi công cụ chỉ đọc để gợi ý bài | Học sinh | Planned |
| Quản trị | Dashboard thống kê | Admin | Deployed (PR #39; CI xanh và deploy thành công theo người dùng báo 2026-11-16; đã xem trang `/quan-tri` trên production bằng tài khoản Admin ngày 2026-11-19) |
| | Quản lý khóa học, bài học, ảnh bài học; đặt ảnh bìa cho bài | Admin | Deployed (API và giao diện `/quan-tri/khoa-hoc`, `/quan-tri/bai-hoc` từ PR #44, đã thử trên Chrome ở máy và trên production ngày 2026-11-19: tạo, đăng, tải ảnh, xóa bài). Kiểm API: ngày 2026-11-13 kiểm đầy đủ tạo/sửa/xóa khóa, bài, ảnh, đăng/ẩn bài, quyền học sinh và khách bằng script: 42/42 đạt trên backend ở máy và 38/38 đạt trên production sau khi người dùng cho phép dùng tài khoản Admin; dữ liệu thử đã xóa, số khóa công khai trước và sau đều là 3; ảnh bìa bài học (D33, migration V4) mới viết, chưa deploy, đã thử ở máy) |
| | Dữ liệu mẫu 7 khóa (Tin học lớp 6 đến 12) / 31 bài và script nạp qua API Admin | Admin | Implemented (31 bài trong `seed/`, đã kiểm tính nhất quán; 3 khóa / 6 bài đầu đã nạp lên bản deploy ngày 2026-11-09, các khóa và bài mới chưa nạp lên production; chạy lại script không tạo trùng) |
| | Quản lý người dùng: danh sách, tìm, khóa/mở, đặt lại mật khẩu | Admin | Tested (API và giao diện; khóa hoặc đặt lại mật khẩu thì mọi session của người đó bị xóa; không tự khóa mình, không khóa Admin cuối cùng; đã thử tay trên Chrome ngày 2026-11-13 với backend và MySQL ở máy; API đã kiểm trên production ngày 2026-11-13 bằng script gọi API thật (48 kiểm tra đạt, chi tiết ở `docs/TESTING.md` mục "Kiểm luồng trên production"); chưa bấm giao diện trên production; đã xem ở cỡ điện thoại giả lập 375×667, chưa thử điện thoại thật) |
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
| 5 | Giao diện đọc bài và đăng ký/đăng nhập (React), tìm kiếm, tiến độ học tập, quản lý người dùng | Done (CP5.0 đến CP5.7 đã gộp vào `main`; còn nợ: bấm giao diện trên production, điện thoại thật) |
| 6 | Admin Dashboard, đăng nhập Google, hoàn thiện giao diện; deploy bản chưa có AI | Done (CP6.1 đến CP6.5; đăng nhập Google, trang Admin và điện thoại thật đã thử trên production 2026-11-19; còn nợ: hủy ở màn hình Google, làm lại giao diện) |
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
