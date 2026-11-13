package vn.spacex.user;

// Mật khẩu tạm chỉ xuất hiện trong phản hồi này, một lần: database chỉ giữ mã băm, log không ghi lại.
public record ResetPasswordResponse(Long id, String email, String temporaryPassword) {
}
