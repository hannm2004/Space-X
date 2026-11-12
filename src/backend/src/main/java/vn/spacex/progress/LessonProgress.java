package vn.spacex.progress;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

// Một dòng cho mỗi cặp (học sinh, bài): UNIQUE(user_id, lesson_id) ở migration V1.
// Giữ id thay vì quan hệ @ManyToOne vì tiến độ chỉ cần biết "của ai" và "bài nào", không cần nạp User hay Lesson.
@Entity
@Table(name = "lesson_progress")
public class LessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProgressStatus status;

    @Column(name = "last_viewed_at", nullable = false)
    private LocalDateTime lastViewedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    protected LessonProgress() {
    }

    public static LessonProgress start(Long userId, Long lessonId, LocalDateTime now) {
        LessonProgress progress = new LessonProgress();
        progress.userId = userId;
        progress.lessonId = lessonId;
        progress.status = ProgressStatus.IN_PROGRESS;
        progress.lastViewedAt = now;
        return progress;
    }

    // Mở lại bài: chỉ đổi thời điểm xem. Bài đã COMPLETED không bị hạ về IN_PROGRESS.
    public void view(LocalDateTime now) {
        this.lastViewedAt = now;
    }

    // Gọi nhiều lần cho cùng một kết quả: thời điểm hoàn thành giữ nguyên từ lần đầu.
    public void complete(LocalDateTime now) {
        this.lastViewedAt = now;
        this.status = ProgressStatus.COMPLETED;
        if (this.completedAt == null) {
            this.completedAt = now;
        }
    }

    public Long getUserId() {
        return userId;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public ProgressStatus getStatus() {
        return status;
    }

    public LocalDateTime getLastViewedAt() {
        return lastViewedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
