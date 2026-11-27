package vn.spacex.course;

import org.springframework.data.domain.Sort;

// Bộ lọc của danh sách khóa công khai. Trường null hoặc khoảng ANY nghĩa là không lọc theo trường đó.
public record CourseFilter(String categorySlug, CourseLevel level, NumberRange hours, NumberRange tuition, Sort sort) {

    private static final Sort DISPLAY_ORDER = Sort.by("sortOrder", "id");

    static CourseFilter of(String category, CourseLevel level, String duration, String price, String sort) {
        String slug = category == null || category.isBlank() ? null : category.trim();
        return new CourseFilter(slug, level, NumberRange.parse(duration), NumberRange.parse(price), sortOf(sort));
    }

    // Chỉ nhận các giá trị liệt kê ở đây: tên cột không bao giờ lấy thẳng từ trình duyệt.
    private static Sort sortOf(String sort) {
        if (sort == null || sort.isBlank()) {
            return DISPLAY_ORDER;
        }
        return switch (sort) {
            case "price_asc" -> Sort.by(Sort.Order.asc("tuitionVnd")).and(DISPLAY_ORDER);
            case "price_desc" -> Sort.by(Sort.Order.desc("tuitionVnd")).and(DISPLAY_ORDER);
            default -> throw new InvalidCourseFilterException();
        };
    }
}
