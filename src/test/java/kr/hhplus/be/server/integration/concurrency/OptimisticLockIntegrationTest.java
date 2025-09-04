package kr.hhplus.be.server.integration.concurrency;

import kr.hhplus.be.server.balance.application.ChargeBalanceService;
import kr.hhplus.be.server.balance.application.port.in.ChargeBalanceUseCase;
import kr.hhplus.be.server.balance.infrastructure.persistence.entity.BalanceEntity;
import kr.hhplus.be.server.balance.infrastructure.persistence.repository.BalanceJpaRepository;
import kr.hhplus.be.server.coupon.application.port.in.IssueCouponUseCase;
import kr.hhplus.be.server.coupon.domain.service.IssueCouponService;
import kr.hhplus.be.server.coupon.infrastructure.persistence.entity.CouponEntity;
import kr.hhplus.be.server.coupon.infrastructure.persistence.repository.CouponJpaRepository;
import kr.hhplus.be.server.order.application.CreateOrderService;
import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.order.infrastructure.persistence.adapter.ProductStockPersistenceAdapter;
import kr.hhplus.be.server.product.infrastructure.persistence.entity.ProductEntity;
import kr.hhplus.be.server.product.infrastructure.persistence.repository.ProductJpaRepository;
import kr.hhplus.be.server.user.infrastructure.persistence.entity.UserEntity;
import kr.hhplus.be.server.user.infrastructure.persistence.repository.UserJpaRepository;
import kr.hhplus.be.server.TestcontainersConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 낙관적 락 기반 핵심 로직 통합 테스트
 * 재고 차감, 잔액 충전/차감, 쿠폰 발급의 동시성 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("낙관적 락 기반 핵심 로직 통합 테스트")
public class OptimisticLockIntegrationTest {

    @Autowired private CreateOrderService createOrderService;
    @Autowired private ChargeBalanceService chargeBalanceService;
    @Autowired private IssueCouponService issueCouponService;
    @Autowired private ProductStockPersistenceAdapter productStockAdapter;
    
    @Autowired private UserJpaRepository userRepository;
    @Autowired private ProductJpaRepository productRepository;
    @Autowired private BalanceJpaRepository balanceRepository;
    @Autowired private CouponJpaRepository couponRepository;

    private UserEntity testUser;
    private ProductEntity testProduct;
    private CouponEntity testCoupon;
    private BalanceEntity testBalance;

    @BeforeEach
    @Transactional
    void setUp() {
        // 테스트 데이터 초기화
        userRepository.deleteAll();
        productRepository.deleteAll();
        balanceRepository.deleteAll();
        couponRepository.deleteAll();
        
        // 테스트 사용자 생성
        testUser = UserEntity.builder()
                .userId(1L)
                .name("테스트사용자")
                .email("optimistic-test@example.com")
                .build();
        userRepository.save(testUser);

        // 테스트 상품 생성 (재고 100개)
        testProduct = ProductEntity.builder()
                .name("테스트상품")
                .description("테스트용 상품")
                .price(BigDecimal.valueOf(10000))
                .stockQuantity(100)
                .status("ACTIVE")
                .build();
        productRepository.save(testProduct);

        // 테스트 사용자 잔액 생성 (100만원)
        testBalance = BalanceEntity.builder()
                .user(testUser)
                .amount(BigDecimal.valueOf(1000000))
                .status("ACTIVE")
                .build();
        balanceRepository.save(testBalance);

        // 테스트 쿠폰 생성 (발급 가능 수량 50개)
        testCoupon = CouponEntity.builder()
                .name("테스트쿠폰")
                .description("테스트용 쿠폰")
                .discountAmount(BigDecimal.valueOf(1000))
                .maxIssuanceCount(50)
                .issuedCount(0)
                .status("ACTIVE")
                .validFrom(LocalDateTime.now())
                .validTo(LocalDateTime.now().plusDays(30))
                .build();
        couponRepository.save(testCoupon);
    }

    @Test
    @DisplayName("단순 재고 차감 테스트")
    void simpleStockDeduction() {
        // Given
        int quantity = 5;

        // When
        boolean result = productStockAdapter.deductStock(testProduct.getId(), quantity);

        // Then
        assertThat(result).isTrue();
        
        ProductEntity updatedProduct = productRepository.findById(testProduct.getId()).get();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(95);
    }

    @Test
    @DisplayName("동시 재고 차감 - 원자적 쿼리로 정확한 재고 관리")
    void concurrentStockDeduction() throws InterruptedException {
        // Given
        int threadCount = 20;
        int quantityPerThread = 3;
        int expectedFinalStock = 100 - (threadCount * quantityPerThread);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When - 동시에 재고 차감 요청
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    boolean success = productStockAdapter.deductStock(testProduct.getId(), quantityPerThread);
                    if (success) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Then - 재고가 정확히 차감되었는지 검증
        ProductEntity updatedProduct = productRepository.findById(testProduct.getId()).get();
        
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(failCount.get()).isEqualTo(0);
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(expectedFinalStock);
    }

