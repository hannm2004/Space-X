package vn.spacex.course;

public class CourseSlugAlreadyExistsException extends RuntimeException {

    // Không đưa slug do người dùng gửi vào thông báo.
    public CourseSlugAlreadyExistsException() {
        super("Slug khoa hoc da ton tai");
    }
}
