package kr.hhplus.be.server.order.application.facade;

import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.coupon.application.port.in.UseCouponUseCase;
import kr.hhplus.be.server.order.application.port.out.DeductBalancePort;
import kr.hhplus.be.server.order.application.port.out.LoadProductPort;
import kr.hhplus.be.server.order.application.port.out.LoadUserPort;
import kr.hhplus.be.server.order.application.port.out.SaveOrderPort;
import kr.hhplus.be.server.order.application.port.out.UpdateProductStockPort;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

    @Mock
    private LoadUserPort loadUserPort;
    
    @Mock
    private LoadProductPort loadProductPort;
    
    @Mock
    private UpdateProductStockPort updateProductStockPort;
    
    @Mock
    private DeductBalancePort deductBalancePort;
    
    @Mock
    private SaveOrderPort saveOrderPort;
    
    @Mock
    private UseCouponUseCase useCouponUseCase;

    private OrderFacade orderFacade;

    @BeforeEach
    void setUp() {
        orderFacade = new OrderFacade(loadUserPort, loadProductPort, updateProductStockPort,
                deductBalancePort, saveOrderPort, useCouponUseCase);
    }

    @Test
    @DisplayName("주문 생성 성공 - 쿠폰 없이")
    void createOrder_Success_WithoutCoupon() {
        // given
        Long userId = 1L;
        Long productId = 1L;
        Integer quantity = 2;
        Long userCouponId = null;
        
        CreateOrderUseCase.CreateOrderCommand command = new CreateOrderUseCase.CreateOrderCommand(
                userId, 
                List.of(new CreateOrderUseCase.OrderItemCommand(productId, quantity)), 
                userCouponId);
        
                LoadProductPort.ProductInfo productInfo = new LoadProductPort.ProductInfo(
                        productId, "테스트 상품", BigDecimal.valueOf(10000), Integer.valueOf(100));
                        
        Order savedOrder = new Order(userId, new ArrayList<>(), BigDecimal.valueOf(20000), userCouponId);
        savedOrder.setId(1L);
        savedOrder.setDiscountedAmount(BigDecimal.valueOf(20000));
        savedOrder.setStatus(Order.OrderStatus.COMPLETED);
        savedOrder.setOrderedAt(LocalDateTime.now());
        
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadProductPort.loadProductById(productId)).thenReturn(java.util.Optional.of(productInfo));
        when(updateProductStockPort.deductStock(productId, quantity)).thenReturn(true);
        when(deductBalancePort.deductBalance(userId, BigDecimal.valueOf(20000))).thenReturn(true);
        when(saveOrderPort.saveOrder(any(Order.class))).thenReturn(savedOrder);

        // when
        CreateOrderUseCase.CreateOrderResult result = orderFacade.createOrder(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getUserCouponId()).isNull();
        assertThat(result.getTotalAmount()).isEqualTo(BigDecimal.valueOf(20000));
        assertThat(result.getDiscountedAmount()).isEqualTo(BigDecimal.valueOf(20000));
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getOrderItems()).hasSize(1);
        assertThat(result.getErrorMessage()).isNull();
        
        verify(loadUserPort).existsById(userId);
        verify(loadProductPort).loadProductById(productId);
        verify(updateProductStockPort).deductStock(productId, quantity);
        verify(deductBalancePort).deductBalance(userId, BigDecimal.valueOf(20000));
        verify(saveOrderPort).saveOrder(any(Order.class));
        verify(useCouponUseCase, never()).useCoupon(any());
    }

    @Test
    @DisplayName("주문 생성 성공 - 쿠폰 사용")
    void createOrder_Success_WithCoupon() {
        // given
        Long userId = 1L;
        Long productId = 1L;
        Integer quantity = 2;
        Long userCouponId = 1L;
        
        CreateOrderUseCase.CreateOrderCommand command = new CreateOrderUseCase.CreateOrderCommand(
                userId, 
                List.of(new CreateOrderUseCase.OrderItemCommand(productId, quantity)), 
                userCouponId);
        
        LoadProductPort.ProductInfo productInfo = new LoadProductPort.ProductInfo(
                productId, "테스트 상품", BigDecimal.valueOf(10000), Integer.valueOf(100));
        
        UseCouponUseCase.UseCouponResult couponResult = UseCouponUseCase.UseCouponResult.success(
                BigDecimal.valueOf(18000), 2000);
        
        Order savedOrder = new Order(userId, new ArrayList<>(), BigDecimal.valueOf(20000), userCouponId);
        savedOrder.setId(1L);
        savedOrder.setDiscountedAmount(BigDecimal.valueOf(18000));
        savedOrder.setStatus(Order.OrderStatus.COMPLETED);
        savedOrder.setOrderedAt(LocalDateTime.now());
        
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadProductPort.loadProductById(productId)).thenReturn(java.util.Optional.of(productInfo));
        when(updateProductStockPort.deductStock(productId, quantity)).thenReturn(true);
        when(useCouponUseCase.useCoupon(any(UseCouponUseCase.UseCouponCommand.class))).thenReturn(couponResult);
        when(deductBalancePort.deductBalance(userId, BigDecimal.valueOf(18000))).thenReturn(true);
        when(saveOrderPort.saveOrder(any(Order.class))).thenReturn(savedOrder);

        // when
        CreateOrderUseCase.CreateOrderResult result = orderFacade.createOrder(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getUserCouponId()).isEqualTo(userCouponId);
        assertThat(result.getTotalAmount()).isEqualTo(BigDecimal.valueOf(20000));
        assertThat(result.getDiscountedAmount()).isEqualTo(BigDecimal.valueOf(18000));
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getOrderItems()).hasSize(1);
        
        verify(loadUserPort).existsById(userId);
        verify(loadProductPort).loadProductById(productId);
        verify(updateProductStockPort).deductStock(productId, quantity);
        verify(useCouponUseCase).useCoupon(any(UseCouponUseCase.UseCouponCommand.class));
        verify(deductBalancePort).deductBalance(userId, BigDecimal.valueOf(18000));
        verify(saveOrderPort).saveOrder(any(Order.class));
    }

    @Test
    @DisplayName("주문 생성 실패 - 사용자가 존재하지 않는 경우")
    void createOrder_Failure_UserNotFound() {
        // given
        Long userId = 999L;
        Long productId = 1L;
        Integer quantity = 2;
        
        CreateOrderUseCase.CreateOrderCommand command = new CreateOrderUseCase.CreateOrderCommand(
                userId, 
                List.of(new CreateOrderUseCase.OrderItemCommand(productId, quantity)), 
                null);
        
        when(loadUserPort.existsById(userId)).thenReturn(false);

        // when
        CreateOrderUseCase.CreateOrderResult result = orderFacade.createOrder(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("사용자를 찾을 수 없습니다.");
        assertThat(result.getOrderId()).isNull();
        
        verify(loadUserPort).existsById(userId);
        verify(loadProductPort, never()).loadProductById(any());
        verify(updateProductStockPort, never()).deductStock(any(), any());
        verify(deductBalancePort, never()).deductBalance(any(), any());
        verify(saveOrderPort, never()).saveOrder(any());
    }

    @Test
    @DisplayName("주문 생성 실패 - 상품이 존재하지 않는 경우")
    void createOrder_Failure_ProductNotFound() {
        // given
        Long userId = 1L;
        Long productId = 999L;
        Integer quantity = 2;
        
        CreateOrderUseCase.CreateOrderCommand command = new CreateOrderUseCase.CreateOrderCommand(
                userId, 
                List.of(new CreateOrderUseCase.OrderItemCommand(productId, quantity)), 
                null);
        
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadProductPort.loadProductById(productId)).thenReturn(java.util.Optional.empty());

        // when
        CreateOrderUseCase.CreateOrderResult result = orderFacade.createOrder(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("상품을 찾을 수 없습니다: " + productId);
        assertThat(result.getOrderId()).isNull();
        
        verify(loadUserPort).existsById(userId);
        verify(loadProductPort).loadProductById(productId);
        verify(updateProductStockPort, never()).deductStock(any(), any());
        verify(deductBalancePort, never()).deductBalance(any(), any());
        verify(saveOrderPort, never()).saveOrder(any());
    }

    @Test
    @DisplayName("주문 생성 실패 - 재고 부족")
    void createOrder_Failure_InsufficientStock() {
        // given
        Long userId = 1L;
        Long productId = 1L;
        Integer quantity = 200; // 재고보다 많은 수량
        
        CreateOrderUseCase.CreateOrderCommand command = new CreateOrderUseCase.CreateOrderCommand(
                userId, 
                List.of(new CreateOrderUseCase.OrderItemCommand(productId, quantity)), 
                null);
        
        LoadProductPort.ProductInfo productInfo = new LoadProductPort.ProductInfo(
                productId, "테스트 상품", BigDecimal.valueOf(10000), Integer.valueOf(100));
        
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadProductPort.loadProductById(productId)).thenReturn(java.util.Optional.of(productInfo));

        // when
        CreateOrderUseCase.CreateOrderResult result = orderFacade.createOrder(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("재고가 부족합니다: 100");
        assertThat(result.getOrderId()).isNull();
        
        verify(loadUserPort).existsById(userId);
        verify(loadProductPort).loadProductById(productId);
        verify(updateProductStockPort, never()).deductStock(any(), any());
        verify(deductBalancePort, never()).deductBalance(any(), any());
        verify(saveOrderPort, never()).saveOrder(any());
    }

    @Test
    @DisplayName("주문 생성 실패 - 잔액 부족")
    void createOrder_Failure_InsufficientBalance() {
        // given
        Long userId = 1L;
        Long productId = 1L;
        Integer quantity = 2;
        
        CreateOrderUseCase.CreateOrderCommand command = new CreateOrderUseCase.CreateOrderCommand(
                userId, 
                List.of(new CreateOrderUseCase.OrderItemCommand(productId, quantity)), 
                null);
        
        LoadProductPort.ProductInfo productInfo = new LoadProductPort.ProductInfo(
                productId, "테스트 상품", BigDecimal.valueOf(10000), Integer.valueOf(100));
        
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadProductPort.loadProductById(productId)).thenReturn(java.util.Optional.of(productInfo));
        when(updateProductStockPort.deductStock(productId, quantity)).thenReturn(true);
        when(deductBalancePort.deductBalance(userId, BigDecimal.valueOf(20000))).thenReturn(false);

        // when
        CreateOrderUseCase.CreateOrderResult result = orderFacade.createOrder(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("잔액이 부족합니다.");
        assertThat(result.getOrderId()).isNull();
        
        verify(loadUserPort).existsById(userId);
        verify(loadProductPort).loadProductById(productId);
        verify(updateProductStockPort).deductStock(productId, quantity);
        verify(deductBalancePort).deductBalance(userId, BigDecimal.valueOf(20000));
        verify(saveOrderPort, never()).saveOrder(any());
    }

    @Test
    @DisplayName("주문 생성 실패 - 쿠폰 사용 실패")
    void createOrder_Failure_CouponUseFailed() {
        // given
        Long userId = 1L;
        Long productId = 1L;
        Integer quantity = 2;
        Long userCouponId = 1L;
        
        CreateOrderUseCase.CreateOrderCommand command = new CreateOrderUseCase.CreateOrderCommand(
                userId, 
                List.of(new CreateOrderUseCase.OrderItemCommand(productId, quantity)), 
                userCouponId);
        
        LoadProductPort.ProductInfo productInfo = new LoadProductPort.ProductInfo(
                productId, "테스트 상품", BigDecimal.valueOf(10000), Integer.valueOf(100));
        
        UseCouponUseCase.UseCouponResult couponResult = UseCouponUseCase.UseCouponResult.failure("쿠폰을 찾을 수 없습니다.");
        
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadProductPort.loadProductById(productId)).thenReturn(java.util.Optional.of(productInfo));
        when(updateProductStockPort.deductStock(productId, quantity)).thenReturn(true);
        when(useCouponUseCase.useCoupon(any(UseCouponUseCase.UseCouponCommand.class))).thenReturn(couponResult);

        // when
        CreateOrderUseCase.CreateOrderResult result = orderFacade.createOrder(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("쿠폰을 찾을 수 없습니다.");
        assertThat(result.getOrderId()).isNull();
        
        verify(loadUserPort).existsById(userId);
        verify(loadProductPort).loadProductById(productId);
        verify(updateProductStockPort).deductStock(productId, quantity);
        verify(useCouponUseCase).useCoupon(any(UseCouponUseCase.UseCouponCommand.class));
        verify(deductBalancePort, never()).deductBalance(any(), any());
        verify(saveOrderPort, never()).saveOrder(any());
    }
} 