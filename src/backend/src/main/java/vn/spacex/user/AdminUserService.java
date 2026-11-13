package vn.spacex.user;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.spacex.common.api.PageResponse;

// actorId luôn do controller lấy từ session, không bao giờ từ dữ liệu trình duyệt gửi lên.
// Việc xóa session của người bị tác động làm ở controller, sau khi transaction ở đây đã commit.
@Service
public class AdminUserService {

    // Mới đăng ký thì đứng đầu; id tăng dần theo thời điểm tạo nên ổn định giữa các trang.
    private static final Sort LIST_ORDER = Sort.by(Sort.Direction.DESC, "id");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;

    public AdminUserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                            TemporaryPasswordGenerator temporaryPasswordGenerator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.temporaryPasswordGenerator = temporaryPasswordGenerator;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> list(String q, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, LIST_ORDER);
        return PageResponse.of(userRepository.search(toLikePattern(q), pageable), AdminUserResponse::from);
    }

    // Không gọi save: User đang được quản lý trong transaction nên thay đổi tự ghi xuống database khi commit.
    @Transactional
    public AdminUserResponse changeEnabled(Long actorId, Long targetId, boolean enabled) {
        User user = find(targetId);
        if (!enabled) {
            if (user.getId().equals(actorId)) {
                throw UserActionNotAllowedException.selfDisable();
            }
            // Chỉ đếm khi thật sự sắp đổi một Admin đang hoạt động thành bị khóa. Hai Admin khóa nhau cùng lúc vẫn
            // có thể cùng qua kiểm tra này (chưa có khóa dòng); chấp nhận ở quy mô đồ án, ghi ở docs/API.md.
            if (user.getRole() == Role.ADMIN && user.isEnabled()
                    && userRepository.countByRoleAndEnabled(Role.ADMIN, true) <= 1) {
                throw UserActionNotAllowedException.lastAdmin();
            }
        }
        user.changeEnabled(enabled);
        return AdminUserResponse.from(user);
    }

    @Transactional
    public ResetPasswordResponse resetPassword(Long actorId, Long targetId) {
        User user = find(targetId);
        if (user.getId().equals(actorId)) {
            throw UserActionNotAllowedException.selfReset();
        }
        // Tài khoản Google đăng nhập bằng Google, không có mật khẩu; đặt mật khẩu sẽ tạo thêm một đường vào ngoài ý muốn.
        if (user.getPasswordHash() == null) {
            throw UserActionNotAllowedException.noPassword();
        }
        String temporaryPassword = temporaryPasswordGenerator.generate();
        user.changePasswordHash(passwordEncoder.encode(temporaryPassword));
        return new ResetPasswordResponse(user.getId(), user.getEmail(), temporaryPassword);
    }

    // null nghĩa là không lọc. Thoát \ % _ để từ khóa người gõ chỉ là chữ thường, không phải ký tự đại diện của LIKE.
    private static String toLikePattern(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String escaped = q.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(UserNotFoundException::new);
    }
}
