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

## 2026-11-01 — Thông tin cá nhân và đổi mật khẩu (việc 3.7)
- **Nhánh:** `feat/profile` (tạo từ `main` sau khi gộp PR #13; chưa commit và chưa mở Pull Request khi viết mục này)
- **Đã làm:**
  - `GET /api/me`, `PUT /api/me` (chỉ sửa `displayName`), `PUT /api/me/password` (`currentPassword`, `newPassword`, `confirmNewPassword`) trong package `vn.spacex.user`: `MeController`, `MeService`, `MeResponse`, `UpdateProfileRequest`, `ChangePasswordRequest`, `WrongCurrentPasswordException`, `NoPasswordAccountException`. `User` thêm `changeDisplayName`, `changePasswordHash`; vẫn không có setter cho `email` và `role`. `GlobalExceptionHandler` thêm hai handler trả 400.
  - Người dùng hiện tại lấy bằng `@AuthenticationPrincipal AppUserDetails` (id từ session); `userId` trên đường dẫn và `id`/`email`/`role` trong thân yêu cầu bị bỏ qua. Mật khẩu mới theo luật đăng ký (8 ký tự đến 72 byte, giữ khoảng trắng), thêm điều kiện khác mật khẩu cũ.
  - Test mới: `ProfileRequestValidationTest` (12), `MeServiceTest` (5), `MeControllerTest` (12), `MeIntegrationTest` (10, MySQL 8.4 thật). Cập nhật `docs/API.md` (mục `/api/me`), `docs/TESTING.md`.
  - Cách làm: theo yêu cầu của người dùng ở buổi này, Claude viết test và code. Người dùng yêu cầu **bỏ qua toàn bộ vấn đáp trong ngày**, nên 3.7 chưa có vấn đáp (nợ cùng câu 4 của 3.6 và vấn đáp 3.5).
- **Kiểm tra:**
  - RED: `.\mvnw test-compile` báo `cannot find symbol` cho `UpdateProfileRequest`, `ChangePasswordRequest`, `MeResponse`, `MeService` (các lớp chưa có).
  - GREEN: 29 test nhanh qua; sau đó `.\mvnw test` → `Tests run: 128, Failures: 0, Errors: 0`, `BUILD SUCCESS` (trước đó 89).
  - Chạy thật: `spring-boot:run` ở cổng 8081 (cổng 8080 đang bị backend cũ giữ) với MySQL Docker, gọi bằng `curl.exe` và cookie jar: `GET /api/me` chưa đăng nhập → 401; đăng nhập → 200; `GET /api/me` → 200 (`displayName`, `email`, `role`, `authProvider`, `createdAt`, không có mật khẩu); `PUT /api/me` kèm `role=ADMIN`, `id=999`, `email` lạ → 200, tên mới được cắt khoảng trắng, `role` vẫn `LEARNER`, email không đổi; `PUT /api/me` tên rỗng → 400 kèm `errors[0].field = displayName`; thiếu CSRF → 403; đổi mật khẩu sai mật khẩu cũ → 400 "Mật khẩu hiện tại không đúng"; mật khẩu mới trùng cũ → 400 `newPasswordDifferentFromCurrent`; đổi đúng → 204; đăng nhập lại bằng mật khẩu cũ → 401. Mật khẩu của `test1@example.com` trong MySQL Docker ở máy giờ là `matkhau456`.
  - **Chưa kiểm:** CI trên Pull Request; trên URL Render (việc 3.8).
- **Lỗi gặp và cách sửa:** không có lỗi biên dịch hay test sau khi viết mã. Hook chặn lần tạo/sửa đầu của vài file nên phải nêu thông tin rồi làm lại.
- **Học được:**
  - `@AuthenticationPrincipal` lấy người dùng từ session nên không cần (và không nên) nhận `userId` từ trình duyệt; test `userIdTrenDuongDanBiBoQua` chứng minh bằng `verify(meService).getProfile(7L)` và `never()` với 999.
  - Record chỉ có trường được khai báo, nên `role`, `email`, `id` trong thân JSON tự bị bỏ qua (mặc định Jackson không báo lỗi trường lạ).
  - Trong `@Transactional`, sửa entity đang được quản lý thì tự ghi xuống database khi commit, không cần gọi `save`.
  - Giới hạn BCrypt tính theo **byte**, không phải ký tự: 24 ký tự `ệ` đúng 72 byte, 25 ký tự là 75 byte.
- **Chưa làm / ghi nhận:** đổi mật khẩu chưa hủy các session đang đăng nhập khác của cùng người dùng.
- **Tiếp theo:** chạy thật bằng `curl.exe` kiểm `/api/me`; commit `test` → `feat` → `docs`, mở Pull Request cho `feat/profile`; sau đó 3.8 (deploy, cookie trên URL thật, cờ `Secure`, đặt `ADMIN_*` trên Render); rồi báo cáo Tuần 3.

---

## 2026-11-01 — CSRF bằng cookie và lưu session trong MySQL (việc 3.6)
- **Nhánh:** `feat/csrf-session` (chưa commit và chưa mở Pull Request khi viết mục này)
- **Đã làm:**
  - CSRF theo `docs/API.md`: `SecurityConfig` dùng `CookieCsrfTokenRepository.withHttpOnlyFalse()` nên mọi `GET` nhận cookie `XSRF-TOKEN`, ghi thì phải gửi lại bằng header `X-XSRF-TOKEN`. Đặt `setCsrfRequestAttributeName(null)` để token được nạp ở mọi yêu cầu (mặc định Spring chỉ nạp khi cần nên `GET` không có cookie).
  - Session trong MySQL: thêm `spring-boot-starter-session-jdbc` (đã duyệt ở D24/D25), migration `V2__create_spring_session_tables.sql` (lấy từ `schema-mysql.sql` trong jar `spring-session-jdbc` 4.1.1, nới `PRINCIPAL_NAME` lên 255), `spring.session.jdbc.initialize-schema=never`, `serialVersionUID` cho `AppUserDetails` vì nó được tuần tự hóa vào cột BLOB.
  - `SessionConfig`: bean `DefaultCookieSerializerCustomizer` đặt `HttpOnly` và `SameSite=Lax` cho cookie `SESSION`.
  - Test mới: `CsrfCookieTest` (6), `SessionJdbcIntegrationTest` (10, MySQL 8.4 thật). Chuyển `AuthIntegrationTest` và `AdminAuthorizationIntegrationTest` từ `MockHttpSession` sang cookie `SESSION` vì Spring Session thay `JSESSIONID`. Cập nhật `docs/TESTING.md`, `docs/API.md`, `docs/DATABASE_DESIGN.md`.
  - Sửa lỗi session ẩn danh: thêm `.requestCache(cache -> cache.disable())` trong `SecurityConfig` và test `yeuCauAnDanhBi401KhongTaoSessionVaKhongPhatCookieSession` (xem mục Lỗi).
  - Cách làm: theo yêu cầu của người dùng ở buổi này, Claude viết test và code. Vấn đáp 3.6: người dùng trả lời 5 câu; câu 2, 3, 5 đạt; câu 1 thiếu ý same-origin (trang độc hại không đọc được cookie `XSRF-TOKEN` nên không đặt được header `X-XSRF-TOKEN`); câu 4 (nguyên nhân lỗi session ẩn danh) **chưa đạt** và người dùng chọn bỏ qua, nên còn nợ. Vấn đáp của 3.5 chưa thấy ghi nhận ở đây.
- **Kiểm tra:**
  - RED: `CsrfCookieTest` 3/6 fail (không có cookie `XSRF-TOKEN`, `POST` có header vẫn bị 403); `SessionJdbcIntegrationTest` 8/8 lỗi `Table 'test.SPRING_SESSION' doesn't exist`.
  - GREEN: `.\mvnw test` → `Tests run: 88, Failures: 0, Errors: 0`, `BUILD SUCCESS` (trước đó 73).
  - RED của lỗi session ẩn danh: `SessionJdbcIntegrationTest` 10 test, 1 fail (`expected: null but was: SESSION=...`). Sau khi sửa: `.\mvnw test` → `Tests run: 89, Failures: 0, Errors: 0`, `BUILD SUCCESS`.
  - Chạy thật bằng `spring-boot:run` + MySQL Docker + `curl.exe` (cookie jar): `GET /api/health` → 200 kèm `Set-Cookie: XSRF-TOKEN`; `POST /api/auth/register` thiếu CSRF → 403 `problem+json`; có CSRF → 201, lặp lại → 409; `POST /api/auth/login` → 200 kèm `Set-Cookie: SESSION=...; Path=/; HttpOnly; SameSite=Lax` (chưa có `Secure`); `SELECT ... FROM SPRING_SESSION` có 1 dòng `PRINCIPAL_NAME = test1@example.com`; `GET /api/admin/dashboard` có cookie → 403, không cookie → 401; tắt rồi bật lại backend, gọi lại vẫn 403 (session còn); `POST /api/auth/logout` → 204 và hai cookie bị xóa. Sau khi sửa lỗi session ẩn danh, `GET /api/admin/dashboard` không cookie → 401 và không còn `Set-Cookie: SESSION`.
  - **Chưa kiểm:** cờ `Secure` của cookie và cookie trên URL Render (việc 3.8); CI trên Pull Request; chạy `V2` trên Aiven.
- **Lỗi gặp và cách sửa:**
  - Cookie `SESSION` mặc định chỉ là `SESSION=...; Path=/`: **không có `HttpOnly`, không có `SameSite`**. Test `cookieSessionCoHttpOnlyVaSameSiteLax` bắt được lỗi này. Thử đặt `server.servlet.session.cookie.http-only/same-site` không có tác dụng với cookie này ở Spring Boot 4 (đã gỡ). Sửa bằng bean `DefaultCookieSerializerCustomizer` (xác định điểm mở rộng bằng `javap` trên jar `spring-boot-session`).
  - Cột `PRINCIPAL_NAME` của thư viện chỉ `VARCHAR(100)` trong khi `users.email` tới 255 ký tự, email dài sẽ làm đăng nhập lỗi 500. Nới lên 255 trong `V2` và thêm test `emailDaiHon100KyTuVanDangNhapVaLuuDuocSession`.
  - Hook chặn lần sửa đầu từng file nên vài lần sửa chạy song song không được áp dụng; phát hiện bằng cách đối chiếu kết quả và làm lại.
  - Khi chạy thật phát hiện yêu cầu chưa đăng nhập tới API cần đăng nhập (ví dụ `GET /api/admin/dashboard`) vẫn nhận `Set-Cookie: SESSION` và để lại một dòng `PRINCIPAL_NAME = NULL` trong `SPRING_SESSION`. Nguyên nhân: khi chặn yêu cầu, Spring lưu nó vào session (`HttpSessionRequestCache`) để quay lại sau khi đăng nhập; với API chỉ trả 401 thì thừa, và khi session nằm trong MySQL thì mỗi yêu cầu ẩn danh ghi thêm một dòng. Sửa bằng cách tắt `requestCache`; test viết trước (RED rồi GREEN).
  - Biến `$jar` của PowerShell rỗng khi mở cửa sổ mới nên `curl.exe -c` nuốt URL làm tên file và báo `no URL specified`; phải đặt lại `$jar` ở mỗi cửa sổ.
  - Sau đăng nhập token `XSRF-TOKEN` **không đổi** (cùng giá trị trước và sau), khác dự đoán ban đầu.
- **Học được:**
  - Với Spring Security 6+, CSRF token mặc định được nạp trễ (deferred) nên cookie không được phát ở `GET`; muốn luôn có cookie phải đặt tên thuộc tính của bộ xử lý là `null`.
  - Đổi sang Spring Session thì cookie là `SESSION` (giá trị Base64) thay `JSESSIONID`, và `MockHttpSession` trong test không còn đại diện cho session thật; test phải truyền cookie như trình duyệt.
  - Mặc định của thư viện không có nghĩa là an toàn: phải kiểm bằng test các cờ `HttpOnly`/`SameSite` thay vì tin vào tài liệu.
  - Đối tượng lưu trong session bị tuần tự hóa vào MySQL, nên không được chứa mã băm mật khẩu (đã có test) và cần `serialVersionUID`.
  - Cảnh báo `Unexpected error occurred in scheduled task ... Communications link failure` khi chạy cả bộ test là nhiễu của job dọn session chạy mỗi phút sau khi container MySQL của lớp test đã tắt.
  - Cấu hình mặc định của Spring Security dành cho web có trang đăng nhập (lưu yêu cầu bị chặn vào session); với API JSON phải tắt, nếu không mỗi yêu cầu ẩn danh có thể tạo session. Chạy thật bằng `curl.exe` bắt được lỗi mà test ban đầu không bắt.
- **Tiếp theo:** trả lời lại câu 4 vấn đáp; commit theo thứ tự `test` → `feat` → `fix` → `docs`, **bật Aiven trước khi gộp** vì Pull Request sẽ chạy `V2` ở production (Render tự deploy); 3.7 thông tin cá nhân và đổi mật khẩu (`feat/profile`).

---

## 2026-11-01 — Phân quyền ADMIN/LEARNER và tạo ADMIN đầu tiên (việc 3.5)
- **Nhánh:** `feat/authorization` (chưa commit và chưa mở Pull Request khi viết mục này)
- **Đã làm:**
  - Chốt thiết kế D26 (chỉ ở máy): API quản trị dưới `/api/admin/**`; phân quyền bằng luật URL `hasRole("ADMIN")`; `ADMIN` đầu tiên tạo từ `ADMIN_EMAIL` + `ADMIN_PASSWORD` (cả hai thiếu → bỏ qua, chỉ thiếu một → lỗi khởi động, email đã thuộc `LEARNER` → lỗi, không tự nâng quyền).
  - Viết test trước (RED) rồi mới viết code (GREEN): `AdminAuthorizationTest` (6), `AdminBootstrapTest` (10), `AdminAuthorizationIntegrationTest` (4, MySQL 8.4 thật bằng Testcontainers), thêm 1 test vào `GlobalExceptionHandlerTest`.
  - `SecurityConfig`: luật `/api/admin/**` đặt trước `/api/**`, thêm `accessDeniedHandler` trả 403 `problem+json`, gom việc ghi lỗi vào hàm `writeProblem` dùng chung với 401. `GlobalExceptionHandler`: handler `AccessDeniedException` → 403 (nếu không có thì handler `Exception` đổi thành 500).
  - `AdminBootstrap` (`ApplicationRunner`), `User.newLocalAdmin` (vẫn không có setter role), `UserRepository.existsByRole`, thuộc tính `app.admin.email/password` đọc từ `ADMIN_EMAIL`/`ADMIN_PASSWORD`, thêm hai biến để trống vào `.env.example`.
  - Cập nhật `docs/TESTING.md` (hai dòng sang `Tested`, sửa bảng tổng quan theo số thật).
  - Cách làm: theo yêu cầu của người dùng ở buổi này, Claude viết test và code. Phần vấn đáp **chưa được trả lời** khi viết mục này, nên chưa coi là xong.
- **Kiểm tra:**
  - RED: `.\mvnw test-compile` lỗi vì thiếu `AdminBootstrap` và `existsByRole`. Dời `AdminBootstrapTest` ra ngoài rồi chạy riêng hai lớp test 403: `AdminAuthorizationTest` 3/6 fail (học sinh gọi API Admin nhận 200, `/api/admin` nhận 404), `GlobalExceptionHandlerTest` 1/4 fail (nhận 500 thay vì 403).
  - GREEN: `.\mvnw test` → `Tests run: 73, Failures: 0, Errors: 0`, `BUILD SUCCESS` (trước đó 52).
  - Chạy ứng dụng thật bằng `.\mvnw spring-boot:run` với MySQL của docker compose: `curl.exe` `GET /api/health` → `200`; `GET /api/admin/dashboard` (chưa đăng nhập) → `401`; `POST /api/auth/login` không có CSRF token → `403`. DBeaver: bảng `users` có đúng 1 dòng, `role = ADMIN`, `auth_provider = LOCAL`, `enabled = 1`, email chữ thường, mã băm bắt đầu `$2a$`.
  - Người dùng báo đã chạy cả hai thử nghiệm thêm (chạy lại lần hai và bỏ `ADMIN_PASSWORD` còn `ADMIN_EMAIL`) và kết quả đúng mong đợi; chưa ghi lại log chi tiết.
  - **Chưa kiểm:** Admin đăng nhập bằng `curl` rồi vào `/api/admin/**` (chưa có cách lấy CSRF token, xem bên dưới; luồng này mới kiểm bằng `AdminAuthorizationIntegrationTest`); đặt `ADMIN_*` và chạy trên Render (việc 3.8); CI trên Pull Request.
- **Lỗi gặp và cách sửa:**
  - `docs/API.md` ghi CSRF token lấy từ cookie `XSRF-TOKEN`, nhưng `SecurityConfig` dùng CSRF mặc định (token nằm trong session, không có cookie, không có endpoint trả token), nên không đăng nhập được bằng `curl`. Chuyển sang việc 3.6.
  - Bảng "Tổng quan" trong `docs/TESTING.md` đã lỗi thời (ghi 6 test backend). Sửa theo số thật: 51 test không cần Docker (8 lớp) và 22 test tích hợp (4 lớp).
  - `accessDeniedHandler` cũng xử lý lỗi thiếu CSRF, nên thông báo 403 do thiếu CSRF cũng là "Bạn không có quyền...". Chấp nhận tạm, xem xét tách ở 3.6.
- **Học được:**
  - Spring lấy luật `requestMatchers` khớp đầu tiên, nên luật cụ thể (`/api/admin/**`) phải đứng trước luật chung (`/api/**`).
  - 403 do bộ lọc ném ra trước khi tới controller, nên `@RestControllerAdvice` không bắt được; phải có `accessDeniedHandler` riêng. Còn `AccessDeniedException` ném từ trong controller thì cần `@ExceptionHandler` riêng.
  - Cấu hình sai thì nên dừng khởi động (fail fast) thay vì chạy mà thiếu Admin; thông báo lỗi chỉ nêu tên biến, không chứa mật khẩu.
  - Mật khẩu giới hạn theo byte UTF-8, không theo ký tự (25 chữ "ạ" là 75 byte, vượt giới hạn BCrypt).
  - Các dòng cảnh báo `HikariPool ... Communications link failure` khi chạy cả bộ test là nhiễu: container của lớp test trước đã tắt trong khi context Spring cũ còn trong cache.
- **Tiếp theo:** trả lời vấn đáp; commit theo thứ tự `test` → `feat` → `docs` rồi mở Pull Request cho `feat/authorization`, chờ CI xanh; sau đó 3.6 (`feat/csrf-session`): đưa CSRF token cho giao diện đúng như `API.md` và lưu session trong MySQL.

---

## 2026-11-01 — Đăng ký, đăng nhập, đăng xuất (việc 3.4)
- **Nhánh:** `feat/auth` (3 commit `87b3e30`, `4025196`, `c3cbd21`; Pull Request chưa mở khi viết mục này)
- **Đã làm:**
  - Viết test trước (RED) rồi mới viết code (GREEN), theo thiết kế D25: `RegisterRequestValidationTest` (14), `AuthServiceTest` (3), `AuthControllerTest` (9), `AuthIntegrationTest` (12, chạy trên MySQL 8.4 thật bằng Testcontainers).
  - `SecurityConfig`: `/api/health`, `/api/auth/register`, `/api/auth/login` công khai, còn lại dưới `/api/**` phải đăng nhập (chưa đăng nhập → 401 `problem+json`); CSRF để bật; đăng xuất `POST /api/auth/logout` trả 204; bean `PasswordEncoder` (BCrypt).
  - Gói `user`: entity `User` (ánh xạ bảng `users`), `UserRepository`, `Role`, `AuthProvider`, `Emails.normalize` (cắt khoảng trắng, chữ thường bằng `Locale.ROOT`).
  - Gói `auth`: `RegisterRequest`/`LoginRequest` (chuẩn hóa email ngay trong constructor, `toString` không in mật khẩu), `AuthService.register`, `AuthController` (`/register`, `/login`), `AppUserDetails` + `AppUserDetailsService`, `AuthConfig` (`AuthenticationManager` dùng `DaoAuthenticationProvider`).
  - `GlobalExceptionHandler`: email trùng → 409; lỗi validation → 400 kèm danh sách `errors` (chỉ tên trường và thông báo, không trả lại giá trị người dùng gửi).
  - Sửa 2 test cũ (`GlobalExceptionHandlerTest`, `HealthControllerTest`) và cập nhật 2 dòng trong `docs/TESTING.md` sang `Tested`.
  - Cách làm: theo lựa chọn của người dùng ở buổi này, Claude viết test và code; người dùng đọc, trả lời vấn đáp rồi mới coi là xong.
- **Kiểm tra:**
  - Test viết trước: `.\mvnw test-compile` báo 58 lỗi biên dịch vì các lớp chưa tồn tại.
  - Sau khi viết code, lần chạy đầu: 2 test fail ở `AuthControllerTest`, và 3 test cũ fail vì `AuthController` thiếu `AuthService` trong `@WebMvcTest` trần. Sau khi sửa: `.\mvnw test` → `Tests run: 52, Failures: 0, Errors: 0`, `BUILD SUCCESS`.
  - **Chưa kiểm:** chạy ứng dụng thật bằng `spring-boot:run` rồi gọi bằng `curl.exe`; cookie session trên URL Render (việc 3.8); thời gian phản hồi giữa tài khoản bị khóa và sai mật khẩu (rủi ro đã ghi ở D25).
- **Lỗi gặp và cách sửa:**
  - Test `GlobalExceptionHandlerTest` và `HealthControllerTest` đã đỏ từ commit `085ac0c` (thêm Spring Security làm `@WebMvcTest` bị chặn 401/403). Xác nhận bằng cách tạm dời `SecurityConfig` ra ngoài, vẫn đỏ. Sửa bằng `@WithMockUser`/`@Import(SecurityConfig)` và `csrf()` cho `POST`.
  - Sau khi thêm `AuthController`, `@WebMvcTest` trần quét luôn nó và báo thiếu bean `AuthService`. Sửa bằng cách chỉ rõ controller cần nạp trong từng test.
  - Lỗi hạ tầng khi xác thực (database sập) bị trả 401 thay vì 500: Spring chọn `@ExceptionHandler` còn xét cả `cause` của exception, nên bọc lại trong `IllegalStateException` vẫn khớp handler `AuthenticationException`. Sửa bằng cách chỉ bắt `BadCredentialsException` và `AccountStatusException`.
  - Test lặp qua nhiều exception gọi `when(mock.authenticate(...))` lần hai nên chính mock ném lỗi. Đổi sang `doThrow(...).when(...)`.
- **Học được:**
  - Mọi thất bại đăng nhập cùng một lỗi 401 để không dò được email nào tồn tại hay đang bị khóa (user enumeration).
  - BCrypt chỉ dùng 72 byte đầu nên giới hạn tính theo byte UTF-8, không theo ký tự (tiếng Việt có dấu chiếm 2–3 byte).
  - Kiểm `existsByEmail` chưa đủ vì hai yêu cầu cùng lúc đều qua được; ràng buộc `UNIQUE` mới là chốt chặn cuối, nên cần `saveAndFlush` và bắt `DataIntegrityViolationException`.
  - Đăng nhập thủ công phải lưu `SecurityContext` vào session và đổi session ID (`changeSessionId`) để chống session fixation.
- **Tiếp theo:** mở Pull Request cho `feat/auth`, chờ CI xanh rồi gộp; sau đó `feat/authorization` (3.5, phân quyền `LEARNER`/`ADMIN` và tạo `ADMIN` đầu tiên từ biến môi trường).

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
