package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetProductDetailUseCase 단위 테스트")
class GetProductDetailUseCaseTest {

    @Mock
    private ProductRepository productRepository;

    private GetProductDetailUseCase getProductDetailUseCase;

    @BeforeEach
    void setUp() {
        getProductDetailUseCase = new GetProductDetailUseCase(productRepository);
    }

    @Test
    @DisplayName("존재하는 상품 상세 조회")
    void getProductDetail_ExistingProduct_ReturnsProduct() {
        // given
        Long productId = 1L;
        Product product = new Product("상품1", "상품1 설명", BigDecimal.valueOf(10000), 10, "전자제품");
        product.setId(productId);
        product.setStatus(Product.ProductStatus.ACTIVE);
        product.setCreatedAt(LocalDateTime.now().minusDays(1));
        product.setUpdatedAt(LocalDateTime.now());

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // when
        Optional<GetProductDetailUseCase.Output> result = getProductDetailUseCase.execute(
                new GetProductDetailUseCase.Input(productId));

        // then
        assertThat(result).isPresent();
        GetProductDetailUseCase.Output output = result.get();
        assertThat(output.getId()).isEqualTo(productId);
        assertThat(output.getName()).isEqualTo("상품1");
        assertThat(output.getCurrentPrice()).isEqualTo(10000);
        assertThat(output.getStock()).isEqualTo(10);
        assertThat(output.getStatus()).isEqualTo("ACTIVE");
        verify(productRepository).findById(productId);
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회 시 빈 결과 반환")
    void getProductDetail_NonExistentProduct_ReturnsEmpty() {
        // given
        Long productId = 999L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // when
        Optional<GetProductDetailUseCase.Output> result = getProductDetailUseCase.execute(
                new GetProductDetailUseCase.Input(productId));

        // then
        assertThat(result).isEmpty();
        verify(productRepository).findById(productId);
    }

    @Test
    @DisplayName("비활성 상품 조회")
    void getProductDetail_InactiveProduct_ReturnsProduct() {
        // given
        Long productId = 1L;
        Product product = new Product("상품1", "상품1 설명", BigDecimal.valueOf(10000), 10, "전자제품");
        product.setId(productId);
        product.setStatus(Product.ProductStatus.INACTIVE);
        product.setCreatedAt(LocalDateTime.now().minusDays(1));
        product.setUpdatedAt(LocalDateTime.now());

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // when
        Optional<GetProductDetailUseCase.Output> result = getProductDetailUseCase.execute(
                new GetProductDetailUseCase.Input(productId));

        // then
        assertThat(result).isPresent();
        GetProductDetailUseCase.Output output = result.get();
        assertThat(output.getStatus()).isEqualTo("INACTIVE");
        verify(productRepository).findById(productId);
    }
} 