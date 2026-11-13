package vn.spacex.user;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Sinh mật khẩu tạm khi admin đặt lại mật khẩu. Dùng SecureRandom (không dùng Random vì đoán được). Bỏ 0 O 1 l I vì dễ nhầm
// khi đọc hoặc gõ lại. 16 ký tự ASCII = 16 byte, dưới giới hạn 72 byte của BCrypt.
@Component
public class TemporaryPasswordGenerator {

    private static final int LENGTH = 16;
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String ALL = UPPER + LOWER + DIGITS;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        List<Character> chars = new ArrayList<>(LENGTH);
        chars.add(pick(UPPER));
        chars.add(pick(LOWER));
        chars.add(pick(DIGITS));
        while (chars.size() < LENGTH) {
            chars.add(pick(ALL));
        }
        // Ba ký tự đầu là cố định theo loại, nên phải xáo trộn để vị trí không đoán được.
        Collections.shuffle(chars, random);

        StringBuilder result = new StringBuilder(LENGTH);
        chars.forEach(result::append);
        return result.toString();
    }

    private char pick(String source) {
        return source.charAt(random.nextInt(source.length()));
    }
}
