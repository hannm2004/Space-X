package vn.spacex.lesson;

// Danh sách công khai: chỉ định danh, tiêu đề và tóm tắt. Không có nội dung bài và không có trạng thái.
public record LessonSummaryResponse(Long id, String slug, String title, String summary) {

    public static LessonSummaryResponse from(Lesson lesson) {
        return new LessonSummaryResponse(lesson.getId(), lesson.getSlug(), lesson.getTitle(), lesson.getSummary());
    }
}
