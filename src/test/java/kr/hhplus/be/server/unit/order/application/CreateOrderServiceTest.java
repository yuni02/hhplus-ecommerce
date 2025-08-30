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
import kr.hhplus.be.server.order.application.port.out.LoadProductPort;
import kr.hhplus.be.server.order.application.port.out.UpdateProductStockPort;
import kr.hhplus.be.server.order.application.port.out.DeductBalancePort;
import kr.hhplus.be.server.balance.application.port.out.LoadBalancePort;
import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.order.domain.service.OrderDomainService;
import kr.hhplus.be.server.shared.event.AsyncEventPublisher;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;

/**
 * CreateOrderService 단위 테스트
 * 실제 구현에 맞춘 테스트
 */
@ExtendWith(MockitoExtension.class)
class CreateOrderServiceTest {

    @Mock
    private OrderDomainService orderDomainService;
    
    @Mock
    private LoadProductPort loadProductPort;
    
    @Mock
    private UpdateProductStockPort updateProductStockPort;
    
    @Mock
    private LoadBalancePort loadBalancePort;
    
    @Mock
    private DeductBalancePort deductBalancePort;
    
    @Mock
    private AsyncEventPublisher eventPublisher;

    private CreateOrderService createOrderService;

    @BeforeEach
    void setUp() {
        createOrderService = new CreateOrderService(
            orderDomainService,
            loadProductPort,
            updateProductStockPort,
            loadBalancePort,
            deductBalancePort
        );      
    }

