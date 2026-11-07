package vn.spacex.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.spacex.user.AuthProvider;
import vn.spacex.user.Role;
import vn.spacex.user.User;
import vn.spacex.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    // Dùng BCrypt thật để kiểm tra mật khẩu đúng là được băm, không phải chuỗi giả.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder);
    }

    private static RegisterRequest request() {
        return new RegisterRequest("Nguyễn Văn A", "a@example.com", "matkhau123", "matkhau123");
    }

    @Test
    void dangKyLuuTaiKhoanHocSinhLocalVoiMatKhauDaBam() {
        when(userRepository.existsByEmail("a@example.com")).thenReturn(false);

        authService.register(request());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("a@example.com");
        assertThat(saved.getDisplayName()).isEqualTo("Nguyễn Văn A");
        assertThat(saved.getRole()).isEqualTo(Role.LEARNER);
        assertThat(saved.getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(saved.getProviderId()).isNull();
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getPasswordHash()).isNotEqualTo("matkhau123").startsWith("$2");
        assertThat(passwordEncoder.matches("matkhau123", saved.getPasswordHash())).isTrue();
    }

    @Test
    void emailDaTonTaiThiNemLoiVaKhongLuu() {
        when(userRepository.existsByEmail("a@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request()))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void haiNguoiDangKyCungLucThiRangBuocUniqueDuocDoiThanhLoiTrungEmail() {
        when(userRepository.existsByEmail("a@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uk_users_email"));

        assertThatThrownBy(() -> authService.register(request()))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }
}
