package vn.spacex.user;

import java.time.LocalDateTime;

// Một dòng trong danh sách người dùng của Admin. Cố ý không có mã băm mật khẩu hay providerId.
public record AdminUserResponse(Long id, String email, String displayName, Role role, AuthProvider authProvider,
                                boolean enabled, LocalDateTime createdAt) {

    static AdminUserResponse from(User user) {
        return new AdminUserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(),
                user.getAuthProvider(), user.isEnabled(), user.getCreatedAt());
    }
}
