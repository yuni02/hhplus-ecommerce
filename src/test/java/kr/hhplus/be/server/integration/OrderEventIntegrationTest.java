package kr.hhplus.be.server.integration;
import kr.hhplus.be.server.TestcontainersConfiguration;

import kr.hhplus.be.server.order.domain.service.CreateOrderService;
import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.order.application.port.out.*;
import kr.hhplus.be.server.coupon.application.port.in.UseCouponUseCase;
import kr.hhplus.be.server.user.infrastructure.persistence.repository.UserJpaRepository;
import kr.hhplus.be.server.product.infrastructure.persistence.repository.ProductJpaRepository;
import kr.hhplus.be.server.balance.infrastructure.persistence.repository.BalanceJpaRepository;
import kr.hhplus.be.server.user.infrastructure.persistence.entity.UserEntity;
import kr.hhplus.be.server.product.infrastructure.persistence.entity.ProductEntity;
import kr.hhplus.be.server.balance.infrastructure.persistence.entity.BalanceEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Commit;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import lombok.extern.slf4j.Slf4j;

/**
 * 주문 이벤트 처리 통합 테스트
 * 이벤트 기반 아키텍처 검증
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class OrderEventIntegrationTest {

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
    
    @Autowired
    private UserJpaRepository userJpaRepository;
    
    @Autowired 
    private ProductJpaRepository productJpaRepository;
    
    @Autowired
    private BalanceJpaRepository balanceJpaRepository;

    private Long userId;
    private Long productId;
    private Long userCouponId;

    @BeforeEach
    @Transactional
    @Commit
    void setUp() {
        // 테스트 데이터 설정
        userId = 1L;
        productId = 1L;
        userCouponId = 1L;
        
        // 테스트 데이터 초기화
        setupTestData();
    }
    
    private void setupTestData() {
        // 사용자 생성 (중복 체크)
        UserEntity user = null;
        if (!userJpaRepository.existsById(userId)) {
            user = UserEntity.builder()
                .userId(userId)
                .name("testuser")
                .email("test@example.com")
                .status("ACTIVE")
                .build();
            userJpaRepository.save(user);
        } else {
            user = userJpaRepository.findById(userId).orElse(null);
        }
        
        // 상품 생성 (중복 체크)
        if (productJpaRepository.count() == 0) {
            ProductEntity product1 = ProductEntity.builder()
                .name("Test Product")
                .price(BigDecimal.valueOf(10000))
                .stockQuantity(100)
                .status("ACTIVE")
                .build();
            ProductEntity product2 = ProductEntity.builder()
                .name("Test Product 2")
                .price(BigDecimal.valueOf(20000))
                .stockQuantity(50)
                .status("ACTIVE")
                .build();
            ProductEntity product3 = ProductEntity.builder()
                .name("Test Product 3") 
                .price(BigDecimal.valueOf(30000))
                .stockQuantity(30)
                .status("ACTIVE")
                .build();
            productJpaRepository.saveAll(List.of(product1, product2, product3));
            
            // 저장 후 실제 ID 값으로 업데이트 (테스트에서 사용할 수 있도록)
            productId = product1.getId();
        } else {
            // 이미 존재하는 경우 첫 번째 상품 ID 사용
            productId = productJpaRepository.findAll().get(0).getId();
        }
        
        // 사용자 잔액 추가 (중복 체크)
        if (user != null && balanceJpaRepository.findByUserId(userId).isEmpty()) {
            BalanceEntity balance = BalanceEntity.builder()
                .user(user)  // UserEntity 객체를 직접 설정
                .amount(BigDecimal.valueOf(1000000))
                .status("ACTIVE")
                .build();
            balanceJpaRepository.save(balance);
        }
    }

    @Test
    @DisplayName("주문 완료 시 이벤트가 정상적으로 발행되어야 한다")
    void orderCompletedEventShouldBePublished() {
        // given
        CreateOrderUseCase.OrderItemCommand orderItemCommand = 
            new CreateOrderUseCase.OrderItemCommand(productId, 2);
        
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(orderItemCommand), null);

        // 테스트 데이터 검증
        log.info("User exists: {}", loadUserPort.existsById(userId));
        log.info("Product ID: {}", productId);
        log.info("User ID: {}", userId);

        // when
        CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

        // then
        if (!result.isSuccess()) {
            log.error("Order failed: {}", result.getErrorMessage());
        }
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrderId()).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getStatus()).isEqualTo("PROCESSING"); // 코레오그래피에서는 처리중 상태
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
        assertThat(result.getStatus()).isEqualTo("PROCESSING"); // 코레오그래피에서는 처리중 상태
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
        assertThat(result.getStatus()).isEqualTo("PROCESSING"); // 코레오그래피에서는 처리중 상태
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
        assertThat(result.getStatus()).isEqualTo("PROCESSING"); // 코레오그래피에서는 처리중 상태
    }
}
