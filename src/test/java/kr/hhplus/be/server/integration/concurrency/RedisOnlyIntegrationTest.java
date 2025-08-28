package kr.hhplus.be.server.integration.concurrency;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RedisOnlyIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withStartupTimeout(Duration.ofMinutes(2));

    private RedisTemplate<String, Object> redisTemplate;
    private RedisConnectionFactory connectionFactory;

    @BeforeEach
    void setUp() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        factory.afterPropertiesSet();
        this.connectionFactory = factory;

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        this.redisTemplate = template;
    }

    @AfterEach
    void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.getConnection().serverCommands().flushAll();
        }
    }

    @Test
    void redis_연결_테스트() {
        // given
        String key = "test:key";
        String value = "test_value";

        // when
        redisTemplate.opsForValue().set(key, value);

        // then
        Object result = redisTemplate.opsForValue().get(key);
        assertThat(result).isEqualTo(value);
    }

    @Test
    void redis_만료시간_테스트() throws InterruptedException {
        // given
        String key = "test:expire";
        String value = "expire_value";
        Duration expireTime = Duration.ofSeconds(1);

        // when
        redisTemplate.opsForValue().set(key, value, expireTime);

        // then
        Object result = redisTemplate.opsForValue().get(key);
        assertThat(result).isEqualTo(value);

        // 만료 시간 대기
        Thread.sleep(1100);

        // 만료 후 확인
        Object expiredResult = redisTemplate.opsForValue().get(key);
        assertThat(expiredResult).isNull();
    }

    @Test
    void redis_해시_테스트() {
        // given
        String hashKey = "test:hash";
        String field = "field1";
        String value = "hash_value";

        // when
        redisTemplate.opsForHash().put(hashKey, field, value);

        // then
        Object result = redisTemplate.opsForHash().get(hashKey, field);
        assertThat(result).isEqualTo(value);
    }

    @Test
    void redis_리스트_테스트() {
        // given
        String listKey = "test:list";
        String value1 = "value1";
        String value2 = "value2";

        // when
        redisTemplate.opsForList().rightPush(listKey, value1);
        redisTemplate.opsForList().rightPush(listKey, value2);

        // then
        Object result1 = redisTemplate.opsForList().leftPop(listKey);
        Object result2 = redisTemplate.opsForList().leftPop(listKey);
        
        assertThat(result1).isEqualTo(value1);
        assertThat(result2).isEqualTo(value2);
    }
}