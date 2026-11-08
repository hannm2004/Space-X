package vn.spacex.lesson;

// Một dòng trong danh sách Admin: có trạng thái nhưng không có nội dung bài.
public record AdminLessonSummaryResponse(Long id, Long courseId, String slug, String title, String summary,
                                         LessonStatus status, int sortOrder) {

    static AdminLessonSummaryResponse from(Lesson lesson) {
        return new AdminLessonSummaryResponse(lesson.getId(), lesson.getCourseId(), lesson.getSlug(),
                lesson.getTitle(), lesson.getSummary(), lesson.getStatus(), lesson.getSortOrder());
    }
}
