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
import kr.hhplus.be.server.product.domain.service.ProductDomainService;
import kr.hhplus.be.server.coupon.domain.service.CouponDomainService;
import kr.hhplus.be.server.balance.domain.service.BalanceDomainService;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;

/**
 * CreateOrderService 오케스트레이션 테스트
 * 도메인 서비스 간 협력과 보상 트랜잭션에 초점을 맞춘 테스트
 */
@ExtendWith(MockitoExtension.class)
class CreateOrderServiceTest {

    @Mock
    private OrderDomainService orderDomainService;
    
    @Mock
    private ProductDomainService productDomainService;
    
    @Mock
    private CouponDomainService couponDomainService;
    
    @Mock
    private BalanceDomainService balanceDomainService;

    private CreateOrderService createOrderService;

    @BeforeEach
    void setUp() {
        createOrderService = new CreateOrderService(
            orderDomainService,
            productDomainService,
            couponDomainService,
            balanceDomainService
        );      
    }

    @Test
    @DisplayName("주문 성공 - 전체 플로우 정상 동작")
    void createOrder_Success_FullFlow() {
        // given
        Long userId = 1L;
        CreateOrderUseCase.OrderItemCommand orderItemCommand = 
            new CreateOrderUseCase.OrderItemCommand(1L, 2);
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), null);

        OrderItem orderItem = OrderItem.builder()
            .productId(1L)
            .productName("테스트 상품")
            .quantity(2)
            .unitPrice(BigDecimal.valueOf(10000))
            .build();

        // Mock domain service responses
        when(orderDomainService.validateOrder(command))
            .thenReturn(OrderDomainService.OrderValidationResult.success());
        
        when(productDomainService.processStockDeduction(command))
            .thenReturn(ProductDomainService.StockProcessResult.success(List.of(orderItem), BigDecimal.valueOf(20000)));
        
        when(couponDomainService.processCouponDiscount(eq(command), any(BigDecimal.class)))
            .thenReturn(CouponDomainService.CouponProcessResult.success(BigDecimal.valueOf(20000), 0));
        
        when(balanceDomainService.processBalanceDeduction(eq(userId), any(BigDecimal.class)))
            .thenReturn(BalanceDomainService.BalanceProcessResult.success(BigDecimal.valueOf(80000)));
        
        Order savedOrder = Order.builder()
            .id(1L)
            .userId(userId)
            .orderItems(List.of(orderItem))
            .totalAmount(BigDecimal.valueOf(20000))
            .discountedAmount(BigDecimal.valueOf(20000))
            .orderedAt(LocalDateTime.now())
            .status(Order.OrderStatus.COMPLETED)
            .build();
        
        when(orderDomainService.createAndSaveOrder(eq(command), any(), any(), any(), any()))
            .thenReturn(OrderDomainService.OrderCreationResult.success(savedOrder));

        // when
        CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrderId()).isEqualTo(1L);
        
        // 모든 도메인 서비스가 순서대로 호출되었는지 검증
        verify(orderDomainService).validateOrder(command);
        verify(productDomainService).processStockDeduction(command);
        verify(couponDomainService).processCouponDiscount(eq(command), any(BigDecimal.class));
        verify(balanceDomainService).processBalanceDeduction(eq(userId), any(BigDecimal.class));
        verify(orderDomainService).createAndSaveOrder(eq(command), any(), any(), any(), any());
    }

    @Test
    @DisplayName("주문 실패 - 검증 실패 시 후속 로직 실행되지 않음")
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
        
        // 검증 실패 후 다른 서비스 호출되지 않았는지 확인
        verify(orderDomainService).validateOrder(command);
        verify(productDomainService, never()).processStockDeduction(any());
        verify(couponDomainService, never()).processCouponDiscount(any(), any());
        verify(balanceDomainService, never()).processBalanceDeduction(any(), any());
        verify(orderDomainService, never()).createAndSaveOrder(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("주문 실패 - 재고 처리 실패 시 보상 트랜잭션 없음")
    void createOrder_Failure_StockProcessingFailed() {
        // given
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(1L, List.of(new CreateOrderUseCase.OrderItemCommand(1L, 2)), null);

        when(orderDomainService.validateOrder(command))
            .thenReturn(OrderDomainService.OrderValidationResult.success());
        
        when(productDomainService.processStockDeduction(command))
            .thenReturn(ProductDomainService.StockProcessResult.failure("재고가 부족합니다."));

        // when
        CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("재고가 부족합니다.");
        
        // 재고 처리 실패 시점에서 보상 트랜잭션이 필요하지 않음
        verify(productDomainService, never()).rollbackStockDeduction(any(), any());
        verify(couponDomainService, never()).rollbackCouponUsage(any(), any());
    }

    @Test
    @DisplayName("주문 실패 - 쿠폰 처리 실패 시 재고 보상 트랜잭션 실행")
    void createOrder_Failure_CouponProcessingFailed_WithStockCompensation() {
        // given
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(1L, List.of(new CreateOrderUseCase.OrderItemCommand(1L, 2)), null);

        OrderItem orderItem = OrderItem.builder()
            .productId(1L)
            .quantity(2)
            .build();

        when(orderDomainService.validateOrder(command))
            .thenReturn(OrderDomainService.OrderValidationResult.success());
        
        when(productDomainService.processStockDeduction(command))
            .thenReturn(ProductDomainService.StockProcessResult.success(List.of(orderItem), BigDecimal.valueOf(20000)));
        
        when(couponDomainService.processCouponDiscount(eq(command), any(BigDecimal.class)))
            .thenReturn(CouponDomainService.CouponProcessResult.failure("쿠폰을 사용할 수 없습니다."));

        // when
        CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("쿠폰을 사용할 수 없습니다.");
        
        // 재고 보상 트랜잭션이 실행되었는지 확인
        verify(productDomainService).rollbackStockDeduction(List.of(orderItem), "쿠폰 사용 실패");
        verify(couponDomainService, never()).rollbackCouponUsage(any(), any());
    }

    @Test
    @DisplayName("주문 실패 - 잔액 차감 실패 시 쿠폰, 재고 보상 트랜잭션 실행")
    void createOrder_Failure_BalanceDeductionFailed_WithFullCompensation() {
        // given
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(1L, List.of(new CreateOrderUseCase.OrderItemCommand(1L, 2)), 1L);

        OrderItem orderItem = OrderItem.builder()
            .productId(1L)
            .quantity(2)
            .build();

        when(orderDomainService.validateOrder(command))
            .thenReturn(OrderDomainService.OrderValidationResult.success());
        
        when(productDomainService.processStockDeduction(command))
            .thenReturn(ProductDomainService.StockProcessResult.success(List.of(orderItem), BigDecimal.valueOf(20000)));
        
        when(couponDomainService.processCouponDiscount(eq(command), any(BigDecimal.class)))
            .thenReturn(CouponDomainService.CouponProcessResult.success(BigDecimal.valueOf(18000), 2000));
        
        when(balanceDomainService.processBalanceDeduction(eq(1L), any(BigDecimal.class)))
            .thenReturn(BalanceDomainService.BalanceProcessResult.failure("잔액이 부족합니다."));

        // when
        CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("잔액이 부족합니다.");
        
        // 쿠폰과 재고 모두 보상 트랜잭션이 실행되었는지 확인
        verify(couponDomainService).rollbackCouponUsage(command, "잔액 차감 실패");
        verify(productDomainService).rollbackStockDeduction(List.of(orderItem), "잔액 차감 실패");
    }

    @Test
    @DisplayName("주문 실패 - 주문 저장 실패 시 모든 보상 트랜잭션 실행")
    void createOrder_Failure_OrderCreationFailed_WithFullCompensation() {
        // given
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(1L, List.of(new CreateOrderUseCase.OrderItemCommand(1L, 2)), 1L);

        OrderItem orderItem = OrderItem.builder()
            .productId(1L)
            .quantity(2)
            .build();

        when(orderDomainService.validateOrder(command))
            .thenReturn(OrderDomainService.OrderValidationResult.success());
        
        when(productDomainService.processStockDeduction(command))
            .thenReturn(ProductDomainService.StockProcessResult.success(List.of(orderItem), BigDecimal.valueOf(20000)));
        
        when(couponDomainService.processCouponDiscount(eq(command), any(BigDecimal.class)))
            .thenReturn(CouponDomainService.CouponProcessResult.success(BigDecimal.valueOf(18000), 2000));
        
        when(balanceDomainService.processBalanceDeduction(eq(1L), any(BigDecimal.class)))
            .thenReturn(BalanceDomainService.BalanceProcessResult.success(BigDecimal.valueOf(80000)));
        
        when(orderDomainService.createAndSaveOrder(eq(command), any(), any(), any(), any()))
            .thenReturn(OrderDomainService.OrderCreationResult.failure("주문 저장 중 오류가 발생했습니다."));

        // when
        CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("주문 저장 중 오류가 발생했습니다.");
        
        // 모든 보상 트랜잭션이 실행되었는지 확인
        verify(couponDomainService).rollbackCouponUsage(command, "주문 저장 실패");
        verify(productDomainService).rollbackStockDeduction(List.of(orderItem), "주문 저장 실패");
    }
}