package vn.spacex.user;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;

// Luật mật khẩu mới giống lúc đăng ký (D25). Mật khẩu giữ nguyên, không cắt khoảng trắng.
public record ChangePasswordRequest(
        @NotNull(message = "Mật khẩu hiện tại không được để trống")
        String currentPassword,

        @NotNull(message = "Mật khẩu mới không được để trống")
        @Size(min = 8, message = "Mật khẩu mới tối thiểu 8 ký tự")
        String newPassword,

        @NotNull(message = "Xác nhận mật khẩu mới không được để trống")
        String confirmNewPassword) {

    // BCrypt chỉ dùng 72 byte đầu nên mật khẩu dài hơn sẽ bị cắt âm thầm; tiếng Việt có dấu chiếm 2-3 byte mỗi ký tự.
    @AssertTrue(message = "Mật khẩu mới tối đa 72 byte (tiếng Việt có dấu chiếm nhiều byte hơn)")
    boolean isNewPasswordWithinBcryptLimit() {
        return newPassword == null || newPassword.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    // Thiếu một trong hai thì đã có lỗi @NotNull riêng, không báo thêm lỗi "không khớp".
    @AssertTrue(message = "Xác nhận mật khẩu mới không khớp")
    boolean isConfirmNewPasswordMatching() {
        return newPassword == null || confirmNewPassword == null || newPassword.equals(confirmNewPassword);
    }

    @AssertTrue(message = "Mật khẩu mới phải khác mật khẩu hiện tại")
    boolean isNewPasswordDifferentFromCurrent() {
        return newPassword == null || currentPassword == null || !newPassword.equals(currentPassword);
    }

    // Record mặc định in cả mật khẩu; ghi đè để mật khẩu không rơi vào log.
    @Override
    public String toString() {
        return "ChangePasswordRequest[***]";
    }
}
