package vn.spacex.auth;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class LoginAttemptLimiterTest {

    private static final Duration CUA_SO = Duration.ofMinutes(15);

    // Đồng hồ chỉnh tay để kiểm việc hết hạn mà không phải chờ.
    private static final class DongHo extends Clock {
        private Instant bayGio = Instant.parse("2026-11-27T10:00:00Z");

        void troi(Duration khoang) {
            bayGio = bayGio.plus(khoang);
        }

        @Override
        public Instant instant() {
            return bayGio;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    private final DongHo dongHo = new DongHo();

    private LoginAttemptLimiter boDem(int theoEmail, int theoIp) {
        return new LoginAttemptLimiter(theoEmail, theoIp, CUA_SO, dongHo);
    }

    private void thuSai(LoginAttemptLimiter boDem, String email, String ip, int soLan) {
        for (int i = 0; i < soLan; i++) {
            boDem.acquire(email, ip);
        }
    }

    @Test
    void duocThuDungSoLanChoPhepRoiLanKeTiepBiTuChoi() {
        LoginAttemptLimiter boDem = boDem(3, 100);

        assertThatCode(() -> thuSai(boDem, "a@example.com", "1.1.1.1", 3)).doesNotThrowAnyException();

        assertThatThrownBy(() -> boDem.acquire("a@example.com", "1.1.1.1"))
                .isInstanceOf(TooManyLoginAttemptsException.class);
    }

    @Test
    void thoiGianChoLaLucLanSaiCuNhatHetHan() {
        LoginAttemptLimiter boDem = boDem(2, 100);
        boDem.acquire("a@example.com", "1.1.1.1");
        dongHo.troi(Duration.ofMinutes(5));
        boDem.acquire("a@example.com", "1.1.1.1");

        TooManyLoginAttemptsException loi = catchThrowableOfType(TooManyLoginAttemptsException.class,
                () -> boDem.acquire("a@example.com", "1.1.1.1"));

        assertThat(loi.retryAfter()).isEqualTo(Duration.ofMinutes(10));
    }

    // Lần bị từ chối không được tính thêm, nếu không người dùng bấm liên tục sẽ bị khóa mãi.
    @Test
    void hetCuaSoThiThuLaiDuocDuTrongLucChoVanBamLienTuc() {
        LoginAttemptLimiter boDem = boDem(2, 100);
        thuSai(boDem, "a@example.com", "1.1.1.1", 2);
        dongHo.troi(Duration.ofMinutes(14));
        assertThatThrownBy(() -> boDem.acquire("a@example.com", "1.1.1.1"))
                .isInstanceOf(TooManyLoginAttemptsException.class);

        dongHo.troi(Duration.ofMinutes(1));

        assertThatCode(() -> boDem.acquire("a@example.com", "1.1.1.1")).doesNotThrowAnyException();
    }

    @Test
    void gioiHanTheoEmailKhongNeDuocBangCachDoiDiaChi() {
        LoginAttemptLimiter boDem = boDem(2, 100);
        thuSai(boDem, "a@example.com", "1.1.1.1", 2);

        assertThatThrownBy(() -> boDem.acquire("a@example.com", "2.2.2.2"))
                .isInstanceOf(TooManyLoginAttemptsException.class);
        assertThatCode(() -> boDem.acquire("b@example.com", "1.1.1.1")).doesNotThrowAnyException();
    }

    @Test
    void gioiHanTheoDiaChiChanViecDoNhieuEmail() {
        LoginAttemptLimiter boDem = boDem(100, 2);
        boDem.acquire("a@example.com", "1.1.1.1");
        boDem.acquire("b@example.com", "1.1.1.1");

        assertThatThrownBy(() -> boDem.acquire("c@example.com", "1.1.1.1"))
                .isInstanceOf(TooManyLoginAttemptsException.class);
        assertThatCode(() -> boDem.acquire("c@example.com", "2.2.2.2")).doesNotThrowAnyException();
    }

    // Bị chặn vì địa chỉ thì không được cộng lỗi vào email của người khác.
    @Test
    void lanBiChanTheoDiaChiKhongTinhVaoEmail() {
        LoginAttemptLimiter boDem = boDem(1, 1);
        boDem.acquire("a@example.com", "1.1.1.1");
        assertThatThrownBy(() -> boDem.acquire("nan-nhan@example.com", "1.1.1.1"))
                .isInstanceOf(TooManyLoginAttemptsException.class);

        assertThatCode(() -> boDem.acquire("nan-nhan@example.com", "2.2.2.2")).doesNotThrowAnyException();
    }

    @Test
    void dangNhapDungXoaSoLanSaiCuaEmailVaKhongTonHanMucDiaChi() {
        LoginAttemptLimiter boDem = boDem(2, 3);
        boDem.acquire("a@example.com", "1.1.1.1");

        for (int i = 0; i < 10; i++) {
            boDem.acquire("a@example.com", "1.1.1.1");
            boDem.succeeded("a@example.com", "1.1.1.1");
        }

        assertThatCode(() -> thuSai(boDem, "a@example.com", "1.1.1.1", 2)).doesNotThrowAnyException();
    }

    // Lỗi máy chủ (database sập) không phải người dùng gõ sai.
    @Test
    void traLaiLanThuKhiLoiKhongDoNguoiDung() {
        LoginAttemptLimiter boDem = boDem(1, 1);
        for (int i = 0; i < 5; i++) {
            boDem.acquire("a@example.com", "1.1.1.1");
            boDem.release("a@example.com", "1.1.1.1");
        }

        assertThatCode(() -> boDem.acquire("a@example.com", "1.1.1.1")).doesNotThrowAnyException();
    }

    @Test
    void cauHinhKhongDuongThiTuChoiKhoiDong() {
        assertThatThrownBy(() -> new LoginAttemptLimiter(0, 1, CUA_SO, dongHo))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LoginAttemptLimiter(1, 0, CUA_SO, dongHo))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LoginAttemptLimiter(1, 1, Duration.ZERO, dongHo))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
