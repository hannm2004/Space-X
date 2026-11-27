package vn.spacex.course;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Khoảng số của bộ lọc, viết "min-max" và được bỏ một đầu: "20-40", "-40", "20-". Hai đầu đều tính cả số đó.
public record NumberRange(long min, long max) {

    public static final NumberRange ANY = new NumberRange(0, Long.MAX_VALUE);

    // Tối đa 10 chữ số nên Long.parseLong không bao giờ tràn.
    private static final Pattern FORMAT = Pattern.compile("(\\d{0,10})-(\\d{0,10})");

    static NumberRange parse(String text) {
        if (text == null || text.isBlank()) {
            return ANY;
        }
        Matcher matcher = FORMAT.matcher(text);
        if (!matcher.matches() || (matcher.group(1).isEmpty() && matcher.group(2).isEmpty())) {
            throw new InvalidCourseFilterException();
        }
        long min = matcher.group(1).isEmpty() ? ANY.min : Long.parseLong(matcher.group(1));
        long max = matcher.group(2).isEmpty() ? ANY.max : Long.parseLong(matcher.group(2));
        if (min > max) {
            throw new InvalidCourseFilterException();
        }
        return new NumberRange(min, max);
    }
}
