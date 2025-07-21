package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.product.domain.ProductDomainService;
import kr.hhplus.be.server.product.domain.ProductStats;
import kr.hhplus.be.server.product.domain.ProductStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetPopularProductsUseCase 단위 테스트")
class GetPopularProductsUseCaseTest {

    @Mock
    private ProductStatsRepository productStatsRepository;

    private GetPopularProductsUseCase getPopularProductsUseCase;

    @BeforeEach
    void setUp() {
        getPopularProductsUseCase = new GetPopularProductsUseCase(productStatsRepository);
    }

    @Test
    @DisplayName("인기 상품 조회 - 상위 3개")
    void getPopularProducts_Top3Products_ReturnsRankedProducts() {
        // given
        ProductStats stats1 = new ProductStats(1L, "상품1");
        stats1.setRecentSalesCount(100);
        stats1.setRecentSalesAmount(BigDecimal.valueOf(1000000));
        stats1.setTotalSalesCount(500);
        stats1.setTotalSalesAmount(BigDecimal.valueOf(5000000));
        stats1.setConversionRate(BigDecimal.valueOf(5.5));
        stats1.setLastOrderDate(LocalDateTime.now().minusHours(2));
        stats1.setRank(1);

        ProductStats stats2 = new ProductStats(2L, "상품2");
        stats2.setRecentSalesCount(200);
        stats2.setRecentSalesAmount(BigDecimal.valueOf(2000000));
        stats2.setTotalSalesCount(800);
        stats2.setTotalSalesAmount(BigDecimal.valueOf(8000000));
        stats2.setConversionRate(BigDecimal.valueOf(8.2));
        stats2.setLastOrderDate(LocalDateTime.now().minusHours(1));
        stats2.setRank(2);

        ProductStats stats3 = new ProductStats(3L, "상품3");
        stats3.setRecentSalesCount(50);
        stats3.setRecentSalesAmount(BigDecimal.valueOf(500000));
        stats3.setTotalSalesCount(200);
        stats3.setTotalSalesAmount(BigDecimal.valueOf(2000000));
        stats3.setConversionRate(BigDecimal.valueOf(2.5));
        stats3.setLastOrderDate(LocalDateTime.now().minusHours(3));
        stats3.setRank(3);

        List<ProductStats> allStats = Arrays.asList(stats1, stats2, stats3);
        List<ProductStats> popularStats = Arrays.asList(stats2, stats1, stats3); // 판매량 순으로 정렬

        when(productStatsRepository.findAll()).thenReturn(allStats);

        // when
        GetPopularProductsUseCase.Output result = getPopularProductsUseCase.execute(
                new GetPopularProductsUseCase.Input(3));

        // then
        assertThat(result.getPopularProducts()).hasSize(3);
        
        GetPopularProductsUseCase.PopularProductOutput firstProduct = result.getPopularProducts().get(0);
        assertThat(firstProduct.getProductId()).isEqualTo(2L);
        assertThat(firstProduct.getProductName()).isEqualTo("상품2");
        assertThat(firstProduct.getRank()).isEqualTo(2);
        assertThat(firstProduct.getRecentSalesCount()).isEqualTo(200);
        assertThat(firstProduct.getRecentSalesAmount()).isEqualTo(2000000L);
        
        verify(productStatsRepository).findAll();
    }

    @Test
    @DisplayName("인기 상품 조회 - 빈 결과")
    void getPopularProducts_NoProducts_ReturnsEmptyList() {
        // given
        when(productStatsRepository.findAll()).thenReturn(Collections.emptyList());

        // when
        GetPopularProductsUseCase.Output result = getPopularProductsUseCase.execute(
                new GetPopularProductsUseCase.Input(5));

        // then
        assertThat(result.getPopularProducts()).isEmpty();
        verify(productStatsRepository).findAll();
    }

    @Test
    @DisplayName("인기 상품 조회 - 상위 1개")
    void getPopularProducts_Top1Product_ReturnsSingleProduct() {
        // given
        ProductStats stats = new ProductStats(1L, "상품1");
        stats.setRecentSalesCount(100);
        stats.setRecentSalesAmount(BigDecimal.valueOf(1000000));
        stats.setTotalSalesCount(500);
        stats.setTotalSalesAmount(BigDecimal.valueOf(5000000));
        stats.setConversionRate(BigDecimal.valueOf(5.5));
        stats.setLastOrderDate(LocalDateTime.now().minusHours(2));
        stats.setRank(1);

        List<ProductStats> allStats = Collections.singletonList(stats);
        when(productStatsRepository.findAll()).thenReturn(allStats);

        // when
        GetPopularProductsUseCase.Output result = getPopularProductsUseCase.execute(
                new GetPopularProductsUseCase.Input(1));

        // then
        assertThat(result.getPopularProducts()).hasSize(1);
        
        GetPopularProductsUseCase.PopularProductOutput product = result.getPopularProducts().get(0);
        assertThat(product.getProductId()).isEqualTo(1L);
        assertThat(product.getProductName()).isEqualTo("상품1");
        assertThat(product.getRank()).isEqualTo(1);
        assertThat(product.getCurrentPrice()).isEqualTo(0); // 별도 조회 필요
        assertThat(product.getStock()).isEqualTo(0); // 별도 조회 필요
        
        verify(productStatsRepository).findAll();
    }
} 