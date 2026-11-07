package vn.spacex.lesson;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LessonContentRepository extends JpaRepository<LessonContent, Long> {

    Optional<LessonContent> findBySlugAndStatus(String slug, LessonStatus status);
}
