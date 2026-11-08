package vn.spacex.course;

// Khác CourseResponse (công khai, 4 trường): Admin cần thêm sortOrder để sửa.
public record AdminCourseResponse(Long id, String slug, String title, String description, int sortOrder) {

    static AdminCourseResponse from(Course course) {
        return new AdminCourseResponse(course.getId(), course.getSlug(), course.getTitle(),
                course.getDescription(), course.getSortOrder());
    }
}
