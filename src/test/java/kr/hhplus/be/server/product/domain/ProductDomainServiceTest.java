package kr.hhplus.be.server.product.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProductDomainService 단위 테스트")
class ProductDomainServiceTest {

    @Test
    @DisplayName("활성 상품 확인")
    void isActiveProduct_ActiveProduct_ReturnsTrue() {
        // given
        Product product = new Product("상품1", "상품1 설명", BigDecimal.valueOf(10000), 10, "전자제품");
        product.setStatus(Product.ProductStatus.ACTIVE);

        // when
        boolean result = ProductDomainService.isActiveProduct(product);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("비활성 상품 확인")
    void isActiveProduct_InactiveProduct_ReturnsFalse() {
        // given
        Product product = new Product("상품1", "상품1 설명", BigDecimal.valueOf(10000), 10, "전자제품");
        product.setStatus(Product.ProductStatus.INACTIVE);

        // when
        boolean result = ProductDomainService.isActiveProduct(product);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("재고 충분 여부 확인 - 충분한 경우")
    void hasSufficientStock_EnoughStock_ReturnsTrue() {
        // given
        Product product = new Product("상품1", "상품1 설명", BigDecimal.valueOf(10000), 10, "전자제품");
        Integer requiredQuantity = 5;

        // when
        boolean result = ProductDomainService.hasSufficientStock(product, requiredQuantity);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("재고 충분 여부 확인 - 부족한 경우")
    void hasSufficientStock_InsufficientStock_ReturnsFalse() {
        // given
        Product product = new Product("상품1", "상품1 설명", BigDecimal.valueOf(10000), 10, "전자제품");
        Integer requiredQuantity = 15;

        // when
        boolean result = ProductDomainService.hasSufficientStock(product, requiredQuantity);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("재고 차감")
    void decreaseStock_ValidQuantity_Success() {
        // given
        Product product = new Product("상품1", "상품1 설명", BigDecimal.valueOf(10000), 10, "전자제품");
        Integer quantity = 3;

        // when
        Product result = ProductDomainService.decreaseStock(product, quantity);

        // then
        assertThat(result.getStock()).isEqualTo(7);
    }

    @Test
    @DisplayName("재고 부족으로 차감 시 예외 발생")
    void decreaseStock_InsufficientStock_ThrowsException() {
        // given
        Product product = new Product("상품1", "상품1 설명", BigDecimal.valueOf(10000), 10, "전자제품");
        Integer quantity = 15;

        // when & then
        assertThatThrownBy(() -> ProductDomainService.decreaseStock(product, quantity))
                .isInstanceOf(Product.InsufficientStockException.class)
                .hasMessage("재고가 부족합니다.");
    }

    @Test
    @DisplayName("재고 증가")
    void increaseStock_ValidQuantity_Success() {
        // given
        Product product = new Product("상품1", "상품1 설명", BigDecimal.valueOf(10000), 10, "전자제품");
        Integer quantity = 5;

        // when
        Product result = ProductDomainService.increaseStock(product, quantity);

        // then
        assertThat(result.getStock()).isEqualTo(15);
    }

    @Test
    @DisplayName("인기 상품 순위 계산")
    void calculatePopularityRanking_ValidStats_ReturnsRankedList() {
        // given
        ProductStats stats1 = new ProductStats(1L, "상품1");
        stats1.setRecentSalesCount(100);

        ProductStats stats2 = new ProductStats(2L, "상품2");
        stats2.setRecentSalesCount(200);

        ProductStats stats3 = new ProductStats(3L, "상품3");
        stats3.setRecentSalesCount(50);

        List<ProductStats> allStats = Arrays.asList(stats1, stats2, stats3);
        int limit = 2;

        // when
        List<ProductStats> result = ProductDomainService.calculatePopularityRanking(allStats, limit);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProductId()).isEqualTo(2L); // 가장 많이 팔린 상품
        assertThat(result.get(0).getRank()).isEqualTo(1);
        assertThat(result.get(1).getProductId()).isEqualTo(1L);
        assertThat(result.get(1).getRank()).isEqualTo(2);
    }

    @Test
    @DisplayName("판매 통계 업데이트")
    void updateSalesStats_ValidData_Success() {
        // given
        ProductStats stats = new ProductStats(1L, "상품1");
        stats.setRecentSalesCount(10);
        stats.setRecentSalesAmount(BigDecimal.valueOf(100000));
        stats.setTotalSalesCount(50);
        stats.setTotalSalesAmount(BigDecimal.valueOf(500000));

        Integer quantity = 5;
        BigDecimal amount = BigDecimal.valueOf(50000);

        // when
        ProductStats result = ProductDomainService.updateSalesStats(stats, quantity, amount);

        // then
        assertThat(result.getRecentSalesCount()).isEqualTo(15);
        assertThat(result.getRecentSalesAmount()).isEqualTo(BigDecimal.valueOf(150000));
        assertThat(result.getTotalSalesCount()).isEqualTo(55);
        assertThat(result.getTotalSalesAmount()).isEqualTo(BigDecimal.valueOf(550000));
        assertThat(result.getLastOrderDate()).isNotNull();
        assertThat(result.getAggregationDate()).isNotNull();
    }

    @Test
    @DisplayName("전환율 계산")
    void calculateConversionRate_ValidData_Success() {
        // given
        ProductStats stats = new ProductStats(1L, "상품1");
        stats.setTotalSalesCount(50);
        Integer totalViews = 1000;

        // when
        ProductStats result = ProductDomainService.calculateConversionRate(stats, totalViews);

        // then
        assertThat(result.getConversionRate()).isEqualTo(BigDecimal.valueOf(5.0000)); // 50/1000 * 100
    }

    @Test
    @DisplayName("전환율 계산 - 조회수가 0인 경우")
    void calculateConversionRate_ZeroViews_NoChange() {
        // given
        ProductStats stats = new ProductStats(1L, "상품1");
        stats.setTotalSalesCount(50);
        Integer totalViews = 0;

        // when
        ProductStats result = ProductDomainService.calculateConversionRate(stats, totalViews);

        // then
        assertThat(result.getConversionRate()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("상품 가격 계산")
    void calculateTotalPrice_ValidData_Success() {
        // given
        Product product = new Product("상품1", "상품1 설명", BigDecimal.valueOf(10000), 10, "전자제품");
        Integer quantity = 3;

        // when
        BigDecimal result = ProductDomainService.calculateTotalPrice(product, quantity);

        // then
        assertThat(result).isEqualTo(BigDecimal.valueOf(30000));
    }

    @Test
    @DisplayName("유효한 상품 확인")
    void isValidProduct_ValidProduct_ReturnsTrue() {
        // given
        Product product = new Product("상품1", "상품1 설명", BigDecimal.valueOf(10000), 10, "전자제품");
        product.setStatus(Product.ProductStatus.ACTIVE);

        // when
        boolean result = ProductDomainService.isValidProduct(product);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("무효한 상품 확인 - null인 경우")
    void isValidProduct_NullProduct_ReturnsFalse() {
        // when
        boolean result = ProductDomainService.isValidProduct(null);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("무효한 상품 확인 - 비활성 상태인 경우")
    void isValidProduct_InactiveProduct_ReturnsFalse() {
        // given
        Product product = new Product("상품1", "상품1 설명", BigDecimal.valueOf(10000), 10, "전자제품");
        product.setStatus(Product.ProductStatus.INACTIVE);

        // when
        boolean result = ProductDomainService.isValidProduct(product);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("무효한 상품 확인 - 가격이 0인 경우")
    void isValidProduct_ZeroPrice_ReturnsFalse() {
        // given
        Product product = new Product("상품1", "상품1 설명", BigDecimal.ZERO, 10, "전자제품");
        product.setStatus(Product.ProductStatus.ACTIVE);

        // when
        boolean result = ProductDomainService.isValidProduct(product);

        // then
        assertThat(result).isFalse();
    }
} 