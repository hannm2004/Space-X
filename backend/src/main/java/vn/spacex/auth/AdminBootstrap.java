package vn.spacex.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import vn.spacex.user.Emails;
import vn.spacex.user.Role;
import vn.spacex.user.User;
import vn.spacex.user.UserRepository;

import java.nio.charset.StandardCharsets;

// Tạo ADMIN đầu tiên khi khởi động từ ADMIN_EMAIL và ADMIN_PASSWORD (D11, D26).
// Cấu hình sai thì ném lỗi để ứng dụng không khởi động, thay vì chạy mà không có Admin.
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private static final int MIN_PASSWORD_LENGTH = 8;
    // BCrypt chỉ dùng 72 byte đầu, giống luật đăng ký (D25).
    private static final int MAX_PASSWORD_BYTES = 72;
    private static final String ADMIN_DISPLAY_NAME = "Quản trị viên";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminBootstrap(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.admin.email:}") String email,
                          @Value("${app.admin.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureAdmin();
    }

    // Thông báo lỗi chỉ nêu tên biến, tuyệt đối không chứa giá trị mật khẩu.
    void ensureAdmin() {
        boolean hasEmail = email != null && !email.isBlank();
        boolean hasPassword = password != null && !password.isBlank();
        if (!hasEmail && !hasPassword) {
            return;
        }
        if (!hasEmail) {
            throw new IllegalStateException("Thiếu ADMIN_EMAIL: phải đặt cả ADMIN_EMAIL và ADMIN_PASSWORD, hoặc bỏ cả hai");
        }
        if (!hasPassword) {
            throw new IllegalStateException("Thiếu ADMIN_PASSWORD: phải đặt cả ADMIN_EMAIL và ADMIN_PASSWORD, hoặc bỏ cả hai");
        }
        if (password.length() < MIN_PASSWORD_LENGTH
                || password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new IllegalStateException("ADMIN_PASSWORD không hợp lệ: tối thiểu "
                    + MIN_PASSWORD_LENGTH + " ký tự và tối đa " + MAX_PASSWORD_BYTES + " byte UTF-8");
        }

        // Đã có ADMIN thì thôi, để cấu hình cũ còn sót lại không làm hỏng lần khởi động sau.
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        String normalizedEmail = Emails.normalize(email);
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalStateException(
                    "ADMIN_EMAIL đã thuộc một tài khoản khác (không phải Admin); không tự nâng quyền, hãy dùng email khác");
        }

        // saveAndFlush để lỗi UNIQUE (nếu hai bản ứng dụng cùng khởi động) nổ ngay tại đây.
        userRepository.saveAndFlush(User.newLocalAdmin(
                normalizedEmail, passwordEncoder.encode(password), ADMIN_DISPLAY_NAME));
        log.info("Da tao tai khoan ADMIN dau tien tu bien moi truong ADMIN_EMAIL");
    }
}
