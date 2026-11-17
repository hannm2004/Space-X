package vn.spacex.auth;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.spacex.user.AuthProvider;
import vn.spacex.user.Emails;
import vn.spacex.user.User;
import vn.spacex.user.UserRepository;

import java.util.Optional;

import static vn.spacex.auth.GoogleLoginRejectedException.Reason;

// Luật đăng nhập bằng Google (UC03, D25, D31). Không biết gì về HTTP hay OAuth2: nhận hồ sơ đã lấy từ ID token.
@Service
public class GoogleAccountService {

    // Giới hạn của cột users.display_name và users.email trong V1.
    private static final int MAX_DISPLAY_NAME = 100;
    private static final int MAX_EMAIL = 255;

    private final UserRepository userRepository;

    public GoogleAccountService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User signIn(GoogleProfile profile) {
        if (isBlank(profile.subject()) || isBlank(profile.email())) {
            throw new GoogleLoginRejectedException(Reason.FAILED);
        }
        // Email chưa xác minh thì chưa chứng minh được người đăng nhập sở hữu địa chỉ đó.
        if (!profile.emailVerified()) {
            throw new GoogleLoginRejectedException(Reason.UNVERIFIED_EMAIL);
        }
        String email = Emails.normalize(profile.email());
        if (email.indexOf('@') < 1 || email.length() > MAX_EMAIL) {
            throw new GoogleLoginRejectedException(Reason.FAILED);
        }

        // Tra theo mã `sub` cố định trước: email của tài khoản Google có thể đổi, mã này thì không.
        Optional<User> existing = userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, profile.subject());
        if (existing.isPresent()) {
            return requireEnabled(existing.get());
        }

        Optional<User> sameEmail = userRepository.findByEmail(email);
        if (sameEmail.isPresent()) {
            // D25: không tự liên kết. Tài khoản mật khẩu giữ nguyên; học sinh đăng nhập bằng email và mật khẩu.
            if (sameEmail.get().getAuthProvider() == AuthProvider.LOCAL) {
                throw new GoogleLoginRejectedException(Reason.EMAIL_USED_BY_PASSWORD);
            }
            // Email đang thuộc một tài khoản Google với mã khác: không để người mới vào tài khoản của người cũ.
            throw new GoogleLoginRejectedException(Reason.FAILED);
        }

        User user = User.newGoogleLearner(email, displayName(profile.name(), email), profile.subject());
        try {
            // saveAndFlush để ràng buộc UNIQUE báo lỗi ngay tại đây (không đợi đến lúc commit).
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Hai tab cùng đăng nhập Google lần đầu cùng lúc: một bên ghi được, bên kia thua. Báo chung, bấm lại là vào.
            throw new GoogleLoginRejectedException(Reason.FAILED);
        }
    }

    private static User requireEnabled(User user) {
        if (!user.isEnabled()) {
            throw new GoogleLoginRejectedException(Reason.ACCOUNT_DISABLED);
        }
        return user;
    }

    // Tên Google là dữ liệu bên ngoài: cắt khoảng trắng, thiếu thì lấy phần trước @ của email, cắt cho vừa cột.
    private static String displayName(String name, String email) {
        String result = name == null ? "" : name.trim();
        if (result.isEmpty()) {
            result = email.substring(0, email.indexOf('@'));
        }
        if (result.codePointCount(0, result.length()) > MAX_DISPLAY_NAME) {
            result = result.substring(0, result.offsetByCodePoints(0, MAX_DISPLAY_NAME));
        }
        return result;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
