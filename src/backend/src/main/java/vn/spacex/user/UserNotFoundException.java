package vn.spacex.user;

public class UserNotFoundException extends RuntimeException {

    // Không đưa id do người gọi gửi vào thông báo.
    public UserNotFoundException() {
        super("Khong tim thay nguoi dung");
    }
}
