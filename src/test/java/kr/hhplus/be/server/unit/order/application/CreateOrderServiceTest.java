package kr.hhplus.be.server.unit.order.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import kr.hhplus.be.server.order.application.CreateOrderService;
import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;    
import kr.hhplus.be.server.order.domain.service.OrderDomainService;
import kr.hhplus.be.server.shared.event.AsyncEventPublisher;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;

/**
 * CreateOrderService 코레오그래피 테스트
 * 이벤트 발행 및 비동기 처리에 초점을 맞춘 테스트
 */
@ExtendWith(MockitoExtension.class)
class CreateOrderServiceTest {

    @Mock
    private OrderDomainService orderDomainService;
    
    @Mock
    private AsyncEventPublisher eventPublisher;

    private CreateOrderService createOrderService;

    @BeforeEach
    void setUp() {
        createOrderService = new CreateOrderService(
            orderDomainService,
            eventPublisher
        );      
    }

    @Test
    @DisplayName("주문 성공 - 이벤트 발행 및 비동기 처리")
    void createOrder_Success_EventPublished() {
        // given
        Long userId = 1L;
        CreateOrderUseCase.OrderItemCommand orderItemCommand = 
            new CreateOrderUseCase.OrderItemCommand(1L, 2);
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), null);

        // Mock domain service response - 주문 검증만 수행
        when(orderDomainService.validateOrder(command))
            .thenReturn(OrderDomainService.OrderValidationResult.success());

        // when
        CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

        // then - 코레오그래피 방식에서는 주문 상태가 PROCESSING이고 이벤트가 발행됨
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getStatus()).isEqualTo("PROCESSING");
        
        // 주문 검증이 호출되었는지 검증
        verify(orderDomainService).validateOrder(command);
        
        // 이벤트가 발행되었는지 검증
        verify(eventPublisher).publishAsync(any());
    }

    @Test
    @DisplayName("주문 실패 - 검증 실패 시 이벤트 발행되지 않음")
    void createOrder_Failure_ValidationFailed() {
        // given
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(1L, List.of(), null);

        when(orderDomainService.validateOrder(command))
            .thenReturn(OrderDomainService.OrderValidationResult.failure("주문 아이템이 없습니다."));

        // when
        CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("주문 아이템이 없습니다.");
        
        // 검증 실패 후 이벤트가 발행되지 않았는지 확인
        verify(orderDomainService).validateOrder(command);
        verify(eventPublisher, never()).publishAsync(any());
    }

    @Test
    @DisplayName("주문 처리 - 유효한 명령에 대해 적절한 OrderId 생성")
    void createOrder_GeneratesOrderId() {
        // given
        Long userId = 1L;
        CreateOrderUseCase.OrderItemCommand orderItemCommand = 
            new CreateOrderUseCase.OrderItemCommand(1L, 2);
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), null);

        when(orderDomainService.validateOrder(command))
            .thenReturn(OrderDomainService.OrderValidationResult.success());

        // when
        CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrderId()).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getStatus()).isEqualTo("PROCESSING");
    }

    @Test
    @DisplayName("주문 처리 - 쿠폰이 있는 경우 쿠폰 ID 포함")
    void createOrder_WithCoupon() {
        // given
        Long userId = 1L;
        Long userCouponId = 100L;
        CreateOrderUseCase.OrderItemCommand orderItemCommand = 
            new CreateOrderUseCase.OrderItemCommand(1L, 2);
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), userCouponId);

        when(orderDomainService.validateOrder(command))
            .thenReturn(OrderDomainService.OrderValidationResult.success());

        // when
        CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getUserCouponId()).isEqualTo(userCouponId);
        
        // 이벤트 발행 검증
        verify(eventPublisher).publishAsync(any());
    }

    @Test
    @DisplayName("주문 처리 - 예외 발생 시 실패 결과 반환")
    void createOrder_ExceptionHandling() {
        // given
        Long userId = 1L;
        CreateOrderUseCase.OrderItemCommand orderItemCommand = 
            new CreateOrderUseCase.OrderItemCommand(1L, 2);
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), null);

        when(orderDomainService.validateOrder(command))
            .thenThrow(new RuntimeException("데이터베이스 연결 오류"));

        // when
        CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("주문 처리 중 오류가 발생했습니다");
        
        // 예외 발생 시 이벤트가 발행되지 않았는지 확인
        verify(eventPublisher, never()).publishAsync(any());
    }
}