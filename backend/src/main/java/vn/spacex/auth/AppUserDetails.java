package vn.spacex.auth;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import vn.spacex.user.Role;
import vn.spacex.user.User;

import java.util.Collection;
import java.util.List;

// Người dùng đang đăng nhập, được giữ trong session. Mã băm mật khẩu bị xóa sau khi xác thực (CredentialsContainer)
// để không nằm trong session; id lấy từ đây thay vì tin userId do trình duyệt gửi.
public final class AppUserDetails implements UserDetails, CredentialsContainer {

    private final Long id;
    private final String email;
    private String passwordHash;
    private final String displayName;
    private final Role role;
    private final boolean enabled;

    public AppUserDetails(Long id, String email, String passwordHash, String displayName, Role role, boolean enabled) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.enabled = enabled;
    }

    static AppUserDetails from(User user) {
        return new AppUserDetails(user.getId(), user.getEmail(), user.getPasswordHash(),
                user.getDisplayName(), user.getRole(), user.isEnabled());
    }

    public Long id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String displayName() {
        return displayName;
    }

    public Role role() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    // Tài khoản Google không có mật khẩu (NULL): trả null để việc so khớp mật khẩu luôn thất bại.
    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }
}