    @Test
    @DisplayName("주문 성공 - 모든 단계가 정상적으로 처리됨")
    void createOrder_Success_AllStepsCompleted() {
        // given
        Long userId = 1L;
        Long productId = 1L;
        CreateOrderUseCase.OrderItemCommand orderItemCommand = 
            new CreateOrderUseCase.OrderItemCommand(productId, 2);
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), null);

        // Mock domain service response
        when(orderDomainService.validateOrder(command))
            .thenReturn(OrderDomainService.OrderValidationResult.success());

        // Mock product info
        LoadProductPort.ProductInfo productInfo = new LoadProductPort.ProductInfo(
            productId, "테스트 상품", "테스트 상품 설명", 10, new BigDecimal("10000"), "ACTIVE"
        );
        when(loadProductPort.loadProductByIdWithLock(productId))
            .thenReturn(java.util.Optional.of(productInfo));

        // Mock stock deduction
        when(updateProductStockPort.deductStockWithPessimisticLock(productId, 2))
            .thenReturn(true);

        // Mock balance
        Balance balance = Balance.builder()
            .userId(userId)
            .amount(new BigDecimal("50000"))
            .status(Balance.BalanceStatus.ACTIVE)
            .build();
        when(loadBalancePort.loadActiveBalanceByUserIdWithLock(userId))
            .thenReturn(java.util.Optional.of(balance));

        // Mock balance deduction
        when(deductBalancePort.deductBalanceWithPessimisticLock(userId, new BigDecimal("20000")))
            .thenReturn(true);

        // when
        CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrderId()).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("20000"));
        
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
        verify(eventPublisher, never()).publishAsync(any());
    }

    @Test
    @DisplayName("주문 실패 - 상품이 존재하지 않는 경우")
    void createOrder_Failure_ProductNotFound() {
        // given
        Long userId = 1L;
        Long productId = 999L;
        CreateOrderUseCase.OrderItemCommand orderItemCommand = 
            new CreateOrderUseCase.OrderItemCommand(productId, 2);
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), null);

        when(orderDomainService.validateOrder(command))
            .thenReturn(OrderDomainService.OrderValidationResult.success());

        when(loadProductPort.loadProductByIdWithLock(productId))
            .thenReturn(java.util.Optional.empty());

        // when
        CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("존재하지 않는 상품입니다");
        
        verify(eventPublisher, never()).publishAsync(any());
    }

    @Test
    @DisplayName("주문 실패 - 재고 부족")
    void createOrder_Failure_InsufficientStock() {
        // given
        Long userId = 1L;
        Long productId = 1L;
        CreateOrderUseCase.OrderItemCommand orderItemCommand = 
            new CreateOrderUseCase.OrderItemCommand(productId, 5);
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), null);

        when(orderDomainService.validateOrder(command))
            .thenReturn(OrderDomainService.OrderValidationResult.success());
         
        LoadProductPort.ProductInfo productInfo = new LoadProductPort.ProductInfo(
            productId, "테스트 상품", "테스트 상품 설명", 2, new BigDecimal("10000"), "ACTIVE"
        );
        when(loadProductPort.loadProductByIdWithLock(productId))
            .thenReturn(java.util.Optional.of(productInfo));

        // when
        CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("재고가 부족합니다");
        
        verify(eventPublisher, never()).publishAsync(any());
    }

    @Test
    @DisplayName("주문 실패 - 잔액 부족")
    void createOrder_Failure_InsufficientBalance() {
        // given
        Long userId = 1L;
        Long productId = 1L;
        CreateOrderUseCase.OrderItemCommand orderItemCommand = 
            new CreateOrderUseCase.OrderItemCommand(productId, 2);
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), null);

        when(orderDomainService.validateOrder(command))
            .thenReturn(OrderDomainService.OrderValidationResult.success());

        LoadProductPort.ProductInfo productInfo = new LoadProductPort.ProductInfo(
            productId, "테스트 상품", "테스트 상품 설명", 10, new BigDecimal("10000"), "ACTIVE"
        );
        when(loadProductPort.loadProductByIdWithLock(productId))
            .thenReturn(java.util.Optional.of(productInfo));

        when(updateProductStockPort.deductStockWithPessimisticLock(productId, 2))
            .thenReturn(true);

        Balance balance = Balance.builder()
            .userId(userId)
            .amount(new BigDecimal("5000"))
            .status(Balance.BalanceStatus.ACTIVE)
            .build(); // 부족한 잔액
        when(loadBalancePort.loadActiveBalanceByUserIdWithLock(userId))
            .thenReturn(java.util.Optional.of(balance));

        // when
        CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("잔액이 부족합니다");
        
        // 재고 롤백이 호출되었는지 확인
        verify(updateProductStockPort).restoreStock(productId, 2);
        verify(eventPublisher, never()).publishAsync(any());
    }

    @Test
    @DisplayName("주문 처리 - 쿠폰이 있는 경우 쿠폰 ID 포함")
    void createOrder_WithCoupon() {
        // given
        Long userId = 1L;
        Long userCouponId = 100L;
        Long productId = 1L;
        CreateOrderUseCase.OrderItemCommand orderItemCommand = 
            new CreateOrderUseCase.OrderItemCommand(productId, 2);
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), userCouponId);

        when(orderDomainService.validateOrder(command))
            .thenReturn(OrderDomainService.OrderValidationResult.success());

        LoadProductPort.ProductInfo productInfo = new LoadProductPort.ProductInfo(
            productId, "테스트 상품", "테스트 상품 설명", 10, new BigDecimal("10000"), "ACTIVE"
        );
        when(loadProductPort.loadProductByIdWithLock(productId))
            .thenReturn(java.util.Optional.of(productInfo));

        when(updateProductStockPort.deductStockWithPessimisticLock(productId, 2))
            .thenReturn(true);

        Balance balance = Balance.builder()
            .userId(userId)
            .amount(new BigDecimal("50000"))
            .status(Balance.BalanceStatus.ACTIVE)
            .build();
        when(loadBalancePort.loadActiveBalanceByUserIdWithLock(userId))
            .thenReturn(java.util.Optional.of(balance));

        when(deductBalancePort.deductBalanceWithPessimisticLock(userId, new BigDecimal("20000")))
            .thenReturn(true);

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