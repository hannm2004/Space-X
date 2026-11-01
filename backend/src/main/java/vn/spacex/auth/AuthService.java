package vn.spacex.auth;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.spacex.user.User;
import vn.spacex.user.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException();
        }
        User user = User.newLocalLearner(
                request.email(), passwordEncoder.encode(request.password()), request.displayName());
        try {
            // saveAndFlush để ràng buộc UNIQUE của email báo lỗi ngay tại đây (không đợi đến lúc commit).
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Hai người đăng ký cùng email cùng lúc: cả hai qua được existsByEmail, chỉ một người ghi được.
            throw new EmailAlreadyExistsException();
        }
    }
}
