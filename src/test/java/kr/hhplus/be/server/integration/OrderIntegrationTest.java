package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.TestcontainersConfiguration;
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
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
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
                .email("test@example.com")
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

    @Test
    @DisplayName("동시 주문 시 재고 차감 동시성 제어 테스트")
    void 동시_주문_재고_차감_동시성_테스트() throws Exception {
        // given
        int threadCount = 10;
        int quantityPerOrder = 1;
        int totalExpectedOrders = Math.min(threadCount, testProduct.getStockQuantity());
        
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 동시에 여러 주문 요청
        CompletableFuture<Void>[] futures = new CompletableFuture[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    CreateOrderUseCase.OrderItemCommand orderItem = 
                        new CreateOrderUseCase.OrderItemCommand(testProduct.getId(), quantityPerOrder);
                    CreateOrderUseCase.CreateOrderCommand command = 
                        new CreateOrderUseCase.CreateOrderCommand(testUser.getUserId(), Arrays.asList(orderItem), null);
                    
                    CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);
                    
                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                        System.out.println("Thread " + index + " 실패: " + result.getErrorMessage());
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.out.println("Thread " + index + " 예외: " + e.getMessage());
                }
            }, executorService);
        }

        CompletableFuture.allOf(futures).join();
        executorService.shutdown();

        // then
        System.out.println("성공: " + successCount.get() + ", 실패: " + failCount.get());
        
        // 성공한 주문은 사용 가능한 재고 범위 내에서만 이루어져야 함
        assertThat(successCount.get()).isLessThanOrEqualTo(totalExpectedOrders);
        assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
        
        // 남은 재고 확인
        ProductEntity updatedProduct = productJpaRepository.findById(testProduct.getId()).get();
        int expectedRemainingStock = testProduct.getStockQuantity() - successCount.get();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(expectedRemainingStock);
    }

    @Test
    @DisplayName("동시 결제 시 잔액 차감 동시성 제어 테스트")
    void 동시_결제_잔액_차감_동시성_테스트() throws Exception {
        // given
        // 각 주문당 10,000원 * 1개 = 10,000원
        // 잔액 50,000원이므로 최대 5개 주문만 성공해야 함
        int threadCount = 10;
        int quantityPerOrder = 1;
        int maxSuccessfulOrders = 5; // 50,000 / 10,000 = 5
        
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 동시에 여러 결제 요청
        CompletableFuture<Void>[] futures = new CompletableFuture[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    CreateOrderUseCase.OrderItemCommand orderItem = 
                        new CreateOrderUseCase.OrderItemCommand(testProduct.getId(), quantityPerOrder);
                    CreateOrderUseCase.CreateOrderCommand command = 
                        new CreateOrderUseCase.CreateOrderCommand(testUser.getUserId(), Arrays.asList(orderItem), null);
                    
                    CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);
                    
                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                        System.out.println("Thread " + index + " 성공");
                    } else {
                        failCount.incrementAndGet();
                        System.out.println("Thread " + index + " 실패: " + result.getErrorMessage());
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.out.println("Thread " + index + " 예외: " + e.getMessage());
                }
            }, executorService);
        }

        CompletableFuture.allOf(futures).join();
        executorService.shutdown();

        // then
        System.out.println("성공: " + successCount.get() + ", 실패: " + failCount.get());
        
        // 성공한 주문은 잔액 범위 내에서만 이루어져야 함
        assertThat(successCount.get()).isLessThanOrEqualTo(maxSuccessfulOrders);
        assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
        
        // 남은 잔액 확인
        BalanceEntity updatedBalance = balanceJpaRepository.findByUserIdAndStatus(testUser.getUserId(), "ACTIVE").get();
        BigDecimal expectedRemainingBalance = testBalance.getAmount()
                .subtract(new BigDecimal("10000").multiply(new BigDecimal(successCount.get())));
        assertThat(updatedBalance.getAmount()).isEqualTo(expectedRemainingBalance);
    }
} 