package vn.spacex.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.spacex.common.api.PageResponse;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    private static final long ADMIN_ID = 1L;
    private static final long HOC_SINH_ID = 7L;
    private static final long ADMIN_KHAC_ID = 2L;
    private static final String MAT_KHAU_TAM = "Abcd2345Efgh6789";

    @Mock
    private UserRepository userRepository;

    @Mock
    private TemporaryPasswordGenerator generator;

    // BCrypt thật để kiểm tra mật khẩu tạm đúng là được băm, không phải chuỗi giả.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AdminUserService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserService(userRepository, passwordEncoder, generator);
    }

    private User taiKhoan(long id, User user) {
        ReflectionTestUtils.setField(user, "id", id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        return user;
    }

    private User hocSinh() {
        return taiKhoan(HOC_SINH_ID,
                User.newLocalLearner("hs@example.com", passwordEncoder.encode("matkhau-cu-1"), "Học sinh"));
    }

    private User adminKhac() {
        return taiKhoan(ADMIN_KHAC_ID,
                User.newLocalAdmin("admin2@example.com", passwordEncoder.encode("matkhau-cu-1"), "Admin hai"));
    }

    // ---- Danh sách ----

    @Test
    void danhSachKhongTimKiemThiTruyenPatternNullVaSapMoiNhatTruoc() {
        User user = User.newLocalLearner("hs@example.com", "hash", "Học sinh");
        ReflectionTestUtils.setField(user, "id", HOC_SINH_ID);
        when(userRepository.search(isNull(), any(Pageable.class)))
                .thenAnswer(i -> new PageImpl<>(List.of(user), i.getArgument(1), 41));

        PageResponse<AdminUserResponse> response = service.list(null, 2, 20);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).search(isNull(), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "id"));
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalItems()).isEqualTo(41);
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(HOC_SINH_ID);
            assertThat(item.email()).isEqualTo("hs@example.com");
            assertThat(item.displayName()).isEqualTo("Học sinh");
            assertThat(item.role()).isEqualTo(Role.LEARNER);
            assertThat(item.authProvider()).isEqualTo(AuthProvider.LOCAL);
            assertThat(item.enabled()).isTrue();
        });
    }

    @Test
    void tuKhoaRongHoacChiCoKhoangTrangNghiaLaKhongTimKiem() {
        when(userRepository.search(isNull(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.list("   ", 0, 20);
        service.list("", 0, 20);

        verify(userRepository, times(2)).search(isNull(), any(Pageable.class));
    }

    @Test
    void tuKhoaDuocCatKhoangTrangVaBocTrongDauPhanTram() {
        when(userRepository.search(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.list("  nguyen  ", 0, 20);

        verify(userRepository).search(eq("%nguyen%"), any(Pageable.class));
    }

    // % và _ là ký tự đại diện của LIKE: nếu không thoát thì tìm "%" sẽ khớp mọi tài khoản.
    @Test
    void kyTuDacBietCuaLikeDuocThoatDeKhongKhopMoiThu() {
        when(userRepository.search(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.list("a%b_c\\d", 0, 20);

        verify(userRepository).search(eq("%a\\%b\\_c\\\\d%"), any(Pageable.class));
    }

    // ---- Khóa / mở khóa ----

    @Test
    void adminKhoaHocSinhThiTaiKhoanBiKhoaVaPhanHoiTraTrangThaiMoi() {
        User user = hocSinh();

        AdminUserResponse response = service.changeEnabled(ADMIN_ID, HOC_SINH_ID, false);

        assertThat(user.isEnabled()).isFalse();
        assertThat(response.enabled()).isFalse();
        assertThat(response.email()).isEqualTo("hs@example.com");
    }

    @Test
    void moKhoaTaiKhoanDangBiKhoaThiBatLaiVaKhongCanKiemTraAdminCuoiCung() {
        User user = hocSinh();
        user.changeEnabled(false);

        AdminUserResponse response = service.changeEnabled(ADMIN_ID, HOC_SINH_ID, true);

        assertThat(user.isEnabled()).isTrue();
        assertThat(response.enabled()).isTrue();
        verify(userRepository, never()).countByRoleAndEnabled(any(), anyBoolean());
    }

    @Test
    void khoaTaiKhoanDaBiKhoaRoiLaThaoTacLapLaiKhongLoi() {
        User user = hocSinh();
        user.changeEnabled(false);

        AdminUserResponse response = service.changeEnabled(ADMIN_ID, HOC_SINH_ID, false);

        assertThat(user.isEnabled()).isFalse();
        assertThat(response.enabled()).isFalse();
    }

    @Test
    void khongTuKhoaMinhVaTaiKhoanKhongDoi() {
        User chinhMinh = taiKhoan(ADMIN_ID, User.newLocalAdmin("admin@example.com", "hash", "Quản trị"));

        assertThatThrownBy(() -> service.changeEnabled(ADMIN_ID, ADMIN_ID, false))
                .isInstanceOf(UserActionNotAllowedException.class)
                .hasMessageContaining("tự khóa");

        assertThat(chinhMinh.isEnabled()).isTrue();
    }

    @Test
    void khongKhoaAdminCuoiCungConHoatDongVaTaiKhoanKhongDoi() {
        User admin = adminKhac();
        when(userRepository.countByRoleAndEnabled(Role.ADMIN, true)).thenReturn(1L);

        assertThatThrownBy(() -> service.changeEnabled(ADMIN_ID, ADMIN_KHAC_ID, false))
                .isInstanceOf(UserActionNotAllowedException.class)
                .hasMessageContaining("Admin cuối cùng");

        assertThat(admin.isEnabled()).isTrue();
    }

    @Test
    void khoaAdminKhiConAdminKhacDangHoatDongThiDuoc() {
        User admin = adminKhac();
        when(userRepository.countByRoleAndEnabled(Role.ADMIN, true)).thenReturn(2L);

        service.changeEnabled(ADMIN_ID, ADMIN_KHAC_ID, false);

        assertThat(admin.isEnabled()).isFalse();
    }

    @Test
    void khongTimThayTaiKhoanKhiKhoaThiNemUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeEnabled(ADMIN_ID, 99L, false))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ---- Đặt lại mật khẩu ----

    @Test
    void datLaiMatKhauLuuMaBamCuaMatKhauTamVaTraMatKhauTam() {
        User user = hocSinh();
        String hashCu = user.getPasswordHash();
        when(generator.generate()).thenReturn(MAT_KHAU_TAM);

        ResetPasswordResponse response = service.resetPassword(ADMIN_ID, HOC_SINH_ID);

        assertThat(response.id()).isEqualTo(HOC_SINH_ID);
        assertThat(response.email()).isEqualTo("hs@example.com");
        assertThat(response.temporaryPassword()).isEqualTo(MAT_KHAU_TAM);
        assertThat(user.getPasswordHash()).isNotEqualTo(hashCu).isNotEqualTo(MAT_KHAU_TAM);
        assertThat(passwordEncoder.matches(MAT_KHAU_TAM, user.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("matkhau-cu-1", user.getPasswordHash())).isFalse();
    }

    @Test
    void datLaiMatKhauChoTaiKhoanDangBiKhoaVanDuocVaKhongTuMoKhoa() {
        User user = hocSinh();
        user.changeEnabled(false);
        when(generator.generate()).thenReturn(MAT_KHAU_TAM);

        service.resetPassword(ADMIN_ID, HOC_SINH_ID);

        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    void khongTuDatLaiMatKhauCuaChinhMinhVaMatKhauKhongDoi() {
        User chinhMinh = taiKhoan(ADMIN_ID, User.newLocalAdmin("admin@example.com", "hash-cu", "Quản trị"));

        assertThatThrownBy(() -> service.resetPassword(ADMIN_ID, ADMIN_ID))
                .isInstanceOf(UserActionNotAllowedException.class)
                .hasMessageContaining("chính mình");

        assertThat(chinhMinh.getPasswordHash()).isEqualTo("hash-cu");
        verifyNoInteractions(generator);
    }

    @Test
    void taiKhoanGoogleKhongCoMatKhauThiKhongDatLaiDuocVaVanKhongCoMatKhau() {
        User google = taiKhoan(HOC_SINH_ID, User.newLocalLearner("g@example.com", null, "Google"));

        assertThatThrownBy(() -> service.resetPassword(ADMIN_ID, HOC_SINH_ID))
                .isInstanceOf(UserActionNotAllowedException.class)
                .hasMessageContaining("Google");

        assertThat(google.getPasswordHash()).isNull();
        verifyNoInteractions(generator);
    }

    @Test
    void khongTimThayTaiKhoanKhiDatLaiMatKhauThiNemUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword(ADMIN_ID, 99L))
                .isInstanceOf(UserNotFoundException.class);
        verifyNoInteractions(generator);
    }
}
