package vn.spacex.lesson;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AdminLessonRequestValidationTest {

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static Set<String> truongLoi(Object request) {
        return validator.validate(request).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private static AdminLessonRequest request(Long courseId, String slug, String title, String summary,
                                              String contentMd, Integer sortOrder) {
        return new AdminLessonRequest(courseId, slug, title, summary, contentMd, sortOrder);
    }

    private static AdminLessonRequest hopLe() {
        return request(1L, "may-tinh-la-gi", "Máy tính là gì", "Giới thiệu", "# Máy tính\n\nNội dung", 1);
    }

    @Test
    void duLieuHopLeKhongCoLoi() {
        assertThat(truongLoi(hopLe())).isEmpty();
    }

    @Test
    void thieuTruongBatBuocThiBaoLoiTungTruong() {
        assertThat(truongLoi(request(null, null, null, null, null, null)))
                .containsExactlyInAnyOrder("courseId", "slug", "title", "contentMd", "sortOrder");
    }

    @Test
    void tomTatCoTheDeTrongHoacNullVaChiCoKhoangTrangDuocLuuThanhNull() {
        assertThat(truongLoi(request(1L, "a", "x", null, "n", 0))).isEmpty();
        assertThat(request(1L, "a", "x", "   ", "n", 0).summary()).isNull();
    }

    @Test
    void noiDungCoTheLaChuoiRongNhungKhongDuocNull() {
        assertThat(truongLoi(request(1L, "a", "x", null, "", 0))).isEmpty();
        assertThat(truongLoi(request(1L, "a", "x", null, null, 0))).containsExactly("contentMd");
    }

    @Test
    void noiDungKhongBiCatKhoangTrangVaGiuNguyenXuongDong() {
        String md = "  # Tiêu đề\n\n    code thụt lề\n";

        assertThat(request(1L, "a", "x", null, md, 0).contentMd()).isEqualTo(md);
    }

    @Test
    void maKhoaPhaiLaSoDuong() {
        assertThat(truongLoi(request(0L, "a", "x", null, "n", 0))).containsExactly("courseId");
        assertThat(truongLoi(request(-5L, "a", "x", null, "n", 0))).containsExactly("courseId");
    }

    @Test
    void slugVaTieuDeChiCoKhoangTrangBiTuChoiVaDuocCatKhoangTrangHaiDau() {
        assertThat(truongLoi(request(1L, "   ", "   ", null, "n", 0))).containsExactlyInAnyOrder("slug", "title");

        AdminLessonRequest r = request(1L, "  bai-1  ", "  Bài 1  ", null, "n", 0);
        assertThat(r.slug()).isEqualTo("bai-1");
        assertThat(r.title()).isEqualTo("Bài 1");
        assertThat(truongLoi(r)).isEmpty();
    }

    @Test
    void slugChiNhanChuThuongSoVaGachNgangNoiGiuaCacTu() {
        assertThat(truongLoi(request(1L, "bai-1-may-tinh", "x", null, "n", 0))).isEmpty();
        assertThat(truongLoi(request(1L, "Bai-1", "x", null, "n", 0))).containsExactly("slug");
        assertThat(truongLoi(request(1L, "bai_1", "x", null, "n", 0))).containsExactly("slug");
        assertThat(truongLoi(request(1L, "bai 1", "x", null, "n", 0))).containsExactly("slug");
        assertThat(truongLoi(request(1L, "-bai", "x", null, "n", 0))).containsExactly("slug");
        assertThat(truongLoi(request(1L, "bai-", "x", null, "n", 0))).containsExactly("slug");
        assertThat(truongLoi(request(1L, "bai--1", "x", null, "n", 0))).containsExactly("slug");
        assertThat(truongLoi(request(1L, "bài-1", "x", null, "n", 0))).containsExactly("slug");
    }

    @Test
    void slugDuoc150KyTuNhungKhongQua150() {
        assertThat(truongLoi(request(1L, "a".repeat(150), "x", null, "n", 0))).isEmpty();
        assertThat(truongLoi(request(1L, "a".repeat(151), "x", null, "n", 0))).containsExactly("slug");
    }

    @Test
    void tieuDeDuoc200KyTuNhungKhongQua200() {
        assertThat(truongLoi(request(1L, "a", "t".repeat(200), null, "n", 0))).isEmpty();
        assertThat(truongLoi(request(1L, "a", "t".repeat(201), null, "n", 0))).containsExactly("title");
    }

    @Test
    void tomTatDuoc500KyTuNhungKhongQua500() {
        assertThat(truongLoi(request(1L, "a", "x", "m".repeat(500), "n", 0))).isEmpty();
        assertThat(truongLoi(request(1L, "a", "x", "m".repeat(501), "n", 0))).containsExactly("summary");
    }

    @Test
    void noiDungDuoc100000KyTuNhungKhongQua() {
        assertThat(truongLoi(request(1L, "a", "x", null, "n".repeat(100_000), 0))).isEmpty();
        assertThat(truongLoi(request(1L, "a", "x", null, "n".repeat(100_001), 0))).containsExactly("contentMd");
    }

    @Test
    void thuTuKhongDuocAm() {
        assertThat(truongLoi(request(1L, "a", "x", null, "n", 0))).isEmpty();
        assertThat(truongLoi(request(1L, "a", "x", null, "n", -1))).containsExactly("sortOrder");
    }

    @Test
    void doiTrangThaiBatBuocCoTrangThai() {
        assertThat(truongLoi(new AdminLessonStatusRequest(null))).containsExactly("status");
        assertThat(truongLoi(new AdminLessonStatusRequest(LessonStatus.PUBLISHED))).isEmpty();
    }
}
