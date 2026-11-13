package vn.spacex.user;

// Admin yêu cầu một thao tác trái quy tắc nghiệp vụ (409). Thông báo là câu cố định, không chứa dữ liệu người gửi,
// nên GlobalExceptionHandler trả thẳng ra cho Admin để biết vì sao bị từ chối.
public class UserActionNotAllowedException extends RuntimeException {

    private UserActionNotAllowedException(String message) {
        super(message);
    }

    public static UserActionNotAllowedException selfDisable() {
        return new UserActionNotAllowedException("Không thể tự khóa tài khoản của chính mình");
    }

    public static UserActionNotAllowedException lastAdmin() {
        return new UserActionNotAllowedException("Không thể khóa Admin cuối cùng còn hoạt động");
    }

    public static UserActionNotAllowedException selfReset() {
        return new UserActionNotAllowedException(
                "Không thể đặt lại mật khẩu của chính mình, hãy dùng chức năng đổi mật khẩu");
    }

    public static UserActionNotAllowedException noPassword() {
        return new UserActionNotAllowedException("Tài khoản Google không có mật khẩu để đặt lại");
    }
}
