package vn.spacex.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Mọi phương thức nhận userId do controller lấy từ session, không bao giờ từ dữ liệu trình duyệt gửi lên.
@Service
public class MeService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public MeService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public MeResponse getProfile(Long userId) {
        return MeResponse.from(find(userId));
    }

    // Không gọi save: User đang được quản lý trong transaction nên thay đổi tự ghi xuống database khi commit.
    @Transactional
    public MeResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = find(userId);
        user.changeDisplayName(request.displayName());
        return MeResponse.from(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = find(userId);
        if (user.getPasswordHash() == null) {
            throw new NoPasswordAccountException();
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new WrongCurrentPasswordException();
        }
        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    // Session còn nhưng tài khoản không còn trong database là trạng thái không mong đợi (hệ thống chưa có chức năng xóa tài khoản).
    private User find(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Khong tim thay tai khoan co id " + userId));
    }
}
