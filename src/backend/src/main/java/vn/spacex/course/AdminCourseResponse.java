package vn.spacex.course;

// Khác CourseResponse (công khai): Admin cần giá trị thô để sửa (sortOrder, trạng thái, id nhóm).
public record AdminCourseResponse(Long id, String slug, String title, String description, int sortOrder,
                                  CourseStatus status, long tuitionVnd, Long categoryId, CourseLevel level,
                                  int totalHours, String coverImageUrl, CourseAccessType accessType) {

    static AdminCourseResponse from(Course course, String coverImageUrl) {
        return new AdminCourseResponse(course.getId(), course.getSlug(), course.getTitle(),
                course.getDescription(), course.getSortOrder(), course.getStatus(), course.getTuitionVnd(),
                course.getCategoryId(), course.getLevel(), course.getTotalHours(), coverImageUrl,
                course.getAccessType());
    }
}
