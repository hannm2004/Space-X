package vn.spacex.lesson;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByCourseIdAndStatusOrderBySortOrderAscIdAsc(Long courseId, LessonStatus status);

    // Tính cả bài DRAFT: khóa ngoại RESTRICT cũng chặn xóa khóa còn bài nháp.
    boolean existsByCourseId(Long courseId);
}
