# Nhật ký phát triển Space-X

Ghi lại từng buổi làm việc, **mục mới nhất ở trên cùng**. Chỉ ghi những gì đã làm và đã kiểm tra thật.

Mẫu một mục:

```markdown
## YYYY-MM-DD — Tiêu đề ngắn
- **Nhánh:** `feat/...`
- **Đã làm:** ...
- **Kiểm tra:** lệnh đã chạy và kết quả (ví dụ `.\mvnw test`: 3 test qua)
- **Lỗi gặp và cách sửa:** ...
- **Học được:** ...
- **Tiếp theo:** ...
```

---

## 2026-10-21 — Khung backend Spring Boot và endpoint /api/health
- **Nhánh:** `feat/backend-skeleton`
- **Đã làm:**
  - Sinh dự án Spring Boot 4.1.1 (Maven, Java 21, chỉ thêm Spring Web) bằng start.spring.io, đặt trong `backend/`.
  - Viết 3 test cho `GET /api/health` trước khi viết code: trả 200 và `{"status":"UP"}`; JSON chỉ có đúng 1 trường; `POST` bị từ chối (405).
  - Thêm `HealthController` và `HealthResponse` (record) để test xanh.
  - Ba commit theo thứ tự: `chore` (khung) → `test` (đỏ) → `feat` (xanh).
- **Kiểm tra:**
  - `.\mvnw test` trên khung mới: 1 test (`contextLoads`) qua.
  - Sau khi viết test: 3 test fail, đều do trả 404 vì chưa có controller.
  - Sau khi thêm controller: `Tests run: 4, Failures: 0, Errors: 0`, `BUILD SUCCESS`.
- **Lỗi gặp và cách sửa:**
  - `.\mvnw test` báo thiếu `.mvn\wrapper\maven-wrapper.properties`: thư mục `.mvn` bị ẩn nên lần chép đầu bị sót. Chép lại bằng `robocopy` từ thư mục giải nén thì hết lỗi.
  - `backend/mvnw` được Git lưu ở chế độ `100644`, CI chạy Linux sẽ báo `Permission denied`. Sửa bằng `git update-index --chmod=+x backend/mvnw`, đổi thành `100755`.
  - Test đầu viết `get("api/health")` thiếu dấu `/` ở đầu, sửa thành `get("/api/health")`.
- **Học được:**
  - TDD: phải thấy test đỏ trước, để biết test thật sự kiểm tra được điều cần kiểm.
  - Gửi `POST` vào đường dẫn chỉ có `GET` thì Spring trả 405; đường dẫn chưa tồn tại thì trả 404.
  - Maven Wrapper (`mvnw`, `.mvn`) giúp máy nào cũng build đúng một phiên bản Maven mà không cần cài sẵn.
- **Tiếp theo:** chạy thử thật bằng `.\mvnw spring-boot:run` và `curl.exe -i http://localhost:8080/api/health`, mở Pull Request, rồi làm `feat/frontend-skeleton`.

## 2026-10-21 — Chạy MySQL 8.4 bằng Docker Compose
- **Nhánh:** `feat/mysql-docker` (Pull Request #1, đã gộp vào `main`)
- **Đã làm:**
  - Viết `docker-compose.yml`: image `mysql:8.4`, mật khẩu và tên database lấy từ `.env`, bảng mã `utf8mb4` với collation `utf8mb4_0900_ai_ci`, cổng chỉ mở trên `127.0.0.1:3306`, volume `mysql-data`, healthcheck.
  - Viết `.env.example` chỉ chứa tên biến và giá trị giả; `.env` thật bị `.gitignore` chặn.
- **Kiểm tra:**
  - `docker compose config --quiet`: không báo lỗi cú pháp.
  - `git check-ignore -v .env .env.example`: `.env` bị chặn, `.env.example` được phép đưa lên.
  - `docker compose ps`: `healthy`.
  - DBeaver: `SELECT VERSION(), @@character_set_server, @@collation_server;` trả `8.4.11`, `utf8mb4`, `utf8mb4_0900_ai_ci`.
- **Lỗi gặp và cách sửa:** không có. Máy đã có volume cũ `space-x_spacex-mysql-data` nhưng tên khác volume mới `space-x_mysql-data`, nên MySQL mới khởi tạo trống, không dùng lại dữ liệu cũ.
- **Học được:**
  - Image là cái khuôn, container là bản đang chạy; volume giữ dữ liệu khi container bị xóa.
  - Trong healthcheck phải viết `$$` để Compose không tự thay biến, mà để container tự đọc.
  - MySQL chỉ đọc các biến `MYSQL_*` ở lần chạy đầu trên volume trống; đổi mật khẩu trong `.env` sau đó không có tác dụng nếu không xóa volume.
  - Sau khi gộp PR thì xóa nhánh: commit đã nằm trong `main`, nhánh chỉ là cái nhãn.
- **Tiếp theo:** khung backend Spring Boot.

## 2026-10-21 — Làm lại dự án từ đầu, chốt use case và ERD
- **Nhánh:** `main` (commit khởi tạo)
- **Đã làm:**
  - Đổi nội dung từ lập trình (Java/SQL/Cấu trúc dữ liệu) sang **Tin học lớp 10, 11, 12**.
  - Tạo lại repo, làm theo quy trình nhánh + Pull Request, không dùng tag.
  - Chốt 23 use case: tài khoản (email và Google), thông tin cá nhân, đổi mật khẩu, khóa học và bài học, tiến độ học tập, chatbot RAG, AI Agent, Admin Dashboard, quản lý khóa học / bài học / ảnh / người dùng.
  - Chốt ERD 6 bảng: `users`, `courses`, `lessons`, `lesson_images`, `lesson_progress`, `knowledge_chunks`.
  - Viết lại `README.md`, `CLAUDE.md`, `docs/API.md`, `docs/DATABASE_DESIGN.md`, `docs/TESTING.md`.
- **Kiểm tra:** chưa có mã nguồn nên chưa có test.
- **Quyết định:** nội dung 2 tầng khóa học → bài học; ảnh bài học lưu trong MySQL vì Render Free không giữ file trên đĩa; đăng nhập Facebook để sau bản đầu.
- **Tiếp theo:** viết `docker-compose.yml` cho MySQL, rồi khung Spring Boot trên nhánh `feat/backend-skeleton`.
