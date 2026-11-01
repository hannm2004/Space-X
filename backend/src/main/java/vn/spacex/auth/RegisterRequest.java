package vn.spacex.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.spacex.user.Emails;

import java.nio.charset.StandardCharsets;

public record RegisterRequest(
        @NotBlank(message = "Tên hiển thị không được để trống")
        @Size(max = 100, message = "Tên hiển thị tối đa 100 ký tự")
        String displayName,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        @Size(max = 255, message = "Email tối đa 255 ký tự")
        String email,

        @NotNull(message = "Mật khẩu không được để trống")
        @Size(min = 8, message = "Mật khẩu tối thiểu 8 ký tự")
        String password,

        @NotNull(message = "Xác nhận mật khẩu không được để trống")
        String confirmPassword) {

    // Chuẩn hóa trước khi kiểm tra (D25). Mật khẩu thì giữ nguyên, kể cả khoảng trắng.
    public RegisterRequest {
        displayName = displayName == null ? null : displayName.trim();
        email = Emails.normalize(email);
    }

    // BCrypt chỉ dùng 72 byte đầu nên mật khẩu dài hơn sẽ bị cắt âm thầm; tiếng Việt có dấu chiếm 2-3 byte mỗi ký tự.
    @AssertTrue(message = "Mật khẩu tối đa 72 byte (tiếng Việt có dấu chiếm nhiều byte hơn)")
    boolean isPasswordWithinBcryptLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    // Thiếu một trong hai thì đã có lỗi @NotNull riêng, không báo thêm lỗi "không khớp".
    @AssertTrue(message = "Xác nhận mật khẩu không khớp")
    boolean isConfirmPasswordMatching() {
        return password == null || confirmPassword == null || password.equals(confirmPassword);
    }

    // Record mặc định in cả mật khẩu; ghi đè để mật khẩu không rơi vào log.
    @Override
    public String toString() {
        return "RegisterRequest[email=" + email + "]";
    }
}
