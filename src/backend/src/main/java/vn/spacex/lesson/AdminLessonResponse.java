package vn.spacex.lesson;

// Chi tiết một bài để Admin sửa (kể cả nháp). Không có người tạo.
public record AdminLessonResponse(Long id, Long courseId, String slug, String title, String summary,
                                  String contentMd, LessonStatus status, int sortOrder) {

    static AdminLessonResponse from(AdminLesson lesson) {
        return new AdminLessonResponse(lesson.getId(), lesson.getCourseId(), lesson.getSlug(), lesson.getTitle(),
                lesson.getSummary(), lesson.getContentMd(), lesson.getStatus(), lesson.getSortOrder());
    }
}
