package vn.spacex.course;

public record CourseResponse(Long id, String slug, String title, String description) {

    static CourseResponse from(Course course) {
        return new CourseResponse(course.getId(), course.getSlug(), course.getTitle(), course.getDescription());
    }
}
