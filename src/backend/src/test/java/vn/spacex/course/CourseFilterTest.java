package vn.spacex.course;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseFilterTest {

    private static CourseFilter gio(String duration) {
        return CourseFilter.of(null, null, duration, null, null);
    }

    @Test
    void khongCoThamSoThiKhongLocGiVaSapTheoThuTuHienThi() {
        CourseFilter filter = CourseFilter.of(null, null, null, null, null);

        assertThat(filter.categorySlug()).isNull();
        assertThat(filter.level()).isNull();
        assertThat(filter.hours()).isEqualTo(NumberRange.ANY);
        assertThat(filter.tuition()).isEqualTo(NumberRange.ANY);
        assertThat(filter.sort()).isEqualTo(Sort.by("sortOrder", "id"));
    }

    @Test
    void thamSoRongCoiNhuKhongGui() {
        assertThat(CourseFilter.of("  ", null, "", " ", "")).isEqualTo(CourseFilter.of(null, null, null, null, null));
    }

    @Test
    void khoangDuHaiDauHoacBoMotDau() {
        assertThat(gio("20-40").hours()).isEqualTo(new NumberRange(20, 40));
        assertThat(gio("-40").hours()).isEqualTo(new NumberRange(0, 40));
        assertThat(gio("20-").hours()).isEqualTo(new NumberRange(20, Long.MAX_VALUE));
        assertThat(gio("30-30").hours()).isEqualTo(new NumberRange(30, 30));
    }

    @Test
    void khoangSaiDangBiTuChoi() {
        for (String sai : new String[] {"-", "40", "abc", "40-20", "-1-5", "1.5-2", "1,000-", "1 - 5",
                "12345678901-"}) {
            assertThatThrownBy(() -> gio(sai)).as(sai).isInstanceOf(InvalidCourseFilterException.class);
        }
    }

    @Test
    void hocPhiDungCungDangKhoang() {
        assertThat(CourseFilter.of(null, null, null, "1000000-3000000", null).tuition())
                .isEqualTo(new NumberRange(1_000_000, 3_000_000));
    }

    @Test
    void sapTheoHocPhiThiKhoaBangGiaGiuThuTuHienThi() {
        assertThat(CourseFilter.of(null, null, null, null, "price_asc").sort())
                .isEqualTo(Sort.by(Sort.Order.asc("tuitionVnd"), Sort.Order.asc("sortOrder"), Sort.Order.asc("id")));
        assertThat(CourseFilter.of(null, null, null, null, "price_desc").sort())
                .isEqualTo(Sort.by(Sort.Order.desc("tuitionVnd"), Sort.Order.asc("sortOrder"), Sort.Order.asc("id")));
    }

    // Không nhận tên cột tùy ý từ trình duyệt.
    @Test
    void kieuSapXepKhacBiTuChoi() {
        assertThatThrownBy(() -> CourseFilter.of(null, null, null, null, "tuitionVnd"))
                .isInstanceOf(InvalidCourseFilterException.class);
    }
}
