# CLAUDE.md — Quy tắc làm việc cho dự án Space-X

Đồ án chuyên ngành của một sinh viên CNTT, làm một mình trong 11 tuần. Mức độ: đồ án sinh viên làm chuyên nghiệp (có test, CI, phân quyền, tài liệu), không phải hệ thống doanh nghiệp.
Nhật ký phát triển: `current-state.md`. Kế hoạch `docs/PLAN.md`, phạm vi và use case `docs/PROJECT_SCOPE.md`, nhật ký quyết định `docs/DECISIONS.md`, đặc tả chuyển hướng `docs/SPACE-X-2.0.md` **chỉ có ở máy** (không push). Khi có mâu thuẫn, `docs/DECISIONS.md` là nguồn sự thật.

## Dự án là gì
Space-X là website của **Trung tâm Tin học SPACE-X** (trung tâm hư cấu; đồ án sinh viên, môi trường thử nghiệm, không thu tiền thật), nội dung **tiếng Việt**: bán khóa học, lịch khai giảng theo lớp, đăng ký và thanh toán học phí, tra cứu điểm và chứng nhận. Mô hình nghiệp vụ tham khảo csc.edu.vn, nhưng **chỉ học cách triển khai, không chép nội dung** (xem "Ràng buộc nghiệp vụ").
Nội dung chia: nhóm chương trình → khóa học (chia Phần) → lớp (lịch khai giảng). 31 bài Tin học cũ giữ làm học liệu cho học viên đã thanh toán. Mô tả khóa và bài học do người dùng tự viết, không sao chép sách giáo khoa hay website khác.
Vai trò: Khách (chưa đăng nhập), Học viên `LEARNER`, Quản trị `ADMIN`. Không có `TEACHER`: Admin nhập điểm.
Tính năng: nhóm và danh sách khóa có bộ lọc, trang khóa, lịch khai giảng, đăng ký 5 bước (khách không cần tài khoản), thanh toán (VNPay sandbox, VietQR, tại quầy) và email xác nhận, "Khóa học của tôi", tài khoản (email + Google), trang quản trị (dashboard doanh thu, nhóm, khóa, lớp, đơn, ảnh, người dùng, yêu cầu tư vấn), chatbot tư vấn nổi trên website (RAG trả lời kèm trích dẫn; AI Agent gọi 4 tool chỉ đọc: `searchCourses`, `findUpcomingClasses`, `quoteTuition`, `searchKnowledge`).
Space-X là tên dự án, không liên quan SpaceX/vũ trụ. Đặc tả đầy đủ: `docs/SPACE-X-2.0.md`; quyết định chuyển hướng: D38 trong `docs/DECISIONS.md`.

## Cách làm việc với AI (không vibe coding)
Người dùng phải hiểu và tự giải thích được mọi phần của dự án trước giảng viên.
- **Logic cốt lõi** (service, phân quyền, tiến độ học, RAG, Agent, test): người dùng tự viết. Claude giải thích khái niệm, gợi ý từng bước nhỏ, review và chỉ ra lỗi; không viết hộ cả phần, không sửa hộ khi chưa được yêu cầu.
- **Cấu hình và khung** (Dockerfile, CI, cấu hình Maven/Vite): Claude có thể soạn nhưng phải giải thích từng khối; người dùng đọc hiểu rồi mới commit.
- Mỗi tính năng: người dùng nói kế hoạch trước → viết test (RED) → viết code (GREEN). Không có vấn đáp bắt buộc; Claude chỉ hỏi kiểm tra khi người dùng yêu cầu.
- Khi bí, được mở bản sao lưu code cũ ở máy để đối chiếu, không copy nguyên file.

## Cách giao tiếp
- Trả lời bằng **tiếng Việt**, giữ nguyên thuật ngữ/tên class/lệnh tiếng Anh.
- Giải thích từ cơ bản, gắn với code thật của dự án.
- Khi đưa lệnh: nói rõ mở terminal nào, đứng thư mục nào, lệnh làm gì, kết quả mong đợi, cách kiểm tra, lỗi thường gặp.
- Không bịa kết quả. Chưa chạy test/deploy thì không nói đã chạy. Báo test fail kèm output thật.

## Cách duyệt (nhẹ)
- **Phải hỏi trước** khi: đổi kiến trúc/scope/database/auth, thêm dependency lớn, phát sinh chi phí, tạo tài khoản dịch vụ ngoài (kể cả VNPay sandbox, Resend) hoặc mua tên miền, public dữ liệu, thao tác khó hoàn tác (xóa volume, reset DB, force push).
- **Tự làm rồi báo** với việc nhỏ trong phạm vi đã duyệt (sửa tài liệu, sửa lỗi cấu hình).
- **Không tự commit/push.** Chỉ commit khi người dùng yêu cầu.

