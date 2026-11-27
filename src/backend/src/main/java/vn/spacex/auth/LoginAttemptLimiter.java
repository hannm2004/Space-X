package vn.spacex.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

// Giới hạn số lần đăng nhập sai (D40, bắt buộc trước khi mở bán), đếm theo email và theo địa chỉ IP trong một cửa sổ
// thời gian trượt. Bộ đếm nằm trong bộ nhớ: ứng dụng chỉ chạy một bản, khởi động lại thì đếm lại từ đầu.
@Component
public class LoginAttemptLimiter {

    // Trần số email và địa chỉ được theo dõi, để việc dò bằng email bịa không làm đầy bộ nhớ.
    private static final int MAX_TRACKED_KEYS = 10_000;
    private static final int MAX_EMAIL_LENGTH = 255;

    private final int maxPerEmail;
    private final int maxPerIp;
    private final Duration window;
    private final Clock clock;

    // Sắp theo lần dùng gần nhất: đầy thì bỏ mục lâu không đụng tới.
    private final Map<String, Deque<Instant>> attempts = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Deque<Instant>> eldest) {
            return size() > MAX_TRACKED_KEYS;
        }
    };

    public LoginAttemptLimiter(@Value("${app.login.max-failures-per-email}") int maxPerEmail,
                               @Value("${app.login.max-failures-per-ip}") int maxPerIp,
                               @Value("${app.login.window}") Duration window,
                               Clock clock) {
        if (maxPerEmail < 1 || maxPerIp < 1 || !window.isPositive()) {
            throw new IllegalArgumentException("Giới hạn đăng nhập (app.login.*) phải là số dương");
        }
        this.maxPerEmail = maxPerEmail;
        this.maxPerIp = maxPerIp;
        this.window = window;
        this.clock = clock;
    }

    // Gọi TRƯỚC khi kiểm mật khẩu và tính luôn lần thử này: nếu chỉ đếm sau khi sai thì một loạt yêu cầu gửi cùng lúc
    // sẽ lọt qua hết. Lần bị từ chối không được tính, nên hết cửa sổ là thử lại được.
    public synchronized void acquire(String email, String ip) {
        Instant now = clock.instant();
        Deque<Instant> byEmail = recent(emailKey(email), now);
        Deque<Instant> byIp = recent(ipKey(ip), now);
        Duration emailWait = waitFor(byEmail, maxPerEmail, now);
        Duration ipWait = waitFor(byIp, maxPerIp, now);
        Duration wait = emailWait.compareTo(ipWait) >= 0 ? emailWait : ipWait;
        if (wait.isPositive()) {
            throw new TooManyLoginAttemptsException(wait);
        }
        byEmail.addLast(now);
        byIp.addLast(now);
    }

    // Đúng mật khẩu: xóa số lần sai của email, trả lại lần thử vừa tính cho địa chỉ.
    public synchronized void succeeded(String email, String ip) {
        attempts.remove(emailKey(email));
        undoLast(ipKey(ip));
    }

    // Không kiểm được mật khẩu vì lỗi máy chủ: không tính cho ai.
    public synchronized void release(String email, String ip) {
        undoLast(emailKey(email));
        undoLast(ipKey(ip));
    }

    private Deque<Instant> recent(String key, Instant now) {
        Deque<Instant> times = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        Instant expired = now.minus(window);
        while (!times.isEmpty() && !times.peekFirst().isAfter(expired)) {
            times.pollFirst();
        }
        return times;
    }

    private Duration waitFor(Deque<Instant> times, int max, Instant now) {
        return times.size() < max ? Duration.ZERO : Duration.between(now, times.peekFirst().plus(window));
    }

    private void undoLast(String key) {
        Deque<Instant> times = attempts.get(key);
        if (times != null) {
            times.pollLast();
        }
    }

    private static String emailKey(String email) {
        return "email:" + (email.length() > MAX_EMAIL_LENGTH ? email.substring(0, MAX_EMAIL_LENGTH) : email);
    }

    private static String ipKey(String ip) {
        return "ip:" + ip;
    }
}
