package vn.spacex.lesson;

// Dùng chung cho bài không tồn tại và bài chưa đăng: người gọi không phân biệt được hai trường hợp.
public class LessonNotFoundException extends RuntimeException {

    // Không đưa slug do người dùng gửi vào thông báo.
    public LessonNotFoundException() {
        super("Khong tim thay bai hoc");
    }
}
