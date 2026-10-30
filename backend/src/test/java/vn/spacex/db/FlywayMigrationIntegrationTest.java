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

@Testcontainers
class FlywayMigrationIntegrationTest {

    @Container
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    private Connection connection;

    @BeforeAll
    static void chayMigration() {
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
    void flywayTaoDuSauBangCuaUngDung() throws SQLException {
        List<String> bang = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT table_name FROM information_schema.tables "
                             + "WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'")) {
            while (rs.next()) {
                bang.add(rs.getString(1));
            }
        }

        assertThat(bang).contains(
                "users", "courses", "lessons", "lesson_images", "lesson_progress", "knowledge_chunks");
    }

    @Test
    void emailKhongDuocTrungKeCaKhacHoaThuong() throws SQLException {
        insertUser("hocsinh@example.com");

        assertThatThrownBy(() -> insertUser("HocSinh@Example.com"))
                .isInstanceOf(SQLIntegrityConstraintViolationException.class);
    }

    @Test
    void khongXoaDuocKhoaHocConBai() throws SQLException {
        long userId = insertUser("admin@example.com");
        long courseId = insertCourse("tin-hoc-10");
        insertLesson(courseId, userId, "bai-1");

        assertThatThrownBy(() -> execute("DELETE FROM courses WHERE id = ?", courseId))
                .isInstanceOf(SQLIntegrityConstraintViolationException.class);
    }

    @Test
    void xoaBaiHocThiXoaLuonAnhTienDoVaDoanTriThuc() throws SQLException {
        long userId = insertUser("admin@example.com");
        long courseId = insertCourse("tin-hoc-10");
        long lessonId = insertLesson(courseId, userId, "bai-1");
        execute("INSERT INTO lesson_images (lesson_id, file_name, content_type, size_bytes, data) "
                + "VALUES (?, 'a.png', 'image/png', 3, X'010203')", lessonId);
        execute("INSERT INTO lesson_progress (user_id, lesson_id) VALUES (?, ?)", userId, lessonId);
        execute("INSERT INTO knowledge_chunks (lesson_id, chunk_index, heading, content) "
                + "VALUES (?, 0, 'Mo dau', 'noi dung')", lessonId);

        execute("DELETE FROM lessons WHERE id = ?", lessonId);

        assertThat(dem("lesson_images", lessonId)).isZero();
        assertThat(dem("lesson_progress", lessonId)).isZero();
        assertThat(dem("knowledge_chunks", lessonId)).isZero();
    }

    @Test
    void moiHocSinhChiCoMotDongTienDoChoMoiBai() throws SQLException {
        long userId = insertUser("hocsinh@example.com");
        long courseId = insertCourse("tin-hoc-10");
        long lessonId = insertLesson(courseId, userId, "bai-1");
        execute("INSERT INTO lesson_progress (user_id, lesson_id) VALUES (?, ?)", userId, lessonId);

        assertThatThrownBy(() ->
                execute("INSERT INTO lesson_progress (user_id, lesson_id) VALUES (?, ?)", userId, lessonId))
                .isInstanceOf(SQLIntegrityConstraintViolationException.class);
    }

    private long insertUser(String email) throws SQLException {
        return execute("INSERT INTO users (email, display_name) VALUES (?, 'Hoc sinh')", email);
    }

    private long insertCourse(String slug) throws SQLException {
        return execute("INSERT INTO courses (slug, title) VALUES (?, 'Tin hoc')", slug);
    }

    private long insertLesson(long courseId, long userId, String slug) throws SQLException {
        return execute("INSERT INTO lessons (course_id, created_by, slug, title, content_md) "
                + "VALUES (?, ?, ?, 'Bai hoc', '')", courseId, userId, slug);
    }

    // Trả về id vừa sinh (với DELETE thì không có id nên trả 0).
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

    private long dem(String bang, long lessonId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM " + bang + " WHERE lesson_id = ?")) {
            ps.setLong(1, lessonId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }
}
