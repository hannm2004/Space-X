package vn.spacex.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Chỉ nhận tên hiển thị. Email, role, id... nếu có trong thân yêu cầu thì bị bỏ qua vì record không có trường đó.
public record UpdateProfileRequest(
        @NotBlank(message = "Tên hiển thị không được để trống")
        @Size(max = 100, message = "Tên hiển thị tối đa 100 ký tự")
        String displayName) {

    public UpdateProfileRequest {
        displayName = displayName == null ? null : displayName.trim();
    }
}
