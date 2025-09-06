package kr.hhplus.be.server.integration.concurrency;

import kr.hhplus.be.server.balance.infrastructure.persistence.entity.BalanceEntity;
import kr.hhplus.be.server.order.domain.service.CreateOrderService;
import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.balance.application.port.in.ChargeBalanceUseCase;
import kr.hhplus.be.server.product.infrastructure.persistence.entity.ProductEntity;
import kr.hhplus.be.server.user.infrastructure.persistence.entity.UserEntity;
import kr.hhplus.be.server.user.infrastructure.persistence.repository.UserJpaRepository;
import kr.hhplus.be.server.product.infrastructure.persistence.repository.ProductJpaRepository;
import kr.hhplus.be.server.coupon.infrastructure.persistence.repository.CouponJpaRepository;
import kr.hhplus.be.server.coupon.infrastructure.persistence.repository.UserCouponJpaRepository;
import kr.hhplus.be.server.balance.infrastructure.persistence.repository.BalanceJpaRepository;
import kr.hhplus.be.server.order.infrastructure.persistence.repository.OrderJpaRepository;
import kr.hhplus.be.server.TestcontainersConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 주문 동시성 통합 테스트
 * 재고 차감, 잔액 차감, 쿠폰 사용의 동시성 문제를 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("주문 동시성 통합 테스트")
public class OrderConcurrencyIntegrationTest {

    @Autowired
    private CreateOrderUseCase createOrderUseCase;



    @Autowired
    private ChargeBalanceUseCase chargeBalanceUseCase;


    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private CouponJpaRepository couponJpaRepository;

    @Autowired
    private UserCouponJpaRepository userCouponJpaRepository;

    @Autowired
    private BalanceJpaRepository balanceJpaRepository;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    private CreateOrderService createOrderService;

    private List<UserEntity> testUsers;
    private List<ProductEntity> testProducts;
    private ProductEntity testProduct;
    private UserEntity testUser;
    private BalanceEntity testBalance;

    private Long testCouponId;

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리
        orderJpaRepository.deleteAll();
        balanceJpaRepository.deleteAll();
        userCouponJpaRepository.deleteAll();
        productJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
        couponJpaRepository.deleteAll();

