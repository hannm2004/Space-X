package vn.spacex.auth;

// Phần thông tin lấy từ ID token của Google mà ứng dụng cần. subject là mã định danh cố định của tài khoản Google
// (không đổi khi người dùng đổi email); emailVerified là claim email_verified. Không giữ mã token hay ảnh đại diện.
public record GoogleProfile(String subject, String email, boolean emailVerified, String name) {
}
