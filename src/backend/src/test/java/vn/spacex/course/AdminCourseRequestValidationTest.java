package vn.spacex.course;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AdminCourseRequestValidationTest {

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static Set<String> truongLoi(AdminCourseRequest request) {
        return validator.validate(request).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private static AdminCourseRequest request(String slug, String title, String description, Integer sortOrder) {
        return new AdminCourseRequest(slug, title, description, sortOrder);
    }

    @Test
    void duLieuHopLeKhongCoLoi() {
        assertThat(truongLoi(request("tin-hoc-10", "Tin học 10", "Chương trình lớp 10", 1))).isEmpty();
    }

    @Test
    void moTaCoTheDeTrongHoacNull() {
        assertThat(truongLoi(request("tin-hoc-10", "Tin học 10", null, 1))).isEmpty();
        assertThat(truongLoi(request("tin-hoc-10", "Tin học 10", "   ", 1))).isEmpty();
    }

    @Test
    void moTaChiCoKhoangTrangDuocLuuThanhNull() {
        assertThat(request("tin-hoc-10", "Tin học 10", "   ", 1).description()).isNull();
    }

    @Test
    void thieuTruongBatBuocThiBaoLoiTungTruong() {
        assertThat(truongLoi(request(null, null, null, null))).containsExactlyInAnyOrder("slug", "title", "sortOrder");
    }

    @Test
    void slugVaTieuDeChiCoKhoangTrangBiTuChoi() {
        assertThat(truongLoi(request("   ", "   ", null, 1))).containsExactlyInAnyOrder("slug", "title");
    }

    @Test
    void slugVaTieuDeDuocCatKhoangTrangHaiDau() {
        AdminCourseRequest r = request("  tin-hoc-10  ", "  Tin học 10  ", null, 1);

        assertThat(r.slug()).isEqualTo("tin-hoc-10");
        assertThat(r.title()).isEqualTo("Tin học 10");
        assertThat(truongLoi(r)).isEmpty();
    }

    @Test
    void slugChiNhanChuThuongSoVaGachNgangNoiGiuaCacTu() {
        assertThat(truongLoi(request("tin-hoc-10", "x", null, 0))).isEmpty();
        assertThat(truongLoi(request("tin hoc", "x", null, 0))).containsExactly("slug");
        assertThat(truongLoi(request("Tin-Hoc", "x", null, 0))).containsExactly("slug");
        assertThat(truongLoi(request("tin_hoc", "x", null, 0))).containsExactly("slug");
        assertThat(truongLoi(request("-tin-hoc", "x", null, 0))).containsExactly("slug");
        assertThat(truongLoi(request("tin-hoc-", "x", null, 0))).containsExactly("slug");
        assertThat(truongLoi(request("tin--hoc", "x", null, 0))).containsExactly("slug");
        assertThat(truongLoi(request("tin-học", "x", null, 0))).containsExactly("slug");
    }

    @Test
    void slugDuoc100KyTuNhungKhongQua100() {
        assertThat(truongLoi(request("a".repeat(100), "x", null, 0))).isEmpty();
        assertThat(truongLoi(request("a".repeat(101), "x", null, 0))).containsExactly("slug");
    }

    @Test
    void tieuDeDuoc200KyTuNhungKhongQua200() {
        assertThat(truongLoi(request("a", "t".repeat(200), null, 0))).isEmpty();
        assertThat(truongLoi(request("a", "t".repeat(201), null, 0))).containsExactly("title");
    }

    @Test
    void moTaDuoc500KyTuNhungKhongQua500() {
        assertThat(truongLoi(request("a", "x", "m".repeat(500), 0))).isEmpty();
        assertThat(truongLoi(request("a", "x", "m".repeat(501), 0))).containsExactly("description");
    }

    @Test
    void thuTuKhongDuocAm() {
        assertThat(truongLoi(request("a", "x", null, 0))).isEmpty();
        assertThat(truongLoi(request("a", "x", null, -1))).containsExactly("sortOrder");
    }
}
