# Danh mục test của Space-X

Cập nhật: 2026-11-08 (đếm bằng lệnh ở mục "Cách đếm lại"). Mỗi khi thêm, xóa hoặc đổi tên test, cập nhật file này trong cùng nhánh. Số liệu phải đếm bằng lệnh, không cộng nhẩm.

## Tổng quan

| | Số test | Số lớp/file | Cần Docker? |
|---|---|---|---|
| Backend (JUnit 5 + Spring Test) | 193 | 22 lớp | Không |
| Frontend (Vitest) | 10 | 2 file | Không |
| Test tích hợp với MySQL thật (Testcontainers) | 90 | 11 lớp | Có |

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
| Flyway tạo đủ 6 bảng trên MySQL thật; email không trùng (kể cả khác hoa/thường); không xóa khóa còn bài; xóa bài thì xóa ảnh, tiến độ, đoạn tri thức; mỗi học sinh một dòng tiến độ mỗi bài | Integration | 3 | Tested (`FlywayMigrationIntegrationTest`, 5 test) |
| Ứng dụng khởi động được với datasource + Flyway `V1` + Hibernate `ddl-auto=validate` trên MySQL thật | Integration | 3 | Tested (`SpacexApplicationIntegrationTest`, 1 test) |
| Đăng ký (D25): email trùng (kể cả khác hoa/thường hoặc có khoảng trắng thừa) → 409, đăng ký bằng email đã thuộc tài khoản Google cũng 409; thiếu trường, email sai định dạng hoặc > 255 ký tự, `displayName` rỗng/chỉ có khoảng trắng hoặc > 100 ký tự, mật khẩu < 8 ký tự, mật khẩu > 72 byte UTF-8 (kể cả khi ≤ 72 ký tự có dấu), `confirmPassword` không khớp → 400; email được cắt khoảng trắng và lưu chữ thường; mật khẩu không bị cắt khoảng trắng; mật khẩu được băm BCrypt, không lưu rõ; `confirmPassword` không được lưu; thân yêu cầu gửi `role: ADMIN` vẫn ra `LEARNER`; đăng ký không tạo session | Slice + Unit + Integration | 3 | Tested (`RegisterRequestValidationTest`, `AuthServiceTest`, `AuthControllerTest`, `AuthIntegrationTest`) |
| Đăng nhập (D25): đúng thông tin (kể cả email viết hoa hoặc có khoảng trắng thừa) → 200 kèm `displayName`, `email`, `role`, có cookie session, thân phản hồi không chứa JWT hay session ID; email không tồn tại, sai mật khẩu, tài khoản bị khóa (kể cả khi mật khẩu đúng) và tài khoản Google (không có mật khẩu) → **cùng** 401, cùng `detail` `Email hoặc mật khẩu không chính xác`, định dạng `problem+json` (so sánh nguyên thân phản hồi giữa các trường hợp); thiếu CSRF → 403; đăng xuất xong thì gọi API cần đăng nhập → 401 | Integration | 3 | Tested (`AuthIntegrationTest`, `AuthControllerTest`) |
| Phân quyền (D26): chưa đăng nhập gọi `/api/admin/**` → 401; `LEARNER` → 403 `problem+json` (thân không lộ email hay tên role, kể cả gọi `/api/admin` và `/api/admin/` không có phần sau); `ADMIN` → 200; `LEARNER` vẫn gọi được API thường; lỗi phân quyền ném từ trong controller ra 403, không phải 500; luồng thật trên MySQL: học sinh đăng nhập gọi API Admin → 403, Admin đăng nhập → 200 | Slice + Integration | 3 | Tested (`AdminAuthorizationTest`, `GlobalExceptionHandlerTest`, `AdminAuthorizationIntegrationTest`) |
| Khởi tạo `ADMIN` đầu tiên (D11, D26): cả hai biến thiếu → bỏ qua, không chạm database; chỉ thiếu một → lỗi khởi động nêu tên biến; mật khẩu < 8 ký tự, > 72 byte (kể cả tiếng Việt có dấu) → lỗi, thông báo không chứa mật khẩu; đã có `ADMIN` → không tạo thêm; email đã thuộc học sinh → lỗi, không tự nâng quyền; email chuẩn hóa, mật khẩu băm BCrypt, role `ADMIN`; mật khẩu không vào log; khởi động thật tạo đúng một `ADMIN` | Unit + Integration | 3 | Tested (`AdminBootstrapTest`, `AdminAuthorizationIntegrationTest`) |
| CSRF bằng cookie (docs/API.md): mọi yêu cầu `GET` nhận cookie `XSRF-TOKEN` (path `/`, JavaScript đọc được); `POST` có cookie và header `X-XSRF-TOKEN` khớp → qua (kể cả dùng đúng token lấy từ một `GET` thật); có cookie thiếu header, header khác cookie, hoặc có header thiếu cookie → 403 `problem+json`; đăng nhập bằng luồng CSRF cookie thật thành công | Slice + Integration | 3 | Tested (`CsrfCookieTest`, `SessionJdbcIntegrationTest`) |
| Session trong MySQL (Spring Session JDBC, `V2`): đăng nhập tạo đúng một dòng `SPRING_SESSION` gắn với email (kể cả email dài hơn 100 ký tự); cookie `SESSION` (không còn `JSESSIONID`) có `HttpOnly` và `SameSite=Lax`; cookie dùng được cho API cần đăng nhập; xóa dòng session trong database thì cookie cũ bị 401; đăng xuất xóa dòng session; đăng nhập thất bại và yêu cầu ẩn danh bị 401 không tạo session hay phát cookie; cột lưu session không chứa mã băm hay mật khẩu | Integration | 3 | Tested (`SessionJdbcIntegrationTest`) |
| Cờ `Secure` của cookie (3.8), chạy trên Tomcat thật cổng ngẫu nhiên (MockMvc sắp xếp bộ lọc khác server thật nên không dùng): sau proxy HTTPS (`X-Forwarded-Proto: https`) cả `SESSION` và `XSRF-TOKEN` có `Secure`, `SESSION` vẫn `HttpOnly` và `SameSite=Lax`; chạy HTTP thường ở máy (không proxy) thì không có `Secure`. Đã xác nhận test đỏ khi tắt `server.forward-headers-strategy` | Integration | 3 | Tested (`SecureCookieRealServerIntegrationTest`) |
| Đổi mật khẩu (`PUT /api/me/password`): dữ liệu hợp lệ → 204 và mã băm BCrypt mới (mật khẩu cũ hết hiệu lực, mật khẩu mới đăng nhập được); sai mật khẩu cũ → 400, mã băm không đổi; tài khoản không có mật khẩu → 400; mật khẩu mới < 8 ký tự, > 72 byte (tính theo byte, tiếng Việt có dấu), xác nhận không khớp, trùng mật khẩu cũ → 400; khoảng trắng trong mật khẩu giữ nguyên; thân phản hồi và `toString` không chứa mật khẩu; chưa đăng nhập → 401, thiếu CSRF → 403; chỉ đổi mật khẩu của chính mình | Unit + Slice + Integration | 3 | Tested (`MeServiceTest`, `ProfileRequestValidationTest`, `MeControllerTest`, `MeIntegrationTest`) |
| Thông tin cá nhân (`GET/PUT /api/me`): chỉ đọc/sửa được của chính mình (userId lấy từ session, `userId` trên đường dẫn và `id`/`email`/`role` trong thân bị bỏ qua); phản hồi không lộ mã băm; tên rỗng/quá 100 ký tự → 400, tên được cắt khoảng trắng; chưa đăng nhập → 401, thiếu CSRF → 403; sửa tên lưu vào MySQL và không đụng tài khoản khác | Unit + Slice + Integration | 3 | Tested (`MeServiceTest`, `ProfileRequestValidationTest`, `MeControllerTest`, `MeIntegrationTest`) |
| Đọc công khai (4.1): khách (chưa đăng nhập) gọi `GET /api/courses` và `GET /api/courses/{slug}/lessons` → 200, mảng rỗng khi chưa có dữ liệu; khóa sắp theo `sort_order` rồi `id`, bài cũng vậy; khóa chỉ có `id`, `slug`, `title`, `description` (`null` khi để trống), bài chỉ có `id`, `slug`, `title`, `summary` (không có nội dung, không có trạng thái); bài `DRAFT` không lọt vào danh sách, khóa chỉ có bài nháp trả `[]` chứ không lỗi; bài khóa khác không lẫn vào; khóa không tồn tại → 404 `problem+json`, `detail` là câu cố định; tiếng Việt có dấu đi qua MySQL và JSON không hỏng; chỉ hai đường dẫn `GET` đó là công khai (`/api/courses/{slug}` vẫn 401, `POST /api/courses` khi chưa đăng nhập → 401) | Slice + Unit + Integration | 4 | Tested (`CourseControllerTest`, `CourseServiceTest`, `CourseIntegrationTest`) |
| Đọc một bài (4.2, `GET /api/lessons/{slug}`): khách chưa đăng nhập → 401, service không được gọi, nội dung bài đã đăng không lọt vào thân phản hồi 401 (luật `/api/**` thật của `SecurityConfig`); đã đăng nhập → 200 với đúng 5 trường (`id`, `slug`, `title`, `summary`, `contentMd`), không có `status`, `courseId`, `createdBy`; `contentMd` giữ nguyên Markdown và tiếng Việt có dấu qua MySQL và JSON; bài `DRAFT` → 404 `problem+json` và không lộ nội dung nháp; slug không tồn tại và bài nháp trả **cùng một** phản hồi 404 (không dò được slug nào là bài nháp); `detail` là câu cố định, không lặp lại slug; service chỉ hỏi bài `PUBLISHED` ngay trong truy vấn | Slice + Unit + Integration | 4 | Tested (`LessonControllerTest`, `LessonServiceTest`, `LessonIntegrationTest`) |
| Admin khóa học (4.3, `POST/PUT/DELETE /api/admin/courses`): khách → 401 và học sinh → 403 cho cả ba thao tác, service không được gọi, database không đổi; Admin thiếu CSRF → 403; tạo → 201 với đúng 5 trường (`id`, `slug`, `title`, `description`, `sortOrder`), ghi đúng vào MySQL và `GET /api/courses` thấy ngay; sửa → 200, `PUT` giữ nguyên slug của chính khóa đó không bị báo trùng; slug trùng khi tạo hoặc khi đổi sang slug của khóa khác → 409 `problem+json`, thông báo cố định không lặp lại slug, database giữ nguyên; hai Admin cùng slug cùng lúc (`DataIntegrityViolationException`) → vẫn 409; khóa không tồn tại → 404 khi sửa và xóa; xóa khóa không có bài → 204 và mất khỏi MySQL; xóa khóa còn bài → 409 và cả khóa lẫn bài vẫn còn (khóa ngoại `RESTRICT`); bài vừa được thêm giữa lúc kiểm tra và lúc xóa vẫn → 409; validation: `slug` bắt buộc, ≤ 100 ký tự, chỉ chữ thường không dấu/số/gạch ngang giữa các từ (từ chối `Tin-Hoc`, `tin_hoc`, `-tin`, `tin--hoc`, `tin-học`, có dấu cách), `title` bắt buộc ≤ 200, `description` ≤ 500 và chỉ khoảng trắng → `null`, `sortOrder` bắt buộc và không âm; `slug`, `title` được cắt khoảng trắng; dữ liệu sai → 400 kèm danh sách trường lỗi; thân không phải JSON → 400; `id` không phải số → 400 với `detail` cố định, không lặp lại giá trị người gửi; tiếng Việt có dấu qua JSON và MySQL không hỏng | Slice + Unit + Integration | 4 | Tested (`AdminCourseControllerTest` 15, `AdminCourseServiceTest` 11, `AdminCourseRequestValidationTest` 11, `AdminCourseIntegrationTest` 14) |
| Admin bài học (4.4, `/api/admin/lessons`): **mọi** endpoint kể cả `GET` — khách → 401, học sinh → 403 (bài nháp và nội dung nháp không lộ trong thân phản hồi 403), Admin thiếu CSRF → 403 ở bốn thao tác ghi, service không được gọi và database không đổi; tạo → 201, luôn `DRAFT`, `created_by` là Admin đang đăng nhập (lấy từ session; `createdBy` và `status` gửi lên trong body bị bỏ qua), nội dung Markdown và tiếng Việt có dấu qua JSON và MySQL nguyên vẹn, học sinh chưa thấy bài nháp (danh sách công khai rỗng, `GET /api/lessons/{slug}` → 404); đăng bài → học sinh đọc được, ẩn bài → biến mất khỏi danh sách công khai và đọc 404; sửa ghi đúng, được chuyển sang khóa khác, giữ nguyên `status` và `created_by`, giữ slug của chính nó không bị báo trùng; slug trùng (kể cả khác khóa) → 409 `problem+json`, hai Admin cùng slug cùng lúc vẫn → 409, database giữ nguyên; khóa không tồn tại → 404 khi tạo và khi chuyển bài; bài không tồn tại → 404 ở xem, sửa, đổi trạng thái, xóa; danh sách: lọc theo `courseId`, theo `status`, theo cả hai, phân trang (`page`, `size`, `totalItems`, trang quá cuối → rỗng), sắp theo khóa/thứ tự/id, mỗi dòng đúng 7 trường và không lộ `contentMd`, `size` > 50 hoặc < 1, `page` < 0, `status`/`courseId` sai kiểu → 400 với `detail` cố định; chi tiết trả đủ 8 trường (có `contentMd`, không có `createdBy`); validation: `courseId` số dương, `slug` ≤ 150 và đúng định dạng, `title` ≤ 200, `summary` ≤ 500 (chỉ khoảng trắng → `null`), `contentMd` không null và ≤ 100000 ký tự (không bị cắt khoảng trắng), `sortOrder` không âm, trạng thái bắt buộc và chỉ nhận `DRAFT`/`PUBLISHED`; xóa bài kéo theo ảnh, tiến độ, đoạn tri thức nhưng giữ người dùng và khóa; khóa chỉ xóa được sau khi hết bài | Slice + Unit + Integration | 4 | Tested (`AdminLessonControllerTest` 23, `AdminLessonServiceTest` 17, `AdminLessonRequestValidationTest` 14, `AdminLessonIntegrationTest` 19) |
| Không xóa được khóa còn bài (409) | Unit | 4 | Planned |
| Ảnh: sai loại → 415; quá 1 MB → 413; đuôi `.png` nhưng nội dung không phải ảnh → 415 | Unit | 4 | Planned |
| Markdown của bài học không chạy được script (XSS) | Unit (frontend) | 4 | Planned |
| Tìm kiếm trả đúng bài với bộ câu hỏi mẫu | Integration | 5 | Planned |
| Tiến độ: chỉ của chính mình; `COMPLETED` không bị hạ về `IN_PROGRESS`; % chỉ tính bài Đã đăng; "Học lại" chỉ xóa đúng khóa đó | Unit + Integration | 5 | Planned |
| Quản lý người dùng: không tự khóa mình, không khóa Admin cuối cùng (409); tài khoản bị khóa không đăng nhập được | Unit | 5 | Planned |
| Dashboard đếm đúng | Integration | 6 | Planned |
| Đăng nhập Google: email chưa xác minh bị từ chối; email đã có tài khoản Google thì đăng nhập vào tài khoản đó; email đã thuộc tài khoản mật khẩu bị từ chối, không tự liên kết (D25); tài khoản mới có vai trò `LEARNER` | Unit | 6 | Planned |
| RAG: trích dẫn chỉ lấy từ đoạn đã truy xuất; số trích dẫn lạ bị loại | Unit | 7 | Planned |
| RAG: không đủ căn cứ → trả lời "không tìm thấy" | Unit | 7 | Planned |
| Nhà cung cấp AI lỗi / quá thời gian / hết hạn mức → 502 / 504 | Unit | 7 | Planned |
| Agent: giới hạn số vòng gọi tool, thời gian, kích thước | Unit | 8 | Planned |
| Agent: chuỗi prompt injection nằm trong nội dung bài không làm Agent đổi hành vi | Unit | 8 | Planned |
| Agent: chỉ có tool chỉ đọc; tool không trả bài Nháp | Unit | 8 | Planned |

## Cách chạy

Chạy riêng từng phần:

```powershell
# Backend (đứng ở src/backend/)
.\mvnw test

# Frontend (đứng ở src/frontend/)
npm test ; npm run lint ; npm run build
```

## Cách đếm lại

PowerShell, đứng ở thư mục gốc dự án:

```powershell
(Get-ChildItem src\backend\src\test -Recurse -Filter *.java | Select-String '^\s*@Test\b' | Measure-Object).Count
(Get-ChildItem src\frontend\src -Recurse -Include *.test.ts,*.test.tsx | Select-String '^\s*(it|test)\(' | Measure-Object).Count
```

Nếu số khác bảng "Tổng quan", sửa bảng cho khớp (không sửa test cho khớp bảng). Lệnh đếm backend đếm cả test tích hợp: 283 = 193 test không cần Docker + 90 test tích hợp.

Chạy riêng test tích hợp (cần Docker Desktop đang chạy, lần đầu phải tải image `mysql:8.4`): `.\mvnw test -Dtest=*IntegrationTest`. Mỗi lớp tích hợp bật một container MySQL riêng nên mỗi lớp mất khoảng 35 giây.
