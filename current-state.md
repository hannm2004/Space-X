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

## 2026-11-10 — CP5.4: tìm kiếm bài bằng FULLTEXT
- **Nhánh:** `feat/lesson-search` (tạo từ `main` sau PR #28); **chưa commit**.
- **Quyết định:** D29 (người dùng chọn FULLTEXT thay vì `LIKE`). Ghi trong `docs/DECISIONS.md` (chỉ ở máy) cùng kết quả kiểm thật.
- **Đã làm (backend):**
  - Migration `V3`: `FULLTEXT KEY ft_lessons_title_summary_content (title, summary, content_md)` cho `lessons`.
  - `LessonRepository.searchPublished`: native query `MATCH ... AGAINST (:q IN NATURAL LANGUAGE MODE)`, chỉ `PUBLISHED`, xếp theo độ liên quan rồi `id`; chỉ chọn các cột entity ánh xạ nên không tải `content_md`. `LessonService.search` cắt khoảng trắng. `GET /api/lessons?q=&page=&size=` (đăng nhập; `q` 1–200 ký tự; `size` 1–50), trả `{ items, page, size, totalItems }`, mỗi bài `id, slug, title, summary`.
- **Đã làm (giao diện):** `searchLessons` trong `learningApi.ts`, `SearchForm` (dùng ở trang chủ và trang kết quả), trang `/tim-kiem` (đang tìm, kết quả, không có bài, 401 mời đăng nhập rồi quay lại đúng trang tìm, lỗi có nút Thử lại, phân trang bằng liên kết), liên kết "Tìm bài" ở header.
- **Kiểm tra:**
  - Backend: RED là lỗi biên dịch (chưa có `search`/`searchPublished`); `.\mvnw test` toàn bộ → `Tests run: 399, Failures: 0, Errors: 0`, `BUILD SUCCESS` (375 + 24: `LessonControllerTest` 6, `LessonServiceTest` 2, `LessonSearchIntegrationTest` 16). Lệnh đếm của `docs/TESTING.md` ra 399 (273 + 126), khớp Maven.
  - Frontend: RED trước khi viết mã; `npm test` → 18 file, **181 test qua** (156 + 25); `npm run lint` không cảnh báo; `npm run build` thành công.
  - Kiểm chứng test bảo mật: tạm đổi điều kiện `PUBLISHED` thành `IN ('PUBLISHED','DRAFT')` thì 2 test về bài nháp đỏ (`totalItems expected:<1> but was:<2>`, `expected:<0> but was:<1>`), rồi hoàn lại.
  - **Chưa kiểm:** giao diện tìm kiếm trên trình duyệt thật; chưa chạy ứng dụng thật với dữ liệu mẫu; chưa deploy; chưa kiểm cỡ màn hình điện thoại; chưa đo thời gian dựng chỉ mục trên bảng lớn.
- **Phát hiện khi kiểm trên MySQL thật:**
  - Test đầu tiên đỏ: tìm "nổi bọt" ra cả bài có chữ "Nội dung". Do collation `utf8mb4_0900_ai_ci` không phân biệt dấu nên "nổi" và "nội" là một từ. Giữ hành vi (đổi lại, gõ không dấu vẫn tìm được), đổi dữ liệu mẫu của test đó và thêm test ghi lại hành vi.
  - Nhiều từ nghĩa là "ít nhất một từ"; bài chứa nhiều từ hơn xếp trước.
  - Từ ngắn hơn 3 ký tự (ví dụ `ab`) bị bỏ qua, trả danh sách rỗng, không lỗi.
  - Thêm chỉ mục khiến InnoDB dựng lại bảng (Flyway cảnh báo `rebuilding table to add column FTS_DOC_ID`); với 6 bài thì không đáng kể.
- **Lỗi gặp và cách sửa:** test `timThayBaiTheoNoiDung...` đỏ vì từ đồng âm khác dấu (xem trên); sửa dữ liệu test, không sửa hành vi.
- **Học được:** FULLTEXT tiếng Việt có giới hạn riêng (độ dài từ tối thiểu, collation không dấu) nên phải kiểm trên MySQL thật chứ không đoán; chế độ tự nhiên an toàn hơn BOOLEAN vì toán tử trong từ khóa không có nghĩa. Muốn biết test bảo mật có tác dụng phải làm hỏng thử để thấy nó đỏ.
- **Tiếp theo:** commit và push nhánh, mở Pull Request, chờ CI; thử giao diện tìm kiếm trên trình duyệt; sau đó CP5.5 (tiến độ học tập).

## 2026-11-10 — CP5.3: giao diện tài khoản và hủy session khi đổi mật khẩu
- **Nhánh:** `feat/password-revoke-sessions` (backend, 3 commit, chưa push) và `feat/frontend-auth` (giao diện, tạo từ nhánh trước; **chưa commit**).
- **Quyết định:** D28 (người dùng chọn): đổi mật khẩu hủy mọi session khác, giữ session đang dùng. Ghi trong `docs/DECISIONS.md` (chỉ ở máy).
- **Đã làm (backend):**
  - `OtherSessionsRevoker` (`vn.spacex.auth`): tìm session theo `PRINCIPAL_NAME` (chính là email, `V2`; email không đổi được) bằng `FindByIndexNameSessionRepository` và xóa mọi session trừ session hiện tại.
  - `MeController.changePassword` gọi nó sau khi `MeService.changePassword` thành công; email và id session lấy từ session đang dùng, không nhận từ trình duyệt.
  - `docs/API.md` bỏ ghi chú "chưa làm"; `docs/TESTING.md` thêm dòng test D28.
- **Đã làm (giao diện):**
  - `authApi.ts` (`fetchCurrentUser` trả `null` khi 401, `login`, `register`, `logout`, `changePassword`), `formErrors.ts` (đổi lỗi thành câu cho người dùng; 403 và 5xx dùng câu của frontend), `authRedirect.ts` (chỉ nhận đường dẫn nội bộ; chặn `//host`, `https://`, dấu `\`, `/dang-nhap`, `/dang-ky`).
  - `AuthContext.tsx` + `useAuth.ts` (hỏi `/api/me` một lần khi mở trang), `FormParts.tsx`; trang `LoginPage`, `RegisterPage`, `ChangePasswordPage`; `Layout.tsx` có khu vực tài khoản (đang hỏi thì chưa hiện gì để tránh nháy).
  - Route `/dang-nhap`, `/dang-ky`, `/doi-mat-khau`. Đăng ký không tự đăng nhập (D24): chuyển sang trang đăng nhập kèm lời nhắn.
- **Kiểm tra:**
  - Backend: RED là lỗi biên dịch (chưa có `OtherSessionsRevoker`); sau khi viết mã, `.\mvnw test` → `Tests run: 375, Failures: 0, Errors: 0`, `BUILD SUCCESS` (367 + 8). Cố ý tắt dòng gọi hủy session thì `doiMatKhauHuyCacSessionKhacVaGiuSessionHienTai` đỏ (`Status expected:<401> but was:<200>`), rồi hoàn lại.
  - Frontend: RED là 7 file không nạp được vì thiếu module; sau khi viết mã `npm test` → 17 file, **156 test qua** (93 + 63 mới); `npm run lint` không cảnh báo; `npm run build` thành công. Lệnh đếm của `docs/TESTING.md` ra 156, khớp Vitest.
  - **Thử thật ở máy** (ứng dụng Spring Boot cổng 8081 + frontend đã build + MySQL Docker Compose): `curl` với cookie jar cho 2 "thiết bị": đăng ký 201, đăng nhập cả hai 200; đổi mật khẩu từ thiết bị A → 204, sau đó A vẫn `200`, B bị `401`; đăng xuất A bằng token CSRF lấy từ jar → 204 (CSRF sau đăng nhập hoạt động); mật khẩu cũ 401, mật khẩu mới 200. **Chrome thật:** mở thẳng `/dang-nhap`; sai mật khẩu hiện thông báo, xóa ô mật khẩu; đăng nhập đúng về trang chủ, thanh điều hướng hiện tên, "Đổi mật khẩu", "Đăng xuất"; F5 trên `/doi-mat-khau` vẫn đăng nhập; đổi mật khẩu hiện lời báo thành công; "Đăng xuất" về trang chủ và hiện lại "Đăng nhập/Đăng ký"; console chỉ có một gợi ý mức verbose.
- **Chưa kiểm / còn dở:**
  - Chưa thử trên Render với cookie `Secure` (cần deploy); chưa kiểm cỡ màn hình điện thoại.
  - Sửa cuối (thêm ô username ẩn vào form đổi mật khẩu theo gợi ý của Chrome) chỉ chạy lại test/lint/build, chưa mở lại Chrome.
  - Giao diện chưa có trang xem/sửa thông tin cá nhân (API đã có).
  - Chưa deploy; frontend chưa commit.
- **Lỗi gặp và cách sửa:**
  - Sau khi đăng xuất từ `/doi-mat-khau`, trang đó thấy "chưa đăng nhập" và tự chuyển sang `/dang-nhap` trước khi lệnh về trang chủ kịp chạy (test đỏ, chờ thêm vẫn đỏ nên là lỗi thật). Sửa bằng cách về trang chủ (công khai) trước, rồi mới gọi hủy session.
  - Cảnh báo lint "Fast refresh" vì `AuthContext.tsx` vừa xuất component vừa xuất hook; tách hook sang `useAuth.ts`.
  - Một file báo cáo Surefire cũ (`SpacexApplicationTests`, 2026-10-30) làm tổng cộng tay ra 376 và 1 lỗi; số đúng là 375 theo Maven.
- **Dữ liệu thử:** tài khoản `kiemtra-5-3@example.com` trong database **ở máy**; không có trên production.
- **Học được:** test jsdom xanh chưa chứng minh luồng cookie/CSRF thật; thứ tự "đổi trạng thái" và "chuyển trang" có thể gây chuyển hướng ngoài ý muốn. Đường dẫn quay lại sau đăng nhập phải lọc (open redirect, vòng lặp).
- **Tiếp theo:** commit giao diện, đẩy hai nhánh, mở Pull Request (backend trước, rồi frontend); sau đó CP5.4 (tìm kiếm bài, cần duyệt phương án FULLTEXT cho `lessons`).

## 2026-11-09 — CP5.2: giao diện đọc (khóa, bài, Markdown)
- **Nhánh:** `feat/frontend-learning`; **chưa commit** (CP5.1 và CP5.2 cùng nằm trong thư mục làm việc).
- **Đã làm:**
  - Thêm dependency: `react-markdown` 10.1.0 (đã duyệt ở D27) và `remark-gfm` 4.0.1 (**hỏi người dùng riêng ngày 2026-11-09 và được duyệt**, vì bài mẫu `thong-tin-va-du-lieu` dùng bảng Markdown mà `react-markdown` một mình không dựng được bảng).
  - `learningApi.ts`: `fetchCourses`, `fetchLessons`, `fetchLesson` gọi qua `apiRequest`, kiểm dữ liệu đúng dạng, mã hóa slug trong đường dẫn.
  - `MarkdownContent.tsx`: dựng Markdown an toàn. Tiêu đề hạ một bậc (trang chỉ có một `h1`); HTML nhúng không thành phần tử; địa chỉ `javascript:`/`data:` bị bỏ; liên kết ngoài mở tab mới với `noopener noreferrer`; chỉ ảnh `/api/images/{id}` được hiển thị (ảnh từ trang ngoài có thể dùng để theo dõi người học nên chỉ hiện chữ thay thế); bảng GFM.
  - `useLoad.ts` (tải dữ liệu, đổi `key` thì tải lại, bỏ kết quả cũ đến muộn, `reload` cho nút Thử lại), `LoadError.tsx`, `lessonLinkState.ts` (trang khóa gửi kèm khóa đang xem sang trang bài, chỉ nhận slug đúng dạng).
  - Trang: `HomePage` (thêm danh sách khóa), `CoursePage` (`/khoa-hoc/:courseSlug`, tên khóa, mô tả, danh sách bài có tóm tắt; 404 → "Không tìm thấy khóa học"), `LessonPage` (`/bai/:lessonSlug`; 401 → mời đăng nhập, 404 → "Không tìm thấy bài học"; bỏ dòng `# Tên bài` ở đầu nội dung nếu trùng tên bài). Route thêm vào `App.tsx`.
  - Cập nhật `docs/TESTING.md`, `README.md`, `docs/PLAN.md`.
- **Kiểm tra:**
  - RED trước: 6 file test không nạp được vì thiếu module. Sau khi viết mã, `npm test` → 11 file, **93 test qua** (42 trước đó + 51 mới: `learningApi` 12, `MarkdownContent` 12, `HomePage` 6, `CoursePage` 7, `LessonPage` 11, `App` thêm 3); `npm run lint` exit 0; `npm run build` thành công. Lệnh đếm của `docs/TESTING.md` ra 93, khớp Vitest.
  - Kiểm chứng test bảo mật: cố ý đổi danh sách ảnh cho phép thành `/.*/` thì 2 test ảnh đỏ, rồi hoàn lại.
  - **Chạy thật trên Chrome** (ứng dụng Spring Boot ở máy cổng 8081 + frontend đã build + MySQL của Docker Compose): trang chủ hiện 3 khóa; mở thẳng `/khoa-hoc/tin-hoc-10` (F5 trên trang con) hiện đúng 2 bài; mở bài khi chưa đăng nhập hiện "Bạn cần đăng nhập để đọc bài học này."; sau khi đăng nhập bằng tài khoản học sinh thử, **đọc đủ 6 bài mẫu**, mỗi bài đúng một `h1`, không còn ký hiệu Markdown thô; bảng ở bài `thong-tin-va-du-lieu` hiển thị thành bảng; giao diện tối đúng; console không có thông báo nào. Đã tắt ứng dụng và xóa bản chép tạm `target/classes/static`.
- **Chưa kiểm / còn dở:**
  - Ảnh trong bài chưa xem trên trình duyệt (chỉ có test; bài mẫu chưa có ảnh, và tải ảnh lên cần đăng nhập Admin).
  - Nút "Đăng nhập" ở trang bài trỏ tới `/dang-nhap`, hiện là trang 404 vì giao diện đăng nhập thuộc CP5.3; do đó người dùng thật chưa đọc được bài trên trình duyệt cho tới CP5.3.
  - Chưa deploy, chưa commit, chưa mở Pull Request; chưa kiểm cỡ màn hình điện thoại.
  - `eslint-disable` cho `react-hooks/exhaustive-deps` trong `useLoad.ts` là chủ ý (`load` được tạo lại mỗi lần render); xóa dòng này thì oxlint cảnh báo.
- **Lỗi gặp và cách sửa:**
  - Thử đọc bài bằng iframe để kiểm 6 bài trong một lượt thì `contentDocument` là `null`: Spring Security mặc định gửi `X-Frame-Options: DENY`. Đổi sang mở từng bài thật; đây cũng xác nhận ứng dụng không cho nhúng vào trang khác.
  - Lỡ xóa dòng `eslint-disable` vì tưởng thừa, oxlint báo cảnh báo `exhaustive-deps`; khôi phục.
- **Dữ liệu thử:** tài khoản `kiemtra-5-2@example.com` (học sinh) tạo trong database **ở máy** để đọc bài; không có trên production.
- **Học được:** test jsdom xanh chưa chứng minh trang đẹp và đúng; phải mở trình duyệt thật. Test an toàn nên được "làm hỏng thử" để biết nó có đỏ thật không.
- **Tiếp theo:** CP5.3: giao diện đăng ký, đăng nhập, đăng xuất, thanh điều hướng theo `/api/me`; **cần chốt trước** việc đổi mật khẩu có hủy các session khác không (đụng auth, chưa có quyết định trong `DECISIONS.md`).

## 2026-11-09 — CP5.1: nền frontend (routing, API client, component test)
- **Nhánh:** `feat/frontend-learning`; **chưa commit** khi viết mục này.
- **Đã làm:**
  - Thêm dependency (đã duyệt ở D27): `react-router` 8.5.1, và dev: `@testing-library/react` 16.3.3, `@testing-library/dom` 10.4.2 (bắt buộc đi kèm bản React), `jsdom` 31.0.0. `react-markdown` chưa cài (để CP5.2).
  - `vite.config.ts`: thêm khối `test` (môi trường `jsdom`, `setupFiles`); `src/test-setup.ts` gọi `cleanup()` sau mỗi test.
  - `src/client.ts`: `apiRequest(method, path, data?)` và `ApiError` (`status`, `title`, `detail`, `fieldErrors`). Yêu cầu ghi tự gắn `X-XSRF-TOKEN` từ cookie `XSRF-TOKEN`; chưa có cookie thì gọi một `GET /api/health` để máy chủ cấp. Lỗi đọc theo `problem+json`; thân lỗi không phải JSON chỉ cho thông báo chung (không lộ nội dung thô); mất kết nối → `ApiError` status 0. `fetchHealth` giữ nguyên.
  - Routing: `main.tsx` bọc `BrowserRouter`; `App.tsx` khai báo route; `Layout.tsx` (thanh đầu trang có liên kết Space-X và nút sáng/tối), `pages/HomePage.tsx` (nội dung trang chủ cũ), `pages/NotFoundPage.tsx`.
  - Backend `common/web/SpaWebConfig.java`: máy chủ trả `index.html` cho đường dẫn không phải file và không thuộc `/api`, để F5 trên trang con của React Router không bị 404. Dùng `ResourceResolver` thay cho controller vì controller sẽ chặn cả file tĩnh thật; tắt bộ nhớ đệm của chuỗi resolver để đường dẫn lạ không làm đầy bộ nhớ.
  - Cập nhật `docs/TESTING.md` (bảng tổng quan, ba dòng mới, hai dòng cũ về `HealthStatus`/`ThemeToggle`), `README.md` (số test, trạng thái), `docs/PLAN.md`.
- **Kiểm tra:**
  - RED trước: `client.test.ts` không nạp được vì thiếu `./client`; 2 test của `App` fail; `SpaWebConfigTest` lỗi biên dịch vì thiếu `SpaWebConfig`. Hai file component test qua ngay vì kiểm component đã có.
  - Frontend (`src/frontend`): `npm test` → 6 file, **42 test qua** (10 cũ + 32 mới: `client` 18, `HealthStatus` 5, `ThemeToggle` 6, `App` 3); `npm run lint` exit 0; `npm run build` thành công. Lệnh đếm trong `docs/TESTING.md` ra 42, khớp Vitest.
  - Backend (`src/backend`): `SpaWebConfigTest` 11 test qua; `.\mvnw test` toàn bộ → `Tests run: 367, Failures: 0, Errors: 0`, `BUILD SUCCESS` (356 + 11). Cuối lần chạy vẫn có dòng `Surefire is going to kill self fork JVM` và các `WARN` của Hikari, giống các lần trước.
  - Chạy thật ở máy: MySQL của Docker Compose + `.\mvnw spring-boot:run` cổng 8081 với frontend đã build chép vào `target/classes/static`; `curl.exe`: `/`, `/khoa-hoc`, `/khoa-hoc/tin-hoc-10/bai/...`, `/dang-nhap` → 200 `text/html` (thân là `index.html`); `/assets/index-*.js` → 200 `text/javascript`; `/assets/khong-co.js` và `/favicon-khong-co.ico` → 404; `/api/khong-co` (khách) → 401 `problem+json`; `/api/health` → 200; `POST /khoa-hoc` thiếu CSRF → 403; `GET` trang vẫn nhận cookie `XSRF-TOKEN`. Đã tắt ứng dụng và xóa bản chép tạm.
- **Chưa kiểm / còn dở:** chưa mở trình duyệt xem trang chủ, trang 404 và nút sáng/tối (test chỉ chạy trong jsdom); chưa chạy `docker build`/bản deploy với `SpaWebConfig`; chưa commit, chưa mở Pull Request; MySQL của Docker Compose đang bật (tôi bật cho lần thử này, dùng `docker compose stop` để tắt, giữ dữ liệu).
- **Lỗi gặp và cách sửa:**
  - Test `GET /` của `SpaWebConfigTest` lần đầu fail: Spring Boot xử lý `/` bằng `forward` tới `index.html`, mà MockMvc không đi theo forward nên thân rỗng. Sửa test: kiểm `forwardedUrl("index.html")` và thêm test riêng cho `/index.html`.
  - Lệnh đếm test frontend chỉ đếm dòng bắt đầu `it(`/`test(`, nên `it.each` (4 trường hợp, 1 dòng) sẽ làm số đếm thấp hơn Vitest; đã tách thành 4 test rõ ràng và ghi lưu ý vào `docs/TESTING.md`.
  - Hướng thiết kế đầu (controller `@GetMapping` chuyển tiếp về `index.html`) bị loại vì controller có ưu tiên cao hơn bộ phục vụ file tĩnh nên sẽ nuốt luôn `/assets/*.js`.
- **Học được:** MockMvc không chạy forward như container thật nên test cần kiểm `forwardedUrl`; bật bộ nhớ đệm cho resolver "trả `index.html` cho mọi đường dẫn lạ" sẽ nhớ mỗi đường dẫn người lạ gõ vào, nên tắt.
- **Tiếp theo:** CP5.2: giao diện đọc (danh sách khóa, danh sách bài, trang đọc bài bằng `react-markdown`, hiển thị ảnh, chuyển sang đăng nhập khi gặp 401), đọc thử đủ 6 bài mẫu trên trình duyệt; cần cài `react-markdown` (đã duyệt ở D27).

## 2026-11-09 — Cập nhật README theo trạng thái sau Tuần 4
- **Nhánh:** `feat/frontend-learning` (tạo từ `main` ở `b96b9d1`); commit tài liệu đầu tiên của nhánh, chưa có mã frontend mới.
- **Đã làm:**
  - `README.md` đang dừng ở mốc 2026-10-31 (ghi "chưa có đăng nhập, bài học", Tuần 3 `In progress`, Tuần 4 `Planned`). Sửa theo `docs/API.md` và `docs/TESTING.md`: banner trạng thái, bảng tính năng, lộ trình.
  - Bảng tính năng: các mục tài khoản, xem khóa/bài, đọc bài, Admin quản lý khóa/bài/ảnh và dữ liệu mẫu chuyển từ `Planned` sang `Tested`, kèm ghi chú **chỉ API, chưa có giao diện**; tìm kiếm, tiến độ, Google, chatbot, Agent, dashboard, quản lý người dùng vẫn `Planned`.
  - Lộ trình: Tuần 3 `Done`; Tuần 4 `Done (mã)` kèm việc còn lại là nạp dữ liệu mẫu lên bản deploy; Tuần 5 ghi thêm giao diện đọc bài và đăng ký/đăng nhập (khớp mục 2026-11-08). Thêm dòng nhắc kiểm `/api/courses` sau khi nạp.
  - Ghi kế hoạch Tuần 5 (checkpoint CP5.0 đến CP5.7) vào `docs/PLAN.md` (file chỉ ở máy).
  - **CP5.0, dữ liệu mẫu trên production:** theo yêu cầu của người dùng, Claude cho script `scripts/seed-sample-data.ps1 -FromEnv` đọc `ADMIN_EMAIL`/`ADMIN_PASSWORD` trong `.env` ở máy (không in ra) và nạp vào https://space-x-47f1.onrender.com. Sau đó cập nhật `README.md`, `docs/PLAN.md` cho khớp.
- **Kiểm tra:**
  - Trước khi nạp, `GET /api/courses` trên Render trả 200 `[]` (lần gọi đầu mất 83,8 giây, có vẻ lúc Render đang ngủ; mới đo một lần).
  - `-DryRun`: đăng nhập Admin thành công bằng thông tin trong `.env` ở máy (trùng với Admin của Render), báo sẽ tạo 3 khóa và 6 bài.
  - Nạp thật: `Khóa: tạo 3, bỏ qua 0. Bài: tạo 6 (đăng 6), bỏ qua 0.`
  - Chạy lần hai: `Khóa: tạo 0, bỏ qua 3. Bài: tạo 0 (đăng 0), bỏ qua 6.` (không tạo trùng).
  - Từ bên ngoài, `curl.exe`: `GET /api/courses` → 200 với `tin-hoc-10`, `tin-hoc-11`, `tin-hoc-12`; `GET /api/courses/{slug}/lessons` của cả ba khóa → 200, mỗi khóa 2 bài đã đăng.
  - Phần sửa tài liệu: không chạy lại test hay build. Số liệu trong README (356 test backend = 249 + 107, 10 test frontend, PR #19 đến #24) lấy từ `docs/TESTING.md` và nhật ký 2026-11-08, không đếm lại.
- **Lỗi gặp và cách sửa:** lần nạp trước (người dùng báo đã chạy mà `/api/courses` vẫn `[]`) **không xác định được nguyên nhân** vì không có kết quả script in ra; lần này `-DryRun` thấy production đang trống. Script hỏi `yes` bằng `Read-Host` nên Claude chạy bằng `"yes" | powershell.exe -File ...` để đưa câu trả lời qua stdin.
- **Chưa kiểm / còn dở:**
  - Chưa đọc nội dung từng bài qua `GET /api/lessons/{slug}` (cần đăng nhập); "đọc thử đủ 6 bài" thuộc CP5.2. Chưa thử ảnh trên production (giới hạn upload qua proxy Render, lưu ảnh vào Aiven).
  - Dữ liệu mẫu là 6 bài ngắn, có dòng đánh dấu "Bài mẫu", chưa phải bài tự viết đầy đủ.
  - Bài nộp Tuần 4 trong `nop-bai/` vẫn ghi dữ liệu mẫu chưa nạp; cần dựng lại trước khi nộp.
  - README ghi "Lộ trình 11 tuần" còn `CLAUDE.md` ghi 12 tuần, chưa đối chiếu với `docs/DECISIONS.md`.
- **Học được:** `-DryRun` đã đăng nhập thật nên vừa kiểm được thông tin Admin vừa cho biết production trống, trước khi ghi gì.
- **Tiếp theo:** CP5.1 (nền frontend: routing, API client gắn `X-XSRF-TOKEN`, component test cho `HealthStatus` và `ThemeToggle`) trên nhánh này.

## 2026-11-08 — Đóng việc 4.6 và Tuần 4; tạo bài nộp Tuần 4
- **Nhánh:** `main` (chỉ đồng bộ, không commit); mục này được commit cùng README ở commit tài liệu đầu tiên của nhánh `feat/frontend-learning` (xem mục 2026-11-09).
- **Đã làm:**
  - Gộp Pull Request #24 (`feat/sample-data`) vào `main` ở `b96b9d1`; đồng bộ `main` ở máy bằng `git switch main`, `git pull`. Người dùng báo deploy thành công.
  - Tuần 4 xong về mặt mã: 4.1 đến 4.6 đều đã gộp (PR #19 đến #24).
  - Tạo bài nộp Tuần 4 trong `nop-bai/` (`.docx` và `.pdf`, chỉ ở máy, bị `.gitignore` chặn): cấu trúc theo bài Tuần 3, số liệu lấy từ nhật ký, `docs/TESTING.md` và kết quả CI.
- **Kiểm tra:**
  - Tự gọi `curl.exe` trên Render: `GET /api/health` → 200 `{"status":"UP"}`; `GET /api/courses` → 200 `[]` (gọi nhiều lần trong buổi).
  - CI trên GitHub đọc qua API công khai (máy không có `gh`): sáu commit gộp `66ad1da`, `8eccae5`, `a578fa9`, `f6814c8`, `45d6d0b`, `b96b9d1` đều có đủ sáu job (`backend`, `frontend`, `docker`, `gitleaks`, hai `CodeQL`) báo `success`.
  - Bài nộp: xuất PDF 6 trang bằng Word. **Chưa xem được hình từng trang** (máy không có công cụ vẽ PDF thành ảnh); mới kiểm bằng chữ, cần mở file xem lại bố cục trước khi nộp.
- **Chưa kiểm / còn dở:**
  - **Dữ liệu mẫu chưa có trên production:** người dùng báo đã chạy script nạp, nhưng `GET /api/courses` vẫn trả `[]`, nên chưa thể ghi là đã nạp. Chưa rõ nguyên nhân (chưa có kết quả in ra của script). Bài nộp đang ghi đúng tình trạng này; sau khi nạp xong cần gọi lại `/api/courses`, rồi dựng lại bài nộp (đổi nội dung các chỗ liên quan).
  - Chưa kiểm API Admin của 4.4, 4.5, 4.6 trên production (cần đăng nhập Admin); giới hạn upload ảnh qua proxy Render và lưu ảnh vào Aiven chưa thử.
- **Lỗi gặp và cách sửa:** `python -c` in tiếng Việt ra console Windows bị `UnicodeEncodeError` (cp1252); ghi kết quả ra tệp UTF-8 rồi đọc. Máy chưa có thư viện `docx` cho Node, nên cài vào thư mục tạm (không đụng dự án) để dựng bài nộp.
- **Học được:** khi người dùng báo "xong" một việc có kiểm được từ bên ngoài (dữ liệu trên production), gọi API công khai để xác nhận trước khi ghi nhật ký.
- **Tiếp theo:** nạp dữ liệu mẫu lên Render (xem lệnh trong `README.md`), xác nhận bằng `/api/courses`, cập nhật bài nộp rồi mới nộp; sau đó Tuần 5 (giao diện đọc, đăng ký và đăng nhập trong React, tìm kiếm, tiến độ học tập, quản lý người dùng).

## 2026-11-08 — Việc 4.6: dữ liệu mẫu (3 khóa, 6 bài)
- **Nhánh:** `feat/sample-data` (tạo từ `main` ở `45d6d0b`, sau khi PR #23 của 4.5 đã gộp)
- **Đã làm:**
  - Thư mục `seed/`: `courses.json` (ba khóa `tin-hoc-10`, `tin-hoc-11`, `tin-hoc-12`), `lessons.json` (6 bài, mỗi khóa 2 bài) và 6 tệp Markdown trong `seed/lessons/<khóa>/`. Mỗi bài có dòng đánh dấu "Bài mẫu" để người dùng thay bằng nội dung tự viết sau.
  - `scripts/seed-sample-data.ps1`: đăng nhập Admin, tạo khóa, tạo bài, đăng bài qua **API Admin có sẵn** (không đụng schema, không cần migration). Chạy lại an toàn: khóa/bài đã có (theo slug) bị bỏ qua, không ghi đè bài đã sửa. Có `-DryRun`, `-NoPublish`, `-FromEnv` (đọc `ADMIN_EMAIL`/`ADMIN_PASSWORD` từ `.env`). Nạp vào địa chỉ không phải máy mình thì đòi gõ "yes". Mật khẩu chỉ nằm trong bộ nhớ.
  - `SeedDataTest` (8 test): dữ liệu mẫu qua đúng luật validate của `AdminCourseRequest`/`AdminLessonRequest`, slug không trùng, bài thuộc khóa có thật, thứ tự không trùng, không có tệp Markdown mồ côi, không có BOM/`<script`/`<iframe`/`javascript:`.
  - Cập nhật `README.md` (cách nạp) và `docs/TESTING.md` (bảng tổng quan 249 test không cần Docker, thêm dòng 4.6).
- **Kiểm tra:**
  - `.\mvnw test -Dtest=SeedDataTest` trong `src/backend`: `Tests run: 8, Failures: 0, Errors: 0`, `BUILD SUCCESS`.
  - Lệnh đếm trong `docs/TESTING.md` ra 356 = 249 + 107, khớp bảng.
  - Theo ghi chú trong `docs/TESTING.md`, script đã được chạy thật ở máy (`-DryRun`, nạp thật, chạy lại lần hai bỏ qua hết, đọc lại bài qua API thấy nội dung giống hệt tệp gốc). Lần này chỉ chạy lại `SeedDataTest`: **chưa chạy lại script** và **chưa chạy lại toàn bộ `.\mvnw test`**.
- **Chưa kiểm:** chưa mở Pull Request, chưa nạp vào bản deploy trên Render; phần script PowerShell không có test tự động (chỉ kiểm tay); nội dung 6 bài là bài mẫu ngắn, chưa phải bài học đầy đủ.
- **Lỗi gặp và cách sửa:** Windows PowerShell 5.1 trả mảng JSON của `ConvertFrom-Json` như một đối tượng (và `[]` thành `$null`), còn PowerShell 7 tách phần tử; script có hàm `ConvertFrom-JsonArray` để cho cùng kết quả ở cả hai bản. Truyền chuỗi JSON thẳng cho `curl.exe` bị mất dấu `"` ở 5.1, nên script ghi thân JSON ra tệp UTF-8 rồi gửi bằng `--data-binary @tệp`.
- **Học được:**
  - Dữ liệu mẫu nên đi qua API có sẵn thay vì chèn thẳng SQL: được kiểm quyền, validate và `created_by` như dữ liệu thật.
  - Test dữ liệu mẫu bằng chính lớp validate của API để biết trước script có bị từ chối hay không.
- **Tiếp theo:** commit nhánh này, mở Pull Request, chờ CI xanh rồi gộp; sau đó nạp dữ liệu mẫu vào bản deploy bằng `-BaseUrl` và kiểm tay trên trình duyệt.

## 2026-11-08 — Đóng việc 4.4 (đã deploy); việc 4.5: ảnh bài học
- **Nhánh:** `feat/lesson-images` (tạo từ `main` ở `f6814c8`, sau khi PR #22 của 4.4 đã gộp)
- **Đã làm:**
  - Gộp Pull Request #22 (`feat/admin-lessons`) vào `main` ở `f6814c8`; `CI`, `Security`, `CodeQL` xanh trên Pull Request và deploy thành công theo báo của người dùng. Tự gọi `GET /api/health` trên production → 200; không tự kiểm được API Admin bài học trên production vì cần đăng nhập Admin.
  - Việc 4.5, bốn endpoint: tải ảnh lên `POST /api/admin/lessons/{id}/images` (multipart, phần tên `file`; 201 `{ id, url }`), danh sách `GET /api/admin/lessons/{id}/images`, xóa `DELETE /api/admin/images/{id}` (204), xem `GET /api/images/{id}` (cần đăng nhập). Ảnh lưu trong MySQL (cột `MEDIUMBLOB` đã có từ `V1`), không cần migration mới.
  - **Kiểm tra nội dung ảnh không dùng thư viện ngoài:** `ImageInspector` nhận diện loại ảnh từ chữ ký byte (PNG, JPEG) kèm đọc phần đầu tệp bằng `ImageIO` của JDK để lấy kích thước (chỉ đọc header, không giải mã cả ảnh, và dùng `MemoryCacheImageInputStream` để không sinh tệp tạm); WebP (JDK không đọc được) kiểm cấu trúc RIFF bằng tay. Chỉ nhận PNG/JPEG/WebP, từ chối HTML, SVG, GIF, tệp rỗng, tệp có chữ ký đúng nhưng phần còn lại là rác, và ảnh có cạnh > 4096 điểm ảnh (chặn "bom giải nén"). Loại lưu và trả về là loại nhận diện được, không phải loại trình duyệt khai báo.
  - Giới hạn 1 MB ở hai tầng: `spring.servlet.multipart.max-file-size=1MB` (request tối đa 2 MB) và kiểm lại trong `ImageService`; vượt → 413 với câu cố định.
  - Quyền xem: học sinh chỉ xem ảnh của bài Đã đăng; ảnh bài Nháp và ảnh không tồn tại trả cùng một 404; Admin xem tất cả. Dữ liệu ảnh chỉ được tải sau khi qua kiểm quyền (truy vấn đầu dùng dạng chiếu `LessonImageInfo` không có cột `data`). Tên tệp chỉ để hiển thị (bỏ đường dẫn, ký tự điều khiển, tối đa 255 ký tự). Phản hồi ảnh có `X-Content-Type-Options: nosniff` và `Cache-Control: private`.
  - Thêm trong `vn.spacex.image`: `ImageType`, `ImageInspector`, `LessonImage`, `LessonImageInfo`, `LessonImageRepository`, `ImageService`, `AdminImageController`, `ImageController`, ba exception, ba record phản hồi. Thêm ba handler và một ghi đè `handleMaxUploadSizeExceededException` trong `GlobalExceptionHandler`; thêm hai dòng cấu hình multipart trong `application.properties`.
  - Cập nhật `docs/API.md` (mục "Ảnh bài học") và `docs/TESTING.md`.
- **Kiểm tra:**
  - Viết test trước: lần chạy RED là 114 lỗi biên dịch vì thiếu các class mới.
  - `.\mvnw test` trong `src/backend`: **348 test, 0 lỗi, `BUILD SUCCESS`** (241 không cần Docker + 107 tích hợp trên MySQL 8.4 thật; trước đó 283, việc 4.5 thêm 65: `ImageInspectorTest` 12, `ImageServiceTest` 20, `ImageControllerTest` 16, `ImageIntegrationTest` 17). Đếm bằng lệnh trong `docs/TESTING.md` ra 348, khớp Surefire. Cuối lần chạy có dòng `[ERROR] Surefire is going to kill self fork JVM` (JVM tắt chậm hơn 30 giây); build vẫn thành công.
  - Chạy thật ở máy: MySQL 8.4 của Docker Compose + `.\mvnw spring-boot:run`, rồi gọi `curl.exe` với ba vai (khách, học sinh, Admin) và tệp ảnh thật. Khách upload → 401; học sinh → 403; Admin thiếu CSRF → 403. Tải PNG thật (đặt tên `../../etc/passwd.png`) → 201, lưu tên `passwd.png`; tải về lại bằng `GET` thì SHA-256 giống hệt tệp đã gửi, `Content-Type: image/png`, `X-Content-Type-Options: nosniff`, `Cache-Control: max-age=3600, private`. JPEG đặt tên `.png` và khai báo `image/gif` → 201 và được lưu là `image/jpeg`. HTML đặt tên `.png`, SVG chứa script, ảnh nén mạnh 6000×6000 (4514 byte) → 415. Tệp 1,2 MB và 3 MB → 413 (cả hai giới hạn: của container và của service). Thiếu phần `file` → 400; bài không tồn tại → 404. Học sinh xem ảnh bài nháp → 404, phản hồi giống hệt ảnh không tồn tại (sau khi bỏ id); đăng bài → học sinh xem được đúng byte; ẩn lại → 404; khách → 401; `id` không phải số → 400 câu cố định. Xóa ảnh: học sinh 403, Admin 204, xóa lần hai 404; xóa bài kéo theo ảnh còn lại; xóa khóa → 204.
  - **Chưa kiểm:** chưa mở Pull Request, chưa deploy, chưa kiểm trên Render (đặc biệt: giới hạn upload qua proxy của Render và kích thước cột/bộ nhớ khi lưu ảnh vào Aiven); chưa chạy lại test frontend (không sửa frontend). Chưa có giới hạn số ảnh mỗi bài. WebP chỉ kiểm cấu trúc RIFF, chưa kiểm kích thước điểm ảnh. Với thiếu phần `file`, `detail` là câu mặc định của Spring "Required part 'file' is not present." (không lặp lại dữ liệu người gửi).
- **Lỗi gặp và cách sửa:**
  - Lỗi biên dịch trong test: `0x90` vượt kiểu `byte`, sửa bằng `(byte) 0x90`.
  - Ba test của `ImageServiceTest` lỗi do cách viết test: gọi một hàm có `when(...)` bên trong `thenReturn(...)` của một `when(...)` khác (`UnfinishedStubbing`) và stub thừa (`UnnecessaryStubbing`). Sửa bằng cách tạo đối tượng giả ra biến riêng trước và dùng `lenient()` cho stub không phải test nào cũng dùng.
  - Lúc viết `ImageService.upload` có gõ nhầm lời gọi `withUrl()` không tồn tại; phát hiện khi đọc lại trước khi biên dịch và sửa ngay.
- **Học được:**
  - Không tin tên tệp và `Content-Type` do trình duyệt gửi; nhận diện loại ảnh từ nội dung và lưu loại nhận diện được. SVG bị loại hẳn vì có thể chứa script.
  - Ảnh nén mạnh nhỏ vài KB vẫn có thể rộng hàng chục nghìn điểm ảnh, nên phải giới hạn cả kích thước điểm ảnh chứ không chỉ số byte.
  - `MockMvc` không áp giới hạn upload của container, nên test dùng MockMvc phải kiểm lại giới hạn trong code và giới hạn thật của container phải kiểm bằng server thật (`curl.exe`).
  - Hibernate `ddl-auto=validate` với cột `MEDIUMBLOB` cần khai báo kiểu cột (`@JdbcTypeCode(SqlTypes.LONGVARBINARY)`) để khớp; test tích hợp trên MySQL thật chứng minh điều này.
  - Dạng chiếu của Spring Data (`LessonImageInfo`) chỉ SELECT các cột cần, nhờ đó kiểm quyền xong mới tải dữ liệu ảnh.
- **Tiếp theo:** commit theo thứ tự `test` → `feat` → `docs`, push, mở Pull Request cho `feat/lesson-images`, đợi `CI`, `Security`, `CodeQL` xanh rồi mới gộp; sau đó việc 4.6 (dữ liệu mẫu: 3 khóa Tin học 10/11/12 và vài bài; cách nạp cần hỏi trước vì đụng database).

## 2026-11-08 — Đóng việc 4.3 (đã deploy), bắt đầu 4.4
- **Nhánh:** `feat/admin-lessons` (tạo từ `main` ở `a578fa9`; mục này là commit tài liệu đầu tiên của nhánh)
- **Đã làm:**
  - Gộp Pull Request #21 (`feat/admin-courses`) vào `main` ở `a578fa9`; `CI`, `Security`, `CodeQL` xanh trên Pull Request theo báo của người dùng; đồng bộ `main` ở máy bằng `git switch main`, `git pull`.
  - Người dùng báo đã chạy script kiểm tra trên Render (đăng nhập Admin, tạo khóa thử, thử trùng slug, sửa, xóa) và deploy thành công. Chi tiết từng mã trả về là kết quả người dùng tự thấy, chưa dán vào nhật ký này.
  - Chốt Tuần 4: 4.1, 4.2, 4.3 xong và đã deploy; còn 4.4 (Admin bài học), 4.5 (ảnh), 4.6 (dữ liệu mẫu).
- **Kiểm tra:**
  - Tự gọi `curl.exe` sau khi người dùng báo deploy: `GET /api/health` → 200 `{"status":"UP"}`; `GET /api/courses` → 200 `[]` (khớp với việc khóa thử đã được xóa).
  - Không tự kiểm được `POST /api/admin/courses` trên production vì cần đăng nhập Admin; việc đó do người dùng chạy bằng script riêng (mật khẩu nhập tại máy).
  - Không xem được trạng thái CI trên `main` bằng lệnh (máy không có `gh`).
- **Lỗi gặp và cách sửa:** không có lỗi mới.
- **Học được:** kiểm tra production cần tài khoản thật nên script phải hỏi mật khẩu lúc chạy (`Read-Host -AsSecureString`), không ghi mật khẩu vào file hay lệnh; script cũng phải tự dọn dữ liệu thử (xóa khóa vừa tạo) để production không còn rác.
- **Tiếp theo:** việc 4.4 trên `feat/admin-lessons`: `GET /api/admin/lessons` (lọc `courseId`, `status`, phân trang), `GET /{id}`, `POST` (mặc định `DRAFT`, `created_by` từ session), `PUT`, `PATCH .../status`, `DELETE`. Viết test (RED) trước rồi code (GREEN). Kết quả ở mục ngay bên dưới.

## 2026-11-08 — Việc 4.4: Admin quản lý bài học
- **Nhánh:** `feat/admin-lessons`
- **Đã làm:**
  - Sáu endpoint dưới `/api/admin/lessons`: danh sách (lọc `courseId`, `status`, phân trang `page`/`size` tối đa 50), chi tiết, tạo (201, luôn `DRAFT`), sửa (200, được chuyển bài sang khóa khác), đổi trạng thái `PATCH .../status` (200), xóa (204). Luật `/api/admin/**` yêu cầu `ADMIN` đã có sẵn, nên không sửa `SecurityConfig`; vì thế cả các `GET` cũng chỉ Admin gọi được, bài nháp không lộ cho học sinh.
  - `created_by` lấy từ session (`@AuthenticationPrincipal`); `createdBy` và `status` gửi lên trong body bị bỏ qua. Slug bài là duy nhất trên toàn bộ bài (không chỉ trong một khóa); trùng → 409 (kiểm tra trước ở service và chốt cuối bằng `UNIQUE` của database).
  - Thêm trong `vn.spacex.lesson`: `AdminLesson` (entity đọc/ghi đủ cột, `created_by` không cập nhật được), `AdminLessonRepository`, `AdminLessonRequest`, `AdminLessonStatusRequest`, `AdminLessonResponse`, `AdminLessonSummaryResponse`, `AdminLessonService`, `AdminLessonController`, `LessonSlugAlreadyExistsException`; thêm `LessonRepository.findForAdmin` (truy vấn lọc có phân trang trên entity `Lesson` không có `content_md`, nên danh sách không tải nội dung); thêm `common.api.PageResponse`; thêm một handler 409 trong `GlobalExceptionHandler`. Không cần migration mới. Lúc này bảng `lessons` có ba entity cùng ánh xạ: `Lesson` (danh sách), `LessonContent` (đọc nội dung, chỉ-đọc), `AdminLesson` (ghi).
  - Xóa bài kéo theo ảnh, tiến độ, đoạn tri thức nhờ `ON DELETE CASCADE` của database (không code thêm); khóa học xóa được sau khi hết bài.
  - Cập nhật `docs/API.md` (mục "Bài học (Admin)") và `docs/TESTING.md`.
- **Kiểm tra:**
  - Viết test trước: lần chạy RED là 112 lỗi biên dịch vì thiếu các class mới.
  - `.\mvnw test` trong `src/backend`: **283 test, 0 lỗi, `BUILD SUCCESS`** (193 không cần Docker + 90 tích hợp trên MySQL 8.4 thật; trước đó 210, việc 4.4 thêm 73: `AdminLessonControllerTest` 23, `AdminLessonServiceTest` 17, `AdminLessonRequestValidationTest` 14, `AdminLessonIntegrationTest` 19). Đếm bằng lệnh trong `docs/TESTING.md` ra 283, khớp Surefire. Cuối lần chạy có dòng `[ERROR] Surefire is going to kill self fork JVM` vì JVM tắt chậm hơn 30 giây sau khi test xong; build vẫn thành công.
  - Chạy thật ở máy: MySQL 8.4 của Docker Compose + `.\mvnw spring-boot:run`, rồi gọi khoảng 45 lệnh `curl.exe` với ba vai (khách, học sinh, Admin). Khách → 401; học sinh `GET` và `POST` → 403; Admin thiếu CSRF → 403; dữ liệu sai → 400 kèm danh sách trường lỗi; tạo hợp lệ (kèm `createdBy: 999`, `status: PUBLISHED` trong body) → 201 và `status` là `DRAFT`; trùng slug → 409. Bài nháp: danh sách công khai `[]`, học sinh đọc → 404, Admin xem chi tiết → 200. Đăng → danh sách công khai có bài, học sinh đọc → 200 kèm nội dung Markdown; ẩn lại → học sinh đọc → 404. `PATCH` với `ARCHIVED` → 400. Sửa → 200; sửa sang slug của bài khác → 409; sửa sang khóa không tồn tại → 404; sửa bài không tồn tại → 404. Danh sách: lọc theo khóa, lọc `PUBLISHED` (rỗng), `page=1&size=1` (`totalItems` 2); `size=51`, `page=-1` → 400; `status` sai và `id` không phải số → 400 với câu cố định. Xóa khóa còn bài → 409; xóa bài → 204, xóa lần hai → 404; xóa hết bài rồi xóa khóa → 204; danh sách cuối cùng rỗng.
  - **Chưa kiểm:** chưa mở Pull Request, chưa deploy, chưa kiểm trên Render; chưa chạy lại test frontend (không sửa frontend). Với `size=51` và `page=-1`, `detail` là câu mặc định của Spring "Validation failure" (không lặp lại giá trị người gửi).
- **Lỗi gặp và cách sửa:** không có lỗi ngoài ý muốn ở bước chạy test.
- **Học được:**
  - Bảng `lessons` có ba entity khác nhau cho ba nhu cầu (danh sách không có nội dung, đọc nội dung chỉ-đọc, ghi đủ cột): tách như vậy để đường đọc công khai không bao giờ tải hay ghi nhầm cột. `ddl-auto=validate` vẫn qua.
  - Bộ lọc tùy chọn viết một lần bằng `(:param is null or cột = :param)` trong JPQL; truy vấn đếm tổng cho phân trang do Spring Data tự sinh.
  - Ràng buộc trên tham số `@RequestParam` (`@Min`, `@Max`) tự cho 400 khi không cần thêm cấu hình, nên không phải bắt `ConstraintViolationException`.
  - `ON DELETE CASCADE` của database lo việc xóa ảnh, tiến độ, đoạn tri thức; test tích hợp chứng minh bằng cách tạo đủ ba loại dữ liệu con rồi xóa bài.
- **Tiếp theo:** commit theo thứ tự `test` → `feat` → `docs`, push, mở Pull Request cho `feat/admin-lessons`, đợi `CI`, `Security`, `CodeQL` xanh rồi mới gộp; sau đó việc 4.5 (ảnh bài học, `feat/lesson-images`).

## 2026-11-08 — Việc 4.3: Admin quản lý khóa học; kiểm tra 4.2 trên Render
- **Nhánh:** `feat/admin-courses` (tạo từ `main` ở `8eccae5`, sau khi PR #20 của 4.2 đã gộp)
- **Đã làm:**
  - `POST /api/admin/courses` (201), `PUT /api/admin/courses/{id}` (200), `DELETE /api/admin/courses/{id}` (204). Luật `/api/admin/**` yêu cầu `ADMIN` đã có sẵn trong `SecurityConfig`, nên không sửa file đó.
  - Thêm trong `vn.spacex.course`: `AdminCourseRequest` (validate và chuẩn hóa), `AdminCourseResponse`, `AdminCourseService`, `AdminCourseController`, `CourseSlugAlreadyExistsException`, `CourseHasLessonsException`. Thêm `Course.create/update`, `CourseRepository.existsBySlug/existsBySlugAndIdNot`, `LessonRepository.existsByCourseId`, hai handler 409 trong `GlobalExceptionHandler`. Không cần migration mới.
  - Quy tắc: slug chỉ chữ thường không dấu, số và gạch ngang giữa các từ (tối đa 100); tiêu đề tối đa 200; mô tả tối đa 500, chỉ khoảng trắng thì lưu `null`; `sortOrder` bắt buộc, không âm. Slug trùng → 409; xóa khóa còn bài (kể cả bài nháp) → 409. Cả hai được kiểm tra trước ở service và còn có chốt cuối ở database (`UNIQUE` của slug, khóa ngoại `RESTRICT`).
  - Cập nhật `docs/API.md` và `docs/TESTING.md`.
  - Trên Render, kiểm tra việc 4.2 (xem mục Kiểm tra).
- **Kiểm tra:**
  - Viết test trước: lần chạy RED là lỗi biên dịch vì thiếu các class mới.
  - `.\mvnw test` trong `src/backend`: **210 test, 0 lỗi, `BUILD SUCCESS`** (139 không cần Docker + 71 tích hợp; trước đó 159, việc 4.3 thêm 51: `AdminCourseControllerTest` 15, `AdminCourseServiceTest` 11, `AdminCourseRequestValidationTest` 11, `AdminCourseIntegrationTest` 14). Đếm bằng lệnh trong `docs/TESTING.md` ra 210, khớp Surefire.
  - Render (URL production), gọi `curl.exe`: `GET /api/health` → 200; `GET /api/courses` → 200 `[]`; đăng nhập Admin → 200 vai trò `ADMIN`; `GET /api/lessons/khong-ton-tai` → 404 `problem+json` "Không tìm thấy bài học". Vậy 4.2 đã chạy trên production, nhưng chưa thử với bài đã đăng thật vì chưa có dữ liệu mẫu (việc 4.6).
  - Chạy thật ở máy: MySQL 8.4 của Docker Compose + `.\mvnw spring-boot:run`, rồi gọi 17 lệnh `curl.exe` với ba vai (khách, học sinh, Admin). Kết quả: khách `POST` → 401; học sinh `POST` và `DELETE` → 403; Admin thiếu header CSRF → 403; dữ liệu sai → 400 kèm danh sách trường lỗi; tạo hợp lệ → 201; tạo trùng slug → 409; `GET /api/courses` công khai thấy hai khóa vừa tạo; sửa → 200; sửa sang slug của khóa khác → 409; sửa khóa không tồn tại → 404; `id` không phải số → 400; xóa → 204 (hai khóa), xóa lần hai → 404; cuối cùng `GET /api/courses` → `[]`. Chưa thử "xóa khóa còn bài → 409" bằng `curl.exe` (chưa có API tạo bài, việc 4.4); ca này chỉ được kiểm bằng test tích hợp.
  - Phát hiện khi chạy thật: `id` không phải số trả 400 với `detail` là câu mặc định của Spring "Failed to convert 'id' with value: 'abc'" (có lặp lại giá trị người gửi); khác với các lỗi khác của dự án dùng câu cố định. Đã sửa: ghi đè `handleTypeMismatch` trong `GlobalExceptionHandler` trả câu cố định "Tham số trong đường dẫn không hợp lệ". Test được sửa trước và thấy đỏ đúng lý do (`detail` vẫn là câu mặc định), sau đó xanh.
  - **Chưa kiểm:** chưa mở Pull Request, chưa kiểm trên Render; chưa chạy lại test frontend (không sửa frontend). Database ở máy còn lại tài khoản học sinh thử `kiemtra-4-3@example.com`.
- **Lỗi gặp và cách sửa:**
  - Gọi API đăng nhập bằng `curl.exe` từ Windows PowerShell 5.1 bị 400 `Failed to read request`: PowerShell làm mất dấu `"` trong chuỗi JSON truyền cho chương trình ngoài. Sửa bằng cách escape thành `\"` trong chuỗi.
  - Đăng nhập báo 401 lần đầu vì dùng nhầm email giữ chỗ chứ không phải `ADMIN_EMAIL` thật.
- **Học được:**
  - Luật `"/api/**"` bắt đăng nhập cho mọi đường dẫn, kể cả đường dẫn chưa tồn tại, nên 401 của khách không chứng minh một endpoint đã được deploy; phải đăng nhập rồi gọi mới phân biệt được bản mới với bản cũ.
  - `AdminBootstrap` bỏ qua khi đã có `ADMIN`, nên đổi `ADMIN_PASSWORD` trên Render không đổi mật khẩu thật; đổi mật khẩu bằng `PUT /api/me/password`.
  - Kiểm tra trước (`existsBy...`) cho thông báo lỗi rõ ràng, còn ràng buộc của database mới là chốt cuối khi hai yêu cầu đến cùng lúc; cần cả hai.
  - `existsBySlugAndIdNot` cho phép `PUT` giữ nguyên slug của chính khóa đó mà không bị báo trùng.
- **Tiếp theo:** chạy thật bằng `spring-boot:run` + `curl.exe`; commit theo thứ tự `test` → `feat` → `docs`; mở Pull Request cho `feat/admin-courses`, đợi `CI`, `Security`, `CodeQL` xanh rồi mới gộp; sau đó việc 4.4 (Admin quản lý bài học).

## 2026-11-07 — Việc 4.2: đọc nội dung một bài học
- **Nhánh:** `feat/lesson-detail` (tạo từ `main` ở `66ad1da` sau khi PR #19 của 4.1 đã gộp và CI trên `main` xanh)
- **Đã làm:**
  - `GET /api/lessons/{slug}` cần đăng nhập: trả `id`, `slug`, `title`, `summary`, `contentMd`; không có trạng thái, mã khóa hay người tạo.
  - Bài không tồn tại và bài `DRAFT` trả **cùng một** 404 `problem+json` (`detail` cố định "Không tìm thấy bài học"), nên không dò được slug nào là bài nháp.
  - Thêm 6 file trong `vn.spacex.lesson`: `LessonContent` (entity chỉ-đọc), `LessonContentRepository`, `LessonDetailResponse`, `LessonNotFoundException`, `LessonService`, `LessonController`; thêm một handler trong `GlobalExceptionHandler`. Không sửa `SecurityConfig`.
  - Cập nhật `docs/API.md` và `docs/TESTING.md`.
- **Kiểm tra:**
  - Viết test trước: lần chạy RED là lỗi biên dịch vì thiếu đúng 6 class (chưa thấy từng test đỏ riêng lẻ). Sau khi có code: `LessonControllerTest` 4, `LessonServiceTest` 2 và `LessonIntegrationTest` 5 (MySQL 8.4 thật) đều qua.
  - `.\mvnw test` trong `src/backend`: **159 test, 0 lỗi, `BUILD SUCCESS`** (102 không cần Docker + 57 tích hợp; trước đó 148, việc 4.2 thêm 11). Đếm bằng lệnh trong `docs/TESTING.md` ra 159, khớp bảng.
  - **Chưa kiểm:** chưa chạy server thật rồi gọi bằng `curl.exe`; chưa kiểm trên Render sau khi gộp; chưa chạy lại test frontend (không sửa frontend).
- **Lỗi gặp và cách sửa:** không có lỗi ngoài ý muốn ở bước chạy test.
- **Học được:**
  - Entity `Lesson` cố ý không ánh xạ `content_md` để danh sách bài không tải nội dung. Nên trang đọc dùng entity thứ hai `LessonContent` trên cùng bảng `lessons`, đánh dấu `@Immutable` (Hibernate không ghi lớp này xuống database). Hai entity cùng một bảng vẫn qua `ddl-auto=validate`.
  - Cùng một câu 404 cho "không tồn tại" và "bài nháp" là cách tránh lộ thông tin: người ngoài không phân biệt được hai trường hợp. Test tích hợp so sánh hai phản hồi giống hệt nhau (trừ đường dẫn).
  - Luật `"/api/**"` yêu cầu đăng nhập đã đủ cho API đọc bài, không cần luật mới; test khách 401 trên Spring Security thật chứng minh điều này.
- **Tiếp theo:** mở Pull Request cho `feat/lesson-detail`, **đợi `CI`, `Security`, `CodeQL` xanh rồi mới gộp**; sau đó việc 4.3 (Admin quản lý khóa học, `feat/admin-courses`). Giao diện đọc và đăng nhập ở Tuần 5.

## 2026-11-07 — Việc 4.1: đọc công khai khóa học và bài học
- **Nhánh:** `feat/courses-read`
- **Đã làm:**
  - Entity `Course`, `Lesson` (kèm enum `LessonStatus`: `DRAFT`/`PUBLISHED`), `CourseRepository`, `LessonRepository`, `CourseService`, `CourseController`, DTO `CourseResponse` và `LessonSummaryResponse` (chỉ `id`, `slug`, `title`, `summary`, không có `content_md`).
  - `GET /api/courses` và `GET /api/courses/{slug}/lessons` công khai: khóa sắp theo `sort_order` rồi `id`; chỉ bài `PUBLISHED`; khóa không tồn tại → 404 `problem+json`, khóa chưa có bài đã đăng → `[]`.
  - `SecurityConfig` mở đúng hai đường dẫn `GET` trên; `GlobalExceptionHandler` thêm handler cho `CourseNotFoundException` (câu cố định, không lặp lại slug người gửi).
  - Cập nhật `docs/API.md` (mô tả hai endpoint, dòng trạng thái đầu file đã cũ) và `docs/TESTING.md` (số test, dòng 4.1 `Tested`).
- **Kiểm tra:**
  - `.\mvnw test` trong `src/backend`: **148 test, 0 lỗi, `BUILD SUCCESS`** (96 test không cần Docker + 52 test tích hợp trên MySQL 8.4 thật). Việc 4.1 thêm 18 test: `CourseControllerTest` 7, `CourseServiceTest` 3, `CourseIntegrationTest` 8 (trước đó 130).
  - Đếm bằng lệnh trong `docs/TESTING.md`: 148 khớp bảng; frontend vẫn 10 test (chưa chạy lại lần này vì không sửa frontend).
  - **Chưa kiểm:** chưa chạy server thật rồi gọi bằng `curl.exe`; chưa kiểm trên Render sau khi gộp.
- **Lỗi gặp và cách sửa:** `CourseIntegrationTest` lần chạy đầu báo `Could not find a valid Docker environment` vì Docker Desktop chưa bật (không phải lỗi code). Bật Docker rồi chạy lại: 8 test qua.
- **Học được:**
  - Trong luật URL của Spring Security, `*` chỉ khớp đúng một đoạn đường dẫn, nên `/api/courses/*/lessons` công khai còn `/api/courses/{slug}` (nếu sau này có) vẫn đòi đăng nhập; luật chỉ áp cho `GET`, còn `POST /api/courses` khi chưa đăng nhập vẫn 401.
  - Loại bài `DRAFT` ngay trong truy vấn (`findByCourseIdAndStatus...`) an toàn hơn lọc sau ở service, vì bài nháp không bao giờ được nạp lên cho đường dẫn công khai.
  - Khóa không tồn tại (404) khác với khóa chưa có bài (200 với `[]`): hai trường hợp có ý nghĩa khác nhau nên không gộp.
- **Tiếp theo:** mở Pull Request cho `feat/courses-read`, chờ CI xanh rồi gộp; sau đó việc 4.2 (`GET /api/lessons/{slug}`, nhánh `feat/lesson-detail`). Giao diện đọc và đăng nhập dời sang Tuần 5.

## 2026-11-07 — Đóng Tuần 3, bắt đầu Tuần 4
- **Nhánh:** `feat/courses-read` (tạo từ `main` ở `07502e3`; mục này là commit tài liệu đầu tiên của nhánh, chưa commit khi viết)
- **Đã làm:**
  - Gộp Pull Request #18 (`feat/move-to-src`) vào `main` ở `07502e3`; đồng bộ `main` ở máy bằng `git switch main`, `git pull`.
  - Chốt Tuần 3 và chia Tuần 4 thành 6 việc (4.1 đến 4.6) trong kế hoạch ở máy; nhánh đầu tiên là `feat/courses-read`.
  - Đổi cách làm việc: bỏ vấn đáp bắt buộc; **giữ nguyên** viết test (RED) trước rồi code (GREEN), vì test là phần bắt buộc của đồ án (ma trận test, CI, nhãn `Tested`). `CLAUDE.md` và `docs/TESTING.md` đã khớp.
- **Kiểm tra:**
  - CI trên Pull Request #18 (commit `6dee3f6`): `CI`, `Security`, `CodeQL` đều `success`.
  - CI trên `main` (`07502e3`): `CI` với cả 3 job `frontend`, `backend`, `docker` đều `success`; `Security` và `CodeQL` `success`. Đây là lần đầu các đường dẫn `src/...` trong `ci.yml` và `codeql-config.yml` chạy trên GitHub.
  - Render, gọi bằng `curl.exe` sau khi người dùng báo deploy thành công: `GET /api/health` → 200 `{"status":"UP"}` kèm `XSRF-TOKEN` có `Secure`; `GET /` → 200; `GET /api/me` khi chưa đăng nhập → 401 `problem+json`. Việc deploy đúng commit `07502e3` dựa vào lời người dùng báo (chưa xem được trang Events của Render).
- **Tổng kết Tuần 3 (tài khoản và nền tảng dữ liệu):**
  - Việc đã xong: 3.1 xử lý lỗi `problem+json` (PR #8); 3.2 và 3.3 JPA, Flyway `V1`, Aiven trên Render (PR #9); 3.4 đăng ký, đăng nhập, đăng xuất (PR #11); 3.5 phân quyền `LEARNER`/`ADMIN` và Admin đầu tiên (PR #12); 3.6 CSRF và session trong MySQL (PR #13); 3.7 thông tin cá nhân, đổi mật khẩu (PR #14); 3.8 cờ `Secure` của cookie và Admin ở production (PR #15); chuyển mã nguồn vào `src/` (PR #18).
  - Số liệu: 130 test backend (86 không cần Docker, 44 tích hợp trên MySQL 8.4 thật) và 10 test frontend; `main` ở `07502e3`.
  - Chưa làm hoặc ghi nhận, mang sang các tuần sau: đổi mật khẩu chưa hủy các session khác của cùng người dùng; thông báo 403 do thiếu CSRF dùng chung câu "Bạn không có quyền..."; chưa đo thời gian thức dậy của Render sau khi có JPA (lần đo cũ 33,5 giây là trước khi có JPA); chưa đo chênh lệch thời gian phản hồi giữa tài khoản bị khóa và sai mật khẩu (rủi ro ở D25); tài khoản thử `kiemtra-3-8@example.com` còn trong database production; test component `HealthStatus` và `ThemeToggle` chưa có (cần thêm Testing Library và jsdom, phải xin duyệt trước); vấn đáp của 3.5 đến 3.8 không còn bắt buộc.
- **Lỗi gặp và cách sửa:** không có lỗi mới.
- **Học được:**
  - Render đặt Auto-Deploy là "sau khi CI xanh", nên ngay sau khi gộp mà gọi `/api/health` thấy 200 thì có thể vẫn là bản cũ; phải chờ CI trên `main` xong mới kết luận về bản mới.
  - Đường dẫn trong `ci.yml` (`working-directory`, `cache-dependency-path`, `path` của artifact) và trong cấu hình CodeQL đều phải đổi theo khi dời thư mục, và chỉ chạy được trên GitHub nên phải xem kết quả CI của Pull Request.
- **Tiếp theo:** việc 4.1 trên `feat/courses-read`: `GET /api/courses` và `GET /api/courses/{slug}/lessons` (chỉ bài `PUBLISHED`, không trả `content_md`). Người dùng nói kế hoạch trước, rồi viết test (RED), rồi code (GREEN).

---

## 2026-11-07 — Đóng việc 3.8 (Admin ở production) và chuyển mã nguồn vào `src/`
- **Nhánh:** `feat/move-to-src` (tạo từ `main` ở `65ee294`; chưa commit và chưa mở Pull Request khi viết mục này)
- **Đã làm:**
  - Đồng bộ máy với GitHub: `git switch main`, `git pull` (fast-forward lên `65ee294`). Pull Request #15 (`feat/secure-cookie`) mang cấu hình `forward-headers` vào `main`; Pull Request #16 (từ nhánh `main` vào `main`) và #17 là hai lần gộp thừa, nội dung không đổi so với `fa750f2`.
  - Kiểm Admin đầu tiên ở production sau khi người dùng đặt `ADMIN_EMAIL` và `ADMIN_PASSWORD` trên Render (xem Kiểm tra).
  - Chuyển mã nguồn: `backend/` → `src/backend/`, `frontend/` → `src/frontend/` bằng `git mv` (77 file được Git ghi nhận là đổi tên, lịch sử giữ nguyên). Sửa các đường dẫn tham chiếu: `Dockerfile` (5 dòng `COPY`), `.dockerignore`, `.github/workflows/ci.yml` (`working-directory`, `cache-dependency-path`, đường dẫn báo cáo test), `.github/codeql/codeql-config.yml`, `application.properties` (`spring.config.import` từ `../.env` thành `../../.env`), `README.md` (hai lệnh `cd`), `docs/TESTING.md` (chú thích và hai lệnh đếm test).
  - Cách làm: theo yêu cầu của người dùng, Claude thực hiện việc di chuyển và viết mục này. Đây là việc đổi vị trí, không thêm hành vi mới nên không viết test mới; 130 test cũ là bằng chứng hành vi không đổi. Người dùng yêu cầu **bỏ qua vấn đáp**, nên vẫn còn nợ vấn đáp 3.5, 3.6 (câu 4), 3.7 và 3.8.
- **Kiểm tra:**
  - Production (Render, `curl.exe` với cookie jar, 2026-11-07): `POST /api/auth/login` bằng tài khoản Admin → 200, `Set-Cookie: SESSION=...; Path=/; Secure; HttpOnly; SameSite=Lax`, thân có `role: ADMIN`; `GET /api/me` → 200 với `role: ADMIN`, `authProvider: LOCAL`, không có mật khẩu; `GET /api/admin/dashboard` → 404 `problem+json` (chưa có controller, nên 404 chứng tỏ đã qua luật `hasRole("ADMIN")`, không phải 401/403); `POST /api/auth/logout` → 204, cả `SESSION` và `XSRF-TOKEN` bị xóa. `GET /api/health` → 200 kèm `XSRF-TOKEN` có `Secure`.
  - CI trên `main` (`65ee294`): `CI`, `Security`, `CodeQL` đều `success` (đọc từ API công khai của GitHub).
  - Sau khi chuyển vào `src/`: `npm run lint` không lỗi; `npm test` → 10 test qua; `npm run build` thành công (đứng ở `src/frontend`). `.\mvnw test` (đứng ở `src/backend`) → `Tests run: 130, Failures: 0, Errors: 0`, `BUILD SUCCESS`. `docker build -t spacex:move-check .` ở thư mục gốc thành công.
  - Chạy thật: `.\mvnw spring-boot:run` ở `src/backend` với MySQL của docker compose, cổng 8081 → `GET /api/health` 200 `{"status":"UP"}`; log có `Schema spacex is up to date` và `Started SpacexApplication in 13.99 seconds`, chứng tỏ biến `DB_*` đã được đọc từ `.env` qua đường dẫn mới. Đã tắt ứng dụng sau khi kiểm.
  - Hai điều chưa kiểm lúc viết mục này (CI trên Pull Request với đường dẫn `src/...`, và Render build lại sau khi gộp) đã được kiểm sau đó, kết quả ở mục "Đóng Tuần 3" phía trên.
- **Lỗi gặp và cách sửa:**
  - `spring.config.import=optional:file:../.env` là đường dẫn tương đối theo thư mục chạy. Sau khi chuyển, nếu không sửa thì `optional:` làm việc không tìm thấy `.env` thành **im lặng**, rồi mới báo `'url' must start with "jdbc"`; bộ test không bắt được vì test dùng `@DynamicPropertySource`. Phát hiện khi tìm các tham chiếu đường dẫn, sửa thành `../../.env` rồi kiểm bằng cách chạy ứng dụng thật.
  - Lần chạy `spring-boot:run` đầu báo `Connection refused` vì container MySQL ở máy chưa bật (`docker compose ps` trống), không phải lỗi đường dẫn. Bật bằng `docker compose up -d` (giữ dữ liệu) rồi chạy lại thì đạt.
  - Lần `.\mvnw test` đầu chạy quá 10 phút nên bị chuyển xuống nền, nhưng kết thúc đúng (`BUILD SUCCESS`); hai lớp tích hợp lớn mất 75 giây và 59 giây vì Testcontainers khởi động MySQL.
  - Cổng 8080 vẫn bị backend cũ của người dùng giữ nên chạy thử ở cổng 8081.
- **Học được:**
  - `git mv` thư mục thì Git ghi nhận từng file là đổi tên và giữ lịch sử; thư mục không theo dõi (`target`, `node_modules`) đi theo vì cả thư mục được di chuyển.
  - Đường dẫn tương đối trong cấu hình (`../.env`) phụ thuộc thư mục chạy, nên đổi cấu trúc thư mục phải rà cả các đường dẫn kiểu này, không chỉ `Dockerfile` và CI.
  - Trong `Dockerfile`, chỉ đường dẫn nguồn của `COPY` (phía máy) đổi; đường dẫn bên trong container (`/frontend`, `/backend`) là tên tự đặt nên giữ nguyên.
  - Test xanh chưa đủ chứng minh cấu hình đúng khi test không đi qua cùng đường cấu hình; phải chạy ứng dụng thật.
- **Chưa làm / ghi nhận:** nợ vấn đáp như trên; đổi mật khẩu chưa hủy các session khác; thông báo 403 do thiếu CSRF dùng chung câu "Bạn không có quyền..."; chưa đo thời gian thức dậy của Render sau khi có JPA; tài khoản thử `kiemtra-3-8@example.com` còn trong database production.
- **Tiếp theo:** đã làm: commit, Pull Request #18, gộp, deploy (xem mục "Đóng Tuần 3" phía trên).

---

## 2026-11-01 — Cờ Secure của cookie và kiểm tra trên URL Render (việc 3.8)
- **Nhánh:** `feat/secure-cookie` (tạo từ `main` sau khi gộp PR #14; chưa commit và chưa mở Pull Request khi viết mục này)
- **Đã làm:**
  - Thêm `server.forward-headers-strategy=framework` vào `application.properties` để ứng dụng đọc `X-Forwarded-Proto` từ proxy của Render; nhờ vậy `request.isSecure()` đúng và cả `SESSION` lẫn `XSRF-TOKEN` tự có cờ `Secure` trên Render mà không có cờ ở máy (`http://localhost`).
  - Test mới `SecureCookieRealServerIntegrationTest` (2 test) chạy trên Tomcat thật cổng ngẫu nhiên, gọi bằng `HttpClient` của JDK. Cập nhật `docs/API.md`, `docs/TESTING.md`.
  - Cách làm: theo yêu cầu của người dùng, Claude viết test và code. Vấn đáp 3.8 bị bỏ qua theo yêu cầu của người dùng (nợ cùng 3.5, 3.6 câu 4, 3.7).
- **Kiểm tra:**
  - Trên URL Render thật (https://space-x-47f1.onrender.com), bằng `curl.exe`: `GET /api/health` → 200 kèm `Set-Cookie: XSRF-TOKEN=...; Path=/; Secure`; đăng ký tài khoản thử → 201; đăng nhập → 200 kèm `Set-Cookie: SESSION=...; Path=/; Secure; HttpOnly; SameSite=Lax`. `GET /api/me` ban đầu 404 vì Render chưa build xong code 3.7; kiểm lại sau thì 200. Cookie `SESSION` lấy từ **bản deploy cũ** vẫn dùng được trên bản deploy mới: session trong Aiven sống sót qua việc Render dựng lại container.
  - Trên Tomcat thật ở máy (cổng 8081): có `X-Forwarded-Proto: https` → cả hai cookie có `Secure`; không có header → không có `Secure`.
  - Test: RED trước khi thêm cấu hình (2 test sau proxy fail). Test MockMvc thêm ban đầu vẫn fail ở `SESSION` dù server thật đúng, vì MockMvc sắp xếp bộ lọc khác server thật; đã bỏ 3 test MockMvc đó và viết lại trên server thật. Chạy với `-Dserver.forward-headers-strategy=none` thì 1 test đỏ, bật cấu hình thì xanh. `.\mvnw test` → `Tests run: 130, Failures: 0, Errors: 0`, `BUILD SUCCESS` (trước đó 128).
  - **Chưa kiểm:** bản Render có cấu hình `forward-headers` mới (chỉ có sau khi gộp PR); Admin đầu tiên ở production (`ADMIN_EMAIL`/`ADMIN_PASSWORD` chưa đặt trên Render, người dùng tự đặt); thời gian thức dậy của Render Free.
- **Lỗi gặp và cách sửa:**
  - Cờ `Secure` trên Render đã đúng **trước khi** repo có cấu hình nào cho nó (nguyên nhân chưa xác định: có thể do Render hoặc một biến môi trường đặt tay). Test chứng minh code trong repo tự nó chưa đảm bảo điều này, nên đặt cấu hình tường minh để không phụ thuộc thứ ngoài repo.
  - Lần chạy `.\mvnw test` đầu tiên cả 6 lớp tích hợp lỗi `Previous attempts to find a Docker environment failed` trong vài giây; Docker vẫn chạy, chạy lại thì qua. Là sự cố tạm của Testcontainers/Docker, không phải lỗi code.
  - Cổng 8080 đang bị backend cũ của người dùng giữ nên bản mới chạy ở cổng 8081 và tắt sau khi kiểm.
- **Ghi nhận:** tài khoản thử `kiemtra-3-8@example.com` (vai trò `LEARNER`, dữ liệu giả) được tạo trong database production trên Aiven để kiểm cookie; có thể khóa hoặc bỏ qua khi có trang quản lý người dùng.
- **Học được:**
  - Sau proxy HTTPS, ứng dụng chỉ biết kết nối là bảo mật qua header `X-Forwarded-Proto`; phải cấu hình đọc header này thì cờ `Secure` mới đúng.
  - Không đặt cứng `Secure` cho cookie: cookie `Secure` không được gửi lại qua `http://localhost`, nên để `request.isSecure()` quyết định.
  - MockMvc không giống hệt server thật (thứ tự bộ lọc); điều gì phụ thuộc thứ tự bộ lọc thì phải kiểm trên server thật. Một test chưa từng thấy đỏ thì chưa đáng tin.
- **Tiếp theo:** commit, mở Pull Request cho `feat/secure-cookie`, chờ CI xanh rồi gộp; người dùng đặt `ADMIN_*` trên Render; kiểm lại cookie và Admin trên Render sau deploy; báo cáo Tuần 3.

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
