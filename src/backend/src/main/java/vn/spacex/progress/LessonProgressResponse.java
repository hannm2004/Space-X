package vn.spacex.progress;

// Trạng thái thật sau khi ghi: gửi IN_PROGRESS cho bài đã COMPLETED thì vẫn nhận lại COMPLETED.
public record LessonProgressResponse(Long lessonId, ProgressStatus status) {
}
