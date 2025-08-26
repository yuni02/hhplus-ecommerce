package kr.hhplus.be.server.unit.order.domain.service;

import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.order.application.port.out.LoadUserPort;
import kr.hhplus.be.server.order.application.port.out.SaveOrderPort;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.domain.service.OrderDomainService;
import kr.hhplus.be.server.shared.event.AsyncEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderDomainServiceTest {

    @Mock
    private LoadUserPort loadUserPort;
    
    @Mock
    private SaveOrderPort saveOrderPort;
    
    @Mock
    private AsyncEventPublisher asyncEventPublisher;

    private OrderDomainService orderDomainService;

    @BeforeEach
    void setUp() {
        orderDomainService = new OrderDomainService(loadUserPort, saveOrderPort, asyncEventPublisher);
    }

    @Test
    @DisplayName("주문 검증 성공 - 유효한 주문")
    void validateOrder_Success_ValidOrder() {
        // given
        Long userId = 1L;
        CreateOrderUseCase.OrderItemCommand orderItem = new CreateOrderUseCase.OrderItemCommand(1L, 2);
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItem), null);

        when(loadUserPort.existsById(userId)).thenReturn(true);

        // when
        OrderDomainService.OrderValidationResult result = orderDomainService.validateOrder(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getErrorMessage()).isNull();
        
        verify(loadUserPort).existsById(userId);
    }

    @Test
    @DisplayName("주문 검증 실패 - 유효하지 않은 사용자 ID")
    void validateOrder_Failure_InvalidUserId() {
        // given
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(null, List.of(), null);

        // when
        OrderDomainService.OrderValidationResult result = orderDomainService.validateOrder(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("유효하지 않은 사용자 ID입니다.");
        
        verify(loadUserPort, never()).existsById(any());
    }

    @Test
    @DisplayName("주문 검증 실패 - 주문 아이템 없음")
    void validateOrder_Failure_EmptyOrderItems() {
        // given
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(1L, List.of(), null);

        // when
        OrderDomainService.OrderValidationResult result = orderDomainService.validateOrder(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("주문 아이템이 없습니다.");
        
        verify(loadUserPort, never()).existsById(any());
    }

    @Test
    @DisplayName("주문 검증 실패 - 유효하지 않은 수량")
    void validateOrder_Failure_InvalidQuantity() {
        // given
        CreateOrderUseCase.OrderItemCommand invalidItem = new CreateOrderUseCase.OrderItemCommand(1L, 0);
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(1L, List.of(invalidItem), null);

        // when
        OrderDomainService.OrderValidationResult result = orderDomainService.validateOrder(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("유효하지 않은 주문 수량입니다.");
        
        verify(loadUserPort, never()).existsById(any());
    }

    @Test
    @DisplayName("주문 검증 실패 - 존재하지 않는 사용자")
    void validateOrder_Failure_UserNotFound() {
        // given
        Long userId = 999L;
        CreateOrderUseCase.OrderItemCommand orderItem = new CreateOrderUseCase.OrderItemCommand(1L, 2);
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItem), null);

        when(loadUserPort.existsById(userId)).thenReturn(false);

        // when
        OrderDomainService.OrderValidationResult result = orderDomainService.validateOrder(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("존재하지 않는 사용자입니다.");
        
        verify(loadUserPort).existsById(userId);
    }

    @Test
    @DisplayName("주문 생성 및 저장 성공")
    void createAndSaveOrder_Success() {
        // given
        Long userId = 1L;
        CreateOrderUseCase.OrderItemCommand orderItemCommand = new CreateOrderUseCase.OrderItemCommand(1L, 2);
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), null);

        OrderItem orderItem = OrderItem.builder()
            .productId(1L)
            .productName("테스트 상품")
            .quantity(2)
            .unitPrice(BigDecimal.valueOf(10000))
            .build();

        List<OrderItem> orderItems = List.of(orderItem);
        BigDecimal totalAmount = BigDecimal.valueOf(20000);
        BigDecimal discountedAmount = BigDecimal.valueOf(20000);
        BigDecimal discountAmount = BigDecimal.ZERO;

        Order savedOrder = Order.builder()
            .id(1L)
            .userId(userId)
            .orderItems(orderItems)
            .totalAmount(totalAmount)
            .discountedAmount(discountedAmount)
            .orderedAt(LocalDateTime.now())
            .status(Order.OrderStatus.COMPLETED)
            .build();

        when(saveOrderPort.saveOrder(any(Order.class))).thenReturn(savedOrder);

        // when
        OrderDomainService.OrderCreationResult result = orderDomainService.createAndSaveOrder(
            command, orderItems, totalAmount, discountedAmount, discountAmount);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrder()).isNotNull();
        assertThat(result.getOrder().getId()).isEqualTo(1L);
        assertThat(result.getOrder().getUserId()).isEqualTo(userId);
        assertThat(result.getErrorMessage()).isNull();
        
        verify(saveOrderPort).saveOrder(any(Order.class));
        // 비동기 이벤트 발행 검증 (데이터 플랫폼 전송, 상품 랭킹 업데이트)
        verify(asyncEventPublisher, times(2)).publishAsync(any(), any());
    }

    @Test
    @DisplayName("주문 생성 및 저장 실패 - 저장 중 예외 발생")
    void createAndSaveOrder_Failure_SaveException() {
        // given
        Long userId = 1L;
        CreateOrderUseCase.OrderItemCommand orderItemCommand = new CreateOrderUseCase.OrderItemCommand(1L, 2);
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), null);

        OrderItem orderItem = OrderItem.builder()
            .productId(1L)
            .productName("테스트 상품")
            .quantity(2)
            .unitPrice(BigDecimal.valueOf(10000))
            .build();

        List<OrderItem> orderItems = List.of(orderItem);
        BigDecimal totalAmount = BigDecimal.valueOf(20000);
        BigDecimal discountedAmount = BigDecimal.valueOf(20000);
        BigDecimal discountAmount = BigDecimal.ZERO;

        when(saveOrderPort.saveOrder(any(Order.class))).thenThrow(new RuntimeException("DB 저장 실패"));

        // when
        OrderDomainService.OrderCreationResult result = orderDomainService.createAndSaveOrder(
            command, orderItems, totalAmount, discountedAmount, discountAmount);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getOrder()).isNull();
        assertThat(result.getErrorMessage()).contains("주문 생성 중 오류가 발생했습니다");
        
        verify(saveOrderPort).saveOrder(any(Order.class));
        verify(asyncEventPublisher, never()).publishAsync(any(), any());
    }

    @Test
    @DisplayName("주문 생성 성공 - 쿠폰 사용")
    void createAndSaveOrder_Success_WithCoupon() {
        // given
        Long userId = 1L;
        Long userCouponId = 1L;
        CreateOrderUseCase.OrderItemCommand orderItemCommand = new CreateOrderUseCase.OrderItemCommand(1L, 2);
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), userCouponId);

        OrderItem orderItem = OrderItem.builder()
            .productId(1L)
            .productName("테스트 상품")
            .quantity(2)
            .unitPrice(BigDecimal.valueOf(10000))
            .build();

        List<OrderItem> orderItems = List.of(orderItem);
        BigDecimal totalAmount = BigDecimal.valueOf(20000);
        BigDecimal discountedAmount = BigDecimal.valueOf(18000);
        BigDecimal discountAmount = BigDecimal.valueOf(2000);

        Order savedOrder = Order.builder()
            .id(1L)
            .userId(userId)
            .userCouponId(userCouponId)
            .orderItems(orderItems)
            .totalAmount(totalAmount)
            .discountedAmount(discountedAmount)
            .discountAmount(discountAmount)
            .orderedAt(LocalDateTime.now())
            .status(Order.OrderStatus.COMPLETED)
            .build();

        when(saveOrderPort.saveOrder(any(Order.class))).thenReturn(savedOrder);

        // when
        OrderDomainService.OrderCreationResult result = orderDomainService.createAndSaveOrder(
            command, orderItems, totalAmount, discountedAmount, discountAmount);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrder().getUserCouponId()).isEqualTo(userCouponId);
        assertThat(result.getOrder().getDiscountAmount()).isEqualTo(discountAmount);
        assertThat(result.getOrder().getDiscountedAmount()).isEqualTo(discountedAmount);
        
        verify(saveOrderPort).saveOrder(any(Order.class));
        verify(asyncEventPublisher, times(2)).publishAsync(any(), any());
    }
}