package vn.spacex.course;

import java.util.Locale;
import java.util.function.Predicate;

// Mã khóa (courses.code, tối đa 20 ký tự) sinh từ slug khi Admin chưa nhập mã. Cùng quy tắc với migration V6.
final class CourseCodes {

    private static final int MAX_LENGTH = 20;

    private CourseCodes() {
    }

    static String fromSlug(String slug, Predicate<String> taken) {
        // Locale.ROOT: chữ hoa không phụ thuộc ngôn ngữ của máy chủ (ví dụ tiếng Thổ Nhĩ Kỳ đổi "i" thành "İ").
        String base = slug.replace("-", "").toUpperCase(Locale.ROOT);
        String code = cut(base, MAX_LENGTH);
        // Hậu tố có gạch ngang nên không bao giờ trùng mã gốc của một slug khác.
        for (int number = 2; taken.test(code); number++) {
            String suffix = "-" + number;
            code = cut(base, MAX_LENGTH - suffix.length()) + suffix;
        }
        return code;
    }

    private static String cut(String text, int length) {
        return text.length() <= length ? text : text.substring(0, length);
    }
}
