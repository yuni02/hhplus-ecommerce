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

import javax.sql.DataSource;

@TestConfiguration
public class TestcontainersConfiguration {

    // MySQL 컨테이너를 Bean으로 등록
    @Bean
    public MySQLContainer<?> mysqlContainer() {
        MySQLContainer<?> container = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("hhplus")
                .withUsername("test")
                .withPassword("test");
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
                .withExposedPorts(6379);
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
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        return new HikariDataSource(config);
    }

    // 테스트용 Redis ConnectionFactory Bean
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory(GenericContainer<?> redisContainer) {
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
    public RedissonClient redissonClient(GenericContainer<?> redisContainer) {
        Config config = new Config();
        String address = "redis://" + redisContainer.getHost() + ":" + redisContainer.getMappedPort(6379);
        config.useSingleServer()
                .setAddress(address)
                .setConnectionMinimumIdleSize(1)
                .setConnectionPoolSize(5)
                .setDatabase(0)
                .setDnsMonitoringInterval(5000);
        
        System.out.println("=== Redisson Test Config ===");
        System.out.println("Redis Address: " + address);
        System.out.println("============================");
        
        return Redisson.create(config);
    }
}