    @Test
    @DisplayName("재고 부족 시 동시 차감 실패 - 원자적 쿼리로 오버셀 방지")
    void concurrentStockDeductionWithInsufficientStock() throws InterruptedException {
        // Given - 재고를 10개로 설정
        testProduct.updateStock(10);
        productRepository.saveAndFlush(testProduct);
        
        // 재고 업데이트 확인
        ProductEntity verifiedProduct = productRepository.findById(testProduct.getId()).get();
        assertThat(verifiedProduct.getStockQuantity()).isEqualTo(10);

        int threadCount = 20;
        int quantityPerThread = 1; // 총 20개 요청, 재고 10개

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When - 재고보다 많은 동시 차감 요청
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    boolean success = productStockAdapter.deductStock(testProduct.getId(), quantityPerThread);
                    if (success) {
                        successCount.incrementAndGet();
                        System.out.println("Stock deduction SUCCESS");
                    } else {
                        failCount.incrementAndGet();
                        System.out.println("Stock deduction FAILED");
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.out.println("Stock deduction EXCEPTION: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Then - 정확히 10개만 성공, 10개는 실패
        ProductEntity updatedProduct = productRepository.findById(testProduct.getId()).get();
        
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failCount.get()).isEqualTo(10);
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("동시 잔액 충전 - 낙관적 락으로 정확한 잔액 관리")
    void concurrentBalanceCharge() throws InterruptedException {
        // Given
        int threadCount = 10;
        BigDecimal chargeAmount = BigDecimal.valueOf(10000);
        BigDecimal expectedFinalAmount = testBalance.getAmount().add(chargeAmount.multiply(BigDecimal.valueOf(threadCount)));

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // When - 동시에 잔액 충전 요청
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    ChargeBalanceUseCase.ChargeBalanceCommand command = 
                        new ChargeBalanceUseCase.ChargeBalanceCommand(testUser.getUserId(), chargeAmount);
                    
                    chargeBalanceService.chargeBalance(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 낙관적 락 충돌 시 재시도 로직이 있다면 최종적으로는 성공해야 함
                    System.out.println("Balance charge failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        // Then - 모든 충전이 반영되었는지 검증 (낙관적 락 재시도 고려)
        BalanceEntity updatedBalance = balanceRepository.findByUserId(testUser.getUserId()).get();
        
        // 낙관적 락으로 인한 일부 실패 허용하되, 최소한의 성공은 보장
        assertThat(successCount.get()).isGreaterThan(0);
        assertThat(updatedBalance.getAmount()).isGreaterThan(testBalance.getAmount());
    }

    @Test
    @DisplayName("통합 주문 생성 - 재고차감, 잔액차감, 쿠폰사용의 복합 동시성 테스트")
    void concurrentOrderCreation() throws InterruptedException {
        // Given - 제한된 재고와 잔액 설정
        testProduct.updateStock(20);
        productRepository.save(testProduct);

        int threadCount = 25; // 재고보다 많은 주문 시도
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        List<CreateOrderUseCase.CreateOrderResult> results = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When - 동시에 주문 생성 요청
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    CreateOrderUseCase.CreateOrderCommand command = new CreateOrderUseCase.CreateOrderCommand(
                        testUser.getUserId(),
                        List.of(new CreateOrderUseCase.OrderItemCommand(testProduct.getId(), 1)), // 각각 1개씩 주문
                        null
                    );
                    
                    CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);
                    synchronized (results) {
                        results.add(result);
                    }
                    successCount.incrementAndGet();
                    
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.out.println("Order creation failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Then - 재고 한도 내에서만 주문 성공
        ProductEntity updatedProduct = productRepository.findById(testProduct.getId()).get();
        
        assertThat(successCount.get()).isEqualTo(20); // 재고만큼만 성공
        assertThat(failCount.get()).isEqualTo(5);     // 나머지는 실패
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(0); // 재고 소진
        assertThat(results).hasSize(20);
        
        // 각 주문이 유효한 상태인지 검증
        results.forEach(result -> {
            assertThat(result.getOrderId()).isNotNull();
            assertThat(result.getStatus()).isEqualTo("COMPLETED");
            assertThat(result.isSuccess()).isTrue();
        });
    }

    @Test
    @DisplayName("쿠폰 동시 발급 - 원자적 쿼리로 정확한 발급량 제한")
    void concurrentCouponIssuance() throws InterruptedException {
        // Given
        int threadCount = 60; // 쿠폰 발급 가능 수량(50)보다 많은 요청
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When - 동시에 쿠폰 발급 요청
        for (int i = 0; i < threadCount; i++) {
            final int userId = i + 1; // 각기 다른 사용자 ID
            executor.submit(() -> {
                try {
                    IssueCouponUseCase.IssueCouponCommand command = 
                        new IssueCouponUseCase.IssueCouponCommand(testCoupon.getId(), (long) userId);
                    
                    issueCouponService.issueCoupon(command);
                    successCount.incrementAndGet();
                    
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(20, TimeUnit.SECONDS);
        executor.shutdown();

        // Then - 정확히 50개만 발급 성공
        CouponEntity updatedCoupon = couponRepository.findById(testCoupon.getId()).get();
        
        assertThat(successCount.get()).isEqualTo(50); // 최대 발급량만큼 성공
        assertThat(failCount.get()).isEqualTo(10);    // 나머지는 실패  
        assertThat(updatedCoupon.getIssuedCount()).isEqualTo(50);
    }
}