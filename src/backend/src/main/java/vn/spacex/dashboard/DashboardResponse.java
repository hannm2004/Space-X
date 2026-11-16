package vn.spacex.dashboard;

import java.util.List;

// Chỉ có số tổng hợp, không có email hay tên của ai (docs/API.md, GET /api/admin/dashboard).
public record DashboardResponse(UserStats users, LessonStats lessons, List<CourseStats> courses,
                                LearningStats learning) {

    public record UserStats(long total, long newLast7Days) {
    }

    public record LessonStats(long total, long draft, long published) {
    }

    // Mỗi khóa một dòng, kể cả khóa chưa có bài (draft = published = 0).
    public record CourseStats(Long courseId, String slug, String title, long draft, long published) {
    }

    // activeLearners: học sinh có ít nhất một dòng tiến độ (đã mở hoặc đã hoàn thành một bài).
    public record LearningStats(long activeLearners, long completedLessons) {
    }
}
