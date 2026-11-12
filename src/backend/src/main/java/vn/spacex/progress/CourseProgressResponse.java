package vn.spacex.progress;

// percent làm tròn xuống (33 bài trên 100 không thành 34) nên chỉ bằng 100 khi hoàn thành hết.
// continueLesson là null khi khóa chưa có bài Đã đăng hoặc đã học xong hết.
public record CourseProgressResponse(Long courseId, String courseSlug, String courseTitle,
                                     int completedLessons, int totalLessons, int percent,
                                     ContinueLessonResponse continueLesson) {
}
