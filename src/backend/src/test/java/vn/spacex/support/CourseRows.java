package vn.spacex.support;

import org.springframework.jdbc.core.JdbcTemplate;

// Chèn khóa miễn phí đã đăng thẳng vào database cho test tích hợp (cần loại khác thì UPDATE sau). Từ V6 mọi khóa phải có nhóm và mã: ở đây dùng nhóm có sẵn do
// migration tạo, còn mã lấy từ slug (slug trong test đều ngắn và không trùng mã).
public final class CourseRows {

    private CourseRows() {
    }

    public static long insert(JdbcTemplate jdbc, String slug, String title, String description, int sortOrder) {
        jdbc.update("""
                INSERT INTO courses (category_id, code, slug, title, description, sort_order, status, access_type)
                VALUES ((SELECT id FROM categories WHERE slug = 'khoa-cho-thieu-nien'),
                        UPPER(REPLACE(?, '-', '')), ?, ?, ?, ?, 'PUBLISHED', 'FREE')""",
                slug, slug, title, description, sortOrder);
        return jdbc.queryForObject("SELECT id FROM courses WHERE slug = ?", Long.class, slug);
    }
}
