package vn.spacex.auth;

// Đăng nhập Google bị từ chối theo luật của ứng dụng (không phải lỗi hạ tầng). Mỗi lý do có một mã ngắn đưa lên địa chỉ
// /dang-nhap?loi=<mã> để giao diện hiện đúng câu; mã chỉ gồm chữ thường, số, gạch ngang nên không chèn được ký tự lạ.
public class GoogleLoginRejectedException extends RuntimeException {

    public enum Reason {
        UNVERIFIED_EMAIL("google-chua-xac-minh"),
        // D25: không tự liên kết với tài khoản mật khẩu; học sinh phải đăng nhập bằng email và mật khẩu.
        EMAIL_USED_BY_PASSWORD("google-email-mat-khau"),
        ACCOUNT_DISABLED("google-bi-khoa"),
        // Mọi trường hợp còn lại, cố ý không nói rõ lý do.
        FAILED("google-that-bai");

        private final String code;

        Reason(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }
    }

    private final Reason reason;

    public GoogleLoginRejectedException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
