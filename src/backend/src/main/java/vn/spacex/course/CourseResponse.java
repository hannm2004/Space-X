package vn.spacex.course;

// coverImageUrl là đường dẫn ảnh bìa khóa (công khai), hoặc null khi khóa chưa có ảnh bìa.
// lessonCount là số bài đã đăng của khóa (không tính bài nháp), 0 khi khóa chưa có bài.
// tuitionVnd = 0 là chưa có học phí (khóa chưa mở đăng ký).
public record CourseResponse(Long id, String slug, String title, String description, String coverImageUrl,
                             int lessonCount, CategoryResponse category, CourseLevel level, int totalHours,
                             long tuitionVnd) {

    static CourseResponse from(Course course, String coverImageUrl, int lessonCount, CategoryResponse category) {
        return new CourseResponse(course.getId(), course.getSlug(), course.getTitle(), course.getDescription(),
                coverImageUrl, lessonCount, category, course.getLevel(), course.getTotalHours(),
                course.getTuitionVnd());
    }
}
