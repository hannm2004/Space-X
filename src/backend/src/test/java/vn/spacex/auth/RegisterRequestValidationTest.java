package vn.spacex.auth;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestValidationTest {

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static RegisterRequest request(String displayName, String email, String password, String confirmPassword) {
        return new RegisterRequest(displayName, email, password, confirmPassword);
    }

    private static Set<String> truongLoi(RegisterRequest request) {
        return validator.validate(request).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    void duLieuHopLeKhongCoLoi() {
        assertThat(truongLoi(request("Nguyễn Văn A", "a@example.com", "matkhau123", "matkhau123"))).isEmpty();
    }

    @Test
    void thieuTruongThiBaoLoiTungTruong() {
        assertThat(truongLoi(request(null, null, null, null)))
                .contains("displayName", "email", "password", "confirmPassword");
    }

    @Test
    void tenHienThiChiCoKhoangTrangBiTuChoi() {
        assertThat(truongLoi(request("   ", "a@example.com", "matkhau123", "matkhau123")))
                .containsExactly("displayName");
    }

    @Test
    void tenHienThiDuoc100KyTuNhungKhongQua100() {
        assertThat(truongLoi(request("a".repeat(100), "a@example.com", "matkhau123", "matkhau123"))).isEmpty();
        assertThat(truongLoi(request("a".repeat(101), "a@example.com", "matkhau123", "matkhau123")))
                .containsExactly("displayName");
    }

    @Test
    void emailSaiDinhDangBiTuChoi() {
        assertThat(truongLoi(request("A", "khong-phai-email", "matkhau123", "matkhau123")))
                .containsExactly("email");
    }

    @Test
    void emailQua255KyTuBiTuChoi() {
        String emailDai = "a".repeat(250) + "@b.com";
        assertThat(truongLoi(request("A", emailDai, "matkhau123", "matkhau123"))).contains("email");
    }

    @Test
    void emailDuocCatKhoangTrangVaDoiChuThuongTruocKhiKiemTra() {
        RegisterRequest request = request("A", "  A@Example.COM  ", "matkhau123", "matkhau123");

        assertThat(request.email()).isEqualTo("a@example.com");
        assertThat(truongLoi(request)).isEmpty();
    }

    @Test
    void tenHienThiDuocCatKhoangTrang() {
        assertThat(request("  Nguyễn Văn A  ", "a@example.com", "matkhau123", "matkhau123").displayName())
                .isEqualTo("Nguyễn Văn A");
    }

    @Test
    void matKhauDuoi8KyTuBiTuChoi() {
        assertThat(truongLoi(request("A", "a@example.com", "1234567", "1234567"))).containsExactly("password");
        assertThat(truongLoi(request("A", "a@example.com", "12345678", "12345678"))).isEmpty();
    }

    @Test
    void matKhauToiDa72ByteUtf8() {
        String du72Byte = "a".repeat(72);
        String quaMotByte = "a".repeat(73);

        assertThat(truongLoi(request("A", "a@example.com", du72Byte, du72Byte))).isEmpty();
        assertThat(truongLoi(request("A", "a@example.com", quaMotByte, quaMotByte)))
                .containsExactly("passwordWithinBcryptLimit");
    }

    @Test
    void matKhauTiengVietCoDauTinhTheoByteChuKhongPhaiKyTu() {
        // 25 ký tự nhưng mỗi 'ệ' chiếm 3 byte UTF-8 nên là 75 byte: vượt giới hạn của BCrypt.
        String matKhau = "ệ".repeat(25);

        assertThat(matKhau.length()).isLessThanOrEqualTo(72);
        assertThat(truongLoi(request("A", "a@example.com", matKhau, matKhau)))
                .containsExactly("passwordWithinBcryptLimit");
    }

    @Test
    void matKhauKhongBiCatKhoangTrang() {
        RegisterRequest request = request("A", "a@example.com", "  mat khau  ", "  mat khau  ");

        assertThat(request.password()).isEqualTo("  mat khau  ");
        assertThat(truongLoi(request)).isEmpty();
    }

    @Test
    void xacNhanMatKhauKhongKhopBiTuChoi() {
        assertThat(truongLoi(request("A", "a@example.com", "matkhau123", "matkhau124")))
                .containsExactly("confirmPasswordMatching");
    }

    @Test
    void toStringKhongLoMatKhau() {
        assertThat(request("A", "a@example.com", "matkhau123", "matkhau123").toString())
                .doesNotContain("matkhau123");
    }
}
