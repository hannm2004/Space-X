package vn.spacex.lesson;

// Kết quả nhóm theo (khóa, trạng thái) của Dashboard; Spring Data tự dựng đối tượng từ các alias trong câu JPQL.
public interface LessonCountByCourse {

    Long getCourseId();

    LessonStatus getStatus();

    long getTotal();
}
