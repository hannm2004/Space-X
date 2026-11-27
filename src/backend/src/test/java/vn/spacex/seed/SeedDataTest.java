package vn.spacex.seed;

import vn.spacex.course.CourseAccessType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.json.JsonParserFactory;
import vn.spacex.course.AdminCourseRequest;
import vn.spacex.course.CourseStatus;
import vn.spacex.lesson.AdminLessonRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

// Dữ liệu mẫu ở thư mục seed/ (gốc dự án) được nạp qua API Admin bằng scripts/seed-sample-data.ps1. Test này bảo đảm
// dữ liệu qua được đúng luật validate của API, để script không bị API từ chối giữa chừng.
// Test chạy ở src/backend nên thư mục seed/ nằm ở ../../seed (giống cách application.properties tìm .env).
class SeedDataTest {

    private static final Path SEED = Path.of("..", "..", "seed");
    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static List<Map<String, Object>> readList(String file) throws IOException {
        String json = Files.readString(SEED.resolve(file), StandardCharsets.UTF_8);
        return JsonParserFactory.getJsonParser().parseList(json).stream()
                .map(o -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = (Map<String, Object>) o;
                    return m;
                })
                .toList();
    }

    private static String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? null : v.toString();
    }

    private static Integer num(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? null : ((Number) v).intValue();
    }

    private static String describe(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream().map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .sorted().collect(Collectors.joining("; "));
    }

    private static AdminCourseRequest toCourse(Map<String, Object> m) {
        // Script gửi thêm trạng thái và loại khóa; học phí, nhóm, trình độ và số giờ để máy chủ đặt mặc định.
        String status = str(m, "status");
        String accessType = str(m, "accessType");
        return new AdminCourseRequest(str(m, "slug"), str(m, "title"), str(m, "description"), num(m, "sortOrder"),
                status == null ? null : CourseStatus.valueOf(status), null, null, null, null,
                accessType == null ? null : CourseAccessType.valueOf(accessType));
    }

    private static String content(Map<String, Object> lesson) throws IOException {
        return Files.readString(SEED.resolve("lessons").resolve(str(lesson, "file")), StandardCharsets.UTF_8);
    }

    private static AdminLessonRequest toLesson(Map<String, Object> m) throws IOException {
        // courseId giả: chỉ để kiểm các luật còn lại; script thay bằng id thật của khóa lúc nạp.
        return new AdminLessonRequest(1L, str(m, "slug"), str(m, "title"), str(m, "summary"), content(m),
                num(m, "sortOrder"));
    }

    // Khóa nạp mới mặc định là bản nháp, không hiện ra trang công khai.
    @Test
    void moiKhoaMauDeuDuocDang() throws IOException {
        assertThat(readList("courses.json")).allSatisfy(c -> assertThat(str(c, "status")).isEqualTo("PUBLISHED"));
    }

    // Khóa nạp mới mặc định là trả phí; 31 bài Tin học phải đọc được miễn phí (D40).
    @Test
    void moiKhoaMauDeuMienPhi() throws IOException {
        assertThat(readList("courses.json"))
                .allSatisfy(c -> assertThat(str(c, "accessType")).isEqualTo("FREE"));
    }

    @Test
    void coDungBayKhoaTinHocLop6Den12() throws IOException {
        List<String> slugs = readList("courses.json").stream().map(c -> str(c, "slug")).toList();

        assertThat(slugs).containsExactly("tin-hoc-6", "tin-hoc-7", "tin-hoc-8", "tin-hoc-9",
                "tin-hoc-10", "tin-hoc-11", "tin-hoc-12");
    }

    @Test
    void moiKhoaQuaDuocLuatValidateCuaApiAdmin() throws IOException {
        for (Map<String, Object> course : readList("courses.json")) {
            assertThat(describe(validator.validate(toCourse(course))))
                    .as("khóa %s", str(course, "slug")).isEmpty();
        }
    }

    @Test
    void moiBaiQuaDuocLuatValidateCuaApiAdmin() throws IOException {
        for (Map<String, Object> lesson : readList("lessons.json")) {
            assertThat(describe(validator.validate(toLesson(lesson))))
                    .as("bài %s", str(lesson, "slug")).isEmpty();
        }
    }

    @Test
    void slugKhoaVaSlugBaiKhongTrungNhau() throws IOException {
        List<String> khoa = readList("courses.json").stream().map(c -> str(c, "slug")).toList();
        List<String> bai = readList("lessons.json").stream().map(l -> str(l, "slug")).toList();

        assertThat(khoa).doesNotHaveDuplicates();
        assertThat(bai).doesNotHaveDuplicates();
    }

    @Test
    void moiBaiThuocMotKhoaCoThatVaThuTuTrongKhoaKhongTrung() throws IOException {
        Set<String> khoa = readList("courses.json").stream().map(c -> str(c, "slug")).collect(Collectors.toSet());
        Set<String> daThay = new HashSet<>();

        for (Map<String, Object> lesson : readList("lessons.json")) {
            String courseSlug = str(lesson, "courseSlug");
            assertThat(khoa).as("bài %s", str(lesson, "slug")).contains(courseSlug);
            assertThat(daThay.add(courseSlug + "#" + num(lesson, "sortOrder")))
                    .as("thứ tự trùng trong khóa %s", courseSlug).isTrue();
        }
    }

    @Test
    void moiKhoaCoItNhatHaiBaiMau() throws IOException {
        List<Map<String, Object>> baiMau = readList("lessons.json");

        for (Map<String, Object> course : readList("courses.json")) {
            long soBai = baiMau.stream().filter(l -> str(l, "courseSlug").equals(str(course, "slug"))).count();
            assertThat(soBai).as("số bài của %s", str(course, "slug")).isGreaterThanOrEqualTo(2);
        }
    }

    @Test
    void moiTepMarkdownDeuDuocDungVaMoiBaiDeuCoTepThat() throws IOException {
        Set<String> duocDung = new HashSet<>();
        for (Map<String, Object> lesson : readList("lessons.json")) {
            String file = str(lesson, "file");
            duocDung.add(file);
            assertThat(Files.isRegularFile(SEED.resolve("lessons").resolve(file)))
                    .as("tệp %s", file).isTrue();
        }

        Set<String> coTrenDia;
        try (Stream<Path> files = Files.walk(SEED.resolve("lessons"))) {
            coTrenDia = files.filter(Files::isRegularFile)
                    .map(p -> SEED.resolve("lessons").relativize(p).toString().replace('\\', '/'))
                    .collect(Collectors.toSet());
        }
        assertThat(coTrenDia).as("không có tệp Markdown mồ côi").isEqualTo(duocDung);
    }

    @Test
    void noiDungBaiLaMarkdownThuanKhongNhungMaThucThiVaKhongCoBOM() throws IOException {
        for (Map<String, Object> lesson : readList("lessons.json")) {
            String md = content(lesson);

            assertThat(md).as("bài %s", str(lesson, "slug")).isNotBlank();
            assertThat(md.charAt(0)).as("BOM đầu tệp của %s", str(lesson, "slug")).isNotEqualTo('﻿');
            assertThat(md.toLowerCase()).doesNotContain("<script").doesNotContain("<iframe").doesNotContain("javascript:");
            // Bài thử hệ thống ghi "Bài mẫu"; bài soạn theo chương trình (D32) ghi nguồn khung chương trình.
            assertThat(md.contains("Bài mẫu") || md.contains("Thông tư 32/2018/TT-BGDĐT"))
                    .as("bài %s phải là bài mẫu hoặc ghi nguồn khung chương trình", str(lesson, "slug")).isTrue();
        }
    }

    @Test
    void moiBaiCoTieuDeCapMotVaMucKiemTraNhanhHoacLaBaiMau() throws IOException {
        for (Map<String, Object> lesson : readList("lessons.json")) {
            String md = content(lesson);
            String slug = str(lesson, "slug");

            assertThat(md.startsWith("# ")).as("bài %s mở đầu bằng tiêu đề cấp 1", slug).isTrue();
            if (md.contains("Thông tư 32/2018/TT-BGDĐT")) {
                assertThat(md).as("bài %s có mục tự kiểm tra", slug).contains("## Kiểm tra nhanh");
            }
        }
    }
}
