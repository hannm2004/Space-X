package vn.spacex.lesson;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.spacex.access.AccessRules;

import java.util.Optional;

public interface LessonContentRepository extends JpaRepository<LessonContent, Long> {

    // Rỗng khi không có bài này hoặc người gọi không được đọc: nội dung không được phép xem không rời database.
    @Query(value = """
            select l.id, l.course_id, l.slug, l.title, l.summary, l.content_md, l.status
            from lessons l
            where l.slug = :slug and""" + AccessRules.LESSON_READABLE,
            nativeQuery = true)
    Optional<LessonContent> findReadableBySlug(@Param("slug") String slug, @Param("userId") long userId,
                                               @Param("admin") boolean admin);
}
