package vn.spacex.user;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileRequestValidationTest {

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static <T> Set<String> truongLoi(T request) {
        return validator.validate(request).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private static ChangePasswordRequest doiMatKhau(String cu, String moi, String xacNhan) {
        return new ChangePasswordRequest(cu, moi, xacNhan);
    }

    @Test
    void doiMatKhauHopLeKhongCoLoi() {
        assertThat(truongLoi(doiMatKhau("matkhau-cu-1", "matkhau-moi-2", "matkhau-moi-2"))).isEmpty();
    }

    @Test
    void doiMatKhauThieuTruongThiBaoLoiTungTruong() {
        assertThat(truongLoi(doiMatKhau(null, null, null)))
                .contains("currentPassword", "newPassword", "confirmNewPassword");
    }

    @Test
    void matKhauMoiDuoi8KyTuBiTuChoi() {
        assertThat(truongLoi(doiMatKhau("matkhau-cu-1", "ngan", "ngan"))).containsExactly("newPassword");
    }

    @Test
    void matKhauMoiTinhTheoByteChuKhongPhaiKyTu() {
        // "ệ" (ệ) chiếm 3 byte UTF-8: 24 ký tự = 72 byte (được), 25 ký tự = 75 byte (quá giới hạn BCrypt).
        String du72Byte = "ệ".repeat(24);
        String qua72Byte = "ệ".repeat(25);

        assertThat(truongLoi(doiMatKhau("matkhau-cu-1", du72Byte, du72Byte))).isEmpty();
        assertThat(truongLoi(doiMatKhau("matkhau-cu-1", qua72Byte, qua72Byte)))
                .containsExactly("newPasswordWithinBcryptLimit");
    }

    @Test
    void xacNhanMatKhauMoiKhongKhopBiTuChoi() {
        assertThat(truongLoi(doiMatKhau("matkhau-cu-1", "matkhau-moi-2", "matkhau-khac-3")))
                .containsExactly("confirmNewPasswordMatching");
    }

    @Test
    void matKhauMoiGiongMatKhauCuBiTuChoi() {
        assertThat(truongLoi(doiMatKhau("matkhau-cu-1", "matkhau-cu-1", "matkhau-cu-1")))
                .containsExactly("newPasswordDifferentFromCurrent");
    }

    @Test
    void khoangTrangTrongMatKhauDuocGiuNguyenKhongBiCat() {
        ChangePasswordRequest request = doiMatKhau(" cu cu cu ", " moi moi ", " moi moi ");

        assertThat(request.currentPassword()).isEqualTo(" cu cu cu ");
        assertThat(request.newPassword()).isEqualTo(" moi moi ");
        assertThat(truongLoi(request)).isEmpty();
    }

    @Test
    void toStringKhongChuaMatKhau() {
        String text = doiMatKhau("matkhau-cu-1", "matkhau-moi-2", "matkhau-moi-2").toString();

        assertThat(text).doesNotContain("matkhau-cu-1").doesNotContain("matkhau-moi-2");
    }

    @Test
    void tenHienThiHopLeKhongCoLoi() {
        assertThat(truongLoi(new UpdateProfileRequest("Nguyễn Văn A"))).isEmpty();
    }

    @Test
    void tenHienThiTrongHoacChiCoKhoangTrangBiTuChoi() {
        assertThat(truongLoi(new UpdateProfileRequest(null))).containsExactly("displayName");
        assertThat(truongLoi(new UpdateProfileRequest("   "))).containsExactly("displayName");
    }

    @Test
    void tenHienThiDuoc100KyTuNhungKhongQua100() {
        assertThat(truongLoi(new UpdateProfileRequest("a".repeat(100)))).isEmpty();
        assertThat(truongLoi(new UpdateProfileRequest("a".repeat(101)))).containsExactly("displayName");
    }

    @Test
    void tenHienThiBiCatKhoangTrangDauCuoi() {
        assertThat(new UpdateProfileRequest("  An  ").displayName()).isEqualTo("An");
    }
}
