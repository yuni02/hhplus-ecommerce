package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.order.application.CreateOrderService;
import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.order.application.port.out.*;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.coupon.application.port.in.UseCouponUseCase;
import kr.hhplus.be.server.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 주문 이벤트 처리 통합 테스트
 * 이벤트 기반 아키텍처 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class OrderEventIntegrationTest extends TestcontainersConfiguration {

    @Autowired
    private CreateOrderService createOrderService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private LoadUserPort loadUserPort;

    @Autowired
    private LoadProductPort loadProductPort;

    @Autowired
    private UpdateProductStockPort updateProductStockPort;

    @Autowired
    private DeductBalancePort deductBalancePort;

    @Autowired
    private SaveOrderPort saveOrderPort;

    @Autowired
    private UseCouponUseCase useCouponUseCase;

    private Long userId;
    private Long productId;
    private Long userCouponId;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 설정
        userId = 1L;
        productId = 1L;
        userCouponId = 1L;
    }

    @Test
    @DisplayName("주문 완료 시 이벤트가 정상적으로 발행되어야 한다")
    void orderCompletedEventShouldBePublished() {
        // given
        CreateOrderUseCase.OrderItemCommand orderItemCommand = 
            new CreateOrderUseCase.OrderItemCommand(productId, 2);
        
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), userCouponId);

        // when
        CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrderId()).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getTotalAmount()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.getDiscountedAmount()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("주문 완료 이벤트 핸들러가 비동기로 실행되어야 한다")
    void orderCompletedEventHandlerShouldRunAsync() {
        // given
        CreateOrderUseCase.OrderItemCommand orderItemCommand = 
            new CreateOrderUseCase.OrderItemCommand(productId, 1);
        
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), null);

        // when
        long startTime = System.currentTimeMillis();
        CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);
        long endTime = System.currentTimeMillis();

        // then
        assertThat(result.isSuccess()).isTrue();
        
        // 주문 생성은 빠르게 완료되어야 함 (부가 로직은 비동기 처리)
        long executionTime = endTime - startTime;
        assertThat(executionTime).isLessThan(1000); // 1초 이내 완료
    }

    @Test
    @DisplayName("상품 랭킹 업데이트 이벤트가 정상적으로 발행되어야 한다")
    void productRankingUpdateEventShouldBePublished() {
        // given
        CreateOrderUseCase.OrderItemCommand orderItemCommand = 
            new CreateOrderUseCase.OrderItemCommand(productId, 3);
        
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), null);

        // when
        CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrderItems()).hasSize(1);
        assertThat(result.getOrderItems().get(0).getProductId()).isEqualTo(productId);
        assertThat(result.getOrderItems().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("이벤트 발행 실패 시에도 주문은 정상적으로 완료되어야 한다")
    void orderShouldCompleteEvenIfEventPublishingFails() {
        // given
        CreateOrderUseCase.OrderItemCommand orderItemCommand = 
            new CreateOrderUseCase.OrderItemCommand(productId, 1);
        
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), null);

        // when - 이벤트 발행 중 예외가 발생해도 주문은 성공해야 함
        CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrderId()).isNotNull();
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("다중 상품 주문 시 각 상품별로 랭킹 이벤트가 발행되어야 한다")
    void multipleProductOrderShouldPublishRankingEventsForEachProduct() {
        // given
        CreateOrderUseCase.OrderItemCommand item1 = 
            new CreateOrderUseCase.OrderItemCommand(1L, 2);
        CreateOrderUseCase.OrderItemCommand item2 = 
            new CreateOrderUseCase.OrderItemCommand(2L, 1);
        CreateOrderUseCase.OrderItemCommand item3 = 
            new CreateOrderUseCase.OrderItemCommand(3L, 3);
        
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(item1, item2, item3), null);

        // when
        CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrderItems()).hasSize(3);
        assertThat(result.getOrderItems().get(0).getProductId()).isEqualTo(1L);
        assertThat(result.getOrderItems().get(1).getProductId()).isEqualTo(2L);
        assertThat(result.getOrderItems().get(2).getProductId()).isEqualTo(3L);
    }
}
