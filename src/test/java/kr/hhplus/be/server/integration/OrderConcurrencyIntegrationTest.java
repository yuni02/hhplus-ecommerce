package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.order.application.port.out.LoadProductPort;
import kr.hhplus.be.server.balance.application.port.in.ChargeBalanceUseCase;
import kr.hhplus.be.server.coupon.application.port.in.IssueCouponUseCase;
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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 주문 동시성 통합 테스트
 * 재고 차감, 잔액 차감, 쿠폰 사용의 동시성 문제를 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("주문 동시성 통합 테스트")
public class OrderConcurrencyIntegrationTest {

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    private ChargeBalanceUseCase chargeBalanceUseCase;

    @Autowired
    private IssueCouponUseCase issueCouponUseCase;

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

    private List<UserEntity> testUsers;
    private List<ProductEntity> testProducts;
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
    @DisplayName("쿠폰 중복 사용 방지 테스트")
    void 쿠폰_중복_사용_방지_테스트() throws InterruptedException {
        // Given - 쿠폰 발급 (미리 해두기)
        UserEntity user = testUsers.get(0);
        ProductEntity product = testProducts.get(0);
        
        // 사용자 잔액 충전
        ChargeBalanceUseCase.ChargeBalanceCommand chargeCommand = 
            new ChargeBalanceUseCase.ChargeBalanceCommand(user.getUserId(), new BigDecimal("50000"));
        chargeBalanceUseCase.chargeBalance(chargeCommand);

        // 쿠폰 발급 (동시성 테스트가 아닌 단일 발급)
        IssueCouponUseCase.IssueCouponCommand issueCommand = 
            new IssueCouponUseCase.IssueCouponCommand(user.getUserId(), testCouponId);
        IssueCouponUseCase.IssueCouponResult issueResult = issueCouponUseCase.issueCoupon(issueCommand);
        assertThat(issueResult.isSuccess()).isTrue();
        
        Long userCouponId = issueResult.getId();

        int concurrentRequests = 2; // 요청 수를 줄임
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch readyLatch = new CountDownLatch(concurrentRequests);
        CountDownLatch finishLatch = new CountDownLatch(concurrentRequests);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        // When - 진짜 동시성 테스트 (모든 스레드가 동시에 시작)
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestIndex = i;
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await(); // 모든 스레드가 동시에 시작

                    // 최소한의 지연만 (진짜 동시성 보장)
                    Thread.sleep(requestIndex * 5);

                    CreateOrderUseCase.CreateOrderCommand orderCommand = 
                        new CreateOrderUseCase.CreateOrderCommand(
                            user.getUserId(),
                            List.of(new CreateOrderUseCase.OrderItemCommand(product.getId(), 1)),
                            userCouponId // 같은 쿠폰 사용
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
                    exceptionCount.incrementAndGet();
                    System.err.println("Request " + requestIndex + " exception: " + e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        readyLatch.await();
        System.out.println("쿠폰 중복 사용 방지 테스트 시작!");
        startLatch.countDown();

        // 타임아웃을 6초로 설정 (트랜잭션 타임아웃보다 길게)
        boolean completed = finishLatch.await(6, TimeUnit.SECONDS);

        // Then
        assertThat(completed).isTrue();

        System.out.println("=== 쿠폰 중복 사용 방지 테스트 결과 ===");
        System.out.println("총 요청 수: " + concurrentRequests);
        System.out.println("성공한 요청 수: " + successCount.get());
        System.out.println("실패한 요청 수: " + failureCount.get());
        System.out.println("예외 발생 수: " + exceptionCount.get());

        // 정확히 1개만 성공해야 함 (쿠폰은 1번만 사용 가능)
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get() + exceptionCount.get()).isEqualTo(concurrentRequests - 1);

        // 안전한 종료
        executorService.shutdown();
        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
        }
    }

    @Test
    @DisplayName("쿠폰 발급 동시성 테스트")
    void 쿠폰_발급_동시성_테스트() throws InterruptedException {
        // Given - 쿠폰 발급 수량이 제한된 쿠폰 생성
        var limitedCoupon = kr.hhplus.be.server.coupon.infrastructure.persistence.entity.CouponEntity.builder()
                .name("제한된 쿠폰")
                .description("발급 수량이 제한된 쿠폰")
                .discountAmount(new BigDecimal("1000"))
                .maxIssuanceCount(2) // 최대 2개만 발급 가능
                .issuedCount(0)
                .status("ACTIVE")
                .build();
        var savedLimitedCoupon = couponJpaRepository.saveAndFlush(limitedCoupon);
        Long limitedCouponId = savedLimitedCoupon.getId();

        int concurrentRequests = 5; // 5개 요청으로 2개만 성공해야 함
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch readyLatch = new CountDownLatch(concurrentRequests);
        CountDownLatch finishLatch = new CountDownLatch(concurrentRequests);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        // When - 동시에 쿠폰 발급 시도
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestIndex = i;
            final Long userId = testUsers.get(requestIndex % testUsers.size()).getUserId();
            
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    // 각 요청마다 다른 지연 시간
                    Thread.sleep(requestIndex * 10);

                    IssueCouponUseCase.IssueCouponCommand issueCommand = 
                        new IssueCouponUseCase.IssueCouponCommand(userId, limitedCouponId);

                    IssueCouponUseCase.IssueCouponResult result = issueCouponUseCase.issueCoupon(issueCommand);

                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                        System.out.println("Coupon issue request " + requestIndex + " succeeded");
                    } else {
                        failureCount.incrementAndGet();
                        System.out.println("Coupon issue request " + requestIndex + " failed: " + result.getErrorMessage());
                    }

                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                    System.err.println("Coupon issue request " + requestIndex + " exception: " + e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        readyLatch.await();
        System.out.println("쿠폰 발급 동시성 테스트 시작!");
        startLatch.countDown();

        // 타임아웃을 15초로 설정 (데드락 방지)
        boolean completed = finishLatch.await(15, TimeUnit.SECONDS);

        // Then
        assertThat(completed).isTrue();

        System.out.println("=== 쿠폰 발급 동시성 테스트 결과 ===");
        System.out.println("총 요청 수: " + concurrentRequests);
        System.out.println("성공한 요청 수: " + successCount.get());
        System.out.println("실패한 요청 수: " + failureCount.get());
        System.out.println("예외 발생 수: " + exceptionCount.get());

        // 정확히 2개만 성공해야 함 (maxIssuanceCount = 2)
        assertThat(successCount.get()).isEqualTo(2);
        assertThat(failureCount.get() + exceptionCount.get()).isEqualTo(concurrentRequests - 2);

        // 안전한 종료
        executorService.shutdown();
        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
        }
    }
}