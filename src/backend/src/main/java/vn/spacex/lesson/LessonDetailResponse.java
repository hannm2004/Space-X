package vn.spacex.lesson;

// Trang đọc bài: đủ nội dung Markdown và đường dẫn ảnh bìa (null nếu chưa có), không có trạng thái, mã khóa hay người tạo.
public record LessonDetailResponse(Long id, String slug, String title, String summary, String contentMd,
                                   String coverImageUrl) {

    public LessonDetailResponse(Long id, String slug, String title, String summary, String contentMd) {
        this(id, slug, title, summary, contentMd, null);
    }

    public static LessonDetailResponse from(LessonContent lesson, String coverImageUrl) {
        return new LessonDetailResponse(lesson.getId(), lesson.getSlug(), lesson.getTitle(), lesson.getSummary(),
                lesson.getContentMd(), coverImageUrl);
    }
}
