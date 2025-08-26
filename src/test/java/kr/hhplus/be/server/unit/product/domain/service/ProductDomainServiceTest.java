package kr.hhplus.be.server.unit.product.domain.service;

import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.domain.StockDeductionCompletedEvent;
import kr.hhplus.be.server.order.domain.StockDeductionRequestedEvent;
import kr.hhplus.be.server.order.domain.StockRestorationCompletedEvent;
import kr.hhplus.be.server.order.domain.StockRestorationRequestedEvent;
import kr.hhplus.be.server.product.domain.service.ProductDomainService;
import kr.hhplus.be.server.shared.event.SynchronousEventProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductDomainServiceTest {

    @Mock
    private SynchronousEventProcessor synchronousEventProcessor;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private StockDeductionCompletedEvent mockSuccessEvent;
    
    @Mock
    private StockDeductionCompletedEvent mockFailureEvent;
    
    @Mock
    private StockRestorationCompletedEvent mockRestorationSuccessEvent;
    
    @Mock
    private StockRestorationCompletedEvent mockRestorationFailureEvent;

    private ProductDomainService productDomainService;

    @BeforeEach
    void setUp() {
        productDomainService = new ProductDomainService(synchronousEventProcessor, eventPublisher);
    }

    @Test
    @DisplayName("재고 차감 성공 - 단일 상품")
    void processStockDeduction_Success_SingleProduct() {
        // given
        Long userId = 1L;
        CreateOrderUseCase.OrderItemCommand orderItemCommand = 
            new CreateOrderUseCase.OrderItemCommand(1L, 2);
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), null);

        when(mockSuccessEvent.isSuccess()).thenReturn(true);
        when(mockSuccessEvent.getProductName()).thenReturn("테스트 상품");
        when(mockSuccessEvent.getUnitPrice()).thenReturn(BigDecimal.valueOf(10000));

        when(synchronousEventProcessor.publishAndWaitForResponse(
            any(StockDeductionRequestedEvent.class), 
            any(String.class), 
            eq(StockDeductionCompletedEvent.class), 
            eq(5)))
            .thenReturn(mockSuccessEvent);

        // when
        ProductDomainService.StockProcessResult result = productDomainService.processStockDeduction(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrderItems()).hasSize(1);
        assertThat(result.getOrderItems().get(0).getProductId()).isEqualTo(1L);
        assertThat(result.getOrderItems().get(0).getProductName()).isEqualTo("테스트 상품");
        assertThat(result.getOrderItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(result.getOrderItems().get(0).getUnitPrice()).isEqualTo(BigDecimal.valueOf(10000));
        assertThat(result.getTotalAmount()).isEqualTo(BigDecimal.valueOf(20000));
        assertThat(result.getErrorMessage()).isNull();
        
        verify(synchronousEventProcessor).publishAndWaitForResponse(
            any(StockDeductionRequestedEvent.class), any(String.class), eq(StockDeductionCompletedEvent.class), eq(5));
    }

    @Test
    @DisplayName("재고 차감 실패 - 재고 부족")
    void processStockDeduction_Failure_InsufficientStock() {
        // given
        Long userId = 1L;
        CreateOrderUseCase.OrderItemCommand orderItemCommand = 
            new CreateOrderUseCase.OrderItemCommand(1L, 100);
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), null);

        when(mockFailureEvent.isSuccess()).thenReturn(false);
        when(mockFailureEvent.getErrorMessage()).thenReturn("재고가 부족합니다.");

        when(synchronousEventProcessor.publishAndWaitForResponse(
            any(StockDeductionRequestedEvent.class), any(String.class), eq(StockDeductionCompletedEvent.class), eq(5)))
            .thenReturn(mockFailureEvent);

        // when
        ProductDomainService.StockProcessResult result = productDomainService.processStockDeduction(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("재고가 부족합니다.");
        assertThat(result.getOrderItems()).isNull();
        assertThat(result.getTotalAmount()).isNull();
        
        verify(synchronousEventProcessor).publishAndWaitForResponse(
            any(StockDeductionRequestedEvent.class), any(String.class), eq(StockDeductionCompletedEvent.class), eq(5));
    }

    @Test
    @DisplayName("재고 차감 실패 - 이벤트 처리 중 예외 발생")
    void processStockDeduction_Failure_EventProcessingException() {
        // given
        Long userId = 1L;
        CreateOrderUseCase.OrderItemCommand orderItemCommand = 
            new CreateOrderUseCase.OrderItemCommand(1L, 2);
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), null);

        when(synchronousEventProcessor.publishAndWaitForResponse(
            any(StockDeductionRequestedEvent.class), any(String.class), eq(StockDeductionCompletedEvent.class), eq(5)))
            .thenThrow(new RuntimeException("이벤트 처리 실패"));

        // when
        ProductDomainService.StockProcessResult result = productDomainService.processStockDeduction(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("재고 처리 중 오류가 발생했습니다");
        
        verify(synchronousEventProcessor).publishAndWaitForResponse(
            any(StockDeductionRequestedEvent.class), any(String.class), eq(StockDeductionCompletedEvent.class), eq(5));
    }

    @Test
    @DisplayName("재고 복원 성공")
    void rollbackStockDeduction_Success() {
        // given
        OrderItem orderItem = OrderItem.builder()
            .productId(1L)
            .quantity(2)
            .build();
        List<OrderItem> orderItems = List.of(orderItem);
        String reason = "쿠폰 사용 실패";

        when(synchronousEventProcessor.publishAndWaitForResponse(
            any(StockRestorationRequestedEvent.class), any(String.class), eq(StockRestorationCompletedEvent.class), eq(3)))
            .thenReturn(mockRestorationSuccessEvent);

        // when
        productDomainService.rollbackStockDeduction(orderItems, reason);

        // then - void 메소드이므로 이벤트 발행만 검증
        verify(synchronousEventProcessor).publishAndWaitForResponse(
            any(StockRestorationRequestedEvent.class), any(String.class), eq(StockRestorationCompletedEvent.class), eq(3));
    }

    @Test
    @DisplayName("재고 복원 실패")
    void rollbackStockDeduction_Failure() {
        // given
        OrderItem orderItem = OrderItem.builder()
            .productId(1L)
            .quantity(2)
            .build();
        List<OrderItem> orderItems = List.of(orderItem);
        String reason = "잔액 차감 실패";

        when(synchronousEventProcessor.publishAndWaitForResponse(
            any(StockRestorationRequestedEvent.class), any(String.class), eq(StockRestorationCompletedEvent.class), eq(3)))
            .thenReturn(mockRestorationFailureEvent);

        // when
        productDomainService.rollbackStockDeduction(orderItems, reason);

        // then - void 메소드이므로 이벤트 발행만 검증 (실패해도 예외 발생하지 않음)
        verify(synchronousEventProcessor).publishAndWaitForResponse(
            any(StockRestorationRequestedEvent.class), any(String.class), eq(StockRestorationCompletedEvent.class), eq(3));
    }

    @Test
    @DisplayName("재고 복원 중 예외 발생")
    void rollbackStockDeduction_Exception() {
        // given
        OrderItem orderItem = OrderItem.builder()
            .productId(1L)
            .quantity(2)
            .build();
        List<OrderItem> orderItems = List.of(orderItem);
        String reason = "주문 저장 실패";

        when(synchronousEventProcessor.publishAndWaitForResponse(
            any(StockRestorationRequestedEvent.class), any(String.class), eq(StockRestorationCompletedEvent.class), eq(3)))
            .thenThrow(new RuntimeException("복원 이벤트 처리 실패"));

        // when
        productDomainService.rollbackStockDeduction(orderItems, reason);

        // then - 예외가 발생해도 메서드는 정상 완료됨 (로그만 남김)
        verify(synchronousEventProcessor).publishAndWaitForResponse(
            any(StockRestorationRequestedEvent.class), any(String.class), eq(StockRestorationCompletedEvent.class), eq(3));
    }
}