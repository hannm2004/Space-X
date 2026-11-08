package vn.spacex.lesson;

// Trang đọc bài: đủ nội dung Markdown nhưng không có trạng thái, mã khóa hay người tạo.
public record LessonDetailResponse(Long id, String slug, String title, String summary, String contentMd) {

    public static LessonDetailResponse from(LessonContent lesson) {
        return new LessonDetailResponse(
                lesson.getId(), lesson.getSlug(), lesson.getTitle(), lesson.getSummary(), lesson.getContentMd());
    }
}
