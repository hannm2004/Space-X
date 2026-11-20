package vn.spacex.course;

// Khác CourseResponse (công khai, 5 trường): Admin cần thêm sortOrder để sửa.
public record AdminCourseResponse(Long id, String slug, String title, String description, int sortOrder,
                                  String coverImageUrl) {

    static AdminCourseResponse from(Course course, String coverImageUrl) {
        return new AdminCourseResponse(course.getId(), course.getSlug(), course.getTitle(),
                course.getDescription(), course.getSortOrder(), coverImageUrl);
    }
}
