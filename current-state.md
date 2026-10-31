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

## 2026-10-31 — Database: Flyway, JPA, Testcontainers và kết nối Aiven trên Render
- **Nhánh:** `feat/database-flyway` (Pull Request #9, gộp vào `main` ở commit `ef90f75`)
- **Đã làm:**
  - Viết migration `V1__create_tables.sql` tạo 6 bảng đúng `docs/DATABASE_DESIGN.md`: khóa ngoại `RESTRICT` và `CASCADE`, `UNIQUE` (email, slug, cặp người dùng–bài), `FULLTEXT` cho `knowledge_chunks`.
  - Viết 6 test tích hợp chạy trên MySQL 8.4 thật bằng Testcontainers: 5 test trong `FlywayMigrationIntegrationTest` (đủ 6 bảng, email không trùng kể cả khác hoa/thường, không xóa khóa còn bài, xóa bài thì xóa ảnh/tiến độ/đoạn tri thức, mỗi học sinh một dòng tiến độ mỗi bài) và `contextLoads` chuyển thành `SpacexApplicationIntegrationTest`.
  - Thêm `spring-boot-starter-data-jpa`, `spring-boot-starter-flyway`, `flyway-mysql`, `mysql-connector-j`; cấu hình datasource bằng biến `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (ở máy đọc từ `.env` qua `spring.config.import`), `ddl-auto=validate`, `open-in-view=false`.
  - Chạy `V1` lên Aiven MySQL, đặt 3 biến `DB_*` trên Render, deploy.
- **Kiểm tra:**
  - Test: viết test trước, `.\mvnw test -Dtest=FlywayMigrationIntegrationTest` đỏ (`No migrations found`; 1 failure, 4 errors `Table 'test.users' doesn't exist`); sau khi thêm `V1` và cấu hình, `.\mvnw test`: `Tests run: 12, Failures: 0, Errors: 0` (6 test không cần Docker, 6 test tích hợp), `BUILD SUCCESS`.
  - Chạy ở máy với MySQL của docker compose: log `Successfully applied 1 migration`, DBeaver thấy 7 bảng (6 bảng ứng dụng và `flyway_schema_history`); chạy lần hai báo `Schema ... is up to date. No migration necessary`.
  - Aiven (truy vấn trực tiếp bằng client MySQL): 7 bảng, `flyway_schema_history` có 1 dòng `success = 1`, MySQL 8.4.11.
  - CI trên Pull Request #9 (commit `6b0aa25`): `CI`, `Security`, `CodeQL` đều `success`, tức 6 test tích hợp chạy được trên Ubuntu của GitHub.
  - Render (2026-10-31): deploy thủ công `ef90f75` hiện `Live`, 1 phút 21 giây. Log: kết nối Aiven (MySQL 8.4.11, driver MySQL Connector/J), `Initialized JPA EntityManagerFactory`, `Tomcat started on port 10000`, `Started SpacexApplication in 43.301 seconds`. `curl.exe -i https://space-x-47f1.onrender.com/api/health`: `200`, `{"status":"UP"}`.
  - Thời gian khởi động trên Render tăng từ 17,9 giây (bản chưa có database, ghi ở D8) lên 43,3 giây (có JPA, Flyway và kết nối Aiven).
  - **Chưa kiểm:** thời gian thức dậy khi ứng dụng ngủ sau khi có JPA (trước đó 33,5 giây); RAM thật trên Render (gói Free không xem được); ứng dụng phản ứng thế nào khi Aiven bị tắt lâu hơn.
- **Lỗi gặp và cách sửa:**
  - Lần chạy Testcontainers đầu tiên báo `Could not find a valid Docker environment` dù `docker version` bình thường; chạy lại ngay thì ổn, chưa rõ nguyên nhân.
  - Comment trong `application.properties` ghi sai là thiếu biến sẽ báo tên biến. Thực tế Spring giữ nguyên chuỗi `${DB_URL}` và báo `'url' must start with "jdbc"`. Đã sửa comment.
  - `Access denied for user` khi chạy ở máy: `.env` có `MYSQL_USER=spacex_app` nhưng `DB_USERNAME` còn là giá trị mẫu `spacex`. Sửa `DB_USERNAME` cho trùng `MYSQL_USER`, và bổ sung ghi chú trong `.env.example` và `README.md`.
  - Aiven gói free đã bị tắt (*powered off*) khi thử kết nối; bật lại thủ công. Tài liệu Aiven không nói ngưỡng thời gian không hoạt động, nên ghi nhận như rủi ro trong D8.
  - Gộp Pull Request #9 **trước khi** đặt biến `DB_*` trên Render nên deploy tự động thất bại (`'url' must start with "jdbc"`). Render vẫn giữ bản cũ `64814bf` chạy bình thường (`/api/health` trả `200`). Sau khi đặt biến và bấm Manual Deploy thì `Live`.
  - Mục nhật ký "Xử lý lỗi thống nhất" (commit `6ce39ea`) nằm trên nhánh `feat/error-handling` nhưng không vào `main`, vì Pull Request #8 được gộp trước khi commit đó được đẩy lên. Đã lấy lại mục đó vào nhánh này.
- **Học được:**
  - File migration đã áp dụng thì không sửa (Flyway so checksum); muốn đổi schema phải viết `V2`. Starter Flyway tự chạy migration khi ứng dụng khởi động.
  - `ddl-auto=validate`: Hibernate chỉ kiểm tra Entity khớp bảng, chỉ Flyway được tạo/sửa schema.
  - `@DynamicPropertySource` được ưu tiên hơn `application.properties`, nên test trỏ vào MySQL của Testcontainers mà không cần biến thật.
  - Auto-Deploy nghĩa là gộp Pull Request là deploy ngay, nên phải đặt biến môi trường trước khi gộp. Khi deploy lỗi, Render giữ bản cũ đang chạy.
  - Phải đẩy hết commit lên nhánh rồi mới gộp Pull Request; commit đẩy sau khi gộp sẽ không vào `main`.
  - Nhập mật khẩu bằng `Read-Host -AsSecureString` và dán vào ô nhập ẩn; chữ trong ngoặc kép của `Read-Host` chỉ là lời nhắc, không phải chỗ nhập.
- **Tiếp theo:** 3.4 đăng ký và đăng nhập (nhánh `feat/auth`): bắt đầu từ kế hoạch và test trước.

## 2026-10-25 — Xử lý lỗi thống nhất bằng ProblemDetail
- **Nhánh:** `feat/error-handling` (Pull Request #8, chưa gộp khi viết mục này)
- **Đã làm:**
  - Viết 3 test trước trong `GlobalExceptionHandlerTest` (`@WebMvcTest`): đường dẫn không tồn tại → 404, sai method → 405, lỗi bất ngờ → 500; cả ba phải trả `application/problem+json`. Test thứ ba dùng một controller giả nằm trong file test (nạp bằng `@Import`) để ném `RuntimeException("mat-khau-bi-mat")`, rồi kiểm tra body không chứa nội dung exception và không có trường `trace`.
  - Viết `GlobalExceptionHandler` (`@RestControllerAdvice`, kế thừa `ResponseEntityExceptionHandler`): lớp cha lo các lỗi của framework (404, 405...), thêm một `@ExceptionHandler(Exception.class)` trả 500 với câu chung chung, còn chi tiết và stack trace chỉ ghi vào log ở server.
  - Cập nhật `docs/API.md` (ví dụ JSON lỗi, thêm mã 405 và 500) và `docs/TESTING.md` (dòng lỗi `problem+json` sang `Tested`; backend 4 → 7 test, 2 → 3 lớp).
  - Ba commit theo thứ tự: `test` → `feat` → `docs` (`917e752`, `b5125e4`, `1714636`).
- **Kiểm tra:**
  - Sau khi viết test, `.\mvnw test`: `Tests run: 7, Failures: 2, Errors: 1`. Hai test 404/405 fail vì `Content type not set`; test 500 báo error vì `ServletException ... mat-khau-bi-mat` văng thẳng ra ngoài. Bốn test cũ vẫn qua.
  - Sau khi thêm handler, `.\mvnw test`: `Tests run: 7, Failures: 0, Errors: 0`, `BUILD SUCCESS`. Log của server có dòng `ERROR ... Loi khong xu ly duoc tai GET /test/loi-bat-ngo` đúng như thiết kế.
  - Đếm test bằng lệnh: 7 test, 3 file test.
  - Trên GitHub, Pull Request #8 (commit `1714636`): `CI`, `Security`, `CodeQL` đều `success` (đọc từ API công khai của GitHub).
  - **Chưa kiểm:** chưa chạy server thật rồi gọi bằng `curl`; chưa kiểm trên Render sau khi gộp. Mới kiểm bằng `MockMvc`.
- **Lỗi gặp và cách sửa:** không có lỗi ngoài ý muốn. Ba lỗi ở bước đỏ là chủ đích của TDD, để chắc test thật sự bắt được lỗi trước khi viết code.
- **Học được:**
  - `ProblemDetail` là lớp có sẵn của Spring cho chuẩn RFC 9457; trả `ProblemDetail` thì Spring tự đặt `Content-Type: application/problem+json`.
  - Khi nhiều `@ExceptionHandler` cùng khớp, Spring chọn handler cụ thể nhất; nên lỗi 404/405 do lớp cha xử lý, chỉ lỗi lạ mới rơi vào handler `Exception`.
  - Bắt `Exception` không phải "nuốt lỗi" nếu lỗi vẫn được ghi log đầy đủ và trả mã 500. Không đưa `ex.getMessage()` vào phản hồi vì có thể lộ thông tin nội bộ.
  - Handler `Exception` sẽ bắt cả `AccessDeniedException` ném từ trong controller và đổi thành 500 thay vì 403; ở Tuần 4 phải thêm handler riêng cho lỗi phân quyền.
- **Tiếp theo:** gộp Pull Request #8, rồi gọi `curl.exe -i https://space-x-47f1.onrender.com/api/khong-ton-tai` sau khi Render deploy xong để kiểm bản thật; sau đó làm `feat/database-flyway` (Spring Data JPA, Flyway, Testcontainers).

## 2026-10-25 — Đóng checkpoint Tuần 2
- **Nhánh:** không mở nhánh riêng; mục này commit cùng nhánh đầu tiên của Tuần 3 (`feat/error-handling`).
- **Đã làm:**
  - Xác nhận cấu hình Render của service `space-x-47f1`: Health Check Path là `/api/health`; Auto-Deploy là `After CI Checks Pass` (Render chỉ deploy khi CI trên `main` xanh).
  - Xóa service Render cũ gắn với repo đã xóa.
  - Chốt cho Tuần 3: truy cập database bằng Spring Data JPA (`ddl-auto=validate`, chỉ Flyway tạo schema); integration test dùng Testcontainers với MySQL 8.4 thật.
- **Kiểm tra:**
  - `curl.exe` gọi `GET https://space-x-47f1.onrender.com/api/health` lúc ứng dụng đang ngủ (2026-10-25 17:49): `200`, mất 33,5 giây. Gọi ngay lần nữa: `200`, 0,19 giây. Mới đo 1 lần.
  - Workflow `CI`, `Security`, `CodeQL` trên `main` (commit `ce50226`) đều `success`.
  - RAM thật trên Render: **không xem được**. Tab Metrics của gói Free chỉ hiện lưu lượng mạng; Memory/CPU chỉ mở cho gói trả phí. Số RAM tham chiếu vẫn là số đo ở máy với giới hạn 512 MB.
- **Lỗi gặp và cách sửa:** suýt ghi nhầm "0 MB" ở tab Metrics là RAM; thực ra đó là lưu lượng mạng.
- **Học được:**
  - Gói Free ngủ khi không có truy cập; request đầu tiên phải chờ ứng dụng khởi động lại (~30 giây), nên trước khi demo phải mở trang trước vài phút.
  - `After CI Checks Pass` nối CI với CD: commit làm đỏ CI sẽ không lên production.
- **Tiếp theo:** Tuần 3, nhánh `feat/error-handling` (lỗi trả `problem+json`). Còn mở: test component `HealthStatus` (cần thêm Testing Library + jsdom, chưa quyết thêm ngay hay để Tuần 4–5).

## 2026-10-22 — Deploy lần đầu lên Render, cập nhật README và danh mục test
- **Nhánh:** `docs/week-2-status` (phần deploy là cấu hình trên trang Render, không có thay đổi mã)
- **Đã làm:**
  - Tạo Web Service mới trên Render từ repo `hannm2004/Space-X`: gói Free, khu vực Singapore, nhánh `main`, Dockerfile `./Dockerfile`. Sau đó đặt Health Check Path (lần deploy thứ hai có trigger "Health check path updated"). Auto-Deploy: `After CI Checks Pass` (xác nhận lại ngày 2026-10-25).
  - Cập nhật `README.md` (trạng thái, địa chỉ Render, lộ trình, cách chạy ở máy) và `docs/TESTING.md` (4 test backend, 10 test frontend, nhãn `Tested` cho các dòng đã có test).
- **Kiểm tra:** từ ngoài mạng, địa chỉ `https://space-x-47f1.onrender.com` (2026-10-22):
  - `GET /api/health`: `200`, `Content-Type: application/json`, nội dung `{"status":"UP"}`.
  - `GET /`: `200`, trang có chữ `Space-X`, có script chống nháy màu và `lang="vi"`.
  - `POST /api/health`: `405`, đúng như test.
  - Trang Deploys của Render: `Deploy succeeded | Live`, 31,2 giây, nguồn là commit `8bd1921` (gộp PR #6). Log: `Started SpacexApplication in 17.697 seconds`, `Tomcat started on port 10000` (Render cấp cổng qua biến `PORT`), `Adding welcome page: ... static/index.html`. Có thông báo gói Free sẽ tự ngủ, request đầu có thể chậm từ 50 giây.
  - Chưa kiểm: RAM thật trên Render (tab Metrics); thời gian thức dậy khi ứng dụng đã ngủ; giá trị chính xác của Health Check Path (cần xác nhận là `/api/health`).
- **Lỗi gặp và cách sửa:** service Render cũ gắn với repo đã xóa nên không dùng được; tạo service mới nên địa chỉ khác (`space-x-47f1`). Service cũ chưa xóa.
- **Học được:**
  - CD của dự án do Render đảm nhiệm: theo dõi repo và tự build bằng `Dockerfile` từ nhánh `main`; GitHub Actions chỉ lo CI.
  - Gói Free tự ngủ khi lâu không có truy cập, nên lần mở đầu chậm.
  - README và danh mục test phải cập nhật cùng lúc với mã, nếu không sẽ ghi sai trạng thái (README từng ghi "chưa có mã nguồn").
- **Tiếp theo:** xem tab Metrics để ghi RAM thật và thử gọi lại sau khi ứng dụng ngủ để đo thời gian thức dậy; Tuần 3: database và Flyway, kết nối Aiven bằng biến môi trường.

## 2026-10-22 — CI trên GitHub Actions: build/test, quét bí mật, CodeQL
- **Nhánh:** `ci/github-actions`
- **Đã làm:**
  - Đưa lại 3 workflow từ bản sao lưu, giữ nguyên nội dung (đã so hash): `ci.yml` (job `backend`: `mvnw test`; job `frontend`: `npm ci`, lint, test, build; job `docker`: build thử ảnh, chỉ chạy sau hai job kia), `security.yml` (gitleaks quét toàn bộ lịch sử Git, chạy thêm mỗi tuần), `codeql.yml` (phân tích Java và TypeScript, chạy thêm mỗi tuần) và `codeql/codeql-config.yml` (bỏ qua mã test).
  - Viết lại mẫu Pull Request `.github/pull_request_template.md` cho khớp quy trình mới (nhắc ghi `current-state.md`, cập nhật `API.md`/`DATABASE_DESIGN.md`/`TESTING.md`).
- **Kiểm tra:**
  - `actionlint` (chạy bằng Docker ở máy): không báo lỗi cú pháp.
  - `gitleaks` (chạy bằng Docker ở máy): quét 20 commit, không phát hiện bí mật.
  - Trên GitHub, Pull Request #6 (commit `62fc2f8`): `CI`, `Security` và `CodeQL` đều `success` (đọc từ API công khai của GitHub; chưa xem chi tiết từng job).
  - Sau khi gộp vào `main` (commit `8bd1921`): cả ba workflow `CI`, `Security`, `CodeQL` đều `success` (kiểm tra lại lúc 2026-10-22 10:01, lượt `CI` ban đầu còn đang chạy rồi đã xong).
- **Lỗi gặp và cách sửa:** không có.
- **Học được:**
  - CI chạy trên máy ảo Linux nên `mvnw` phải có quyền chạy (`100755` trong Git), đó là lý do trước đó phải sửa bằng `git update-index --chmod=+x`.
  - gitleaks quét cả lịch sử vì repo công khai: bí mật lỡ commit rồi thì xóa ở commit sau vẫn còn trong lịch sử.
  - Job `docker` chạy sau `backend` và `frontend` (`needs`) để không tốn thời gian build ảnh khi test đã đỏ.
- **Tiếp theo:** mở Pull Request, chờ cả ba workflow xanh rồi gộp; sau đó kết nối Render để deploy `/api/health`.

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
