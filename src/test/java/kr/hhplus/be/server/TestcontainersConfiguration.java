package kr.hhplus.be.server;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@Configuration
public class TestcontainersConfiguration {

    private static MySQLContainer<?> mysqlContainer;
    private static final Object lock = new Object();

    @Bean
    @ServiceConnection
    public MySQLContainer<?> mysqlContainer() {
        synchronized (lock) {
            if (mysqlContainer == null) {
                mysqlContainer = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                        .withDatabaseName("hhplus")
                        .withUsername("test")
                        .withPassword("test")
                        .withReuse(true);
                mysqlContainer.start();
            }
        }
        return mysqlContainer;
    }

    @PreDestroy
    public void preDestroy() {
        synchronized (lock) {
            if (mysqlContainer != null && mysqlContainer.isRunning()) {
                mysqlContainer.stop();
            }
        }
    }
}