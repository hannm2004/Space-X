package vn.spacex.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import vn.spacex.user.AuthProvider;
import vn.spacex.user.Role;
import vn.spacex.user.User;
import vn.spacex.user.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleAccountServiceTest {

    private static final String SUB = "109876543210987654321";
    private static final String EMAIL = "hocsinh@gmail.com";

    @Mock
    private UserRepository userRepository;

    private GoogleAccountService service;

    @BeforeEach
    void setUp() {
        service = new GoogleAccountService(userRepository);
    }

    private static GoogleProfile hoSo(String email, boolean daXacMinh, String ten) {
        return new GoogleProfile(SUB, email, daXacMinh, ten);
    }

    private static User taiKhoanGoogle(String providerSub, boolean enabled) {
        User user = User.newGoogleLearner(EMAIL, "Học Sinh", providerSub);
        ReflectionTestUtils.setField(user, "id", 7L);
        ReflectionTestUtils.setField(user, "enabled", enabled);
        return user;
    }

    private void khongCoTaiKhoanNao() {
        when(userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
    }

    private User taoMoi(GoogleProfile profile) {
        khongCoTaiKhoanNao();
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(call -> call.getArgument(0));
        service.signIn(profile);
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        return saved.getValue();
    }

    // ---- Email chưa xác minh ----

    @Test
    void emailChuaXacMinhBiTuChoiVaKhongDungToiDatabase() {
        assertThatThrownBy(() -> service.signIn(hoSo(EMAIL, false, "Học Sinh")))
                .isInstanceOfSatisfying(GoogleLoginRejectedException.class,
                        e -> assertThat(e.reason()).isEqualTo(GoogleLoginRejectedException.Reason.UNVERIFIED_EMAIL));

        verifyNoInteractions(userRepository);
    }

    @Test
    void thieuEmailHoacThieuMaDinhDanhBiTuChoiChung() {
        assertThatThrownBy(() -> service.signIn(new GoogleProfile(SUB, null, true, "A")))
                .isInstanceOfSatisfying(GoogleLoginRejectedException.class,
                        e -> assertThat(e.reason()).isEqualTo(GoogleLoginRejectedException.Reason.FAILED));
        assertThatThrownBy(() -> service.signIn(new GoogleProfile(" ", EMAIL, true, "A")))
                .isInstanceOfSatisfying(GoogleLoginRejectedException.class,
                        e -> assertThat(e.reason()).isEqualTo(GoogleLoginRejectedException.Reason.FAILED));

        verifyNoInteractions(userRepository);
    }

    // ---- Tạo tài khoản mới ----

    @Test
    void emailMoiTaoTaiKhoanGoogleVaiTroLearnerKhongCoMatKhau() {
        User user = taoMoi(hoSo(EMAIL, true, "Học Sinh"));

        assertThat(user.getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(user.getProviderId()).isEqualTo(SUB);
        assertThat(user.getRole()).isEqualTo(Role.LEARNER);
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getEmail()).isEqualTo(EMAIL);
        assertThat(user.getDisplayName()).isEqualTo("Học Sinh");
    }

    @Test
    void emailTuGoogleDuocChuanHoaGiongDangKy() {
        when(userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(call -> call.getArgument(0));

        User user = service.signIn(hoSo("  HocSinh@Gmail.COM ", true, "Học Sinh"));

        assertThat(user.getEmail()).isEqualTo(EMAIL);
    }

    @Test
    void tenTrongThiLayPhanTruocDauAtCuaEmail() {
        assertThat(taoMoi(hoSo(EMAIL, true, "   ")).getDisplayName()).isEqualTo("hocsinh");
    }

    @Test
    void tenKhongCoThiLayPhanTruocDauAtCuaEmail() {
        assertThat(taoMoi(hoSo(EMAIL, true, null)).getDisplayName()).isEqualTo("hocsinh");
    }

    @Test
    void tenQuaDaiBiCatDu100KyTuDeKhopCotDatabase() {
        assertThat(taoMoi(hoSo(EMAIL, true, "A".repeat(150))).getDisplayName()).hasSize(100);
    }

    @Test
    void tenCoKhoangTrangDauCuoiDuocCat() {
        assertThat(taoMoi(hoSo(EMAIL, true, "  Học Sinh  ")).getDisplayName()).isEqualTo("Học Sinh");
    }

    // ---- Tài khoản đã có ----

    @Test
    void daCoTaiKhoanGoogleCungMaDinhDanhThiDangNhapVaoDoVaKhongTaoThem() {
        User co = taiKhoanGoogle(SUB, true);
        when(userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, SUB)).thenReturn(Optional.of(co));

        User user = service.signIn(hoSo(EMAIL, true, "Tên Khác"));

        assertThat(user).isSameAs(co);
        verify(userRepository, never()).saveAndFlush(any());
        // Tên đã lưu không bị ghi đè bởi tên mới từ Google.
        assertThat(user.getDisplayName()).isEqualTo("Học Sinh");
    }

    @Test
    void taiKhoanGoogleBiAdminKhoaBiTuChoi() {
        when(userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, SUB))
                .thenReturn(Optional.of(taiKhoanGoogle(SUB, false)));

        assertThatThrownBy(() -> service.signIn(hoSo(EMAIL, true, "Học Sinh")))
                .isInstanceOfSatisfying(GoogleLoginRejectedException.class,
                        e -> assertThat(e.reason()).isEqualTo(GoogleLoginRejectedException.Reason.ACCOUNT_DISABLED));
    }

    // D25: không tự liên kết với tài khoản mật khẩu cùng email.
    @Test
    void emailDaThuocTaiKhoanMatKhauBiTuChoiVaKhongTaoHayLienKet() {
        User matKhau = User.newLocalLearner(EMAIL, "$2a$hash", "Học Sinh");
        when(userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(matKhau));

        assertThatThrownBy(() -> service.signIn(hoSo(EMAIL, true, "Học Sinh")))
                .isInstanceOfSatisfying(GoogleLoginRejectedException.class, e -> assertThat(e.reason())
                        .isEqualTo(GoogleLoginRejectedException.Reason.EMAIL_USED_BY_PASSWORD));

        verify(userRepository, never()).saveAndFlush(any());
        assertThat(matKhau.getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(matKhau.getProviderId()).isNull();
    }

    // Email này từng thuộc một tài khoản Google khác (người khác, cùng địa chỉ cũ): không được vào tài khoản đó.
    @Test
    void emailThuocTaiKhoanGoogleKhacMaDinhDanhBiTuChoiChung() {
        when(userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(taiKhoanGoogle("999-khac", true)));

        assertThatThrownBy(() -> service.signIn(hoSo(EMAIL, true, "Học Sinh")))
                .isInstanceOfSatisfying(GoogleLoginRejectedException.class,
                        e -> assertThat(e.reason()).isEqualTo(GoogleLoginRejectedException.Reason.FAILED));

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void haiLanDangNhapDauCungLucThiNguoiThuaBaoThatBaiChungVaKhongLoiRaNgoai() {
        khongCoTaiKhoanNao();
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("uk"));

        assertThatThrownBy(() -> service.signIn(hoSo(EMAIL, true, "Học Sinh")))
                .isInstanceOfSatisfying(GoogleLoginRejectedException.class,
                        e -> assertThat(e.reason()).isEqualTo(GoogleLoginRejectedException.Reason.FAILED));
    }

    // Mã đưa lên địa chỉ chuyển hướng: chỉ gồm chữ thường, số và gạch ngang để không chèn được ký tự lạ.
    @Test
    void maLyDoChiGomChuThuongSoVaGachNgang() {
        for (GoogleLoginRejectedException.Reason reason : GoogleLoginRejectedException.Reason.values()) {
            assertThat(reason.code()).matches("[a-z0-9-]+").startsWith("google-");
        }
    }
}
