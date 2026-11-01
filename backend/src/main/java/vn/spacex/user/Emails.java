package vn.spacex.user;

import java.util.Locale;

public final class Emails {

    private Emails() {
    }

    // Locale.ROOT để kết quả không phụ thuộc ngôn ngữ máy (ví dụ chữ 'I' trong locale Thổ Nhĩ Kỳ), D25.
    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
