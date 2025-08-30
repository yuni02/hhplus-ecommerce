package kr.hhplus.be.server.unit.product.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.hhplus.be.server.product.application.GetPopularProductsService;
import kr.hhplus.be.server.product.application.port.out.LoadProductPort;
import kr.hhplus.be.server.product.application.port.in.GetPopularProductsUseCase;
import kr.hhplus.be.server.product.application.port.in.ProductRankingUseCase;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetPopularProductsServiceTest {

    @Mock
    private LoadProductPort loadProductPort;
    
    @Mock
    private ProductRankingUseCase productRankingService;

    private GetPopularProductsService getPopularProductsService;    

    @BeforeEach
    void setUp() {
        getPopularProductsService = new GetPopularProductsService(loadProductPort, productRankingService);
    }

    @Test
    @DisplayName("인기 상품 조회 성공")
    void getPopularProducts_Success() {
        // given
        int limit = 5;
        GetPopularProductsUseCase.GetPopularProductsCommand command = 
            new GetPopularProductsUseCase.GetPopularProductsCommand(limit);

        List<Long> topProductIds = List.of(1L, 2L);
        
        LocalDateTime now = LocalDateTime.now();
        LoadProductPort.ProductInfo productInfo1 = new LoadProductPort.ProductInfo(
            1L, "상품A", "상품A 설명", 10000, 100, "ACTIVE", "전자제품", now, now);
        LoadProductPort.ProductInfo productInfo2 = new LoadProductPort.ProductInfo(
            2L, "상품B", "상품B 설명", 15000, 50, "ACTIVE", "의류", now, now);

        when(productRankingService.getTopProductIds(limit)).thenReturn(topProductIds);
        when(productRankingService.getProductRankingInfo(1L))
            .thenReturn(new ProductRankingUseCase.ProductRankingInfo(0L, 100.0));
        when(productRankingService.getProductRankingInfo(2L))
            .thenReturn(new ProductRankingUseCase.ProductRankingInfo(1L, 80.0));
        when(loadProductPort.loadProductById(1L)).thenReturn(Optional.of(productInfo1));
        when(loadProductPort.loadProductById(2L)).thenReturn(Optional.of(productInfo2));

        // when
        GetPopularProductsUseCase.GetPopularProductsResult result = 
            getPopularProductsService.getPopularProducts(command);

        // then
        assertThat(result.getPopularProducts()).hasSize(2);
        
        GetPopularProductsUseCase.PopularProductInfo product1 = result.getPopularProducts().get(0);
        assertThat(product1.getProductId()).isEqualTo(1L);
        assertThat(product1.getProductName()).isEqualTo("상품A");
        assertThat(product1.getCurrentPrice()).isEqualTo(10000);
        assertThat(product1.getStock()).isEqualTo(100);
        assertThat(product1.getRecentSalesCount()).isEqualTo(100);
        assertThat(product1.getRank()).isEqualTo(1); // Redis rank 0 + 1
        
        GetPopularProductsUseCase.PopularProductInfo product2 = result.getPopularProducts().get(1);
        assertThat(product2.getProductId()).isEqualTo(2L);
        assertThat(product2.getProductName()).isEqualTo("상품B");
        assertThat(product2.getCurrentPrice()).isEqualTo(15000);
        assertThat(product2.getStock()).isEqualTo(50);
        assertThat(product2.getRecentSalesCount()).isEqualTo(80);
        assertThat(product2.getRank()).isEqualTo(2); // Redis rank 1 + 1
        
        verify(productRankingService).getTopProductIds(limit);
        verify(productRankingService).getProductRankingInfo(1L);
        verify(productRankingService).getProductRankingInfo(2L);
        verify(loadProductPort).loadProductById(1L);
        verify(loadProductPort).loadProductById(2L);
    }

    @Test
    @DisplayName("인기 상품 조회 성공 - 빈 결과")
    void getPopularProducts_Success_EmptyResult() {
        // given
        int limit = 5;
        GetPopularProductsUseCase.GetPopularProductsCommand command = 
            new GetPopularProductsUseCase.GetPopularProductsCommand(limit);

        when(productRankingService.getTopProductIds(limit)).thenReturn(List.of());

        // when
        GetPopularProductsUseCase.GetPopularProductsResult result = 
            getPopularProductsService.getPopularProducts(command);

        // then
        assertThat(result.getPopularProducts()).isEmpty();
        
        verify(productRankingService).getTopProductIds(limit);
        verify(productRankingService, never()).getProductRank(anyLong());
        verify(productRankingService, never()).getProductSalesScore(anyLong());
        verify(loadProductPort, never()).loadProductById(anyLong());
    }

    @Test
    @DisplayName("인기 상품 조회 성공 - 상품 정보가 없는 경우")
    void getPopularProducts_Success_WithMissingProductInfo() {
        // given
        int limit = 5;
        GetPopularProductsUseCase.GetPopularProductsCommand command = 
            new GetPopularProductsUseCase.GetPopularProductsCommand(limit);

        List<Long> topProductIds = List.of(999L);
        
        when(productRankingService.getTopProductIds(limit)).thenReturn(topProductIds);
        // getProductRankingInfo Mock 제거 - 상품 정보가 없으면 호출되지 않음
        when(loadProductPort.loadProductById(999L)).thenReturn(Optional.empty());

        // when
        GetPopularProductsUseCase.GetPopularProductsResult result = 
            getPopularProductsService.getPopularProducts(command);

        // then
        assertThat(result.getPopularProducts()).isEmpty(); // 상품 정보가 없으면 필터링됨
        
        verify(productRankingService).getTopProductIds(limit);
        verify(productRankingService, never()).getProductRankingInfo(999L); // 상품 정보가 없으므로 호출되지 않음
        verify(loadProductPort).loadProductById(999L);
    }

    @Test
    @DisplayName("인기 상품 조회 성공 - limit 1개")
    void getPopularProducts_Success_LimitOne() {
        // given
        int limit = 1;
        GetPopularProductsUseCase.GetPopularProductsCommand command = 
            new GetPopularProductsUseCase.GetPopularProductsCommand(limit);

        List<Long> topProductIds = List.of(1L);
        
        LocalDateTime now = LocalDateTime.now();
        LoadProductPort.ProductInfo productInfo = new LoadProductPort.ProductInfo(
            1L, "최고 인기 상품", "최고 인기 상품 설명", 20000, 200, "ACTIVE", "베스트", now, now);

        when(productRankingService.getTopProductIds(limit)).thenReturn(topProductIds);
        when(productRankingService.getProductRankingInfo(1L))
            .thenReturn(new ProductRankingUseCase.ProductRankingInfo(0L, 200.0));
        when(loadProductPort.loadProductById(1L)).thenReturn(Optional.of(productInfo));

        // when
        GetPopularProductsUseCase.GetPopularProductsResult result = 
            getPopularProductsService.getPopularProducts(command);

        // then
        assertThat(result.getPopularProducts()).hasSize(1);
        
        GetPopularProductsUseCase.PopularProductInfo product = result.getPopularProducts().get(0);
        assertThat(product.getProductId()).isEqualTo(1L);
        assertThat(product.getProductName()).isEqualTo("최고 인기 상품");
        assertThat(product.getCurrentPrice()).isEqualTo(20000);
        assertThat(product.getStock()).isEqualTo(200);
        assertThat(product.getRecentSalesCount()).isEqualTo(200);
        assertThat(product.getRank()).isEqualTo(1);
        
        verify(productRankingService).getTopProductIds(limit);
        verify(productRankingService).getProductRankingInfo(1L);
        verify(loadProductPort).loadProductById(1L);
    }

    @Test
    @DisplayName("인기 상품 조회 성공 - 높은 limit 값")
    void getPopularProducts_Success_HighLimit() {
        // given
        int limit = 100;
        GetPopularProductsUseCase.GetPopularProductsCommand command = 
            new GetPopularProductsUseCase.GetPopularProductsCommand(limit);

        List<Long> topProductIds = List.of(1L, 2L, 3L);
        
        LocalDateTime now = LocalDateTime.now();
        LoadProductPort.ProductInfo productInfo1 = new LoadProductPort.ProductInfo(
            1L, "상품1", "상품1 설명", 10000, 100, "ACTIVE", "카테고리1", now, now);
        LoadProductPort.ProductInfo productInfo2 = new LoadProductPort.ProductInfo(
            2L, "상품2", "상품2 설명", 12000, 80, "ACTIVE", "카테고리2", now, now);
        LoadProductPort.ProductInfo productInfo3 = new LoadProductPort.ProductInfo(
            3L, "상품3", "상품3 설명", 8000, 60, "ACTIVE", "카테고리3", now, now);

        when(productRankingService.getTopProductIds(limit)).thenReturn(topProductIds);
        when(productRankingService.getProductRankingInfo(1L))
            .thenReturn(new ProductRankingUseCase.ProductRankingInfo(0L, 50.0));
        when(productRankingService.getProductRankingInfo(2L))
            .thenReturn(new ProductRankingUseCase.ProductRankingInfo(1L, 40.0));
        when(productRankingService.getProductRankingInfo(3L))
            .thenReturn(new ProductRankingUseCase.ProductRankingInfo(2L, 30.0));
        when(loadProductPort.loadProductById(1L)).thenReturn(Optional.of(productInfo1));
        when(loadProductPort.loadProductById(2L)).thenReturn(Optional.of(productInfo2));
        when(loadProductPort.loadProductById(3L)).thenReturn(Optional.of(productInfo3));

        // when
        GetPopularProductsUseCase.GetPopularProductsResult result = 
            getPopularProductsService.getPopularProducts(command);

        // then
        assertThat(result.getPopularProducts()).hasSize(3);
        
        // 순위별로 정렬되어 있는지 확인
        assertThat(result.getPopularProducts().get(0).getRank()).isEqualTo(1);
        assertThat(result.getPopularProducts().get(1).getRank()).isEqualTo(2);
        assertThat(result.getPopularProducts().get(2).getRank()).isEqualTo(3);
        
        verify(productRankingService).getTopProductIds(limit);
        verify(productRankingService).getProductRankingInfo(1L);
        verify(productRankingService).getProductRankingInfo(2L);
        verify(productRankingService).getProductRankingInfo(3L);
        verify(loadProductPort).loadProductById(1L);
        verify(loadProductPort).loadProductById(2L);
        verify(loadProductPort).loadProductById(3L);
    }

    @Test
    @DisplayName("인기 상품 조회 실패 - 예외 발생")
    void getPopularProducts_Failure_Exception() {
        // given
        int limit = 5;
        GetPopularProductsUseCase.GetPopularProductsCommand command = 
            new GetPopularProductsUseCase.GetPopularProductsCommand(limit);

        when(productRankingService.getTopProductIds(limit))
            .thenThrow(new RuntimeException("Redis 연결 오류"));

        // when
        GetPopularProductsUseCase.GetPopularProductsResult result = 
            getPopularProductsService.getPopularProducts(command);

        // then
        assertThat(result.getPopularProducts()).isEmpty();
        
        verify(productRankingService).getTopProductIds(limit);
        verify(productRankingService, never()).getProductRank(anyLong());
        verify(productRankingService, never()).getProductSalesScore(anyLong());
        verify(loadProductPort, never()).loadProductById(anyLong());
    }

    @Test
    @DisplayName("인기 상품 조회 성공 - 0건 limit")
    void getPopularProducts_Success_ZeroLimit() {
        // given
        int limit = 0;
        GetPopularProductsUseCase.GetPopularProductsCommand command = 
            new GetPopularProductsUseCase.GetPopularProductsCommand(limit);

        when(productRankingService.getTopProductIds(limit)).thenReturn(List.of());

        // when
        GetPopularProductsUseCase.GetPopularProductsResult result = 
            getPopularProductsService.getPopularProducts(command);

        // then
        assertThat(result.getPopularProducts()).isEmpty();
        
        verify(productRankingService).getTopProductIds(limit);
        verify(productRankingService, never()).getProductRank(anyLong());
        verify(productRankingService, never()).getProductSalesScore(anyLong());
        verify(loadProductPort, never()).loadProductById(anyLong());
    }

    @Test
    @DisplayName("인기 상품 조회 성공 - 부분적 상품 정보 누락")
    void getPopularProducts_Success_PartialProductInfoMissing() {
        // given
        int limit = 3;
        GetPopularProductsUseCase.GetPopularProductsCommand command = 
            new GetPopularProductsUseCase.GetPopularProductsCommand(limit);

        List<Long> topProductIds = List.of(1L, 2L, 999L);
        
        LocalDateTime now = LocalDateTime.now();
        LoadProductPort.ProductInfo productInfo1 = new LoadProductPort.ProductInfo(
            1L, "상품1", "상품1 설명", 10000, 100, "ACTIVE", "카테고리1", now, now);
        LoadProductPort.ProductInfo productInfo2 = new LoadProductPort.ProductInfo(
            2L, "상품2", "상품2 설명", 12000, 80, "ACTIVE", "카테고리2", now, now);

        when(productRankingService.getTopProductIds(limit)).thenReturn(topProductIds);
        when(productRankingService.getProductRankingInfo(1L))
            .thenReturn(new ProductRankingUseCase.ProductRankingInfo(0L, 50.0));
        when(productRankingService.getProductRankingInfo(2L))
            .thenReturn(new ProductRankingUseCase.ProductRankingInfo(1L, 40.0));
        // 999L에 대한 getProductRankingInfo Mock 제거 - 상품 정보가 없으면 호출되지 않음
        when(loadProductPort.loadProductById(1L)).thenReturn(Optional.of(productInfo1));
        when(loadProductPort.loadProductById(2L)).thenReturn(Optional.of(productInfo2));
        when(loadProductPort.loadProductById(999L)).thenReturn(Optional.empty()); // 삭제된 상품

        // when
        GetPopularProductsUseCase.GetPopularProductsResult result = 
            getPopularProductsService.getPopularProducts(command);

        // then
        assertThat(result.getPopularProducts()).hasSize(2); // 삭제된 상품은 필터링됨
        
        // 첫 번째와 두 번째는 정상 상품
        assertThat(result.getPopularProducts().get(0).getCurrentPrice()).isEqualTo(10000);
        assertThat(result.getPopularProducts().get(1).getCurrentPrice()).isEqualTo(12000);
        
        verify(productRankingService).getTopProductIds(limit);
        verify(productRankingService).getProductRankingInfo(1L);
        verify(productRankingService).getProductRankingInfo(2L);
        verify(productRankingService, never()).getProductRankingInfo(999L); // 상품 정보가 없으므로 호출되지 않음
        verify(loadProductPort).loadProductById(1L);
        verify(loadProductPort).loadProductById(2L);
        verify(loadProductPort).loadProductById(999L);
    }

    @Test
    @DisplayName("인기 상품 조회 성공 - 랭킹 정보가 없는 경우")
    void getPopularProducts_Success_WithMissingRankingInfo() {
        // given
        int limit = 2;
        GetPopularProductsUseCase.GetPopularProductsCommand command = 
            new GetPopularProductsUseCase.GetPopularProductsCommand(limit);

        List<Long> topProductIds = List.of(1L, 2L);
        
        LocalDateTime now = LocalDateTime.now();
        LoadProductPort.ProductInfo productInfo1 = new LoadProductPort.ProductInfo(
            1L, "상품1", "상품1 설명", 10000, 100, "ACTIVE", "카테고리1", now, now);
        LoadProductPort.ProductInfo productInfo2 = new LoadProductPort.ProductInfo(
            2L, "상품2", "상품2 설명", 12000, 80, "ACTIVE", "카테고리2", now, now);

        when(productRankingService.getTopProductIds(limit)).thenReturn(topProductIds);
        when(productRankingService.getProductRankingInfo(1L))
            .thenReturn(new ProductRankingUseCase.ProductRankingInfo(null, null)); // 랭킹 정보 없음
        when(productRankingService.getProductRankingInfo(2L))
            .thenReturn(new ProductRankingUseCase.ProductRankingInfo(1L, 40.0));
        when(loadProductPort.loadProductById(1L)).thenReturn(Optional.of(productInfo1));
        when(loadProductPort.loadProductById(2L)).thenReturn(Optional.of(productInfo2));

        // when
        GetPopularProductsUseCase.GetPopularProductsResult result = 
            getPopularProductsService.getPopularProducts(command);

        // then
        assertThat(result.getPopularProducts()).hasSize(2);
        
        // 첫 번째 상품은 기본값으로 설정
        assertThat(result.getPopularProducts().get(0).getRank()).isEqualTo(1); // null + 1 = 1
        assertThat(result.getPopularProducts().get(0).getRecentSalesCount()).isEqualTo(0); // null -> 0
        
        // 두 번째 상품은 정상값
        assertThat(result.getPopularProducts().get(1).getRank()).isEqualTo(2); // 1 + 1 = 2
        assertThat(result.getPopularProducts().get(1).getRecentSalesCount()).isEqualTo(40);
        
        verify(productRankingService).getTopProductIds(limit);
        verify(productRankingService).getProductRankingInfo(1L);
        verify(productRankingService).getProductRankingInfo(2L);
        verify(loadProductPort).loadProductById(1L);
        verify(loadProductPort).loadProductById(2L);
    }
} 