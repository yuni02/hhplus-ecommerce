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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
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

    @Test
    @DisplayName("주문 생성 성공 - 재고 복구 불필요")
    void createOrder_Success_NoStockRestore() {
        // given
        Long userId = 1L;
        Long productId = 1L;
        Integer quantity = 2;
        BigDecimal productPrice = BigDecimal.valueOf(10000);
        
        CreateOrderUseCase.CreateOrderCommand command = new CreateOrderUseCase.CreateOrderCommand(
                userId,
                List.of(new CreateOrderUseCase.OrderItemCommand(productId, quantity)),
                null // 쿠폰 없음
        );
        
        // Mock 설정
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadProductPort.loadProductById(productId))
                .thenReturn(Optional.of(new LoadProductPort.ProductInfo(productId, "테스트 상품", productPrice, 100)));
        when(updateProductStockPort.deductStock(productId, quantity)).thenReturn(true);
        when(deductBalancePort.deductBalance(userId, productPrice.multiply(BigDecimal.valueOf(quantity))))
                .thenReturn(true);
        
        Order mockOrder = new Order(userId, List.of(), productPrice.multiply(BigDecimal.valueOf(quantity)), null);
        mockOrder.setId(1L);
        when(saveOrderPort.saveOrder(any(Order.class))).thenReturn(mockOrder);

        // when
        CreateOrderUseCase.CreateOrderResult result = orderFacade.createOrder(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrderId()).isEqualTo(1L);
        
        // 재고 복구가 호출되지 않았는지 확인
        verify(updateProductStockPort, never()).restoreStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("쿠폰 할인 실패 시 재고 복구")
    void createOrder_CouponDiscountFailure_StockRestored() {
        // given
        Long userId = 1L;
        Long productId = 1L;
        Long userCouponId = 1L;
        Integer quantity = 2;
        BigDecimal productPrice = BigDecimal.valueOf(10000);
        
        CreateOrderUseCase.CreateOrderCommand command = new CreateOrderUseCase.CreateOrderCommand(
                userId,
                List.of(new CreateOrderUseCase.OrderItemCommand(productId, quantity)),
                userCouponId
        );
        
        // Mock 설정
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadProductPort.loadProductById(productId))
                .thenReturn(Optional.of(new LoadProductPort.ProductInfo(productId, "테스트 상품", productPrice, 100)));
        when(updateProductStockPort.deductStock(productId, quantity)).thenReturn(true);
        
        // 쿠폰 사용 실패 시뮬레이션
        when(useCouponUseCase.useCoupon(any(UseCouponUseCase.UseCouponCommand.class)))
                .thenReturn(UseCouponUseCase.UseCouponResult.failure("쿠폰을 찾을 수 없습니다."));

        // when
        CreateOrderUseCase.CreateOrderResult result = orderFacade.createOrder(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("쿠폰을 찾을 수 없습니다.");
        
        // 재고 복구가 호출되었는지 확인
        verify(updateProductStockPort).restoreStock(productId, quantity);
        
        // 잔액 차감이 호출되지 않았는지 확인
        verify(deductBalancePort, never()).deductBalance(anyLong(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("잔액 부족 시 재고 복구")
    void createOrder_InsufficientBalance_StockRestored() {
        // given
        Long userId = 1L;
        Long productId = 1L;
        Integer quantity = 2;
        BigDecimal productPrice = BigDecimal.valueOf(10000);
        
        CreateOrderUseCase.CreateOrderCommand command = new CreateOrderUseCase.CreateOrderCommand(
                userId,
                List.of(new CreateOrderUseCase.OrderItemCommand(productId, quantity)),
                null // 쿠폰 없음
        );
        
        // Mock 설정
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadProductPort.loadProductById(productId))
                .thenReturn(Optional.of(new LoadProductPort.ProductInfo(productId, "테스트 상품", productPrice, 100)));
        when(updateProductStockPort.deductStock(productId, quantity)).thenReturn(true);
        
        // 잔액 부족 시뮬레이션
        when(deductBalancePort.deductBalance(userId, productPrice.multiply(BigDecimal.valueOf(quantity))))
                .thenReturn(false);

        // when
        CreateOrderUseCase.CreateOrderResult result = orderFacade.createOrder(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("잔액이 부족합니다.");
        
        // 재고 복구가 호출되었는지 확인
        verify(updateProductStockPort).restoreStock(productId, quantity);
        
        // 주문 저장이 호출되지 않았는지 확인
        verify(saveOrderPort, never()).saveOrder(any(Order.class));
    }

    @Test
    @DisplayName("주문 저장 실패 시 재고 복구")
    void createOrder_OrderSaveFailure_StockRestored() {
        // given
        Long userId = 1L;
        Long productId = 1L;
        Integer quantity = 2;
        BigDecimal productPrice = BigDecimal.valueOf(10000);
        
        CreateOrderUseCase.CreateOrderCommand command = new CreateOrderUseCase.CreateOrderCommand(
                userId,
                List.of(new CreateOrderUseCase.OrderItemCommand(productId, quantity)),
                null // 쿠폰 없음
        );
        
        // Mock 설정
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadProductPort.loadProductById(productId))
                .thenReturn(Optional.of(new LoadProductPort.ProductInfo(productId, "테스트 상품", productPrice, 100)));
        when(updateProductStockPort.deductStock(productId, quantity)).thenReturn(true);
        when(deductBalancePort.deductBalance(userId, productPrice.multiply(BigDecimal.valueOf(quantity))))
                .thenReturn(true);
        
        // 주문 저장 실패 시뮬레이션
        when(saveOrderPort.saveOrder(any(Order.class)))
                .thenThrow(new RuntimeException("데이터베이스 연결 오류"));

        // when
        CreateOrderUseCase.CreateOrderResult result = orderFacade.createOrder(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("주문 생성 중 오류가 발생했습니다");
        assertThat(result.getErrorMessage()).contains("데이터베이스 연결 오류");
        
        // 재고 복구가 호출되었는지 확인
        verify(updateProductStockPort).restoreStock(productId, quantity);
    }

    @Test
    @DisplayName("여러 상품 주문 시 일부 실패로 재고 복구")
    void createOrder_MultipleProducts_PartialFailure_StockRestored() {
        // given
        Long userId = 1L;
        Long productId1 = 1L;
        Long productId2 = 2L;
        Integer quantity1 = 2;
        Integer quantity2 = 3;
        BigDecimal productPrice1 = BigDecimal.valueOf(10000);
        BigDecimal productPrice2 = BigDecimal.valueOf(15000);
        
        CreateOrderUseCase.CreateOrderCommand command = new CreateOrderUseCase.CreateOrderCommand(
                userId,
                List.of(
                        new CreateOrderUseCase.OrderItemCommand(productId1, quantity1),
                        new CreateOrderUseCase.OrderItemCommand(productId2, quantity2)
                ),
                null // 쿠폰 없음
        );
        
        // Mock 설정
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadProductPort.loadProductById(productId1))
                .thenReturn(Optional.of(new LoadProductPort.ProductInfo(productId1, "테스트 상품 1", productPrice1, 100)));
        when(loadProductPort.loadProductById(productId2))
                .thenReturn(Optional.of(new LoadProductPort.ProductInfo(productId2, "테스트 상품 2", productPrice2, 100)));
        when(updateProductStockPort.deductStock(productId1, quantity1)).thenReturn(true);
        when(updateProductStockPort.deductStock(productId2, quantity2)).thenReturn(true);
        
        // 잔액 부족 시뮬레이션
        when(deductBalancePort.deductBalance(eq(userId), any(BigDecimal.class)))
                .thenReturn(false);

        // when
        CreateOrderUseCase.CreateOrderResult result = orderFacade.createOrder(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("잔액이 부족합니다.");
        
        // 두 상품 모두 재고 복구가 호출되었는지 확인
        verify(updateProductStockPort).restoreStock(productId1, quantity1);
        verify(updateProductStockPort).restoreStock(productId2, quantity2);
    }

    @Test
    @DisplayName("재고 복구 실패 시 로그 기록")
    void createOrder_StockRestoreFailure_LogsError() {
        // given
        Long userId = 1L;
        Long productId = 1L;
        Integer quantity = 2;
        BigDecimal productPrice = BigDecimal.valueOf(10000);
        
        CreateOrderUseCase.CreateOrderCommand command = new CreateOrderUseCase.CreateOrderCommand(
                userId,
                List.of(new CreateOrderUseCase.OrderItemCommand(productId, quantity)),
                null // 쿠폰 없음
        );
        
        // Mock 설정
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadProductPort.loadProductById(productId))
                .thenReturn(Optional.of(new LoadProductPort.ProductInfo(productId, "테스트 상품", productPrice, 100)));
        when(updateProductStockPort.deductStock(productId, quantity)).thenReturn(true);
        
        // 잔액 부족 시뮬레이션
        when(deductBalancePort.deductBalance(userId, productPrice.multiply(BigDecimal.valueOf(quantity))))
                .thenReturn(false);
        
        // 재고 복구 실패 시뮬레이션
        when(updateProductStockPort.restoreStock(productId, quantity))
                .thenThrow(new RuntimeException("재고 복구 실패"));

        // when
        CreateOrderUseCase.CreateOrderResult result = orderFacade.createOrder(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("잔액이 부족합니다.");
        
        // 재고 복구가 호출되었는지 확인
        verify(updateProductStockPort).restoreStock(productId, quantity);
    }

    @Test
    @DisplayName("주문 검증 실패 시 재고 복구 불필요")
    void createOrder_ValidationFailure_NoStockRestore() {
        // given
        Long userId = 999L; // 존재하지 않는 사용자
        
        CreateOrderUseCase.CreateOrderCommand command = new CreateOrderUseCase.CreateOrderCommand(
                userId,
                List.of(new CreateOrderUseCase.OrderItemCommand(1L, 2)),
                null
        );
        
        // Mock 설정
        when(loadUserPort.existsById(userId)).thenReturn(false);

        // when
        CreateOrderUseCase.CreateOrderResult result = orderFacade.createOrder(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("사용자를 찾을 수 없습니다");
        
        // 재고 차감이나 복구가 호출되지 않았는지 확인
        verify(updateProductStockPort, never()).deductStock(anyLong(), anyInt());
        verify(updateProductStockPort, never()).restoreStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("재고 부족 시 재고 복구 불필요")
    void createOrder_InsufficientStock_NoStockRestore() {
        // given
        Long userId = 1L;
        Long productId = 1L;
        Integer quantity = 200; // 재고보다 많은 수량
        BigDecimal productPrice = BigDecimal.valueOf(10000);
        
        CreateOrderUseCase.CreateOrderCommand command = new CreateOrderUseCase.CreateOrderCommand(
                userId,
                List.of(new CreateOrderUseCase.OrderItemCommand(productId, quantity)),
                null
        );
        
        // Mock 설정
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadProductPort.loadProductById(productId))
                .thenReturn(Optional.of(new LoadProductPort.ProductInfo(productId, "테스트 상품", productPrice, 100)));

        // when
        CreateOrderUseCase.CreateOrderResult result = orderFacade.createOrder(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("재고가 부족합니다: 100");
        
        // 재고 차감이나 복구가 호출되지 않았는지 확인 (재고 확인 단계에서 실패했으므로)
        verify(updateProductStockPort, never()).deductStock(anyLong(), anyInt());
        verify(updateProductStockPort, never()).restoreStock(anyLong(), anyInt());
    }
} 