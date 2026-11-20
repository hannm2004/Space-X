package vn.spacex.course;

// coverImageUrl là đường dẫn ảnh bìa khóa (công khai), hoặc null khi khóa chưa có ảnh bìa.
// lessonCount là số bài đã đăng của khóa (không tính bài nháp), 0 khi khóa chưa có bài.
public record CourseResponse(Long id, String slug, String title, String description, String coverImageUrl,
                             int lessonCount) {

    static CourseResponse from(Course course, String coverImageUrl, int lessonCount) {
        return new CourseResponse(course.getId(), course.getSlug(), course.getTitle(), course.getDescription(),
                coverImageUrl, lessonCount);
    }
}
