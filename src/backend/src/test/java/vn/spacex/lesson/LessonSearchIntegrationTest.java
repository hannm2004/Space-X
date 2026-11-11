package vn.spacex.lesson;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Tìm kiếm FULLTEXT (D29) phải chạy trên MySQL 8.4 thật: H2 không có MATCH ... AGAINST. Luật phân quyền là của SecurityConfig thật.
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class LessonSearchIntegrationTest {

    @Container
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @DynamicPropertySource
    static void cauHinhDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private long tacGiaId;
    private long khoaId;

    @BeforeEach
    void donDuLieu() {
        jdbc.update("DELETE FROM lessons");
        jdbc.update("DELETE FROM courses");
        jdbc.update("DELETE FROM users");
        jdbc.update("""
                INSERT INTO users (email, password_hash, display_name, role, auth_provider, enabled)
                VALUES ('tacgia@example.com', 'khong-dung-de-dang-nhap', 'Tác giả', 'ADMIN', 'LOCAL', 1)""");
        tacGiaId = jdbc.queryForObject("SELECT id FROM users WHERE email = 'tacgia@example.com'", Long.class);
        jdbc.update("INSERT INTO courses (slug, title, description, sort_order) VALUES ('tin-hoc-10', 'Tin học 10', 'mo ta', 1)");
        khoaId = jdbc.queryForObject("SELECT id FROM courses WHERE slug = 'tin-hoc-10'", Long.class);
    }

    private void themBai(String slug, String tieuDe, String tomTat, String noiDung, String trangThai) {
        jdbc.update("""
                INSERT INTO lessons (course_id, created_by, slug, title, summary, content_md, status, sort_order)
                VALUES (?, ?, ?, ?, ?, ?, ?, 1)""",
                khoaId, tacGiaId, slug, tieuDe, tomTat, noiDung, trangThai);
    }

    private ResultActions tim(String tuKhoa) throws Exception {
        return mockMvc.perform(get("/api/lessons").param("q", tuKhoa).with(user("hocsinh")));
    }

    @Test
    void chuaDangNhapTimKiemBi401() throws Exception {
        themBai("bai-1", "Mạng máy tính", "Tóm tắt", "Nội dung về mạng", "PUBLISHED");

        mockMvc.perform(get("/api/lessons").param("q", "mạng")).andExpect(status().isUnauthorized());
    }

    @Test
    void chiMucFulltextCuaBaiHocCoDuBaCotTieuDeTomTatVaNoiDung() {
        Integer soCot = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = 'lessons'
                  AND index_name = 'ft_lessons_title_summary_content' AND index_type = 'FULLTEXT'
                  AND column_name IN ('title', 'summary', 'content_md')""", Integer.class);

        assertThat(soCot).isEqualTo(3);
    }

    @Test
    void timThayBaiTheoNoiDungBaiVietKhongCanTrungTieuDe() throws Exception {
        themBai("sap-xep", "Sắp xếp dữ liệu", "Các cách sắp xếp", "Thuật toán nổi bọt đổi chỗ hai phần tử liền kề", "PUBLISHED");
        themBai("mang", "Mạng máy tính", "Tóm tắt mạng", "Giao thức mạng", "PUBLISHED");

        tim("nổi bọt")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[*].slug", contains("sap-xep")));
    }

    @Test
    void timThayBaiTheoTieuDeVaTheoTomTat() throws Exception {
        themBai("a", "Thuật toán tìm kiếm", "Tóm tắt khác", "Nội dung khác", "PUBLISHED");
        themBai("b", "Bài khác", "Khái niệm entropy trong thông tin", "Nội dung khác", "PUBLISHED");

        tim("thuật toán").andExpect(jsonPath("$.items[*].slug", contains("a")));
        tim("entropy").andExpect(jsonPath("$.items[*].slug", contains("b")));
    }

    @Test
    void baiNhapKhopTuKhoaKhongXuatHienVaKhongDuocDemVaoTong() throws Exception {
        themBai("da-dang", "Mạng máy tính", "Tóm tắt", "Giao thức mạng", "PUBLISHED");
        themBai("ban-nhap", "Mạng ngang hàng", "Tóm tắt", "BAN-NHAP-VE-MANG", "DRAFT");

        tim("mạng")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[*].slug", contains("da-dang")));
    }

    @Test
    void baiDangDaDangThanhNhapThiBienMatKhoiKetQua() throws Exception {
        themBai("bai-1", "Mạng máy tính", "Tóm tắt", "Nội dung", "PUBLISHED");
        tim("mạng").andExpect(jsonPath("$.totalItems").value(1));

        jdbc.update("UPDATE lessons SET status = 'DRAFT' WHERE slug = 'bai-1'");

        tim("mạng").andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void khongKhopBaiNaoThiTraDanhSachRongVaVan200() throws Exception {
        themBai("bai-1", "Mạng máy tính", "Tóm tắt", "Nội dung", "PUBLISHED");

        tim("xyzkhongtontai")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    // Kết quả thật của collation utf8mb4_0900_ai_ci (D29): từ khóa không dấu khớp được nội dung có dấu.
    @Test
    void tuKhoaKhongDauKhopDuocNoiDungCoDau() throws Exception {
        themBai("thong-tin", "Thông tin và dữ liệu", "Tóm tắt", "Nội dung", "PUBLISHED");

        tim("thong tin").andExpect(jsonPath("$.items[*].slug", contains("thong-tin")));
    }

    // Hệ quả thật của collation không phân biệt dấu (D29): "nổi" và "nội" là một từ với MySQL, nên tìm "nổi" ra cả bài có "nội".
    // Chấp nhận ở mức đồ án; đổi lại người học gõ không dấu vẫn tìm được.
    @Test
    void tuKhacDauNhungCungMatChuKhongDauDuocCoiLaMotTu() throws Exception {
        themBai("noi-quy", "Nội quy phòng máy", "Tóm tắt", "Giữ vệ sinh", "PUBLISHED");

        tim("nổi").andExpect(jsonPath("$.items[*].slug", contains("noi-quy")));
    }

    // Chế độ tự nhiên là "ít nhất một từ": nhiều từ không có nghĩa là bắt buộc đủ mọi từ; bài chứa cả hai từ xếp trước.
    @Test
    void nhieuTuKhopBaiChuaItNhatMotTuVaBaiChuaCaHaiTuXepTruoc() throws Exception {
        themBai("mot-tu", "Mạng máy tính", "Tóm tắt", "Giao thức", "PUBLISHED");
        themBai("ca-hai", "Mạng và thuật toán", "Tóm tắt", "Định tuyến thuật toán", "PUBLISHED");
        themBai("khong", "Bài khác", "Tóm tắt", "Giao diện", "PUBLISHED");

        tim("mạng thuật toán")
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items[*].slug", contains("ca-hai", "mot-tu")));
    }

    @Test
    void baiNhacTuKhoaNhieuLanHonXepTruoc() throws Exception {
        themBai("it", "Bài về máy", "Tóm tắt về máy", "Chỉ nhắc mạng một lần", "PUBLISHED");
        themBai("nhieu", "Mạng máy tính", "Mạng là gì", "Mạng nối các máy, mạng cục bộ, mạng diện rộng, mạng toàn cầu", "PUBLISHED");

        tim("mạng").andExpect(jsonPath("$.items[*].slug", contains("nhieu", "it")));
    }

    @Test
    void phanTrangChiaDungKhongTrungKhongSotVaTongDemDuBaiKhop() throws Exception {
        themBai("m1", "Mạng một", "Tóm tắt", "mạng", "PUBLISHED");
        themBai("m2", "Mạng hai", "Tóm tắt", "mạng", "PUBLISHED");
        themBai("m3", "Mạng ba", "Tóm tắt", "mạng", "PUBLISHED");

        String trang0 = mockMvc.perform(get("/api/lessons").param("q", "mạng").param("size", "2").with(user("hocsinh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.size").value(2))
                .andReturn().getResponse().getContentAsString();
        String trang1 = mockMvc.perform(get("/api/lessons").param("q", "mạng").param("size", "2").param("page", "1")
                        .with(user("hocsinh")))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andReturn().getResponse().getContentAsString();

        // Ba bài khác nhau qua hai trang: không trùng, không sót.
        long gapDuoc = Stream.of("m1", "m2", "m3")
                .filter(slug -> (trang0 + trang1).contains("\"" + slug + "\""))
                .count();
        assertThat(gapDuoc).isEqualTo(3);

        mockMvc.perform(get("/api/lessons").param("q", "mạng").param("size", "2").param("page", "9")
                        .with(user("hocsinh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalItems").value(3));
    }

    @Test
    void moiKetQuaChiCoBonTruongKhongCoNoiDungHayTrangThai() throws Exception {
        themBai("bai-1", "Mạng máy tính", "Tóm tắt", "Nội dung mạng", "PUBLISHED");

        tim("mạng")
                .andExpect(jsonPath("$.items[0].*", hasSize(4)))
                .andExpect(jsonPath("$.items[0].id").exists())
                .andExpect(jsonPath("$.items[0].slug").value("bai-1"))
                .andExpect(jsonPath("$.items[0].title").value("Mạng máy tính"))
                .andExpect(jsonPath("$.items[0].summary").value("Tóm tắt"))
                .andExpect(jsonPath("$.items[0].contentMd").doesNotExist())
                .andExpect(jsonPath("$.items[0].status").doesNotExist());
    }

    // Chế độ tự nhiên bỏ qua toán tử của chế độ BOOLEAN (+ - * " ( ) < > ~ @), nên từ khóa lạ không gây lỗi 500
    // và không đổi nghĩa truy vấn.
    @Test
    void kyTuToanTuTrongTuKhoaKhongGayLoiVaKhongDoiNghiaTruyVan() throws Exception {
        themBai("bai-1", "Mạng máy tính", "Tóm tắt", "Nội dung", "PUBLISHED");

        tim("+mạng -tin \"máy* (x) @4 ~ < >").andExpect(status().isOk());
        tim("%").andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(0)));
        tim("_").andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void tuKhoaCoDauNhayVaCauSqlKhongLamLoTatCaBai() throws Exception {
        themBai("bai-1", "Mạng máy tính", "Tóm tắt", "Nội dung", "PUBLISHED");
        themBai("bai-2", "Thuật toán", "Tóm tắt", "Nội dung", "PUBLISHED");

        tim("' OR 1=1 --")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    // Giới hạn đã biết của InnoDB FULLTEXT (D29): từ ngắn hơn 3 ký tự bị bỏ qua nên không ra kết quả, nhưng không lỗi.
    @Test
    void tuKhoaChiGomTuQuaNganKhongRaKetQuaNhungKhongLoi() throws Exception {
        themBai("bai-1", "Số ab và cd", "ab cd", "ab ab ab cd cd", "PUBLISHED");

        tim("ab")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalItems").value(0));
    }
}
