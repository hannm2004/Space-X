package vn.spacex.db;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// V6 sửa bảng courses đã có dữ liệu thật trên production, nên test dựng database dừng ở V5, nạp khóa kiểu cũ,
// rồi mới chạy các migration còn lại: đúng con đường mà bản chạy thật sẽ đi.
@Testcontainers
class CourseCatalogMigrationIntegrationTest {

    @Container
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    private static final String SLUG_QUA_DAI = "khoa-hoc-co-slug-dai-hon-hai-muoi-ky-tu";

    private Connection connection;

    @BeforeAll
    static void nangCapTuV5CoSanDuLieu() throws SQLException {
        Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .target("5")
                .load()
                .migrate();
        try (Connection c = DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO courses (slug, title, description, sort_order) VALUES (?, ?, ?, ?)")) {
            List<String> slugs = new ArrayList<>();
            for (int lop = 6; lop <= 12; lop++) {
                slugs.add("tin-hoc-" + lop);
            }
            // Hai trường hợp chỉ có thể gặp ở database trên máy: slug quá dài, và hai slug ra cùng một mã.
            slugs.add(SLUG_QUA_DAI);
            slugs.add("lap-trinh");
            slugs.add("laptrinh");
            int thuTu = 0;
            for (String slug : slugs) {
                ps.setString(1, slug);
                ps.setString(2, "Khóa " + slug);
                ps.setString(3, "Mô tả " + slug);
                ps.setInt(4, ++thuTu);
                ps.executeUpdate();
            }
        }
        Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .load()
                .migrate();
    }

    // Mỗi test chạy trong một transaction rồi rollback, nên dữ liệu thử không rò sang test khác.
    @BeforeEach
    void moKetNoi() throws SQLException {
        connection = DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
        connection.setAutoCommit(false);
    }

    @AfterEach
    void dongKetNoi() throws SQLException {
        connection.rollback();
        connection.close();
    }

    @Test
    void v6TaoCacBangCuaDanhMucKhoa() throws SQLException {
        assertThat(cot("SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()"))
                .contains("categories", "course_modules", "course_faqs", "site_images", "banners");
    }

    @Test
    void coSanNhomKhoaChoThieuNien() throws SQLException {
        assertThat(cot("SELECT CONCAT(slug, '|', name) FROM categories"))
                .containsExactly("khoa-cho-thieu-nien|Khóa cho thiếu niên");
    }

    @Test
    void bayKhoaCuDuocDienNhomMaVaGiaTriMacDinhVaVanHienThi() throws SQLException {
        List<String> dong = cot("""
                SELECT CONCAT_WS('|', c.slug, c.code, g.slug, c.level, c.course_type, c.total_hours, c.sessions,
                                 c.tuition_vnd, c.status)
                FROM courses c JOIN categories g ON g.id = c.category_id
                WHERE c.slug LIKE 'tin-hoc-%' ORDER BY c.sort_order""");

        assertThat(dong).containsExactly(
                dongKhoaCu(6), dongKhoaCu(7), dongKhoaCu(8), dongKhoaCu(9),
                dongKhoaCu(10), dongKhoaCu(11), dongKhoaCu(12));
    }

    @Test
    void khoaCuGiuNguyenTenVaMoTaCotMoiChuaCoNoiDungThiRong() throws SQLException {
        assertThat(cot("""
                SELECT CONCAT_WS('|', title, description, sort_order,
                                 COALESCE(overview_md, audience_md, outcomes_md, assessment_md, 'trong'))
                FROM courses WHERE slug = 'tin-hoc-6'"""))
                .containsExactly("Khóa tin-hoc-6|Mô tả tin-hoc-6|1|trong");
    }

    @Test
    void slugQuaDaiHoacTrungMaThiDungMaDuPhongTheoId() throws SQLException {
        List<String> dong = cot("SELECT CONCAT(slug, '|', code, '|', id) FROM courses "
                + "WHERE slug IN ('" + SLUG_QUA_DAI + "', 'lap-trinh', 'laptrinh')");

        assertThat(dong).hasSize(3).allSatisfy(d -> {
            String[] phan = d.split("\\|");
            assertThat(phan[1]).isEqualTo("KH-" + phan[2]);
        });
    }

