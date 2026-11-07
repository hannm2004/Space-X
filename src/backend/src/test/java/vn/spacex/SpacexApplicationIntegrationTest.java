package vn.spacex;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@Testcontainers
class SpacexApplicationIntegrationTest {

	@Container
	static final MySQLContainer mysql = new MySQLContainer("mysql:8.4");

	// Nguồn thuộc tính động được ưu tiên hơn application.properties, nên test không cần biến DB_* thật.
	@DynamicPropertySource
	static void cauHinhDatabase(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mysql::getJdbcUrl);
		registry.add("spring.datasource.username", mysql::getUsername);
		registry.add("spring.datasource.password", mysql::getPassword);
	}

	// Khởi động được nghĩa là Flyway đã chạy V1 và Hibernate (ddl-auto=validate) chấp nhận schema.
	@Test
	void contextLoads() {
	}

}
