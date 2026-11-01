package vn.spacex.user;

import java.time.LocalDateTime;

// Không có trường mật khẩu hay mã băm: phản hồi chỉ gồm thông tin hiển thị của chính người dùng.
public record MeResponse(String displayName, String email, Role role, AuthProvider authProvider,
                         LocalDateTime createdAt) {

    static MeResponse from(User user) {
        return new MeResponse(user.getDisplayName(), user.getEmail(), user.getRole(),
                user.getAuthProvider(), user.getCreatedAt());
    }
}
