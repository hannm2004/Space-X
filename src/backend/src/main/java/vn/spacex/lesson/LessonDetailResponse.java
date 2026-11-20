package vn.spacex.lesson;

// Trang đọc bài: đủ nội dung Markdown, đường dẫn ảnh bìa (null nếu chưa có), khóa chứa bài và bài trước/sau trong khóa
// (null ở bài đầu/cuối khóa). Không có trạng thái, mã khóa hay người tạo.
public record LessonDetailResponse(Long id, String slug, String title, String summary, String contentMd,
                                   String coverImageUrl, ContentRef course, ContentRef previous, ContentRef next) {

    public LessonDetailResponse(Long id, String slug, String title, String summary, String contentMd) {
        this(id, slug, title, summary, contentMd, null, null, null, null);
    }

    public static LessonDetailResponse from(LessonContent lesson, String coverImageUrl, ContentRef course,
                                            ContentRef previous, ContentRef next) {
        return new LessonDetailResponse(lesson.getId(), lesson.getSlug(), lesson.getTitle(), lesson.getSummary(),
                lesson.getContentMd(), coverImageUrl, course, previous, next);
    }
}
