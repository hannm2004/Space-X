package vn.spacex.course;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CourseCodesTest {

    private static final String SLUG_DAI = "lap-trinh-ung-dung-web-nang-cao";

    @Test
    void maLaChuHoaCuaSlugBoGachNgang() {
        assertThat(CourseCodes.fromSlug("tin-hoc-10", code -> false)).isEqualTo("TINHOC10");
    }

    @Test
    void slugDaiThiCatConHaiMuoiKyTu() {
        assertThat(CourseCodes.fromSlug(SLUG_DAI, code -> false)).isEqualTo("LAPTRINHUNGDUNGWEBNA");
    }

    @Test
    void maDaCoNguoiDungThiThemSoThuTu() {
        assertThat(CourseCodes.fromSlug("tin-hoc-10", Set.of("TINHOC10")::contains)).isEqualTo("TINHOC10-2");
    }

    @Test
    void soThuTuTangToiKhiGapMaConTrong() {
        assertThat(CourseCodes.fromSlug("tin-hoc-10", Set.of("TINHOC10", "TINHOC10-2")::contains))
                .isEqualTo("TINHOC10-3");
    }

    @Test
    void maDaiBiTrungThiCatThemDeSoThuTuVanVuaHaiMuoiKyTu() {
        String ma = CourseCodes.fromSlug(SLUG_DAI, Set.of("LAPTRINHUNGDUNGWEBNA")::contains);

        assertThat(ma).isEqualTo("LAPTRINHUNGDUNGWEB-2").hasSize(20);
    }
}
