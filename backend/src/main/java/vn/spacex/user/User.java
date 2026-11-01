package vn.spacex.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    // NULL với tài khoản Google (không có mật khẩu).
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    private AuthProvider authProvider;

    @Column(name = "provider_id")
    private String providerId;

    @Column(nullable = false)
    private boolean enabled;

    // Do database tự điền (DEFAULT CURRENT_TIMESTAMP), ứng dụng chỉ đọc.
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected User() {
    }

    // Vai trò luôn là LEARNER: không có đường nào để dữ liệu từ trình duyệt chọn role (D25).
    public static User newLocalLearner(String email, String passwordHash, String displayName) {
        User user = new User();
        user.email = email;
        user.passwordHash = passwordHash;
        user.displayName = displayName;
        user.role = Role.LEARNER;
        user.authProvider = AuthProvider.LOCAL;
        user.enabled = true;
        return user;
    }

    // Chỉ AdminBootstrap gọi (D26). Vẫn không có setter role, nên dữ liệu từ trình duyệt không chọn được role.
    public static User newLocalAdmin(String email, String passwordHash, String displayName) {
        User user = newLocalLearner(email, passwordHash, displayName);
        user.role = Role.ADMIN;
        return user;
    }

    // Chỉ có hai trường được phép đổi sau khi tạo (3.7). Cố ý không có setter cho email và role.
    public void changeDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Role getRole() {
        return role;
    }

    public AuthProvider getAuthProvider() {
        return authProvider;
    }

    public String getProviderId() {
        return providerId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
