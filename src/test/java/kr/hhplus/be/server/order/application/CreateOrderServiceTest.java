package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.order.application.port.out.LoadUserPort;
import kr.hhplus.be.server.order.application.port.out.LoadProductPort;
import kr.hhplus.be.server.order.application.port.out.UpdateProductStockPort;
import kr.hhplus.be.server.order.application.port.out.DeductBalancePort;
import kr.hhplus.be.server.order.application.port.out.SaveOrderPort;
import kr.hhplus.be.server.coupon.application.port.in.UseCouponUseCase;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrderServiceTest {

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

    private CreateOrderService createOrderService;

    @BeforeEach
    void setUp() {
        createOrderService = new CreateOrderService(
            loadUserPort, 
            loadProductPort, 
            updateProductStockPort, 
            deductBalancePort, 
            saveOrderPort, 
            useCouponUseCase
        );
    }

    // @Test
    // @DisplayName("주문 생성 성공 - 쿠폰 없이")
    // void createOrder_Success_WithoutCoupon() {
    //     // given
    //     Long userId = 1L;
    //     CreateOrderUseCase.OrderItemCommand orderItemCommand = 
    //         new CreateOrderUseCase.OrderItemCommand(1L, 2);
    //     CreateOrderUseCase.CreateOrderCommand command = 
    //         new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), null);

    //     LoadProductPort.ProductInfo productInfo = new LoadProductPort.ProductInfo(
    //         1L, "상품A", BigDecimal.valueOf(10000), 100);

    //     Order savedOrder = new Order(userId, List.of(), BigDecimal.valueOf(20000), null);
    //     savedOrder.setId(1L);
    //     savedOrder.setOrderedAt(LocalDateTime.now());

    //     when(loadUserPort.existsById(userId)).thenReturn(true);
    //     when(loadProductPort.loadProductById(1L)).thenReturn(Optional.of(productInfo));
    //     when(updateProductStockPort.deductStock(1L, 2)).thenReturn(true);
    //     when(deductBalancePort.deductBalance(eq(userId), eq(BigDecimal.valueOf(20000)))).thenReturn(true);
    //     when(saveOrderPort.saveOrder(any(Order.class))).thenReturn(savedOrder);

    //     // when
    //     CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

    //     // then
    //     assertThat(result.isSuccess()).isTrue();
    //     assertThat(result.getOrderId()).isEqualTo(1L);
    //     assertThat(result.getUserId()).isEqualTo(userId);
    //     assertThat(result.getTotalAmount()).isEqualTo(BigDecimal.valueOf(20000));
    //     assertThat(result.getDiscountedAmount()).isEqualTo(BigDecimal.valueOf(20000));
    //     assertThat(result.getUserCouponId()).isNull();
    //     assertThat(result.getStatus()).isEqualTo("COMPLETED");
        
    //     verify(loadUserPort).existsById(userId);
    //     verify(loadProductPort).loadProductById(1L);
    //     verify(updateProductStockPort).deductStock(1L, 2);
    //     verify(deductBalancePort).deductBalance(userId, BigDecimal.valueOf(20000));
    //     verify(saveOrderPort).saveOrder(any(Order.class));
    //     verify(useCouponUseCase, never()).useCoupon(any());
    // }

    // @Test
    // @DisplayName("주문 생성 성공 - 쿠폰 사용")
    // void createOrder_Success_WithCoupon() {
    //     // given
    //     Long userId = 1L;
    //     Long userCouponId = 1L;
    //     CreateOrderUseCase.OrderItemCommand orderItemCommand = 
    //         new CreateOrderUseCase.OrderItemCommand(1L, 2);
    //     CreateOrderUseCase.CreateOrderCommand command = 
    //         new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), userCouponId);

    //     LoadProductPort.ProductInfo productInfo = new LoadProductPort.ProductInfo(
    //         1L, "상품A", BigDecimal.valueOf(10000), 100);

    //     UseCouponUseCase.UseCouponResult couponResult = 
    //         UseCouponUseCase.UseCouponResult.success(BigDecimal.valueOf(18000), 2000);

    //     Order savedOrder = new Order(userId, List.of(), BigDecimal.valueOf(20000), userCouponId);
    //     savedOrder.setId(1L);
    //     savedOrder.setOrderedAt(LocalDateTime.now());
    //     savedOrder.setDiscountedAmount(BigDecimal.valueOf(18000));

    //     when(loadUserPort.existsById(userId)).thenReturn(true);
    //     when(loadProductPort.loadProductById(1L)).thenReturn(Optional.of(productInfo));
    //     when(updateProductStockPort.deductStock(1L, 2)).thenReturn(true);
    //     when(useCouponUseCase.useCoupon(any(UseCouponUseCase.UseCouponCommand.class))).thenReturn(couponResult);
    //     when(deductBalancePort.deductBalance(eq(userId), eq(BigDecimal.valueOf(18000)))).thenReturn(true);
    //     when(saveOrderPort.saveOrder(any(Order.class))).thenReturn(savedOrder);

    //     // when
    //     CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

    //     // then
    //     assertThat(result.isSuccess()).isTrue();
    //     assertThat(result.getOrderId()).isEqualTo(1L);
    //     assertThat(result.getUserId()).isEqualTo(userId);
    //     assertThat(result.getTotalAmount()).isEqualTo(BigDecimal.valueOf(20000));
    //     assertThat(result.getDiscountedAmount()).isEqualTo(BigDecimal.valueOf(18000));
    //     assertThat(result.getUserCouponId()).isEqualTo(userCouponId);
    //     assertThat(result.getStatus()).isEqualTo("COMPLETED");
        
    //     verify(loadUserPort).existsById(userId);
    //     verify(loadProductPort).loadProductById(1L);
    //     verify(updateProductStockPort).deductStock(1L, 2);
    //     verify(useCouponUseCase).useCoupon(any(UseCouponUseCase.UseCouponCommand.class));
    //     verify(deductBalancePort).deductBalance(userId, BigDecimal.valueOf(18000));
    //     verify(saveOrderPort).saveOrder(any(Order.class));
    // }

    // @Test
    // @DisplayName("주문 생성 실패 - 사용자가 존재하지 않음")
    // void createOrder_Failure_UserNotFound() {
    //     // given
    //     Long userId = 999L;
    //     CreateOrderUseCase.OrderItemCommand orderItemCommand = 
    //         new CreateOrderUseCase.OrderItemCommand(1L, 2);
    //     CreateOrderUseCase.CreateOrderCommand command = 
    //         new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), null);

    //     when(loadUserPort.existsById(userId)).thenReturn(false);

    //     // when
    //     CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

    //     // then
    //     assertThat(result.isSuccess()).isFalse();
    //     assertThat(result.getErrorMessage()).isEqualTo("사용자를 찾을 수 없습니다.");
        
    //     verify(loadUserPort).existsById(userId);
    //     verify(loadProductPort, never()).loadProductById(any());
    //     verify(updateProductStockPort, never()).deductStock(any(), any());
    //     verify(saveOrderPort, never()).saveOrder(any());
    // }

    // @Test
    // @DisplayName("주문 생성 실패 - 상품이 존재하지 않음")
    // void createOrder_Failure_ProductNotFound() {
    //     // given
    //     Long userId = 1L;
    //     CreateOrderUseCase.OrderItemCommand orderItemCommand = 
    //         new CreateOrderUseCase.OrderItemCommand(999L, 2);
    //     CreateOrderUseCase.CreateOrderCommand command = 
    //         new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), null);

    //     when(loadUserPort.existsById(userId)).thenReturn(true);
    //     when(loadProductPort.loadProductById(999L)).thenReturn(Optional.empty());

    //     // when
    //     CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

    //     // then
    //     assertThat(result.isSuccess()).isFalse();
    //     assertThat(result.getErrorMessage()).isEqualTo("상품을 찾을 수 없습니다: 999");
        
    //     verify(loadUserPort).existsById(userId);
    //     verify(loadProductPort).loadProductById(999L);
    //     verify(updateProductStockPort, never()).deductStock(any(), any());
    //     verify(saveOrderPort, never()).saveOrder(any());
    // }

    // @Test
    // @DisplayName("주문 생성 실패 - 재고 부족")
    // void createOrder_Failure_InsufficientStock() {
    //     // given
    //     Long userId = 1L;
    //     CreateOrderUseCase.OrderItemCommand orderItemCommand = 
    //         new CreateOrderUseCase.OrderItemCommand(1L, 200); // 재고보다 많은 수량
    //     CreateOrderUseCase.CreateOrderCommand command = 
    //         new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), null);

    //     LoadProductPort.ProductInfo productInfo = new LoadProductPort.ProductInfo(
    //         1L, "상품A", BigDecimal.valueOf(10000), 100); // 재고 100개

    //     when(loadUserPort.existsById(userId)).thenReturn(true);
    //     when(loadProductPort.loadProductById(1L)).thenReturn(Optional.of(productInfo));

    //     // when
    //     CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

    //     // then
    //     assertThat(result.isSuccess()).isFalse();
    //     assertThat(result.getErrorMessage()).isEqualTo("재고가 부족합니다. 상품: 1");
        
    //     verify(loadUserPort).existsById(userId);
    //     verify(loadProductPort).loadProductById(1L);
    //     verify(updateProductStockPort, never()).deductStock(any(), any());
    //     verify(saveOrderPort, never()).saveOrder(any());
    // }

    // @Test
    // @DisplayName("주문 생성 실패 - 재고 차감 실패")
    // void createOrder_Failure_StockDeductionFailed() {
    //     // given
    //     Long userId = 1L;
    //     CreateOrderUseCase.OrderItemCommand orderItemCommand = 
    //         new CreateOrderUseCase.OrderItemCommand(1L, 2);
    //     CreateOrderUseCase.CreateOrderCommand command = 
    //         new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), null);

    //     LoadProductPort.ProductInfo productInfo = new LoadProductPort.ProductInfo(
    //         1L, "상품A", BigDecimal.valueOf(10000), 100);

    //     when(loadUserPort.existsById(userId)).thenReturn(true);
    //     when(loadProductPort.loadProductById(1L)).thenReturn(Optional.of(productInfo));
    //     when(updateProductStockPort.deductStock(1L, 2)).thenReturn(false); // 재고 차감 실패

    //     // when
    //     CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

    //     // then
    //     assertThat(result.isSuccess()).isFalse();
    //     assertThat(result.getErrorMessage()).isEqualTo("재고 차감에 실패했습니다.");
        
    //     verify(loadUserPort).existsById(userId);
    //     verify(loadProductPort).loadProductById(1L);
    //     verify(updateProductStockPort).deductStock(1L, 2);
    //     verify(deductBalancePort, never()).deductBalance(any(), any());
    //     verify(saveOrderPort, never()).saveOrder(any());
    // }

    // @Test
    // @DisplayName("주문 생성 실패 - 쿠폰 사용 실패")
    // void createOrder_Failure_CouponUsageFailed() {
    //     // given
    //     Long userId = 1L;
    //     Long userCouponId = 1L;
    //     CreateOrderUseCase.OrderItemCommand orderItemCommand = 
    //         new CreateOrderUseCase.OrderItemCommand(1L, 2);
    //     CreateOrderUseCase.CreateOrderCommand command = 
    //         new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), userCouponId);

    //     LoadProductPort.ProductInfo productInfo = new LoadProductPort.ProductInfo(
    //         1L, "상품A", BigDecimal.valueOf(10000), 100);

    //     UseCouponUseCase.UseCouponResult couponResult = 
    //         UseCouponUseCase.UseCouponResult.failure("쿠폰을 사용할 수 없습니다.");

    //     when(loadUserPort.existsById(userId)).thenReturn(true);
    //     when(loadProductPort.loadProductById(1L)).thenReturn(Optional.of(productInfo));
    //     when(updateProductStockPort.deductStock(1L, 2)).thenReturn(true);
    //     when(useCouponUseCase.useCoupon(any(UseCouponUseCase.UseCouponCommand.class))).thenReturn(couponResult);

    //     // when
    //     CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

    //     // then
    //     assertThat(result.isSuccess()).isFalse();
    //     assertThat(result.getErrorMessage()).isEqualTo("쿠폰을 사용할 수 없습니다.");
        
    //     verify(loadUserPort).existsById(userId);
    //     verify(loadProductPort).loadProductById(1L);
    //     verify(updateProductStockPort).deductStock(1L, 2);
    //     verify(useCouponUseCase).useCoupon(any(UseCouponUseCase.UseCouponCommand.class));
    //     verify(updateProductStockPort).restoreStock(1L, 2); // 재고 복구 확인
    //     verify(deductBalancePort, never()).deductBalance(any(), any());
    //     verify(saveOrderPort, never()).saveOrder(any());
    // }

    // @Test
    // @DisplayName("주문 생성 실패 - 잔액 차감 실패")
    // void createOrder_Failure_BalanceDeductionFailed() {
    //     // given
    //     Long userId = 1L;
    //     CreateOrderUseCase.OrderItemCommand orderItemCommand = 
    //         new CreateOrderUseCase.OrderItemCommand(1L, 2);
    //     CreateOrderUseCase.CreateOrderCommand command = 
    //         new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), null);

    //     LoadProductPort.ProductInfo productInfo = new LoadProductPort.ProductInfo(
    //         1L, "상품A", BigDecimal.valueOf(10000), 100);

    //     when(loadUserPort.existsById(userId)).thenReturn(true);
    //     when(loadProductPort.loadProductById(1L)).thenReturn(Optional.of(productInfo));
    //     when(updateProductStockPort.deductStock(1L, 2)).thenReturn(true);
    //     when(deductBalancePort.deductBalance(eq(userId), eq(BigDecimal.valueOf(20000)))).thenReturn(false);

    //     // when
    //     CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

    //     // then
    //     assertThat(result.isSuccess()).isFalse();
    //     assertThat(result.getErrorMessage()).isEqualTo("잔액이 부족합니다.");
        
    //     verify(loadUserPort).existsById(userId);
    //     verify(loadProductPort).loadProductById(1L);
    //     verify(updateProductStockPort).deductStock(1L, 2);
    //     verify(deductBalancePort).deductBalance(userId, BigDecimal.valueOf(20000));
    //     verify(updateProductStockPort).restoreStock(1L, 2); // 재고 복구 확인
    //     verify(saveOrderPort, never()).saveOrder(any());
    // }

    // @Test
    // @DisplayName("주문 생성 성공 - 여러 상품")
    // void createOrder_Success_MultipleProducts() {
    //     // given
    //     Long userId = 1L;
    //     CreateOrderUseCase.OrderItemCommand orderItem1 = 
    //         new CreateOrderUseCase.OrderItemCommand(1L, 2);
    //     CreateOrderUseCase.OrderItemCommand orderItem2 = 
    //         new CreateOrderUseCase.OrderItemCommand(2L, 1);
    //     CreateOrderUseCase.CreateOrderCommand command = 
    //         new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItem1, orderItem2), null);

    //     LoadProductPort.ProductInfo product1 = new LoadProductPort.ProductInfo(
    //         1L, "상품A", BigDecimal.valueOf(10000), 100);
    //     LoadProductPort.ProductInfo product2 = new LoadProductPort.ProductInfo(
    //         2L, "상품B", BigDecimal.valueOf(15000), 50);

    //     Order savedOrder = new Order(userId, List.of(), BigDecimal.valueOf(35000), null);
    //     savedOrder.setId(1L);
    //     savedOrder.setOrderedAt(LocalDateTime.now());

    //     when(loadUserPort.existsById(userId)).thenReturn(true);
    //     when(loadProductPort.loadProductById(1L)).thenReturn(Optional.of(product1));
    //     when(loadProductPort.loadProductById(2L)).thenReturn(Optional.of(product2));
    //     when(updateProductStockPort.deductStock(1L, 2)).thenReturn(true);
    //     when(updateProductStockPort.deductStock(2L, 1)).thenReturn(true);
    //     when(deductBalancePort.deductBalance(eq(userId), eq(BigDecimal.valueOf(35000)))).thenReturn(true);
    //     when(saveOrderPort.saveOrder(any(Order.class))).thenReturn(savedOrder);

    //     // when
    //     CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

    //     // then
    //     assertThat(result.isSuccess()).isTrue();
    //     assertThat(result.getOrderId()).isEqualTo(1L);
    //     assertThat(result.getTotalAmount()).isEqualTo(BigDecimal.valueOf(35000)); // 20000 + 15000
    //     assertThat(result.getDiscountedAmount()).isEqualTo(BigDecimal.valueOf(35000));
        
    //     verify(loadUserPort).existsById(userId);
    //     verify(loadProductPort).loadProductById(1L);
    //     verify(loadProductPort).loadProductById(2L);
    //     verify(updateProductStockPort).deductStock(1L, 2);
    //     verify(updateProductStockPort).deductStock(2L, 1);
    //     verify(deductBalancePort).deductBalance(userId, BigDecimal.valueOf(35000));
    //     verify(saveOrderPort).saveOrder(any(Order.class));
    // }

    // @Test
    // @DisplayName("주문 생성 실패 - 잘못된 주문 아이템 (빈 목록)")
    // void createOrder_Failure_EmptyOrderItems() {
    //     // given
    //     Long userId = 1L;
    //     CreateOrderUseCase.CreateOrderCommand command = 
    //         new CreateOrderUseCase.CreateOrderCommand(userId, List.of(), null);

    //     // when
    //     CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

    //     // then
    //     assertThat(result.isSuccess()).isFalse();
    //     assertThat(result.getErrorMessage()).isEqualTo("주문 상품이 없습니다.");
        
    //     verify(loadUserPort, never()).existsById(any());
    //     verify(loadProductPort, never()).loadProductById(any());
    // }

    // @Test
    // @DisplayName("주문 생성 실패 - 잘못된 수량 (0 또는 음수)")
    // void createOrder_Failure_InvalidQuantity() {
    //     // given
    //     Long userId = 1L;
    //     CreateOrderUseCase.OrderItemCommand orderItemCommand = 
    //         new CreateOrderUseCase.OrderItemCommand(1L, 0); // 잘못된 수량
    //     CreateOrderUseCase.CreateOrderCommand command = 
    //         new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), null);

    //     // when
    //     CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

    //     // then
    //     assertThat(result.isSuccess()).isFalse();
    //     assertThat(result.getErrorMessage()).isEqualTo("주문 수량은 1개 이상이어야 합니다.");
        
    //     verify(loadUserPort, never()).existsById(any());
    //     verify(loadProductPort, never()).loadProductById(any());
    // }
} 