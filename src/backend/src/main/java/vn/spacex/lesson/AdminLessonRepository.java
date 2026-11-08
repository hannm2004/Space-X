package vn.spacex.lesson;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminLessonRepository extends JpaRepository<AdminLesson, Long> {

    boolean existsBySlug(String slug);

    // Dùng khi sửa: slug hiện tại của chính bài đó không bị tính là trùng.
    boolean existsBySlugAndIdNot(String slug, Long id);
}
