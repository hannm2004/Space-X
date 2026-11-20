package vn.spacex.course;

// coverImageUrl là đường dẫn ảnh bìa khóa (công khai), hoặc null khi khóa chưa có ảnh bìa.
public record CourseResponse(Long id, String slug, String title, String description, String coverImageUrl) {

    static CourseResponse from(Course course, String coverImageUrl) {
        return new CourseResponse(course.getId(), course.getSlug(), course.getTitle(), course.getDescription(),
                coverImageUrl);
    }
}
