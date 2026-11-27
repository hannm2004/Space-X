package vn.spacex.access;

// Quy tắc quyền đọc của D40 mục 2, viết một lần bằng SQL và được ghép vào mọi truy vấn trả nội dung cho người đọc
// (đọc bài, ảnh bài, tìm bài, tiến độ; sau này cả searchKnowledge). Lọc ngay trong truy vấn nên dữ liệu không được
// phép xem không bao giờ rời database. Không viết lại quy tắc này ở nơi khác.
//
// Truy vấn dùng các đoạn dưới đây phải là native query, đặt bí danh "c" cho courses và "l" cho lessons, và truyền
// tham số :userId (kèm :admin nếu đoạn đó có dùng).
public final class AccessRules {

    // Người dùng (không phải Admin) được học khóa c: khóa miễn phí đang đăng, hoặc có ghi danh còn hiệu lực ở khóa
    // đang đăng hay đã lưu trữ. Khóa nháp không bao giờ khớp.
    private static final String COURSE_OPEN_TO_USER = """
            ((c.status = 'PUBLISHED' and c.access_type = 'FREE')
              or (c.status in ('PUBLISHED', 'ARCHIVED') and exists (
                    select 1 from enrollments e
                    where e.course_id = c.id and e.user_id = :userId and e.status = 'ACTIVE')))""";

    public static final String COURSE_ACCESSIBLE = " (:admin = true or " + COURSE_OPEN_TO_USER + ") ";

    public static final String LESSON_READABLE = " (:admin = true or (l.status = 'PUBLISHED' and exists ("
            + "select 1 from courses c where c.id = l.course_id and " + COURSE_OPEN_TO_USER + "))) ";

    private AccessRules() {
    }
}
