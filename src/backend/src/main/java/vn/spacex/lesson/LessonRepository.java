package vn.spacex.lesson;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.spacex.access.AccessRules;

import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByCourseIdAndStatusOrderBySortOrderAscIdAsc(Long courseId, LessonStatus status);

    // 1 nếu người gọi được đọc bài này (AccessRules), 0 nếu không được hoặc bài không tồn tại.
    @Query(value = "select count(*) from lessons l where l.id = :id and" + AccessRules.LESSON_READABLE,
            nativeQuery = true)
    long countReadable(@Param("id") Long id, @Param("userId") long userId, @Param("admin") boolean admin);

    // Tiến độ học (CP5.5): các bài đã đăng mà người gọi được đọc. Entity Lesson không có content_md nên nhẹ.
    @Query(value = """
            select l.id, l.course_id, l.slug, l.title, l.summary, l.status, l.sort_order
            from lessons l
            where l.status = 'PUBLISHED' and""" + AccessRules.LESSON_READABLE + """
            order by l.sort_order, l.id""",
            nativeQuery = true)
    List<Lesson> findReadablePublished(@Param("userId") long userId, @Param("admin") boolean admin);

    // Tìm trong các bài người gọi được đọc, bằng FULLTEXT (D29, migration V3). Chế độ tự nhiên: toán tử như + - * "
    // trong từ khóa không có nghĩa. Chỉ chọn các cột entity Lesson ánh xạ nên không tải content_md. Thứ tự (độ liên
    // quan giảm dần, rồi id) nằm trong câu truy vấn, vì vậy Pageable truyền vào không được có Sort. Câu đếm dùng cùng
    // điều kiện để totalItems không lộ số bài người gọi không được xem.
    @Query(value = """
            select l.id, l.course_id, l.slug, l.title, l.summary, l.status, l.sort_order
            from lessons l
            where match(l.title, l.summary, l.content_md) against (:q in natural language mode)
              and""" + AccessRules.LESSON_READABLE + """
            order by match(l.title, l.summary, l.content_md) against (:q in natural language mode) desc, l.id""",
            countQuery = """
            select count(*)
            from lessons l
            where match(l.title, l.summary, l.content_md) against (:q in natural language mode)
              and""" + AccessRules.LESSON_READABLE,
            nativeQuery = true)
    Page<Lesson> searchReadable(@Param("q") String q, @Param("userId") long userId, @Param("admin") boolean admin,
                                Pageable pageable);

    // Dashboard của Admin: đếm toàn bộ bài, kể cả bài nháp.
    long countByStatus(LessonStatus status);

    // Một truy vấn cho mọi khóa (thay vì mỗi khóa một truy vấn). Khóa chưa có bài không có dòng nào ở đây.
    @Query("""
            select l.courseId as courseId, l.status as status, count(l) as total
            from Lesson l
            group by l.courseId, l.status""")
    List<LessonCountByCourse> countLessonsByCourseAndStatus();

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
