package vn.spacex.common.time;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// Đồng hồ là một bean để test thay bằng đồng hồ cố định, không phải chờ hay so sánh với giờ thật.
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
