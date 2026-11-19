package vn.spacex.lesson;

// Danh sách công khai: định danh, tiêu đề, tóm tắt và đường dẫn ảnh bìa (null nếu bài chưa có). Không có nội dung bài
// và không có trạng thái.
public record LessonSummaryResponse(Long id, String slug, String title, String summary, String coverImageUrl) {

    public LessonSummaryResponse(Long id, String slug, String title, String summary) {
        this(id, slug, title, summary, null);
    }

    public static LessonSummaryResponse from(Lesson lesson, String coverImageUrl) {
        return new LessonSummaryResponse(lesson.getId(), lesson.getSlug(), lesson.getTitle(), lesson.getSummary(),
                coverImageUrl);
    }
}
