package kr.hhplus.be.server.product.infrastructure;

import kr.hhplus.be.server.product.application.GetProductDetailUseCase;
import kr.hhplus.be.server.product.application.GetPopularProductsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductAdapter 단위 테스트")
class ProductAdapterTest {

    private ProductAdapter productAdapter;

    @BeforeEach
    void setUp() {
        productAdapter = new ProductAdapter();
    }

    @Test
    @DisplayName("상품 상세 조회 요청 변환 - 유효한 ID")
    void adaptGetProductDetailRequest_ValidId_ReturnsInput() {
        // given
        Long productId = 1L;

        // when
        GetProductDetailUseCase.Input result = productAdapter.adaptGetProductDetailRequest(productId);

        // then
        assertThat(result.getProductId()).isEqualTo(productId);
    }

    @Test
    @DisplayName("상품 상세 조회 요청 변환 - null ID")
    void adaptGetProductDetailRequest_NullId_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> productAdapter.adaptGetProductDetailRequest(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 상품 ID입니다.");
    }

    @Test
    @DisplayName("상품 상세 조회 요청 변환 - 음수 ID")
    void adaptGetProductDetailRequest_NegativeId_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> productAdapter.adaptGetProductDetailRequest(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 상품 ID입니다.");
    }

    @Test
    @DisplayName("상품 상세 조회 응답 변환")
    void adaptGetProductDetailResponse_ValidOutput_ReturnsMap() {
        // given
        LocalDateTime now = LocalDateTime.now();
        GetProductDetailUseCase.Output output = new GetProductDetailUseCase.Output(
                1L, "상품1", 10000, 10, "ACTIVE", now, now);

        // when
        Map<String, Object> result = productAdapter.adaptGetProductDetailResponse(output);

        // then
        assertThat(result).containsKey("message");
        assertThat(result).containsKey("product");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> product = (Map<String, Object>) result.get("product");
        assertThat(product.get("id")).isEqualTo(1L);
        assertThat(product.get("name")).isEqualTo("상품1");
        assertThat(product.get("currentPrice")).isEqualTo(10000);
        assertThat(product.get("stock")).isEqualTo(10);
        assertThat(product.get("status")).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("인기 상품 조회 요청 변환 - 유효한 limit")
    void adaptGetPopularProductsRequest_ValidLimit_ReturnsInput() {
        // given
        int limit = 5;

        // when
        GetPopularProductsUseCase.Input result = productAdapter.adaptGetPopularProductsRequest(limit);

        // then
        assertThat(result.getLimit()).isEqualTo(limit);
    }

    @Test
    @DisplayName("인기 상품 조회 요청 변환 - 0 limit")
    void adaptGetPopularProductsRequest_ZeroLimit_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> productAdapter.adaptGetPopularProductsRequest(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("조회 개수는 양수여야 합니다.");
    }

    @Test
    @DisplayName("인기 상품 조회 요청 변환 - 음수 limit")
    void adaptGetPopularProductsRequest_NegativeLimit_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> productAdapter.adaptGetPopularProductsRequest(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("조회 개수는 양수여야 합니다.");
    }

    @Test
    @DisplayName("인기 상품 조회 응답 변환 - 상품이 있는 경우")
    void adaptGetPopularProductsResponse_WithProducts_ReturnsMap() {
        // given
        LocalDateTime now = LocalDateTime.now();
        GetPopularProductsUseCase.PopularProductOutput product = new GetPopularProductsUseCase.PopularProductOutput(
                1L, "상품1", 10000, 10, 500, 5000000L, 100, 1000000L, 5.5, now, 1);
        
        GetPopularProductsUseCase.Output output = new GetPopularProductsUseCase.Output(
                List.of(product));

        // when
        Map<String, Object> result = productAdapter.adaptGetPopularProductsResponse(output);

        // then
        assertThat(result).containsKey("message");
        assertThat(result).containsKey("stats");
        assertThat(result).containsKey("popularProducts");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) result.get("stats");
        assertThat(stats.get("totalProducts")).isEqualTo(1);
        assertThat(stats.get("totalRecentSales")).isEqualTo(100);
        assertThat(stats.get("totalRecentRevenue")).isEqualTo(1000000L);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> products = (List<Map<String, Object>>) result.get("popularProducts");
        assertThat(products).hasSize(1);
        assertThat(products.get(0).get("productId")).isEqualTo(1L);
        assertThat(products.get(0).get("productName")).isEqualTo("상품1");
    }

    @Test
    @DisplayName("인기 상품 조회 응답 변환 - 빈 결과")
    void adaptGetPopularProductsResponse_EmptyProducts_ReturnsMap() {
        // given
        GetPopularProductsUseCase.Output output = new GetPopularProductsUseCase.Output(
                List.of());

        // when
        Map<String, Object> result = productAdapter.adaptGetPopularProductsResponse(output);

        // then
        assertThat(result).containsKey("message");
        assertThat(result).containsKey("stats");
        assertThat(result).containsKey("popularProducts");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) result.get("stats");
        assertThat(stats.get("totalProducts")).isEqualTo(0);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> products = (List<Map<String, Object>>) result.get("popularProducts");
        assertThat(products).isEmpty();
    }
} 