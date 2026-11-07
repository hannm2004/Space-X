package vn.spacex.auth;

import jakarta.validation.constraints.NotBlank;
import vn.spacex.user.Emails;

public record LoginRequest(
        @NotBlank(message = "Email không được để trống") String email,
        @NotBlank(message = "Mật khẩu không được để trống") String password) {

    public LoginRequest {
        email = Emails.normalize(email);
    }

    @Override
    public String toString() {
        return "LoginRequest[email=" + email + "]";
    }
}
