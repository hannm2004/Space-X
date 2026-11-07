package vn.spacex.user;

// Tài khoản Google không có mật khẩu (password_hash NULL) nên không có gì để đổi.
public class NoPasswordAccountException extends RuntimeException {
}
