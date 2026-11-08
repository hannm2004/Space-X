package vn.spacex.lesson;

public class LessonSlugAlreadyExistsException extends RuntimeException {

    // Không đưa slug do người dùng gửi vào thông báo.
    public LessonSlugAlreadyExistsException() {
        super("Slug bai hoc da ton tai");
    }
}
