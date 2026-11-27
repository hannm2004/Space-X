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

// V7 đổi quyền đọc của dữ liệu đang có trên production: dừng ở V6, nạp khóa, bài và tiến độ kiểu cũ, rồi mới chạy
// các migration còn lại, đúng con đường mà bản chạy thật sẽ đi.
@Testcontainers
class AccessRulesMigrationIntegrationTest {

    @Container
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    private Connection connection;

    @BeforeAll
    static void nangCapTuV6CoSanDuLieu() throws SQLException {
        Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .target("6")
                .load()
                .migrate();
        try (Connection c = DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
             Statement st = c.createStatement()) {
            st.executeUpdate("""
                    INSERT INTO users (id, email, password_hash, display_name, role) VALUES
                    (1, 'tacgia@example.com', 'x', 'Tác giả', 'ADMIN'),
                    (2, 'a@example.com', 'x', 'A', 'LEARNER'),
                    (3, 'b@example.com', 'x', 'B', 'LEARNER'),
                    (4, 'chua-hoc@example.com', 'x', 'C', 'LEARNER')""");
            st.executeUpdate("""
                    INSERT INTO courses (id, category_id, code, slug, title, status)
                    SELECT n.id, g.id, n.code, n.slug, n.slug, 'PUBLISHED'
                    FROM categories g
                    JOIN (SELECT 1 AS id, 'K1' AS code, 'co-bai-1' AS slug
                          UNION ALL SELECT 2, 'K2', 'co-bai-2'
                          UNION ALL SELECT 3, 'K3', 'chua-co-bai') n""");
            st.executeUpdate("""
                    INSERT INTO lessons (id, course_id, created_by, slug, title, content_md, status) VALUES
                    (1, 1, 1, 'bai-1', 'Bài 1', 'x', 'PUBLISHED'),
                    (2, 1, 1, 'bai-2', 'Bài 2', 'x', 'PUBLISHED'),
                    (3, 2, 1, 'bai-nhap', 'Bài nháp', 'x', 'DRAFT')""");
            // A học hai bài của khóa 1 và một bài của khóa 2; B học một bài của khóa 1; C chưa học gì.
            st.executeUpdate("INSERT INTO lesson_progress (user_id, lesson_id) VALUES (2, 1), (2, 2), (2, 3), (3, 1)");
        }
        Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .load()
                .migrate();
    }

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
    void khoaDangCoBaiChuyenSangMienPhiConKhoaChuaCoBaiLaTraPhi() throws SQLException {
        assertThat(cot("SELECT CONCAT(slug, '|', access_type) FROM courses ORDER BY id"))
                .containsExactly("co-bai-1|FREE", "co-bai-2|FREE", "chua-co-bai|PAID");
    }

    // Quên đặt loại thì khóa mới bị khóa lại, không lỡ tay mở miễn phí.
    @Test
    void khoaMoiKhongNoiLoaiThiMacDinhLaTraPhi() throws SQLException {
        execute("""
                INSERT INTO courses (category_id, code, slug, title)
                SELECT id, 'MOI', 'khoa-moi', 'Khóa mới' FROM categories LIMIT 1""");

        assertThat(cot("SELECT access_type FROM courses WHERE slug = 'khoa-moi'")).containsExactly("PAID");
    }

    @Test
    void loaiKhoaChiNhanMienPhiHoacTraPhi() {
        assertThatThrownBy(() -> execute("UPDATE courses SET access_type = 'GIFT' WHERE id = 1"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("access_type");
    }

    @Test
    void nguoiDaCoTienDoDuocBuQuyenMoiKhoaDungMotDongNguonMigration() throws SQLException {
        assertThat(cot("""
                SELECT CONCAT_WS('|', user_id, course_id, status, source, COALESCE(granted_by, 'khong'))
                FROM enrollments ORDER BY user_id, course_id"""))
                .containsExactly(
                        "2|1|ACTIVE|MIGRATION|khong",
                        "2|2|ACTIVE|MIGRATION|khong",
                        "3|1|ACTIVE|MIGRATION|khong");
    }

    @Test
    void moiCapNguoiDungKhoaChiCoMotDongGhiDanh() {
        assertThatThrownBy(() -> execute(
                "INSERT INTO enrollments (user_id, course_id, source) VALUES (2, 1, 'FREE')"))
                .isInstanceOf(SQLIntegrityConstraintViolationException.class);
    }

    @Test
    void capQuyenTayBatBuocGhiNguoiCap() throws SQLException {
        assertThatThrownBy(() -> execute(
                "INSERT INTO enrollments (user_id, course_id, source) VALUES (4, 1, 'ADMIN')"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("chk_enrollments_granted_by");

        execute("INSERT INTO enrollments (user_id, course_id, source, granted_by) VALUES (4, 1, 'ADMIN', 1)");

        assertThat(cot("SELECT granted_by FROM enrollments WHERE user_id = 4")).containsExactly("1");
    }

    @Test
    void khongXoaDuocKhoaDaCoNguoiGhiDanh() throws SQLException {
        execute("INSERT INTO enrollments (user_id, course_id, source) VALUES (4, 3, 'FREE')");

        assertThatThrownBy(() -> execute("DELETE FROM courses WHERE id = 3"))
                .isInstanceOf(SQLIntegrityConstraintViolationException.class);
    }

    @Test
    void baiCuChuaThuocModuleVaXoaModuleThiBaiConNguyen() throws SQLException {
        assertThat(cot("SELECT COUNT(*) FROM lessons WHERE module_id IS NOT NULL")).containsExactly("0");
        execute("INSERT INTO course_modules (id, course_id, title, content_md) VALUES (50, 1, 'Phần 1', 'x')");
        execute("UPDATE lessons SET module_id = 50 WHERE id = 1");

        execute("DELETE FROM course_modules WHERE id = 50");

        assertThat(cot("SELECT CONCAT(id, '|', COALESCE(module_id, 'khong')) FROM lessons WHERE id = 1"))
                .containsExactly("1|khong");
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

    private void execute(String sql) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }
}
