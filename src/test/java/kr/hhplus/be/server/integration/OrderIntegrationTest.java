package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.order.application.CreateOrderService;
import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.order.infrastructure.persistence.entity.OrderEntity;
import kr.hhplus.be.server.order.infrastructure.persistence.entity.OrderItemEntity;
import kr.hhplus.be.server.order.infrastructure.persistence.repository.OrderJpaRepository;
import kr.hhplus.be.server.order.infrastructure.persistence.repository.OrderItemJpaRepository;
import kr.hhplus.be.server.product.infrastructure.persistence.entity.ProductEntity;
import kr.hhplus.be.server.product.infrastructure.persistence.repository.ProductJpaRepository;
import kr.hhplus.be.server.user.infrastructure.persistence.entity.UserEntity;
import kr.hhplus.be.server.user.infrastructure.persistence.repository.UserJpaRepository;
import kr.hhplus.be.server.balance.infrastructure.persistence.entity.BalanceEntity;
import kr.hhplus.be.server.balance.infrastructure.persistence.repository.BalanceJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Order 도메인 통합테스트")
class OrderIntegrationTest {

    @Autowired
    private CreateOrderService createOrderService;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    private OrderItemJpaRepository orderItemJpaRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private BalanceJpaRepository balanceJpaRepository;

    private UserEntity testUser;
    private ProductEntity testProduct;
    private BalanceEntity testBalance;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        orderItemJpaRepository.deleteAll();
        orderJpaRepository.deleteAll();
        balanceJpaRepository.deleteAll();
        productJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        // 테스트용 사용자 생성
        testUser = UserEntity.builder()
                .userId(1L)  // userId 설정
                .name("testuser")
                .status("ACTIVE")
                .build();
        testUser = userJpaRepository.saveAndFlush(testUser);

        // 테스트용 상품 생성
        testProduct = ProductEntity.builder()
                .name("테스트 상품")
                .description("테스트 상품 설명")
                .price(new BigDecimal("10000"))
                .stockQuantity(100)
                .status("ACTIVE")
                .build();
        testProduct = productJpaRepository.saveAndFlush(testProduct);

        // 테스트용 잔액 생성
        testBalance = BalanceEntity.builder()
                .userId(testUser.getUserId())  // userId 필드 사용
                .amount(new BigDecimal("50000"))
                .status("ACTIVE")
                .build();
        balanceJpaRepository.saveAndFlush(testBalance);
    }

    @Test
    @DisplayName("주문 생성 성공")
    void 주문_생성_성공() {
        // given
        Long userId = testUser.getUserId();
        Long productId = testProduct.getId();
        Integer quantity = 2;

        CreateOrderUseCase.OrderItemCommand orderItem = new CreateOrderUseCase.OrderItemCommand(productId, quantity);
        CreateOrderUseCase.CreateOrderCommand command = new CreateOrderUseCase.CreateOrderCommand(userId, Arrays.asList(orderItem), null);

        // when
        CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getTotalAmount()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.getStatus()).isEqualTo("COMPLETED");

        // 주문이 올바르게 저장되었는지 확인
        List<OrderEntity> orders = orderJpaRepository.findByUserId(userId);
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getTotalAmount()).isEqualTo(result.getTotalAmount());

        // 주문 아이템이 올바르게 저장되었는지 확인
        List<OrderItemEntity> orderItems = orderItemJpaRepository.findByOrderId(orders.get(0).getId());
        assertThat(orderItems).hasSize(1);
        assertThat(orderItems.get(0).getProductId()).isEqualTo(productId);
        assertThat(orderItems.get(0).getQuantity()).isEqualTo(quantity);
    }

    @Test
    @DisplayName("주문 생성 실패 - 존재하지 않는 상품")
    void 주문_생성_실패_존재하지_않는_상품() {
        // given
        Long userId = testUser.getUserId();
        Long nonExistentProductId = 9999L;
        Integer quantity = 1;

                 CreateOrderUseCase.OrderItemCommand orderItem = new CreateOrderUseCase.OrderItemCommand(nonExistentProductId, quantity);
        CreateOrderUseCase.CreateOrderCommand command = new CreateOrderUseCase.CreateOrderCommand(userId, Arrays.asList(orderItem), null);

        // when
        CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("상품을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("주문 생성 실패 - 잔액 부족")
    void 주문_생성_실패_잔액_부족() {
        // given
        Long userId = testUser.getUserId();
        Long productId = testProduct.getId();
        Integer quantity = 10; // 총 100,000원 (잔액 50,000원보다 많음)

                 CreateOrderUseCase.OrderItemCommand orderItem = new CreateOrderUseCase.OrderItemCommand(productId, quantity);
        CreateOrderUseCase.CreateOrderCommand command = new CreateOrderUseCase.CreateOrderCommand(userId, Arrays.asList(orderItem), null);

        // when
        CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("잔액이 부족합니다");
    }
} 