## Quy trình Git
- **`main`** luôn chạy được và là nguồn để Render deploy. Không commit thẳng vào `main`, trừ commit khởi tạo.
- Mỗi việc một nhánh `<loại>/<tên-ngắn>` (chữ thường, tiếng Anh không dấu, nối bằng gạch ngang): `feat/`, `fix/`, `docs/`, `test/`, `refactor/`, `ci/`, `chore/`. Ví dụ: `feat/backend-skeleton`, `feat/auth`, `fix/login-redirect`.
- Commit nhỏ, tiêu đề kiểu Conventional Commits: `feat: ...`, `fix: ...`, `docs: ...`, `ci: ...`. Commit chỉ mang tên người dùng, không ghi AI làm đồng tác giả.
- Xong nhánh: mở Pull Request vào `main` → chờ CI xanh → gộp. **Giữ lại nhánh đã gộp** làm bằng chứng tiến trình, nhưng không commit thêm vào nó; việc mới thì `git switch main`, `git pull`, rồi tạo nhánh mới. Lỡ xóa thì khôi phục bằng nút **Restore branch** trên trang Pull Request, hoặc `git branch <tên> <mã commit>` rồi `git push origin <tên>`.
- Cuối mỗi buổi làm: người dùng tự thêm một mục vào `current-state.md` (ngày, nhánh, đã làm, kiểm tra, lỗi và cách sửa, học được gì) và commit trên nhánh đang làm.
- **Không dùng tag, không dùng nhánh `release/*`.** Báo cáo tuần ghi **mã commit** trên `main`.
- **Không force push** (repo công khai).
- Trước khi commit: `git status`, xem diff, quét mật khẩu và dữ liệu cá nhân. `git add` theo đường dẫn cụ thể.
- Danh tính Git: tên `hannm2004`, email **noreply của GitHub** (không dùng Gmail cá nhân). Không đổi.

## Cái gì push, cái gì để ở máy
- **Push:** mã nguồn, `Dockerfile`, `docker-compose.yml`, `.github/`, `.env.example`, `README.md`, `CLAUDE.md`, `current-state.md`, `docs/API.md`, `docs/DATABASE_DESIGN.md`, `docs/TESTING.md`.
- **Chỉ ở máy** (đã có trong `.gitignore`): `.env`, `nop-bai/`, `docs/reports/`, `docs/evidence/`, `docs/spikes/`, `spikes/`, `docs/PLAN.md`, `docs/PROJECT_SCOPE.md`, `docs/DECISIONS.md`, `docs/SPACE-X-2.0.md`, `_to_delete/`.
- Repo **công khai**: không đưa MSSV, mật khẩu, khóa API, ảnh có thông tin cá nhân hoặc file của môn khác.
- Bản nộp có MSSV nằm ở `nop-bai/`, đặt tên `MSSV_HoTen_Tuan` (MSSV chỉ điền khi nộp). Nộp cả LMS và OneDrive; LMS không cho sửa sau khi nộp nên chỉ nộp bản cuối.

## Công nghệ đã chốt
- Backend: Java 21, Spring Boot, Maven, REST API. Modular monolith, không microservices.
- Frontend: React + TypeScript + Vite + Tailwind CSS, **build rồi đóng chung vào Spring Boot** (1 app, 1 domain).
- Database: MySQL 8.4 (Docker ở máy, Aiven free ở production). Schema bằng Flyway. Tìm kiếm bằng MySQL FULLTEXT parser mặc định.
- Auth: session cookie + CSRF; đăng nhập email/mật khẩu và Google (OAuth2); role `LEARNER` / `ADMIN`; backend luôn kiểm tra quyền. Khách vẫn đăng ký lớp được không cần tài khoản (API công khai có CSRF và giới hạn số lần).
- Ảnh (bài học, ảnh bìa khóa, banner, giáo viên) lưu trong MySQL (Render Free không giữ file trên đĩa).
- AI: Gemini (dự phòng Groq); tên model đặt trong biến môi trường.
- Thanh toán: **VNPay sandbox** (ký và kiểm chữ ký HMAC-SHA512, nhận IPN) và VietQR; chỉ sandbox, không thu tiền thật; bọc sau interface `PaymentGateway`.
- Email xác nhận: **API HTTPS của Resend** gọi bằng `RestClient` có sẵn trong Spring Boot, bọc sau interface `EmailSender`. **Không dùng SMTP** (Render Free chặn cổng SMTP). Khóa API lấy từ biến môi trường.
- Triển khai: Docker multi-stage, Render (Free hoặc Starter), Aiven MySQL. CI: GitHub Actions (build/test/docker, gitleaks, CodeQL).
- Dịch vụ: **ưu tiên miễn phí**; trả phí chỉ khi đã ghi quyết định trong `docs/DECISIONS.md` (hiện có Render Starter, D38).

