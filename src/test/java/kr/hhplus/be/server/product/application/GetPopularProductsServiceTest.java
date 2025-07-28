package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.product.application.port.in.GetPopularProductsUseCase;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetPopularProductsServiceTest {

    @Mock
    private LoadProductPort loadProductPort;
    
    @Mock
    private LoadProductStatsPort loadProductStatsPort;

    private GetPopularProductsService getPopularProductsService;

    @BeforeEach
    void setUp() {
        getPopularProductsService = new GetPopularProductsService(loadProductPort, loadProductStatsPort);
    }

    // @Test
    // @DisplayName("인기 상품 조회 성공")
    // void getPopularProducts_Success() {
    //     // given
    //     int limit = 5;
    //     GetPopularProductsUseCase.GetPopularProductsCommand command = 
    //         new GetPopularProductsUseCase.GetPopularProductsCommand(limit);

    //     LoadProductStatsPort.ProductStatsInfo statsInfo1 = new LoadProductStatsPort.ProductStatsInfo(
    //         1L, "상품A", 100, 1000000L, 500, 5000000L, 1, 0.75);
    //     LoadProductStatsPort.ProductStatsInfo statsInfo2 = new LoadProductStatsPort.ProductStatsInfo(
    //         2L, "상품B", 80, 800000L, 400, 4000000L, 2, 0.65);

    //     LoadProductPort.ProductInfo productInfo1 = new LoadProductPort.ProductInfo(
    //         1L, "상품A", "상품A 설명", 10000, 100, "ACTIVE", "전자제품");
    //     LoadProductPort.ProductInfo productInfo2 = new LoadProductPort.ProductInfo(
    //         2L, "상품B", "상품B 설명", 15000, 50, "ACTIVE", "의류");

    //     when(loadProductStatsPort.loadTopProductsBySales(limit)).thenReturn(List.of(statsInfo1, statsInfo2));
    //     when(loadProductPort.loadProductById(1L)).thenReturn(Optional.of(productInfo1));
    //     when(loadProductPort.loadProductById(2L)).thenReturn(Optional.of(productInfo2));

    //     // when
    //     GetPopularProductsUseCase.GetPopularProductsResult result = 
    //         getPopularProductsService.getPopularProducts(command);

    //     // then
    //     assertThat(result.getPopularProducts()).hasSize(2);
        
    //     GetPopularProductsUseCase.PopularProductInfo product1 = result.getPopularProducts().get(0);
    //     assertThat(product1.getProductId()).isEqualTo(1L);
    //     assertThat(product1.getProductName()).isEqualTo("상품A");
    //     assertThat(product1.getCurrentPrice()).isEqualTo(10000);
    //     assertThat(product1.getStock()).isEqualTo(100);
    //     assertThat(product1.getTotalSalesCount()).isEqualTo(500);
    //     assertThat(product1.getTotalSalesAmount()).isEqualTo(5000000L);
    //     assertThat(product1.getRecentSalesCount()).isEqualTo(100);
    //     assertThat(product1.getRecentSalesAmount()).isEqualTo(1000000L);
    //     assertThat(product1.getConversionRate()).isEqualTo(0.75);
    //     assertThat(product1.getRank()).isEqualTo(1);
        
    //     GetPopularProductsUseCase.PopularProductInfo product2 = result.getPopularProducts().get(1);
    //     assertThat(product2.getProductId()).isEqualTo(2L);
    //     assertThat(product2.getProductName()).isEqualTo("상품B");
    //     assertThat(product2.getCurrentPrice()).isEqualTo(15000);
    //     assertThat(product2.getStock()).isEqualTo(50);
    //     assertThat(product2.getRank()).isEqualTo(2);
        
    //     verify(loadProductStatsPort).loadTopProductsBySales(limit);
    //     verify(loadProductPort).loadProductById(1L);
    //     verify(loadProductPort).loadProductById(2L);
    // }

    // @Test
    // @DisplayName("인기 상품 조회 성공 - 빈 결과")
    // void getPopularProducts_Success_EmptyResult() {
    //     // given
    //     int limit = 5;
    //     GetPopularProductsUseCase.GetPopularProductsCommand command = 
    //         new GetPopularProductsUseCase.GetPopularProductsCommand(limit);

    //     when(loadProductStatsPort.loadTopProductsBySales(limit)).thenReturn(List.of());

    //     // when
    //     GetPopularProductsUseCase.GetPopularProductsResult result = 
    //         getPopularProductsService.getPopularProducts(command);

    //     // then
    //     assertThat(result.getPopularProducts()).isEmpty();
        
    //     verify(loadProductStatsPort).loadTopProductsBySales(limit);
    //     verify(loadProductPort, never()).loadProductById(any());
    // }

    // @Test
    // @DisplayName("인기 상품 조회 성공 - 상품 정보가 없는 경우")
    // void getPopularProducts_Success_WithMissingProductInfo() {
    //     // given
    //     int limit = 5;
    //     GetPopularProductsUseCase.GetPopularProductsCommand command = 
    //         new GetPopularProductsUseCase.GetPopularProductsCommand(limit);

    //     LoadProductStatsPort.ProductStatsInfo statsInfo = new LoadProductStatsPort.ProductStatsInfo(
    //         999L, "삭제된 상품", 50, 500000L, 250, 2500000L, 1, 0.60);

    //     when(loadProductStatsPort.loadTopProductsBySales(limit)).thenReturn(List.of(statsInfo));
    //     when(loadProductPort.loadProductById(999L)).thenReturn(Optional.empty());

    //     // when
    //     GetPopularProductsUseCase.GetPopularProductsResult result = 
    //         getPopularProductsService.getPopularProducts(command);

    //     // then
    //     assertThat(result.getPopularProducts()).hasSize(1);
        
    //     GetPopularProductsUseCase.PopularProductInfo product = result.getPopularProducts().get(0);
    //     assertThat(product.getProductId()).isEqualTo(999L);
    //     assertThat(product.getProductName()).isEqualTo("삭제된 상품");
    //     assertThat(product.getCurrentPrice()).isEqualTo(0); // 기본값
    //     assertThat(product.getStock()).isEqualTo(0); // 기본값
    //     assertThat(product.getTotalSalesCount()).isEqualTo(250);
    //     assertThat(product.getTotalSalesAmount()).isEqualTo(2500000L);
    //     assertThat(product.getRank()).isEqualTo(1);
        
    //     verify(loadProductStatsPort).loadTopProductsBySales(limit);
    //     verify(loadProductPort).loadProductById(999L);
    // }

    // @Test
    // @DisplayName("인기 상품 조회 성공 - limit 1개")
    // void getPopularProducts_Success_LimitOne() {
    //     // given
    //     int limit = 1;
    //     GetPopularProductsUseCase.GetPopularProductsCommand command = 
    //         new GetPopularProductsUseCase.GetPopularProductsCommand(limit);

    //     LoadProductStatsPort.ProductStatsInfo statsInfo = new LoadProductStatsPort.ProductStatsInfo(
    //         1L, "최고 인기 상품", 200, 2000000L, 1000, 10000000L, 1, 0.85);

    //     LoadProductPort.ProductInfo productInfo = new LoadProductPort.ProductInfo(
    //         1L, "최고 인기 상품", "최고 인기 상품 설명", 20000, 200, "ACTIVE", "베스트");

    //     when(loadProductStatsPort.loadTopProductsBySales(limit)).thenReturn(List.of(statsInfo));
    //     when(loadProductPort.loadProductById(1L)).thenReturn(Optional.of(productInfo));

    //     // when
    //     GetPopularProductsUseCase.GetPopularProductsResult result = 
    //         getPopularProductsService.getPopularProducts(command);

    //     // then
    //     assertThat(result.getPopularProducts()).hasSize(1);
        
    //     GetPopularProductsUseCase.PopularProductInfo product = result.getPopularProducts().get(0);
    //     assertThat(product.getProductId()).isEqualTo(1L);
    //     assertThat(product.getProductName()).isEqualTo("최고 인기 상품");
    //     assertThat(product.getCurrentPrice()).isEqualTo(20000);
    //     assertThat(product.getStock()).isEqualTo(200);
    //     assertThat(product.getTotalSalesCount()).isEqualTo(1000);
    //     assertThat(product.getTotalSalesAmount()).isEqualTo(10000000L);
    //     assertThat(product.getConversionRate()).isEqualTo(0.85);
    //     assertThat(product.getRank()).isEqualTo(1);
        
    //     verify(loadProductStatsPort).loadTopProductsBySales(limit);
    //     verify(loadProductPort).loadProductById(1L);
    // }

    // @Test
    // @DisplayName("인기 상품 조회 성공 - 높은 limit 값")
    // void getPopularProducts_Success_HighLimit() {
    //     // given
    //     int limit = 100;
    //     GetPopularProductsUseCase.GetPopularProductsCommand command = 
    //         new GetPopularProductsUseCase.GetPopularProductsCommand(limit);

    //     // 실제로는 3개만 반환
    //     LoadProductStatsPort.ProductStatsInfo statsInfo1 = new LoadProductStatsPort.ProductStatsInfo(
    //         1L, "상품1", 50, 500000L, 250, 2500000L, 1, 0.70);
    //     LoadProductStatsPort.ProductStatsInfo statsInfo2 = new LoadProductStatsPort.ProductStatsInfo(
    //         2L, "상품2", 40, 400000L, 200, 2000000L, 2, 0.60);
    //     LoadProductStatsPort.ProductStatsInfo statsInfo3 = new LoadProductStatsPort.ProductStatsInfo(
    //         3L, "상품3", 30, 300000L, 150, 1500000L, 3, 0.50);

    //     LoadProductPort.ProductInfo productInfo1 = new LoadProductPort.ProductInfo(
    //         1L, "상품1", "상품1 설명", 10000, 100, "ACTIVE", "카테고리1");
    //     LoadProductPort.ProductInfo productInfo2 = new LoadProductPort.ProductInfo(
    //         2L, "상품2", "상품2 설명", 12000, 80, "ACTIVE", "카테고리2");
    //     LoadProductPort.ProductInfo productInfo3 = new LoadProductPort.ProductInfo(
    //         3L, "상품3", "상품3 설명", 8000, 60, "ACTIVE", "카테고리3");

    //     when(loadProductStatsPort.loadTopProductsBySales(limit))
    //         .thenReturn(List.of(statsInfo1, statsInfo2, statsInfo3));
    //     when(loadProductPort.loadProductById(1L)).thenReturn(Optional.of(productInfo1));
    //     when(loadProductPort.loadProductById(2L)).thenReturn(Optional.of(productInfo2));
    //     when(loadProductPort.loadProductById(3L)).thenReturn(Optional.of(productInfo3));

    //     // when
    //     GetPopularProductsUseCase.GetPopularProductsResult result = 
    //         getPopularProductsService.getPopularProducts(command);

    //     // then
    //     assertThat(result.getPopularProducts()).hasSize(3);
        
    //     // 순위별로 정렬되어 있는지 확인
    //     assertThat(result.getPopularProducts().get(0).getRank()).isEqualTo(1);
    //     assertThat(result.getPopularProducts().get(1).getRank()).isEqualTo(2);
    //     assertThat(result.getPopularProducts().get(2).getRank()).isEqualTo(3);
        
    //     verify(loadProductStatsPort).loadTopProductsBySales(limit);
    //     verify(loadProductPort).loadProductById(1L);
    //     verify(loadProductPort).loadProductById(2L);
    //     verify(loadProductPort).loadProductById(3L);
    // }

    // @Test
    // @DisplayName("인기 상품 조회 실패 - 예외 발생")
    // void getPopularProducts_Failure_Exception() {
    //     // given
    //     int limit = 5;
    //     GetPopularProductsUseCase.GetPopularProductsCommand command = 
    //         new GetPopularProductsUseCase.GetPopularProductsCommand(limit);

    //     when(loadProductStatsPort.loadTopProductsBySales(limit))
    //         .thenThrow(new RuntimeException("통계 데이터 조회 오류"));

    //     // when
    //     GetPopularProductsUseCase.GetPopularProductsResult result = 
    //         getPopularProductsService.getPopularProducts(command);

    //     // then
    //     assertThat(result.getPopularProducts()).isEmpty();
        
    //     verify(loadProductStatsPort).loadTopProductsBySales(limit);
    //     verify(loadProductPort, never()).loadProductById(any());
    // }

    // @Test
    // @DisplayName("인기 상품 조회 성공 - 0건 limit")
    // void getPopularProducts_Success_ZeroLimit() {
    //     // given
    //     int limit = 0;
    //     GetPopularProductsUseCase.GetPopularProductsCommand command = 
    //         new GetPopularProductsUseCase.GetPopularProductsCommand(limit);

    //     when(loadProductStatsPort.loadTopProductsBySales(limit)).thenReturn(List.of());

    //     // when
    //     GetPopularProductsUseCase.GetPopularProductsResult result = 
    //         getPopularProductsService.getPopularProducts(command);

    //     // then
    //     assertThat(result.getPopularProducts()).isEmpty();
        
    //     verify(loadProductStatsPort).loadTopProductsBySales(limit);
    //     verify(loadProductPort, never()).loadProductById(any());
    // }

    // @Test
    // @DisplayName("인기 상품 조회 성공 - 부분적 상품 정보 누락")
    // void getPopularProducts_Success_PartialProductInfoMissing() {
    //     // given
    //     int limit = 3;
    //     GetPopularProductsUseCase.GetPopularProductsCommand command = 
    //         new GetPopularProductsUseCase.GetPopularProductsCommand(limit);

    //     LoadProductStatsPort.ProductStatsInfo statsInfo1 = new LoadProductStatsPort.ProductStatsInfo(
    //         1L, "상품1", 50, 500000L, 250, 2500000L, 1, 0.70);
    //     LoadProductStatsPort.ProductStatsInfo statsInfo2 = new LoadProductStatsPort.ProductStatsInfo(
    //         2L, "상품2", 40, 400000L, 200, 2000000L, 2, 0.60);
    //     LoadProductStatsPort.ProductStatsInfo statsInfo3 = new LoadProductStatsPort.ProductStatsInfo(
    //         999L, "삭제된 상품", 30, 300000L, 150, 1500000L, 3, 0.50);

    //     LoadProductPort.ProductInfo productInfo1 = new LoadProductPort.ProductInfo(
    //         1L, "상품1", "상품1 설명", 10000, 100, "ACTIVE", "카테고리1");
    //     LoadProductPort.ProductInfo productInfo2 = new LoadProductPort.ProductInfo(
    //         2L, "상품2", "상품2 설명", 12000, 80, "ACTIVE", "카테고리2");

    //     when(loadProductStatsPort.loadTopProductsBySales(limit))
    //         .thenReturn(List.of(statsInfo1, statsInfo2, statsInfo3));
    //     when(loadProductPort.loadProductById(1L)).thenReturn(Optional.of(productInfo1));
    //     when(loadProductPort.loadProductById(2L)).thenReturn(Optional.of(productInfo2));
    //     when(loadProductPort.loadProductById(999L)).thenReturn(Optional.empty()); // 삭제된 상품

    //     // when
    //     GetPopularProductsUseCase.GetPopularProductsResult result = 
    //         getPopularProductsService.getPopularProducts(command);

    //     // then
    //     assertThat(result.getPopularProducts()).hasSize(3);
        
    //     // 첫 번째와 두 번째는 정상 상품
    //     assertThat(result.getPopularProducts().get(0).getCurrentPrice()).isEqualTo(10000);
    //     assertThat(result.getPopularProducts().get(1).getCurrentPrice()).isEqualTo(12000);
        
    //     // 세 번째는 삭제된 상품으로 기본값
    //     assertThat(result.getPopularProducts().get(2).getCurrentPrice()).isEqualTo(0);
    //     assertThat(result.getPopularProducts().get(2).getStock()).isEqualTo(0);
    //     assertThat(result.getPopularProducts().get(2).getProductName()).isEqualTo("삭제된 상품");
        
    //     verify(loadProductStatsPort).loadTopProductsBySales(limit);
    //     verify(loadProductPort).loadProductById(1L);
    //     verify(loadProductPort).loadProductById(2L);
    //     verify(loadProductPort).loadProductById(999L);
    // }
} 