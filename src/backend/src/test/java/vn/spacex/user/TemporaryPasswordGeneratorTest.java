package vn.spacex.user;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TemporaryPasswordGeneratorTest {

    private static final int SO_LAN = 500;

    private final TemporaryPasswordGenerator generator = new TemporaryPasswordGenerator();

    @Test
    void matKhauTamDai16KyTuVaVuaGioiHan72ByteCuaBcrypt() {
        for (int i = 0; i < SO_LAN; i++) {
            String matKhau = generator.generate();

            assertThat(matKhau).hasSize(16);
            assertThat(matKhau.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(72);
        }
    }

    // Admin đọc hoặc gõ lại cho học sinh nên bỏ các ký tự dễ nhầm: 0 O 1 l I.
    @Test
    void chiGomChuVaSoKhongCoKyTuDeNham() {
        for (int i = 0; i < SO_LAN; i++) {
            assertThat(generator.generate()).matches("[A-HJ-NP-Za-km-z2-9]{16}");
        }
    }

    // Luôn có đủ chữ hoa, chữ thường và số để qua mọi yêu cầu độ mạnh mật khẩu.
    @Test
    void luonCoChuHoaChuThuongVaSo() {
        for (int i = 0; i < SO_LAN; i++) {
            String matKhau = generator.generate();

            assertThat(matKhau).containsPattern("[A-Z]").containsPattern("[a-z]").containsPattern("[0-9]");
        }
    }

    @Test
    void moiLanSinhMotMatKhauKhacNhau() {
        Set<String> daSinh = new HashSet<>();
        for (int i = 0; i < SO_LAN; i++) {
            daSinh.add(generator.generate());
        }

        assertThat(daSinh).hasSize(SO_LAN);
    }

    // Không được luôn đặt ký tự theo cùng một vị trí (ví dụ luôn bắt đầu bằng chữ hoa): xáo trộn sau khi chọn.
    @Test
    void viTriCuaChuHoaChuThuongVaSoKhongCoDinh() {
        Set<Boolean> kyTuDauLaChuHoa = new HashSet<>();
        Set<Boolean> kyTuDauLaSo = new HashSet<>();
        for (int i = 0; i < SO_LAN; i++) {
            char dau = generator.generate().charAt(0);
            kyTuDauLaChuHoa.add(Character.isUpperCase(dau));
            kyTuDauLaSo.add(Character.isDigit(dau));
        }

        assertThat(kyTuDauLaChuHoa).containsExactlyInAnyOrder(true, false);
        assertThat(kyTuDauLaSo).containsExactlyInAnyOrder(true, false);
    }
}
