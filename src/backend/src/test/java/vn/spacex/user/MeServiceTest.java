package vn.spacex.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeServiceTest {

    private static final long USER_ID = 1L;

    @Mock
    private UserRepository userRepository;

    // Dùng BCrypt thật để kiểm tra mật khẩu mới đúng là được băm, không phải chuỗi giả.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private MeService meService;

    @BeforeEach
    void setUp() {
        meService = new MeService(userRepository, passwordEncoder);
    }

    private User coTaiKhoan(String matKhauHash) {
        User user = User.newLocalLearner("a@example.com", matKhauHash, "Nguyễn Văn A");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void layThongTinTraVeDungTaiKhoanTheoIdCuaNguoiDangNhap() {
        coTaiKhoan(passwordEncoder.encode("matkhau-cu-1"));

        MeResponse response = meService.getProfile(USER_ID);

        assertThat(response.displayName()).isEqualTo("Nguyễn Văn A");
        assertThat(response.email()).isEqualTo("a@example.com");
        assertThat(response.role()).isEqualTo(Role.LEARNER);
        assertThat(response.authProvider()).isEqualTo(AuthProvider.LOCAL);
        verify(userRepository).findById(USER_ID);
    }

    @Test
    void suaTenHienThiDoiTenVaTraVeThongTinMoi() {
        User user = coTaiKhoan(passwordEncoder.encode("matkhau-cu-1"));

        MeResponse response = meService.updateProfile(USER_ID, new UpdateProfileRequest("Trần Thị B"));

        assertThat(user.getDisplayName()).isEqualTo("Trần Thị B");
        assertThat(response.displayName()).isEqualTo("Trần Thị B");
        assertThat(user.getEmail()).isEqualTo("a@example.com");
        assertThat(user.getRole()).isEqualTo(Role.LEARNER);
    }

    @Test
    void doiMatKhauDungMatKhauCuThiLuuMaBamMoi() {
        User user = coTaiKhoan(passwordEncoder.encode("matkhau-cu-1"));

        meService.changePassword(USER_ID, new ChangePasswordRequest("matkhau-cu-1", "matkhau-moi-2", "matkhau-moi-2"));

        assertThat(user.getPasswordHash()).isNotEqualTo("matkhau-moi-2").startsWith("$2");
        assertThat(passwordEncoder.matches("matkhau-moi-2", user.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("matkhau-cu-1", user.getPasswordHash())).isFalse();
    }

    @Test
    void saiMatKhauCuThiNemLoiVaKhongDoiMaBam() {
        String hashCu = passwordEncoder.encode("matkhau-cu-1");
        User user = coTaiKhoan(hashCu);

        assertThatThrownBy(() -> meService.changePassword(
                USER_ID, new ChangePasswordRequest("sai-mat-khau", "matkhau-moi-2", "matkhau-moi-2")))
                .isInstanceOf(WrongCurrentPasswordException.class);

        assertThat(user.getPasswordHash()).isEqualTo(hashCu);
    }

    @Test
    void taiKhoanKhongCoMatKhauThiNemLoiRiengVaKhongDoiGi() {
        User user = coTaiKhoan(null);

        assertThatThrownBy(() -> meService.changePassword(
                USER_ID, new ChangePasswordRequest("bat-ky-gi", "matkhau-moi-2", "matkhau-moi-2")))
                .isInstanceOf(NoPasswordAccountException.class);

        assertThat(user.getPasswordHash()).isNull();
    }
}
