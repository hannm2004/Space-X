package vn.spacex.course;

// Khác CourseResponse (công khai): Admin cần thêm sortOrder, trạng thái và học phí để sửa.
public record AdminCourseResponse(Long id, String slug, String title, String description, int sortOrder,
                                  CourseStatus status, long tuitionVnd, String coverImageUrl) {

    static AdminCourseResponse from(Course course, String coverImageUrl) {
        return new AdminCourseResponse(course.getId(), course.getSlug(), course.getTitle(),
                course.getDescription(), course.getSortOrder(), course.getStatus(), course.getTuitionVnd(),
                coverImageUrl);
    }
}
