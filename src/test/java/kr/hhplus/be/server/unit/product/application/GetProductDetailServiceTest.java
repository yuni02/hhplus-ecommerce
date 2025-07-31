package kr.hhplus.be.server.unit.product.application;

import kr.hhplus.be.server.product.application.GetProductDetailService;
import kr.hhplus.be.server.product.application.port.in.GetProductDetailUseCase;
import kr.hhplus.be.server.product.application.port.out.LoadProductPort;
import kr.hhplus.be.server.product.application.port.out.LoadProductStatsPort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetProductDetailServiceTest {

    @Mock
    private LoadProductPort loadProductPort;
    
    @Mock
    private LoadProductStatsPort loadProductStatsPort;

    private GetProductDetailService getProductDetailService;

    @BeforeEach
    void setUp() {
        getProductDetailService = new GetProductDetailService(loadProductPort, loadProductStatsPort);
    }

//     @Test
//     @DisplayName("상품 상세 조회 성공")
//     void getProductDetail_Success() {
//         // given
//         Long productId = 1L;
//         GetProductDetailUseCase.GetProductDetailCommand command = 
//             new GetProductDetailUseCase.GetProductDetailCommand(productId);

//         LoadProductPort.ProductInfo productInfo = new LoadProductPort.ProductInfo(
//             productId, "상품A", "상품A 설명", 10000, 100, "ACTIVE", "전자제품");

//         when(loadProductPort.loadProductById(productId)).thenReturn(Optional.of(productInfo));

//         // when
//         Optional<GetProductDetailUseCase.GetProductDetailResult> result = 
//             getProductDetailService.getProductDetail(command);

//         // then
//         assertThat(result).isPresent();
//         GetProductDetailUseCase.GetProductDetailResult productDetail = result.get();
//         assertThat(productDetail.getId()).isEqualTo(productId);
//         assertThat(productDetail.getName()).isEqualTo("상품A");
//         assertThat(productDetail.getCurrentPrice()).isEqualTo(10000);
//         assertThat(productDetail.getStock()).isEqualTo(100);
//         assertThat(productDetail.getStatus()).isEqualTo("ACTIVE");
        
//         verify(loadProductPort).loadProductById(productId);
//     }

//     @Test
//     @DisplayName("상품 상세 조회 실패 - 상품이 존재하지 않음")
//     void getProductDetail_Failure_ProductNotFound() {
//         // given
//         Long productId = 999L;
//         GetProductDetailUseCase.GetProductDetailCommand command = 
//             new GetProductDetailUseCase.GetProductDetailCommand(productId);

//         when(loadProductPort.loadProductById(productId)).thenReturn(Optional.empty());

//         // when
//         Optional<GetProductDetailUseCase.GetProductDetailResult> result = 
//             getProductDetailService.getProductDetail(command);

//         // then
//         assertThat(result).isEmpty();
        
//         verify(loadProductPort).loadProductById(productId);
//         verify(loadProductStatsPort, never()).loadProductStatsByProductId(any());
//     }

//     @Test
//     @DisplayName("상품 상세 조회 실패 - 잘못된 상품 ID")
//     void getProductDetail_Failure_InvalidProductId() {
//         // given
//         Long productId = null;
//         GetProductDetailUseCase.GetProductDetailCommand command = 
//             new GetProductDetailUseCase.GetProductDetailCommand(productId);

//         // when
//         Optional<GetProductDetailUseCase.GetProductDetailResult> result = 
//             getProductDetailService.getProductDetail(command);

//         // then
//         assertThat(result).isEmpty();
        
//         verify(loadProductPort, never()).loadProductById(any());
//         verify(loadProductStatsPort, never()).loadProductStatsByProductId(any());
//     }

//     @Test
//     @DisplayName("상품 상세 조회 성공 - 비활성 상품도 조회 가능")
//     void getProductDetail_Success_InactiveProduct() {
//         // given
//         Long productId = 1L;
//         GetProductDetailUseCase.GetProductDetailCommand command = 
//             new GetProductDetailUseCase.GetProductDetailCommand(productId);

//         LoadProductPort.ProductInfo productInfo = new LoadProductPort.ProductInfo(
//             productId, "비활성 상품", "비활성 상품 설명", 5000, 0, "INACTIVE", "기타");

//         when(loadProductPort.loadProductById(productId)).thenReturn(Optional.of(productInfo));

//         // when
//         Optional<GetProductDetailUseCase.GetProductDetailResult> result = 
//             getProductDetailService.getProductDetail(command);

//         // then
//         assertThat(result).isPresent();
//         GetProductDetailUseCase.GetProductDetailResult productDetail = result.get();
//         assertThat(productDetail.getId()).isEqualTo(productId);
//         assertThat(productDetail.getName()).isEqualTo("비활성 상품");
//         assertThat(productDetail.getCurrentPrice()).isEqualTo(5000);
//         assertThat(productDetail.getStock()).isEqualTo(0);
//         assertThat(productDetail.getStatus()).isEqualTo("INACTIVE");
        
//         verify(loadProductPort).loadProductById(productId);
//     }

//     @Test
//     @DisplayName("상품 상세 조회 실패 - 예외 발생")
//     void getProductDetail_Failure_Exception() {
//         // given
//         Long productId = 1L;
//         GetProductDetailUseCase.GetProductDetailCommand command = 
//             new GetProductDetailUseCase.GetProductDetailCommand(productId);

//         when(loadProductPort.loadProductById(productId))
//             .thenThrow(new RuntimeException("데이터베이스 오류"));

//         // when
//         Optional<GetProductDetailUseCase.GetProductDetailResult> result = 
//             getProductDetailService.getProductDetail(command);

//         // then
//         assertThat(result).isEmpty();
        
//         verify(loadProductPort).loadProductById(productId);
//         verify(loadProductStatsPort, never()).loadProductStatsByProductId(any());
//     }

//     @Test
//     @DisplayName("상품 상세 조회 성공 - 재고가 0인 상품")
//     void getProductDetail_Success_ZeroStock() {
//         // given
//         Long productId = 1L;
//         GetProductDetailUseCase.GetProductDetailCommand command = 
//             new GetProductDetailUseCase.GetProductDetailCommand(productId);

//         LoadProductPort.ProductInfo productInfo = new LoadProductPort.ProductInfo(
//             productId, "품절 상품", "품절 상품 설명", 15000, 0, "SOLD_OUT", "의류");

//         when(loadProductPort.loadProductById(productId)).thenReturn(Optional.of(productInfo));

//         // when
//         Optional<GetProductDetailUseCase.GetProductDetailResult> result = 
//             getProductDetailService.getProductDetail(command);

//         // then
//         assertThat(result).isPresent();
//         GetProductDetailUseCase.GetProductDetailResult productDetail = result.get();
//         assertThat(productDetail.getId()).isEqualTo(productId);
//         assertThat(productDetail.getName()).isEqualTo("품절 상품");
//         assertThat(productDetail.getCurrentPrice()).isEqualTo(15000);
//         assertThat(productDetail.getStock()).isEqualTo(0);
//         assertThat(productDetail.getStatus()).isEqualTo("SOLD_OUT");
        
//         verify(loadProductPort).loadProductById(productId);
//     }

//     @Test
//     @DisplayName("상품 상세 조회 성공 - 높은 가격 상품")
//     void getProductDetail_Success_HighPriceProduct() {
//         // given
//         Long productId = 1L;
//         GetProductDetailUseCase.GetProductDetailCommand command = 
//             new GetProductDetailUseCase.GetProductDetailCommand(productId);

//         LoadProductPort.ProductInfo productInfo = new LoadProductPort.ProductInfo(
//             productId, "고가 상품", "고가 상품 설명", 1000000, 5, "ACTIVE", "명품");

//         when(loadProductPort.loadProductById(productId)).thenReturn(Optional.of(productInfo));

//         // when
//         Optional<GetProductDetailUseCase.GetProductDetailResult> result = 
//             getProductDetailService.getProductDetail(command);

//         // then
//         assertThat(result).isPresent();
//         GetProductDetailUseCase.GetProductDetailResult productDetail = result.get();
//         assertThat(productDetail.getId()).isEqualTo(productId);
//         assertThat(productDetail.getName()).isEqualTo("고가 상품");
//         assertThat(productDetail.getCurrentPrice()).isEqualTo(1000000);
//         assertThat(productDetail.getStock()).isEqualTo(5);
//         assertThat(productDetail.getStatus()).isEqualTo("ACTIVE");
        
//         verify(loadProductPort).loadProductById(productId);
//     }
} 