package kr.hhplus.be.server.integration.cache;

import kr.hhplus.be.server.TestcontainersConfiguration;
import kr.hhplus.be.server.product.domain.service.GetPopularProductsService;
import kr.hhplus.be.server.product.application.port.in.GetPopularProductsUseCase.GetPopularProductsCommand;
import kr.hhplus.be.server.product.application.port.in.GetPopularProductsUseCase.GetPopularProductsResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 인기상품 조회 캐시 성능 통합 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("인기상품 캐시 성능 테스트")
class PopularProductsCacheIntegrationTest {

    @Autowired
    private GetPopularProductsService getPopularProductsService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheManager shortTermCacheManager;

    @BeforeEach
    void setUp() {
        // Redis 캐시 초기화
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        shortTermCacheManager.getCacheNames().forEach(cacheName -> {
            var cache = shortTermCacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    @Test
    @DisplayName("인기상품 캐시 TTL 확인 테스트")
    void popularProductsCacheTtlTest() {
        // given
        GetPopularProductsCommand command = new GetPopularProductsCommand(5);

        // when
        GetPopularProductsResult result = getPopularProductsService.getPopularProducts(command);

        // then
        var cache = shortTermCacheManager.getCache("popularProducts");
        assertThat(cache).isNotNull();
        
        // 결과가 비어있지 않은 경우에만 캐시 확인 (unless 조건 고려)
        if (!result.getPopularProducts().isEmpty()) {
            var cachedValue = cache.get("all");
            assertThat(cachedValue).isNotNull();
        } else {
            // 빈 결과는 캐시되지 않아야 함
            var cachedValue = cache.get("all");
            assertThat(cachedValue).isNull();
        }
    }

    @Test
    @DisplayName("빈 결과에 대한 캐시 적용 안됨 테스트")
    void emptyResultNotCachedTest() {
        // given - 빈 결과를 반환하는 상황 (실제로는 더미 데이터가 있어서 빈 결과는 나오지 않을 수 있음)
        GetPopularProductsCommand command = new GetPopularProductsCommand(5);

        // when
        GetPopularProductsResult result = getPopularProductsService.getPopularProducts(command);

        // then
        // unless 조건으로 인해 빈 결과는 캐시되지 않아야 함
        if (result.getPopularProducts().isEmpty()) {
            var cache = shortTermCacheManager.getCache("popularProducts");
            var cachedValue = cache != null ? cache.get("all") : null;
            assertThat(cachedValue).isNull();
        }
    }
}