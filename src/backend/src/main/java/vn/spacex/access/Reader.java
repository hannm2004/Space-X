package vn.spacex.access;

import vn.spacex.auth.AppUserDetails;
import vn.spacex.user.Role;

// Người đang đọc, lấy từ session: đủ hai điều quy tắc quyền cần biết. Không bao giờ dựng từ dữ liệu trình duyệt gửi.
public record Reader(long userId, boolean admin) {

    public static Reader of(AppUserDetails me) {
        return new Reader(me.id(), me.role() == Role.ADMIN);
    }
}
