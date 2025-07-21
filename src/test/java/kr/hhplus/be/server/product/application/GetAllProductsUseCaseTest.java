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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetAllProductsUseCase 단위 테스트")
class GetAllProductsUseCaseTest {

    @Mock
    private ProductRepository productRepository;

    private GetAllProductsUseCase getAllProductsUseCase;

    @BeforeEach
    void setUp() {
        getAllProductsUseCase = new GetAllProductsUseCase(productRepository);
    }

    @Test
    @DisplayName("활성 상품 목록 조회")
    void getAllProducts_ActiveProducts_ReturnsProducts() {
        // given
        Product product1 = new Product("상품1", "상품1 설명", BigDecimal.valueOf(10000), 10, "전자제품");
        product1.setId(1L);
        product1.setStatus(Product.ProductStatus.ACTIVE);

        Product product2 = new Product("상품2", "상품2 설명", BigDecimal.valueOf(20000), 5, "의류");
        product2.setId(2L);
        product2.setStatus(Product.ProductStatus.ACTIVE);

        List<Product> expectedProducts = Arrays.asList(product1, product2);

        when(productRepository.findAllActive()).thenReturn(expectedProducts);

        // when
        List<Product> result = getAllProductsUseCase.execute();

        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(product1, product2);
        verify(productRepository).findAllActive();
    }

    @Test
    @DisplayName("활성 상품이 없는 경우 빈 목록 반환")
    void getAllProducts_NoActiveProducts_ReturnsEmptyList() {
        // given
        when(productRepository.findAllActive()).thenReturn(Collections.emptyList());

        // when
        List<Product> result = getAllProductsUseCase.execute();

        // then
        assertThat(result).isEmpty();
        verify(productRepository).findAllActive();
    }

    @Test
    @DisplayName("단일 활성 상품 조회")
    void getAllProducts_SingleActiveProduct_ReturnsProduct() {
        // given
        Product product = new Product("상품1", "상품1 설명", BigDecimal.valueOf(10000), 10, "전자제품");
        product.setId(1L);
        product.setStatus(Product.ProductStatus.ACTIVE);

        List<Product> expectedProducts = Collections.singletonList(product);

        when(productRepository.findAllActive()).thenReturn(expectedProducts);

        // when
        List<Product> result = getAllProductsUseCase.execute();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(product);
        verify(productRepository).findAllActive();
    }
} 