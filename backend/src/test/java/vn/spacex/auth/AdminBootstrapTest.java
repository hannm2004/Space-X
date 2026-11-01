package vn.spacex.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.spacex.user.AuthProvider;
import vn.spacex.user.Role;
import vn.spacex.user.User;
import vn.spacex.user.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Quy tắc khởi tạo ADMIN đầu tiên theo D11 và D26.
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class AdminBootstrapTest {

    private static final String EMAIL = "admin@example.com";
    private static final String MAT_KHAU = "mat-khau-admin-1";

    @Mock
    private UserRepository userRepository;

    // BCrypt thật để kiểm tra mật khẩu được băm đúng, không phải chuỗi giả.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AdminBootstrap bootstrap(String email, String password) {
        return new AdminBootstrap(userRepository, passwordEncoder, email, password);
    }

    @Test
    void camHaiBienDeuThieuThiBoQuaVaKhongChamDatabase() {
        bootstrap("", "").ensureAdmin();
        bootstrap(null, null).ensureAdmin();
        bootstrap("   ", "  ").ensureAdmin();

        verifyNoInteractions(userRepository);
    }

    @Test
    void chiCoEmailThieuMatKhauThiBaoLoiKhoiDongNeuTenBien() {
        assertThatThrownBy(() -> bootstrap(EMAIL, "").ensureAdmin())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PASSWORD");

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void chiCoMatKhauThieuEmailThiBaoLoiKhoiDongNeuTenBien() {
        assertThatThrownBy(() -> bootstrap(null, MAT_KHAU).ensureAdmin())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_EMAIL")
                .hasMessageNotContaining(MAT_KHAU);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void matKhauNganHon8KyTuThiBaoLoiVaKhongLoMatKhau() {
        String ngan = "1234567";

        assertThatThrownBy(() -> bootstrap(EMAIL, ngan).ensureAdmin())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PASSWORD")
                .hasMessageNotContaining(ngan);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void matKhauDung72ByteDuocChapNhanCon73ByteThiBiTuChoi() {
        assertThatCode(() -> bootstrap(EMAIL, "a".repeat(72)).ensureAdmin()).doesNotThrowAnyException();

        assertThatThrownBy(() -> bootstrap(EMAIL, "a".repeat(73)).ensureAdmin())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PASSWORD");
    }

    // 25 ký tự nhưng mỗi chữ "ạ" chiếm 3 byte UTF-8 → 75 byte, BCrypt sẽ cắt âm thầm nếu không chặn.
    @Test
    void matKhauTiengVietCoDauTinhTheoByteChuKhongTheoKyTu() {
        String vietnam = "ạ".repeat(25);

        assertThatThrownBy(() -> bootstrap(EMAIL, vietnam).ensureAdmin())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PASSWORD");
    }

    @Test
    void daCoAdminThiKhongTaoThem() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(true);

        bootstrap(EMAIL, MAT_KHAU).ensureAdmin();

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void emailDaThuocHocSinhThiBaoLoiVaKhongTuNangQuyen() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        User hocSinh = User.newLocalLearner(EMAIL, "hash", "Học sinh");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(hocSinh));

        assertThatThrownBy(() -> bootstrap(EMAIL, MAT_KHAU).ensureAdmin())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_EMAIL")
                .hasMessageNotContaining(MAT_KHAU);

        verify(userRepository, never()).saveAndFlush(any());
        assertThat(hocSinh.getRole()).isEqualTo(Role.LEARNER);
    }

    @Test
    void chuaCoAdminThiTaoTaiKhoanAdminVoiEmailChuanHoaVaMatKhauDaBam() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);

        bootstrap("  Admin@Example.COM ", MAT_KHAU).ensureAdmin();

        verify(userRepository).findByEmail(EMAIL);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo(EMAIL);
        assertThat(saved.getRole()).isEqualTo(Role.ADMIN);
        assertThat(saved.getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(saved.getProviderId()).isNull();
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getDisplayName()).isNotBlank();
        assertThat(saved.getPasswordHash()).isNotEqualTo(MAT_KHAU).startsWith("$2");
        assertThat(passwordEncoder.matches(MAT_KHAU, saved.getPasswordHash())).isTrue();
    }

    @Test
    void khongGhiMatKhauVaoLog(CapturedOutput output) {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);

        bootstrap(EMAIL, MAT_KHAU).ensureAdmin();

        // Phải có log báo đã tạo (để chắc test thật sự bắt được log), nhưng không chứa mật khẩu.
        assertThat(output.getAll()).contains("ADMIN").doesNotContain(MAT_KHAU);
    }
}