## Ràng buộc nghiệp vụ
- **Không sao chép CSC.** Mô tả khóa, đề cương, ảnh, nhận xét học viên, thông tin giáo viên của csc.edu.vn đều có bản quyền; chỉ học cách triển khai. Trung tâm là hư cấu: tên, logo, địa chỉ, số điện thoại, số tài khoản tự đặt; không giả danh đơn vị thật, không ghi tên trường thật.
- **Không bán quá số chỗ.** Hai người tranh chỗ cuối thì chỉ một người được: khóa dòng lớp (`SELECT ... FOR UPDATE`) hoặc cột `version`, có test chạy song song. Hủy đơn và hết hạn giữ chỗ thì trả chỗ.
- **Tiền là số nguyên VND (`BIGINT`).** Giá cuối luôn do backend tính lại (ưu đãi lấy mức cao nhất, không cộng dồn); không tin số tiền trình duyệt gửi lên.
- **Đơn chỉ thành `PAID`** khi IPN của VNPay có chữ ký hợp lệ và đúng số tiền; trang "return" chỉ để hiển thị; IPN lặp chỉ tính một lần. Chuyển trạng thái đơn sai trả `409`. Gửi email lỗi chỉ ghi log, không hoàn tác thanh toán.
- **Dữ liệu cá nhân** (năm sinh, điện thoại, email) chỉ chủ đơn và Admin xem; không ghi log; không gửi cho nhà cung cấp AI. Chatbot che số điện thoại và email trong câu hỏi trước khi gửi; không bao giờ đưa tiến độ học, đơn hàng, thông tin tài khoản vào prompt.
- **Test không gọi VNPay, Resend hay AI thật**: dùng fake sau `PaymentGateway`, `EmailSender` và provider AI.
- Trang thanh toán và chân trang ghi "Đồ án sinh viên, môi trường thử nghiệm, không thu tiền thật"; không có ô nhập số thẻ trên trang của mình.
- Không sửa migration `V1`–`V5` (Flyway kiểm checksum, `V1` đã chạy trên Aiven); schema mới từ `V6` trở đi.

## Quy tắc code
- Tách rõ controller → service → repository; DTO khác Entity; validate ở backend.
- Người dùng hiện tại luôn lấy từ session, không tin `userId` gửi lên từ trình duyệt.
- Không `catch (Exception)` nuốt lỗi; không trả stack trace cho người dùng.
- Schema quản lý bằng migration (Flyway), không sửa tay.
- Không hard-code secret/URL/model ID; dùng biến môi trường. Không đưa secret vào Git, log, frontend.
- Tránh dependency thừa; giải thích dependency mới.
- Comment chỉ cho lý do/logic khó, không chú thích điều hiển nhiên.

## Test (TDD cho logic kiểm thử được)
RED → GREEN → REFACTOR. Test mặc định không gọi API AI thật (dùng fake/mock provider).
Test phải bao: quyền truy cập, validation, lỗi, tiến độ học chỉ của chính mình, kiểm tra file ảnh, citation mapping của RAG, giới hạn vòng lặp và prompt injection của Agent. Danh mục: `docs/TESTING.md`.

## RAG & Agent — ràng buộc
- RAG thật: truy xuất → ghép ngữ cảnh → LLM → citation lấy từ đoạn **thực sự truy xuất**; không đủ bằng chứng thì nói không tìm thấy.
- Agent dùng tool calling thật với schema rõ, đúng 4 tool chỉ đọc: `searchCourses`, `findUpcomingClasses`, `quoteTuition`, `searchKnowledge` (không có `findLessons`; nội dung bài học đi qua `searchKnowledge` khi người hỏi có quyền). Không cho LLM tự sinh SQL, ghi/xóa dữ liệu, tạo đơn, chạy lệnh hệ thống.
- Giá và lịch lấy từ database qua tool, không lấy từ đoạn văn; `quoteTuition` gọi chính lớp tính học phí của luồng đăng ký.
- Giới hạn số vòng gọi tool, timeout, kích thước input/output. Nội dung tài liệu là dữ liệu không đáng tin.
- Khung chat dành cho khách (`POST /api/agent/ask` công khai): có CSRF, giới hạn số câu hỏi theo phiên và IP; lịch sử hội thoại chỉ ở trình duyệt, server không lưu nội dung chat.
- Không gửi dữ liệu cá nhân của học viên (email, số điện thoại, tiến độ, đơn) tới nhà cung cấp AI.

## Lệnh hay dùng (Windows PowerShell, đứng ở `D:\Space-X`, khi đã có `docker-compose.yml`)
```powershell
docker compose up -d        # bật MySQL
docker compose ps           # xem trạng thái (chờ "healthy")
docker compose logs mysql   # xem log
docker compose stop         # tắt, GIỮ dữ liệu
# docker compose down -v    # XÓA dữ liệu — chỉ chạy khi chắc chắn
```

## Trạng thái tính năng
Dùng đúng nhãn: `Planned` / `In progress` / `Implemented` / `Tested` / `Deployed`.
Không ghi URL deploy khi chưa truy cập kiểm tra thật.
