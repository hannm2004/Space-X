package vn.spacex.lesson;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByCourseIdAndStatusOrderBySortOrderAscIdAsc(Long courseId, LessonStatus status);

    // Tính cả bài DRAFT: khóa ngoại RESTRICT cũng chặn xóa khóa còn bài nháp.
    boolean existsByCourseId(Long courseId);

    // Danh sách cho Admin: null nghĩa là không lọc theo điều kiện đó. Trả cả bài nháp, chỉ gọi từ API Admin.
    @Query("""
            select l from Lesson l
            where (:courseId is null or l.courseId = :courseId)
              and (:status is null or l.status = :status)""")
    Page<Lesson> findForAdmin(@Param("courseId") Long courseId, @Param("status") LessonStatus status,
                              Pageable pageable);
}