    @Test
    void themKhoaPhaiCoNhom() {
        assertThatThrownBy(() -> execute("INSERT INTO courses (code, slug, title) VALUES ('A1', 'a1', 'A')"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("category_id");
    }

    @Test
    void themKhoaPhaiCoMa() throws SQLException {
        long nhom = nhomMacDinh();

        assertThatThrownBy(() -> execute("INSERT INTO courses (category_id, slug, title) VALUES (?, 'a1', 'A')", nhom))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("code");
    }

    @Test
    void maKhoaKhongDuocTrungKeCaKhacHoaThuong() throws SQLException {
        assertThatThrownBy(() -> themKhoa("tinhoc10", "khoa-khac"))
                .isInstanceOf(SQLIntegrityConstraintViolationException.class);
    }

    @Test
    void khoaMoiMacDinhLaBanNhapCoBanNganHanChuaCoHocPhi() throws SQLException {
        long id = themKhoa("MOI", "khoa-moi");

        assertThat(cot("SELECT CONCAT_WS('|', level, course_type, total_hours, sessions, tuition_vnd, status) "
                + "FROM courses WHERE id = " + id))
                .containsExactly("BASIC|SHORT|0|0|0|DRAFT");
    }

    @Test
    void hocPhiKhongDuocAm() throws SQLException {
        long id = themKhoa("MOI", "khoa-moi");

        assertThatThrownBy(() -> execute("UPDATE courses SET tuition_vnd = -1 WHERE id = ?", id))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("chk_courses_tuition");
    }

    @Test
    void moTaDayDuLuuDuocDaiHon500KyTu() throws SQLException {
        long id = themKhoa("MOI", "khoa-moi");
        execute("UPDATE courses SET overview_md = ? WHERE id = ?", "ă".repeat(5000), id);

        assertThat(cot("SELECT CHAR_LENGTH(overview_md) FROM courses WHERE id = " + id)).containsExactly("5000");
    }

    @Test
    void khongXoaDuocNhomConKhoa() throws SQLException {
        long nhom = nhomMacDinh();

        assertThatThrownBy(() -> execute("DELETE FROM categories WHERE id = ?", nhom))
                .isInstanceOf(SQLIntegrityConstraintViolationException.class);
    }

    @Test
    void xoaKhoaThiXoaLuonCacPhanVaHoiDap() throws SQLException {
        long id = themKhoa("MOI", "khoa-moi");
        execute("INSERT INTO course_modules (course_id, sort_order, title, content_md) VALUES (?, 1, 'Phần 1', 'x')", id);
        execute("INSERT INTO course_faqs (course_id, sort_order, question, answer) VALUES (?, 1, 'Hỏi?', 'Đáp')", id);

        execute("DELETE FROM courses WHERE id = ?", id);

        assertThat(cot("SELECT COUNT(*) FROM course_modules WHERE course_id = " + id)).containsExactly("0");
        assertThat(cot("SELECT COUNT(*) FROM course_faqs WHERE course_id = " + id)).containsExactly("0");
    }

    @Test
    void khongXoaDuocAnhDangDungLamBanner() throws SQLException {
        long anh = themAnh("BANNER");
        execute("INSERT INTO banners (image_id, title, link_path) VALUES (?, 'Khai giảng', '/khoa-hoc/tin-hoc-6')", anh);

        assertThatThrownBy(() -> execute("DELETE FROM site_images WHERE id = ?", anh))
                .isInstanceOf(SQLIntegrityConstraintViolationException.class);
    }

    @Test
    void anhChiThuocMotTrongBaLoai() {
        assertThatThrownBy(() -> themAnh("KHAC"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("kind");
    }

    @Test
    void coChiMucFulltextDeTimKhoaTheoTenVaMoTa() throws SQLException {
        assertThat(cot("""
                SELECT column_name FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = 'courses' AND index_type = 'FULLTEXT'
                ORDER BY seq_in_index"""))
                .containsExactly("title", "description");
    }

    private static String dongKhoaCu(int lop) {
        return "tin-hoc-%d|TINHOC%d|khoa-cho-thieu-nien|BASIC|SHORT|0|0|0|PUBLISHED".formatted(lop, lop);
    }

    private long nhomMacDinh() throws SQLException {
        return Long.parseLong(cot("SELECT id FROM categories WHERE slug = 'khoa-cho-thieu-nien'").getFirst());
    }

    private long themKhoa(String code, String slug) throws SQLException {
        return execute("INSERT INTO courses (category_id, code, slug, title) VALUES (?, ?, ?, 'Khóa')",
                nhomMacDinh(), code, slug);
    }

    private long themAnh(String loai) throws SQLException {
        return execute("INSERT INTO site_images (kind, file_name, content_type, size_bytes, data) "
                + "VALUES (?, 'a.png', 'image/png', 3, X'010203')", loai);
    }

    // Cột đầu tiên của mọi dòng, dạng chuỗi.
    private List<String> cot(String sql) throws SQLException {
        List<String> ketQua = new ArrayList<>();
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                ketQua.add(rs.getString(1));
            }
        }
        return ketQua;
    }

    // Trả về id vừa sinh (với UPDATE, DELETE thì không có id nên trả 0).
    private long execute(String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0;
            }
        }
    }
}
