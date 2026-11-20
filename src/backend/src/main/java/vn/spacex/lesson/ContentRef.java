package vn.spacex.lesson;

// Tham chiếu gọn tới một khóa hoặc một bài (chỉ slug và tiêu đề) để trang bài dựng đường dẫn, "Bài trước", "Bài sau".
// Cố ý không có mã số, trạng thái hay nội dung.
public record ContentRef(String slug, String title) {
}