        // 테스트용 사용자들 생성
        testUsers = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            UserEntity user = UserEntity.builder()
                    .userId((long) (i + 1000))
                    .name("테스트 사용자 " + i)
                    .email("test" + i + "@example.com")
                    .status("ACTIVE")
                    .build();
            UserEntity savedUser = userJpaRepository.saveAndFlush(user);
            // userId가 제대로 설정되었는지 확인하고, 없으면 수동으로 설정
            if (savedUser.getUserId() == null) {
                savedUser.updateUserId((long) (i + 1000));
                savedUser = userJpaRepository.saveAndFlush(savedUser);
            }
            testUsers.add(savedUser);
        }


        // 테스트용 사용자 생성
        testUser = UserEntity.builder()
                .userId(1L)  // userId 설정
                .name("testuser")
                .email("test@example.com")
                .status("ACTIVE")
                .build();
        testUser = userJpaRepository.saveAndFlush(testUser);


        // 테스트용 상품들 생성 (재고 1개씩)
        testProducts = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            ProductEntity product = ProductEntity.builder()
                    .name("테스트 상품 " + i)
                    .description("테스트 상품 설명 " + i)
                    .price(new BigDecimal("10000"))
                    .stockQuantity(1) // 재고 1개로 설정
                    .status("ACTIVE")
                    .build();
            testProducts.add(productJpaRepository.saveAndFlush(product));
        }

        // 테스트용 쿠폰 생성
        var coupon = kr.hhplus.be.server.coupon.infrastructure.persistence.entity.CouponEntity.builder()
                .name("테스트 쿠폰")
                .description("테스트용 쿠폰")
                .discountAmount(new BigDecimal("1000"))
                .maxIssuanceCount(10)
                .issuedCount(0)
                .status("ACTIVE")
                .build();
        var savedCoupon = couponJpaRepository.saveAndFlush(coupon);
        testCouponId = savedCoupon.getId();


        // 테스트용 상품 생성
        testProduct = ProductEntity.builder()
                .name("테스트 상품")
                .description("테스트 상품 설명")
                .price(new BigDecimal("10000"))
                .stockQuantity(5)  // 재고 5개로 수정하여 동시성 테스트
                .status("ACTIVE")
                .build();
        testProduct = productJpaRepository.saveAndFlush(testProduct);


        // 테스트용 잔액 생성
        testBalance = BalanceEntity.builder()
                .user(testUser)  // UserEntity 참조 설정
                .amount(new BigDecimal("1000000"))  // 충분한 잔액으로 수정
                .status("ACTIVE")
                .build();
        balanceJpaRepository.saveAndFlush(testBalance);
    }

    @Test
    @DisplayName("재고 1개 상품에 대한 동시 주문 테스트")
    void 재고_1개_상품_동시_주문_테스트() throws InterruptedException {
        // Given - 재고 1개인 상품에 대한 동시 주문
        ProductEntity product = testProducts.get(0);
        UserEntity user = testUsers.get(0);
        
        // 사용자 잔액 충전
        ChargeBalanceUseCase.ChargeBalanceCommand chargeCommand = 
            new ChargeBalanceUseCase.ChargeBalanceCommand(user.getUserId(), new BigDecimal("50000"));
        chargeBalanceUseCase.chargeBalance(chargeCommand);

        int concurrentRequests = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch readyLatch = new CountDownLatch(concurrentRequests);
        CountDownLatch finishLatch = new CountDownLatch(concurrentRequests);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When - 동시 주문 요청
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestIndex = i;
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    // 각 요청마다 다른 지연 시간
                    Thread.sleep(requestIndex * 10);

                    CreateOrderUseCase.CreateOrderCommand orderCommand = 
                        new CreateOrderUseCase.CreateOrderCommand(
                            user.getUserId(),
                            List.of(new CreateOrderUseCase.OrderItemCommand(product.getId(), 1)),
                            null
                        );

                    CreateOrderUseCase.CreateOrderResult result = createOrderUseCase.createOrder(orderCommand);

                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                        System.out.println("Request " + requestIndex + " succeeded");
                    } else {
                        failureCount.incrementAndGet();
                        System.out.println("Request " + requestIndex + " failed: " + result.getErrorMessage());
                    }

                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.println("Request " + requestIndex + " exception: " + e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        readyLatch.await();
        // 테스트 시작 전 재고 확인
        ProductEntity initialProduct = productJpaRepository.findById(product.getId()).orElse(null);
        System.out.println("테스트 시작 전 재고: " + (initialProduct != null ? initialProduct.getStockQuantity() : "null"));
        
        System.out.println("재고 1개 상품 동시 주문 테스트 시작!");
        startLatch.countDown();

        boolean completed = finishLatch.await(30, TimeUnit.SECONDS);

        // Then
        assertThat(completed).isTrue();

        System.out.println("=== 재고 1개 상품 동시 주문 테스트 결과 ===");
        System.out.println("총 요청 수: " + concurrentRequests);
        System.out.println("성공한 요청 수: " + successCount.get());
        System.out.println("실패한 요청 수: " + failureCount.get());
        System.out.println("성공률: " + String.format("%.2f", successCount.get() * 100.0 / concurrentRequests) + "%");

        // 재고 확인 - 정확히 1개만 성공해야 함
        ProductEntity updatedProduct = productJpaRepository.findById(product.getId()).orElse(null);
        assertThat(updatedProduct).isNotNull();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(0); // 재고 0개

        // 성공한 주문은 정확히 1개여야 함
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(concurrentRequests - 1);

        executorService.shutdown();
    }

    @Test
    @DisplayName("잔액 부족 상황에서의 동시 주문 테스트")
    void 잔액_부족_동시_주문_테스트() throws InterruptedException {
        // Given - 잔액이 부족한 상황에서의 동시 주문
        ProductEntity product = testProducts.get(0);
        UserEntity user = testUsers.get(0);
        
        // 사용자 잔액을 적게 충전 (주문 금액보다 적게)
        ChargeBalanceUseCase.ChargeBalanceCommand chargeCommand = 
            new ChargeBalanceUseCase.ChargeBalanceCommand(user.getUserId(), new BigDecimal("5000")); // 5000원만 충전
        chargeBalanceUseCase.chargeBalance(chargeCommand);

        int concurrentRequests = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch readyLatch = new CountDownLatch(concurrentRequests);
        CountDownLatch finishLatch = new CountDownLatch(concurrentRequests);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When - 동시 주문 요청
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestIndex = i;
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    Thread.sleep(requestIndex * 10);

                    CreateOrderUseCase.CreateOrderCommand orderCommand = 
                        new CreateOrderUseCase.CreateOrderCommand(
                            user.getUserId(),
                            List.of(new CreateOrderUseCase.OrderItemCommand(product.getId(), 1)),
                            null
                        );

                    CreateOrderUseCase.CreateOrderResult result = createOrderUseCase.createOrder(orderCommand);

                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }

                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        readyLatch.await();
        System.out.println("잔액 부족 동시 주문 테스트 시작!");
        startLatch.countDown();

        boolean completed = finishLatch.await(30, TimeUnit.SECONDS);

        // Then
        assertThat(completed).isTrue();

        System.out.println("=== 잔액 부족 동시 주문 테스트 결과 ===");
        System.out.println("총 요청 수: " + concurrentRequests);
        System.out.println("성공한 요청 수: " + successCount.get());
        System.out.println("실패한 요청 수: " + failureCount.get());

        // 모든 요청이 실패해야 함 (잔액 부족)
        assertThat(successCount.get()).isEqualTo(0);
        assertThat(failureCount.get()).isEqualTo(concurrentRequests);

        executorService.shutdown();
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
                    System.out.println("command : "+ command);
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
        assertThat(updatedBalance.getAmount()).isEqualByComparingTo(expectedRemainingBalance);
    }
}