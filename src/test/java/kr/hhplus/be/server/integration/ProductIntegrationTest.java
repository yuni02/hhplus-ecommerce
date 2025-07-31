package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.product.application.GetProductDetailService;
import kr.hhplus.be.server.product.application.GetPopularProductsService;
import kr.hhplus.be.server.product.application.port.in.GetProductDetailUseCase;
import kr.hhplus.be.server.product.application.port.in.GetPopularProductsUseCase;
import kr.hhplus.be.server.product.infrastructure.persistence.entity.ProductEntity;
import kr.hhplus.be.server.product.infrastructure.persistence.repository.ProductJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Product 도메인 통합테스트")
class ProductIntegrationTest {

    @Autowired
    private GetProductDetailService getProductDetailService;

    @Autowired
    private GetPopularProductsService getPopularProductsService;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    private ProductEntity testProduct;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        productJpaRepository.deleteAll();

        // 테스트용 상품 생성
        testProduct = ProductEntity.builder()
                .name("테스트 상품")
                .description("테스트 상품 설명")
                .currentPrice(new BigDecimal("10000"))
                .stock(100)
                .status("ACTIVE")
                .build();
        testProduct = productJpaRepository.save(testProduct);
    }

    @Test
    @DisplayName("상품 상세 조회 성공")
    void 상품_상세_조회_성공() {
        // given
        
        Long productId = testProduct.getId();

        // when
        GetProductDetailUseCase.GetProductDetailCommand command = new GetProductDetailUseCase.GetProductDetailCommand(productId);
        Optional<GetProductDetailUseCase.GetProductDetailResult> result = getProductDetailService.getProductDetail(command);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(productId);
        assertThat(result.get().getName()).isEqualTo("테스트 상품");
        assertThat(result.get().getCurrentPrice()).isEqualTo(new BigDecimal("10000"));
        assertThat(result.get().getStock()).isEqualTo(100);
    }

    @Test
    @DisplayName("상품 상세 조회 실패 - 존재하지 않는 상품")
    void 상품_상세_조회_실패_존재하지_않는_상품() {
        // given
        Long nonExistentProductId = 9999L;

        // when
        GetProductDetailUseCase.GetProductDetailCommand command = new GetProductDetailUseCase.GetProductDetailCommand(nonExistentProductId);
        Optional<GetProductDetailUseCase.GetProductDetailResult> result = getProductDetailService.getProductDetail(command);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("인기 상품 조회 성공")
    void 인기_상품_조회_성공() {
        // when
        GetPopularProductsUseCase.GetPopularProductsCommand command = new GetPopularProductsUseCase.GetPopularProductsCommand(10);
        GetPopularProductsUseCase.GetPopularProductsResult result = getPopularProductsService.getPopularProducts(command);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPopularProducts()).isNotNull();
    }
} 