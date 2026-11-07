package vn.spacex.course;

public class CourseNotFoundException extends RuntimeException {

    // Không đưa slug do người dùng gửi vào thông báo.
    public CourseNotFoundException() {
        super("Khong tim thay khoa hoc");
    }
}
