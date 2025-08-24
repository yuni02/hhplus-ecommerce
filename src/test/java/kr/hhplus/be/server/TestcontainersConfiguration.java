package kr.hhplus.be.server;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.sql.DataSource;

@TestConfiguration
public class TestcontainersConfiguration {

    // MySQL 컨테이너를 Bean으로 등록
    @Bean
    public MySQLContainer<?> mysqlContainer() {
        MySQLContainer<?> container = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("hhplus")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true) // 컨테이너 재사용 활성화
                .withInitScript("schema.sql"); // 초기 스키마 로드
        container.start();
        
        System.out.println("=== MySQL Container Started ===");
        System.out.println("JDBC URL: " + container.getJdbcUrl());
        System.out.println("Username: " + container.getUsername());
        System.out.println("Password: " + container.getPassword());
        System.out.println("Host: " + container.getHost());
        System.out.println("Port: " + container.getMappedPort(3306));
        System.out.println("===============================");
        
        return container;
    }

    // Redis 컨테이너를 Bean으로 등록
    @Bean
    public GenericContainer<?> redisContainer() {
        GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .withReuse(true); // 컨테이너 재사용 활성화
        container.start();
        
        System.out.println("=== Redis Container Started ===");
        System.out.println("Host: " + container.getHost());
        System.out.println("Port: " + container.getMappedPort(6379));
        System.out.println("===============================");
        
        return container;
    }

    // 테스트용 DataSource Bean
    @Bean
    @Primary
    public DataSource dataSource(MySQLContainer<?> mysqlContainer) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(mysqlContainer.getJdbcUrl() + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8");
        config.setUsername(mysqlContainer.getUsername());
        config.setPassword(mysqlContainer.getPassword());
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(5); // 연결 풀 크기 줄임
        config.setMinimumIdle(2); // 최소 연결 수 줄임
        config.setConnectionTimeout(10000); // 연결 타임아웃 줄임
        config.setIdleTimeout(300000); // 유휴 타임아웃 줄임
        config.setMaxLifetime(900000); // 최대 수명 줄임
        return new HikariDataSource(config);
    }

    // 테스트용 Redis ConnectionFactory Bean
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory(@Qualifier("redisContainer") GenericContainer<?> redisContainer) {
        return new LettuceConnectionFactory(redisContainer.getHost(), redisContainer.getMappedPort(6379));
    }

    // 테스트용 RedisTemplate Bean
    @Bean
    @Primary
    public org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        org.springframework.data.redis.core.RedisTemplate<String, Object> template = new org.springframework.data.redis.core.RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Key serializer
        template.setKeySerializer(new org.springframework.data.redis.serializer.StringRedisSerializer());
        template.setHashKeySerializer(new org.springframework.data.redis.serializer.StringRedisSerializer());
        
        // Value serializer
        template.setValueSerializer(new org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }

    // 테스트용 RedissonClient Bean - Redisson 설정을 Override
    @Bean
    @Primary
    public RedissonClient redissonClient(@Qualifier("redisContainer") GenericContainer<?> redisContainer) {
        Config config = new Config();
        String address = "redis://" + redisContainer.getHost() + ":" + redisContainer.getMappedPort(6379);
        config.useSingleServer()
                .setAddress(address)
                .setConnectionMinimumIdleSize(1)
                .setConnectionPoolSize(3) // 연결 풀 크기 줄임
                .setDatabase(0)
                .setDnsMonitoringInterval(10000) // DNS 모니터링 간격 늘림
                .setConnectTimeout(5000) // 연결 타임아웃 줄임
                .setRetryAttempts(2) // 재시도 횟수 줄임
                .setRetryInterval(1000); // 재시도 간격 줄임
        
        System.out.println("=== Redisson Test Config ===");
        System.out.println("Redis Address: " + address);
        System.out.println("============================");
        
        return Redisson.create(config);
    }
}