package vn.spacex.progress;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.spacex.user.Role;

import java.util.List;
import java.util.Optional;

// Mọi truy vấn đều có userId: không có cách nào đọc hay xóa tiến độ mà không chỉ rõ là của ai.
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    Optional<LessonProgress> findByUserIdAndLessonId(Long userId, Long lessonId);

    List<LessonProgress> findByUserId(Long userId);

    // Dashboard của Admin: chỉ đếm số tổng hợp, không đọc dòng tiến độ của ai. Lọc theo role để việc quản trị viên
    // đọc thử bài không làm sai số liệu học sinh.
    @Query("""
            select count(distinct p.userId) from LessonProgress p, User u
            where u.id = p.userId and u.role = :role""")
    long countDistinctUsersByRole(@Param("role") Role role);

    @Query("""
            select count(p) from LessonProgress p, User u
            where u.id = p.userId and u.role = :role and p.status = :status""")
    long countByStatusAndUserRole(@Param("status") ProgressStatus status, @Param("role") Role role);

    // Học lại khóa: chỉ xóa dòng của người này thuộc các bài trong khóa (kể cả bài nháp, để học lại là sạch hẳn).
    @Modifying
    @Query("""
            delete from LessonProgress p
            where p.userId = :userId
              and p.lessonId in (select l.id from Lesson l where l.courseId = :courseId)""")
    int deleteByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);
}
