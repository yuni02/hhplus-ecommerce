package kr.hhplus.be.server.product.application.facade;

import kr.hhplus.be.server.product.application.port.in.GetPopularProductsUseCase;
import kr.hhplus.be.server.product.application.port.in.GetProductDetailUseCase;
import kr.hhplus.be.server.product.application.port.out.LoadProductPort;
import kr.hhplus.be.server.product.application.port.out.LoadProductStatsPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductFacadeTest {

    @Mock
    private LoadProductPort loadProductPort;
    
    @Mock
    private LoadProductStatsPort loadProductStatsPort;

    private ProductFacade productFacade;

    @BeforeEach
    void setUp() {
        productFacade = new ProductFacade(loadProductPort, loadProductStatsPort);
    }

    @Test
    @DisplayName("상품 상세 조회 성공")
    void getProductDetail_Success() {
        // given
        Long productId = 1L;
        
        GetProductDetailUseCase.GetProductDetailCommand command = 
            new GetProductDetailUseCase.GetProductDetailCommand(productId);
        
        LoadProductPort.ProductInfo productInfo = new LoadProductPort.ProductInfo(
                productId, "테스트 상품", "테스트 상품 설명", 10000, 50, "ACTIVE", "전자제품");
        
        LoadProductStatsPort.ProductStatsInfo statsInfo = new LoadProductStatsPort.ProductStatsInfo(
                productId, "테스트 상품", 10, 100000L, 100, 1000000L, 1, 0.8);
        
        when(loadProductPort.loadProductById(productId)).thenReturn(Optional.of(productInfo));
        when(loadProductStatsPort.loadProductStatsByProductId(productId)).thenReturn(Optional.of(statsInfo));

        // when
        Optional<GetProductDetailUseCase.GetProductDetailResult> result = productFacade.getProductDetail(command);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(productId);
        assertThat(result.get().getName()).isEqualTo("테스트 상품");
        assertThat(result.get().getCurrentPrice()).isEqualTo(10000);
        assertThat(result.get().getStock()).isEqualTo(50);
        assertThat(result.get().getStatus()).isEqualTo("ACTIVE");
        
        verify(loadProductPort).loadProductById(productId);
        verify(loadProductStatsPort).loadProductStatsByProductId(productId);
    }

    @Test
    @DisplayName("상품 상세 조회 실패 - 상품이 존재하지 않는 경우")
    void getProductDetail_Failure_ProductNotFound() {
        // given
        Long productId = 999L;
        
        GetProductDetailUseCase.GetProductDetailCommand command = 
            new GetProductDetailUseCase.GetProductDetailCommand(productId);
        
        when(loadProductPort.loadProductById(productId)).thenReturn(Optional.empty());

        // when
        Optional<GetProductDetailUseCase.GetProductDetailResult> result = productFacade.getProductDetail(command);

        // then
        assertThat(result).isEmpty();
        
        verify(loadProductPort).loadProductById(productId);
        verify(loadProductStatsPort, never()).loadProductStatsByProductId(any());
    }

    @Test
    @DisplayName("상품 상세 조회 성공 - 통계 정보가 없는 경우")
    void getProductDetail_Success_WithoutStats() {
        // given
        Long productId = 1L;
        
        GetProductDetailUseCase.GetProductDetailCommand command = 
            new GetProductDetailUseCase.GetProductDetailCommand(productId);
        
        LoadProductPort.ProductInfo productInfo = new LoadProductPort.ProductInfo(
                productId, "테스트 상품", "테스트 상품 설명", 10000, 50, "ACTIVE", "전자제품");
        
        when(loadProductPort.loadProductById(productId)).thenReturn(Optional.of(productInfo));
        when(loadProductStatsPort.loadProductStatsByProductId(productId)).thenReturn(Optional.empty());

        // when
        Optional<GetProductDetailUseCase.GetProductDetailResult> result = productFacade.getProductDetail(command);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(productId);
        assertThat(result.get().getName()).isEqualTo("테스트 상품");
        assertThat(result.get().getCurrentPrice()).isEqualTo(10000);
        assertThat(result.get().getStock()).isEqualTo(50);
        assertThat(result.get().getStatus()).isEqualTo("ACTIVE");
        
        verify(loadProductPort).loadProductById(productId);
        verify(loadProductStatsPort).loadProductStatsByProductId(productId);
    }

    @Test
    @DisplayName("인기 상품 조회 성공")
    void getPopularProducts_Success() {
        // given
        int limit = 5;
        
        GetPopularProductsUseCase.GetPopularProductsCommand command = 
            new GetPopularProductsUseCase.GetPopularProductsCommand(limit);
        
        LoadProductStatsPort.ProductStatsInfo statsInfo1 = new LoadProductStatsPort.ProductStatsInfo(
                1L, "인기 상품 1", 20, 200000L, 200, 2000000L, 1, 0.9);
        LoadProductStatsPort.ProductStatsInfo statsInfo2 = new LoadProductStatsPort.ProductStatsInfo(
                2L, "인기 상품 2", 15, 150000L, 150, 1500000L, 2, 0.8);
        
        LoadProductPort.ProductInfo productInfo1 = new LoadProductPort.ProductInfo(
                1L, "인기 상품 1", "인기 상품 1 설명", 10000, 100, "ACTIVE", "전자제품");
        LoadProductPort.ProductInfo productInfo2 = new LoadProductPort.ProductInfo(
                2L, "인기 상품 2", "인기 상품 2 설명", 8000, 80, "ACTIVE", "전자제품");
        
        when(loadProductStatsPort.loadTopProductsBySales(limit)).thenReturn(List.of(statsInfo1, statsInfo2));
        when(loadProductPort.loadProductById(1L)).thenReturn(Optional.of(productInfo1));
        when(loadProductPort.loadProductById(2L)).thenReturn(Optional.of(productInfo2));

        // when
        GetPopularProductsUseCase.GetPopularProductsResult result = productFacade.getPopularProducts(command);

        // then
        assertThat(result.getPopularProducts()).hasSize(2);
        
        GetPopularProductsUseCase.PopularProductInfo firstProduct = result.getPopularProducts().get(0);
        assertThat(firstProduct.getProductId()).isEqualTo(1L);
        assertThat(firstProduct.getProductName()).isEqualTo("인기 상품 1");
        assertThat(firstProduct.getCurrentPrice()).isEqualTo(10000);
        assertThat(firstProduct.getStock()).isEqualTo(100);
        assertThat(firstProduct.getTotalSalesCount()).isEqualTo(200);
        assertThat(firstProduct.getTotalSalesAmount()).isEqualTo(2000000L);
        assertThat(firstProduct.getRecentSalesCount()).isEqualTo(20);
        assertThat(firstProduct.getRecentSalesAmount()).isEqualTo(200000L);
        assertThat(firstProduct.getConversionRate()).isEqualTo(0.9);
        assertThat(firstProduct.getRank()).isEqualTo(1);
        
        GetPopularProductsUseCase.PopularProductInfo secondProduct = result.getPopularProducts().get(1);
        assertThat(secondProduct.getProductId()).isEqualTo(2L);
        assertThat(secondProduct.getProductName()).isEqualTo("인기 상품 2");
        assertThat(secondProduct.getCurrentPrice()).isEqualTo(8000);
        assertThat(secondProduct.getStock()).isEqualTo(80);
        assertThat(secondProduct.getTotalSalesCount()).isEqualTo(150);
        assertThat(secondProduct.getTotalSalesAmount()).isEqualTo(1500000L);
        assertThat(secondProduct.getRecentSalesCount()).isEqualTo(15);
        assertThat(secondProduct.getRecentSalesAmount()).isEqualTo(150000L);
        assertThat(secondProduct.getConversionRate()).isEqualTo(0.8);
        assertThat(secondProduct.getRank()).isEqualTo(2);
        
        verify(loadProductStatsPort).loadTopProductsBySales(limit);
        verify(loadProductPort).loadProductById(1L);
        verify(loadProductPort).loadProductById(2L);
    }

    @Test
    @DisplayName("인기 상품 조회 성공 - 상품 정보가 없는 경우")
    void getPopularProducts_Success_WithMissingProductInfo() {
        // given
        int limit = 5;
        
        GetPopularProductsUseCase.GetPopularProductsCommand command = 
            new GetPopularProductsUseCase.GetPopularProductsCommand(limit);
        
        LoadProductStatsPort.ProductStatsInfo statsInfo = new LoadProductStatsPort.ProductStatsInfo(
                1L, "인기 상품 1", 20, 200000L, 200, 2000000L, 1, 0.9);
        
        when(loadProductStatsPort.loadTopProductsBySales(limit)).thenReturn(List.of(statsInfo));
        when(loadProductPort.loadProductById(1L)).thenReturn(Optional.empty());

        // when
        GetPopularProductsUseCase.GetPopularProductsResult result = productFacade.getPopularProducts(command);

        // then
        assertThat(result.getPopularProducts()).hasSize(1);
        
        GetPopularProductsUseCase.PopularProductInfo product = result.getPopularProducts().get(0);
        assertThat(product.getProductId()).isEqualTo(1L);
        assertThat(product.getProductName()).isEqualTo("알 수 없는 상품");
        assertThat(product.getCurrentPrice()).isEqualTo(0);
        assertThat(product.getStock()).isEqualTo(0);
        assertThat(product.getTotalSalesCount()).isEqualTo(200);
        assertThat(product.getTotalSalesAmount()).isEqualTo(2000000L);
        
        verify(loadProductStatsPort).loadTopProductsBySales(limit);
        verify(loadProductPort).loadProductById(1L);
    }

    @Test
    @DisplayName("인기 상품 조회 성공 - 빈 결과")
    void getPopularProducts_Success_EmptyResult() {
        // given
        int limit = 5;
        
        GetPopularProductsUseCase.GetPopularProductsCommand command = 
            new GetPopularProductsUseCase.GetPopularProductsCommand(limit);
        
        when(loadProductStatsPort.loadTopProductsBySales(limit)).thenReturn(List.of());

        // when
        GetPopularProductsUseCase.GetPopularProductsResult result = productFacade.getPopularProducts(command);

        // then
        assertThat(result.getPopularProducts()).isEmpty();
        
        verify(loadProductStatsPort).loadTopProductsBySales(limit);
        verify(loadProductPort, never()).loadProductById(any());
    }
} 