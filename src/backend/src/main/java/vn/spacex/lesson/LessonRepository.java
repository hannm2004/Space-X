package vn.spacex.lesson;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByCourseIdAndStatusOrderBySortOrderAscIdAsc(Long courseId, LessonStatus status);

    // Tiến độ học (CP5.5): kiểm tra bài đã đăng và tính tổng bài mỗi khóa. Entity Lesson không có content_md nên nhẹ.
    Optional<Lesson> findByIdAndStatus(Long id, LessonStatus status);

    List<Lesson> findByStatusOrderBySortOrderAscIdAsc(LessonStatus status);

    // Tìm bài Đã đăng bằng FULLTEXT (D29, migration V3). Chế độ tự nhiên: toán tử như + - * " trong từ khóa không có nghĩa.
    // Chỉ chọn các cột entity Lesson ánh xạ nên không tải content_md. Thứ tự (độ liên quan giảm dần, rồi id) nằm trong
    // câu truy vấn, vì vậy Pageable truyền vào không được có Sort.
    @Query(value = """
            select l.id, l.course_id, l.slug, l.title, l.summary, l.status, l.sort_order
            from lessons l
            where l.status = 'PUBLISHED'
              and match(l.title, l.summary, l.content_md) against (:q in natural language mode)
            order by match(l.title, l.summary, l.content_md) against (:q in natural language mode) desc, l.id""",
            countQuery = """
            select count(*)
            from lessons l
            where l.status = 'PUBLISHED'
              and match(l.title, l.summary, l.content_md) against (:q in natural language mode)""",
            nativeQuery = true)
    Page<Lesson> searchPublished(@Param("q") String q, Pageable pageable);

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
