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

## 2026-10-22 — Dockerfile nhiều giai đoạn, đóng giao diện React vào JAR
- **Nhánh:** `feat/docker`
- **Đã làm:**
  - Viết `Dockerfile` 3 giai đoạn: (1) Node 24 build giao diện ra `dist/`; (2) JDK 21 + Maven chép `dist/` vào `static/` rồi đóng JAR; (3) ảnh chạy chỉ có JRE, chạy bằng user thường `spacex`. JVM chạy với `MaxRAMPercentage=70`, `SerialGC`, `TieredStopAtLevel=1`; cổng lấy từ biến `PORT` (mặc định 8080).
  - Viết `.dockerignore` chặn `.env`, `nop-bai`, `docs`, `spikes`, `.git`, `node_modules`, `target` khỏi ngữ cảnh build.
- **Kiểm tra:**
  - `docker build -t spacex:local .`: thành công.
  - `docker run --memory=512m -p 8080:8080 spacex:local`: `GET /api/health` trả `200` và `{"status":"UP"}`; `GET /` trả `200`, trang có chữ `Space-X` và script chống nháy màu, cùng cổng 8080.
  - `docker stats`: 92,44 MiB / 512 MiB; log `Started SpacexApplication in 3.81 seconds`. Container chỉ bị giới hạn bộ nhớ, không giới hạn CPU, nên chưa phản ánh Render Free.
  - Mở `http://localhost:8080` bằng trình duyệt: nút sáng/tối đổi màu được.
  - Chưa kiểm: tải lại trang giữ lựa chọn, không nháy màu, khối trạng thái hệ thống, độ tương phản bằng công cụ.
- **Lỗi gặp và cách sửa:** `docker run --name spacex-test` báo trùng tên vì máy còn container cũ từ dự án trước. Không xóa container cũ mà chạy bằng tên khác (`spacex-dockercheck`).
- **Học được:**
  - Chia nhiều giai đoạn để ảnh cuối chỉ có JRE và JAR, không mang theo Node, Maven hay mã nguồn.
  - Chép `pom.xml` và tải thư viện trước khi chép `src`, để sửa code không làm tải lại thư viện (tận dụng tầng cache của Docker).
  - `.dockerignore` giữ `.env` ngoài ảnh Docker, nên mật khẩu không lọt vào ảnh.
  - Spring Boot tự phục vụ file trong `static/` ở địa chỉ `/`, nên một ứng dụng phục vụ cả giao diện lẫn API.
- **Tiếp theo:** mở Pull Request, rồi làm `ci/github-actions`.

## 2026-10-22 — Khung frontend React, nút sáng/tối và hiện trạng thái hệ thống
- **Nhánh:** `feat/frontend-skeleton`
- **Đã làm:**
  - Tạo dự án Vite + React + TypeScript (dùng Oxlint), thêm Tailwind CSS 4 và Vitest.
  - Viết logic sáng/tối thành các hàm thuần trong `theme.ts` (viết test trước), thêm đoạn script trong `index.html` để trang không nháy màu khi tải, và component `ThemeToggle` lưu lựa chọn vào `localStorage` (có `try/catch` phòng trình duyệt chặn).
  - Viết hàm `fetchHealth` (viết test trước) và component `HealthStatus` hiện 3 trạng thái: đang kiểm tra, bình thường, lỗi kèm nút "Thử lại". Thêm `proxy` trong `vite.config.ts` để khi chạy `npm run dev` thì `/api` được chuyển sang backend cổng 8080.
- **Kiểm tra:**
  - `npm test`: 10 test qua (6 test sáng/tối; 4 test gọi API: thành công, lỗi HTTP 503, JSON sai dạng, mất kết nối). Hai file test đều đã chạy đỏ trước khi viết code.
  - `npm run lint`: không lỗi. `npm run build`: thành công.
  - Chưa kiểm bằng trình duyệt thật cùng backend khi viết mục này; `ThemeToggle` và `HealthStatus` chưa có test tự động (cần thêm thư viện giả lập trình duyệt, sẽ xin duyệt trước khi thêm).
- **Lỗi gặp và cách sửa:** nhánh này được tạo khi đang đứng ở nhánh `docs/keep-merged-branches` nên mang theo commit docs chưa gộp vào `main`. Cách xử lý: gộp Pull Request docs trước, rồi mới mở Pull Request của frontend để PR chỉ chứa commit của riêng nó.
- **Học được:**
  - `fetch` không ném lỗi khi server trả 503, phải tự kiểm tra `response.ok`.
  - Dữ liệu từ mạng phải kiểm tra hình dạng (nhận là `unknown` rồi thu hẹp kiểu), không tin vô điều kiện.
  - Muốn không nháy màu khi tải trang thì phải đặt class `dark` trước khi React chạy.
  - Hàm thuần dễ test vì không phụ thuộc trình duyệt. Trước khi tạo nhánh mới phải `git switch main` và `git pull`, không tạo từ nhánh đang đứng.
- **Tiếp theo:** mở Pull Request, rồi làm `feat/docker` (đóng frontend vào JAR, `Dockerfile` nhiều giai đoạn).

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
  - Sau khi gộp PR, nhánh chỉ là cái nhãn trỏ vào commit: xóa nhánh không mất commit, lỡ xóa thì khôi phục được bằng nút Restore branch. Quyết định giữ lại các nhánh đã gộp làm bằng chứng tiến trình.
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
