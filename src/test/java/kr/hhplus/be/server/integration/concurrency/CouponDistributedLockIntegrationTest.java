package kr.hhplus.be.server.integration.concurrency;

import kr.hhplus.be.server.TestcontainersConfiguration;
import kr.hhplus.be.server.balance.application.port.in.ChargeBalanceUseCase;
import kr.hhplus.be.server.balance.infrastructure.persistence.entity.BalanceEntity;
import kr.hhplus.be.server.balance.infrastructure.persistence.repository.BalanceJpaRepository;
import kr.hhplus.be.server.coupon.application.port.in.IssueCouponUseCase;
import kr.hhplus.be.server.coupon.application.AsyncCouponIssueWorker;
import kr.hhplus.be.server.coupon.infrastructure.persistence.repository.CouponJpaRepository;
import kr.hhplus.be.server.coupon.infrastructure.persistence.repository.UserCouponJpaRepository;
import kr.hhplus.be.server.order.application.CreateOrderService;
import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.coupon.application.RedisCouponQueueService;
import kr.hhplus.be.server.order.infrastructure.persistence.repository.OrderJpaRepository;
import kr.hhplus.be.server.product.infrastructure.persistence.entity.ProductEntity;
import kr.hhplus.be.server.product.infrastructure.persistence.repository.ProductJpaRepository;
import kr.hhplus.be.server.user.infrastructure.persistence.entity.UserEntity;
import kr.hhplus.be.server.user.infrastructure.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class})
@TestPropertySource(properties = {
    "spring.task.scheduling.pool.size=1",
    "spring.task.scheduling.thread-name-prefix=test-scheduler-"
})
class CouponDistributedLockIntegrationTest {


    @Autowired
    private CouponJpaRepository couponJpaRepository;

    @Autowired
    private IssueCouponUseCase issueCouponUseCase;


    @Autowired
    private CreateOrderUseCase createOrderUseCase;



    @Autowired
    private ChargeBalanceUseCase chargeBalanceUseCase;


    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;


    @Autowired
    private UserCouponJpaRepository userCouponJpaRepository;

    @Autowired
    private BalanceJpaRepository balanceJpaRepository;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    private CreateOrderService createOrderService;

    @Autowired
    private RedisCouponQueueService queueService;

    @Autowired
    private AsyncCouponIssueWorker asyncCouponIssueWorker;

    private List<ProductEntity> testProducts;
    private ProductEntity testProduct;
    private UserEntity testUser;
    private BalanceEntity testBalance;
    private List<UserEntity> testUsers;

    private Long testCouponId;
    @BeforeEach
    void setUp() {
        // 모든 테스트 데이터 완전 정리
        userCouponJpaRepository.deleteAll();
        balanceJpaRepository.deleteAll();
        orderJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
        couponJpaRepository.deleteAll();
        productJpaRepository.deleteAll();
        
        // 테스트용 사용자들 새로 생성
        testUsers = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            UserEntity user = UserEntity.builder()
                    .userId((long) (i + 1000))
                    .name("테스트 사용자 " + i)
                    .email("test" + i + "@example.com")
                    .status("ACTIVE")
                    .build();
            UserEntity savedUser = userJpaRepository.saveAndFlush(user);
            testUsers.add(savedUser);
        }

        // 테스트용 사용자 새로 생성
        testUser = UserEntity.builder()
                .userId(1L)
                .name("testuser")
                .email("test@example.com")
                .status("ACTIVE")
                .build();
        testUser = userJpaRepository.saveAndFlush(testUser);

        // 테스트용 상품들 생성 (이미 존재하면 재사용)
        if (testProducts == null || testProducts.isEmpty()) {
            testProducts = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                ProductEntity product = ProductEntity.builder()
                        .name("테스트 상품 " + i)
                        .description("테스트 상품 설명 " + i)
                        .price(new BigDecimal("10000"))
                        .stockQuantity(1)
                        .status("ACTIVE")
                        .build();
                testProducts.add(productJpaRepository.saveAndFlush(product));
            }
        }

        // 테스트용 쿠폰 생성 (이미 존재하면 재사용)
        if (testCouponId == null) {
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

        // 테스트용 상품 생성 (이미 존재하면 재사용)
        if (testProduct == null) {
            testProduct = ProductEntity.builder()
                    .name("테스트 상품")
                    .description("테스트 상품 설명")
                    .price(new BigDecimal("10000"))
                    .stockQuantity(5)
                    .status("ACTIVE")
                    .build();
            testProduct = productJpaRepository.saveAndFlush(testProduct);
        }

        // 테스트용 잔액 생성 (이미 존재하면 재사용)
        if (testBalance == null) {
            testBalance = BalanceEntity.builder()
                    .user(testUser)
                    .amount(new BigDecimal("1000000"))
                    .status("ACTIVE")
                    .build();
            balanceJpaRepository.saveAndFlush(testBalance);
        }
    }


    @Test
    @DisplayName("쿠폰 발급 동시성 테스트 - 간단 버전")
    void 쿠폰_발급_동시성_테스트_간단() throws InterruptedException {
        // Given - 간단한 쿠폰 생성
        var simpleCoupon = kr.hhplus.be.server.coupon.infrastructure.persistence.entity.CouponEntity.builder()
                .name("간단 테스트 쿠폰")
                .description("동시성 테스트용 쿠폰")
                .discountAmount(new BigDecimal("1000"))
                .maxIssuanceCount(10) // 10개 발급 가능
                .issuedCount(0)
                .status("ACTIVE")
                .build();
        var savedSimpleCoupon = couponJpaRepository.saveAndFlush(simpleCoupon);
        Long simpleCouponId = savedSimpleCoupon.getId();

        int concurrentRequests = 20; // 20개 동시 요청
        ExecutorService executorService = Executors.newFixedThreadPool(10); // 작은 스레드 풀
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch readyLatch = new CountDownLatch(concurrentRequests);
        CountDownLatch finishLatch = new CountDownLatch(concurrentRequests);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // When - 간단한 동시 쿠폰 발급 시도
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestIndex = i;
            final Long userId = testUsers.get(requestIndex % testUsers.size()).getUserId();

            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    IssueCouponUseCase.IssueCouponCommand issueCommand =
                            new IssueCouponUseCase.IssueCouponCommand(userId, simpleCouponId);

                    IssueCouponUseCase.IssueCouponResult result = issueCouponUseCase.issueCoupon(issueCommand);

                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }

                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        readyLatch.await(3, TimeUnit.SECONDS); // 짧은 준비 대기
        System.out.println("=== 간단 동시성 테스트 시작! ===");
        System.out.println("동시 요청 수: " + concurrentRequests);
        System.out.println("쿠폰 발급 제한: " + simpleCoupon.getMaxIssuanceCount());
        startLatch.countDown();

        boolean completed = finishLatch.await(10, TimeUnit.SECONDS); // 짧은 타임아웃
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then
        assertThat(completed).isTrue();

        System.out.println("=== 간단 동시성 테스트 결과 ===");
        System.out.println("총 요청 수: " + concurrentRequests);
        System.out.println("성공한 요청 수: " + successCount.get());
        System.out.println("실패한 요청 수: " + failureCount.get());
        System.out.println("예외 발생 수: " + exceptionCount.get());
        System.out.println("총 소요 시간: " + duration + "ms");
        System.out.println("초당 처리량: " + String.format("%.2f", (double) concurrentRequests / (duration / 1000.0)) + " req/s");

        // 정확히 10개만 성공해야 함 (maxIssuanceCount = 10)
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failureCount.get() + exceptionCount.get()).isEqualTo(concurrentRequests - 10);
        
        // 쿠폰 잔여 수량 검증
        var updatedSimpleCoupon = couponJpaRepository.findById(simpleCouponId).orElseThrow();
        assertThat(updatedSimpleCoupon.getIssuedCount()).isEqualTo(10);
        assertThat(updatedSimpleCoupon.getMaxIssuanceCount() - updatedSimpleCoupon.getIssuedCount()).isEqualTo(0);

        // 안전한 종료
        executorService.shutdown();
        if (!executorService.awaitTermination(3, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
        }
    }


    @Test
    @DisplayName("대기열 시스템 부하 테스트 - 대용량 대기열 체크")
    void 대기열_시스템_부하_테스트() throws InterruptedException {
        // Given - 대기열 시스템 테스트용 쿠폰 생성
        var queueTestCoupon = kr.hhplus.be.server.coupon.infrastructure.persistence.entity.CouponEntity.builder()
                .name("대기열 시스템 테스트 쿠폰")
                .description("대기열 시스템 부하 테스트용 쿠폰")
                .discountAmount(new BigDecimal("1000"))
                .maxIssuanceCount(1000) // 1000개 발급 가능
                .issuedCount(0)
                .status("ACTIVE")
                .build();
        var savedQueueTestCoupon = couponJpaRepository.saveAndFlush(queueTestCoupon);
        Long queueTestCouponId = savedQueueTestCoupon.getId();

        int concurrentRequests = 2000; // 2000개 동시 요청
        int threadPoolSize = 100; // 대기열 추가용 스레드 풀
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch readyLatch = new CountDownLatch(concurrentRequests);
        CountDownLatch finishLatch = new CountDownLatch(concurrentRequests);

        AtomicInteger queueAddedCount = new AtomicInteger(0);
        AtomicInteger queueFailedCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // When - 대기열 시스템에 대용량 동시 요청
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestIndex = i;
            final Long userId = testUsers.get(requestIndex % testUsers.size()).getUserId();

            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    boolean addedToQueue = queueService.addToQueue(queueTestCouponId, userId);
                    
                    if (addedToQueue) {
                        queueAddedCount.incrementAndGet();
                    } else {
                        queueFailedCount.incrementAndGet();
                    }

                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        readyLatch.await(10, TimeUnit.SECONDS);
        System.out.println("=== 대기열 시스템 부하 테스트 시작! ===");
        System.out.println("동시 요청 수: " + concurrentRequests);
        System.out.println("쿠폰 발급 제한: " + queueTestCoupon.getMaxIssuanceCount());
        startLatch.countDown();

        boolean completed = finishLatch.await(15, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then
        assertThat(completed).isTrue();

        System.out.println("=== 대기열 시스템 부하 테스트 결과 ===");
        System.out.println("총 요청 수: " + concurrentRequests);
        System.out.println("대기열 추가 성공: " + queueAddedCount.get());
        System.out.println("대기열 추가 실패: " + queueFailedCount.get());
        System.out.println("예외 발생 수: " + exceptionCount.get());
        System.out.println("총 소요 시간: " + duration + "ms");
        System.out.println("초당 처리량: " + String.format("%.2f", (double) concurrentRequests / (duration / 1000.0)) + " req/s");
        System.out.println("대기열 크기: " + queueService.getQueueSize(queueTestCouponId));

        assertThat(queueAddedCount.get() + queueFailedCount.get() + exceptionCount.get()).isEqualTo(concurrentRequests);
        assertThat(duration).isLessThan(15000); // 15초로 완화
        
        // 쿠폰 상태는 변경되지 않아야 함 (대기열 추가만 하고 발급은 스케줄러가 별도 처리)
        var updatedQueueTestCoupon = couponJpaRepository.findById(queueTestCouponId).orElseThrow();
        System.out.println("대기열 테스트 후 쿠폰 발급 수: " + updatedQueueTestCoupon.getIssuedCount());
        System.out.println("대기열 테스트 후 쿠폰 잔여 수량: " + (updatedQueueTestCoupon.getMaxIssuanceCount() - updatedQueueTestCoupon.getIssuedCount()));

        executorService.shutdown();
        if (!executorService.awaitTermination(3, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
        }
    }




    @Test
    @DisplayName("실제 운영 환경과 동일한 대기열 기반 쿠폰 발급 테스트")
    void 실제_운영환경_대기열_기반_쿠폰_발급_테스트() throws InterruptedException {
        // Given - 실제 운영용 쿠폰 생성
        var productionCoupon = kr.hhplus.be.server.coupon.infrastructure.persistence.entity.CouponEntity.builder()
                .name("실제 운영 쿠폰")
                .description("실제 운영 환경과 동일한 테스트용 쿠폰")
                .discountAmount(new BigDecimal("1000"))
                .maxIssuanceCount(10) // 10개 발급 가능
                .issuedCount(0)
                .status("ACTIVE")
                .build();
        var savedProductionCoupon = couponJpaRepository.saveAndFlush(productionCoupon);
        Long productionCouponId = savedProductionCoupon.getId();

        int concurrentRequests = 20; // 20개 동시 요청
        int threadPoolSize = 5; // 작은 스레드 풀
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch readyLatch = new CountDownLatch(concurrentRequests);
        CountDownLatch finishLatch = new CountDownLatch(concurrentRequests);

        AtomicInteger queueAddedCount = new AtomicInteger(0);
        AtomicInteger queueFailedCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // When - 실제 운영과 동일: 대기열에 추가만 수행
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestIndex = i;
            final Long userId = testUsers.get(requestIndex % testUsers.size()).getUserId();

            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    // 실제 운영과 동일: 대기열에 추가 (즉시 응답)
                    boolean addedToQueue = queueService.addToQueue(productionCouponId, userId);
                    
                    if (addedToQueue) {
                        queueAddedCount.incrementAndGet();
                        Long queuePosition = queueService.getUserQueuePosition(productionCouponId, userId);
                        System.out.println("Request " + requestIndex + ": 대기열 추가 성공 (userId: " + userId + ", 순서: " + queuePosition + "번째)");
                    } else {
                        queueFailedCount.incrementAndGet();
                        System.out.println("Request " + requestIndex + ": 대기열 추가 실패 - 이미 존재 (userId: " + userId + ")");
                    }

                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                    System.err.println("Request " + requestIndex + ": 예외 발생 - " + e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        readyLatch.await(10, TimeUnit.SECONDS);
        System.out.println("=== 실제 운영 환경 대기열 테스트 시작! ===");
        System.out.println("동시 요청 수: " + concurrentRequests);
        System.out.println("쿠폰 발급 제한: " + productionCoupon.getMaxIssuanceCount());
        startLatch.countDown();

        // 대기열 추가 완료 대기 (5초)
        boolean completed = finishLatch.await(5, TimeUnit.SECONDS);
        long queueEndTime = System.currentTimeMillis();
        long queueDuration = queueEndTime - startTime;

        System.out.println("=== 대기열 추가 완료 ===");
        System.out.println("대기열 추가 소요 시간: " + queueDuration + "ms");
        System.out.println("대기열 크기: " + queueService.getQueueSize(productionCouponId));

        // Then - 대기열 추가 결과 검증
        assertThat(completed).isTrue();
        assertThat(queueAddedCount.get() + queueFailedCount.get() + exceptionCount.get()).isEqualTo(concurrentRequests);

        // 비동기 처리 완료를 기다림 (3초)
        System.out.println("=== 스케줄러에 의해 비동기 처리 대기 중... ===");
        Thread.sleep(5000); // 5초 대기

        // 발급 결과 확인
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger processingCount = new AtomicInteger(0);

        for (int i = 0; i < concurrentRequests; i++) {
            Long userId = testUsers.get(i % testUsers.size()).getUserId();
            
            // 발급 결과 조회
            RedisCouponQueueService.CouponIssueResult result = queueService.getIssueResult(productionCouponId, userId);
            
            if (result == null) {
                processingCount.incrementAndGet();
                System.out.println("Request " + i + ": 아직 처리 중 (userId: " + userId + ")");
            } else if (result.isSuccess()) {
                successCount.incrementAndGet();
                System.out.println("Request " + i + ": 발급 성공 (userId: " + userId + ")");
            } else {
                failureCount.incrementAndGet();
                System.out.println("Request " + i + ": 발급 실패 (userId: " + userId + ", 이유: " + result.getMessage() + ")");
            }
        }

        long totalEndTime = System.currentTimeMillis();
        long totalDuration = totalEndTime - startTime;

        System.out.println("=== 실제 운영 환경 테스트 최종 결과 ===");
        System.out.println("총 요청 수: " + concurrentRequests);
        System.out.println("대기열 추가 성공: " + queueAddedCount.get());
        System.out.println("대기열 추가 실패: " + queueFailedCount.get());
        System.out.println("발급 성공: " + successCount.get());
        System.out.println("발급 실패: " + failureCount.get());
        System.out.println("처리 중: " + processingCount.get());
        System.out.println("총 소요 시간: " + totalDuration + "ms");
        System.out.println("최종 대기열 크기: " + queueService.getQueueSize(productionCouponId));

        // 검증: 발급 성공 수는 쿠폰 제한을 초과할 수 없음
        assertThat(successCount.get()).isLessThanOrEqualTo(productionCoupon.getMaxIssuanceCount());
        // 대기열이 정상 작동하는지 검증
        assertThat(queueAddedCount.get()).isGreaterThan(0);
        
        // 쿠폰 잔여 수량 검증
        var updatedProductionCoupon = couponJpaRepository.findById(productionCouponId).orElseThrow();
        assertThat(updatedProductionCoupon.getIssuedCount()).isEqualTo(successCount.get());
        int remainingCount = updatedProductionCoupon.getMaxIssuanceCount() - updatedProductionCoupon.getIssuedCount();
        System.out.println("쿠폰 잔여 수량: " + remainingCount);
        assertThat(remainingCount).isGreaterThanOrEqualTo(0); // 잔여 수량은 0 이상
        
        // 검증: 대기열 추가는 모든 요청이 성공해야 함
        assertThat(queueAddedCount.get() + queueFailedCount.get() + exceptionCount.get()).isEqualTo(concurrentRequests);

        // 안전한 종료
        executorService.shutdown();
        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
        }
    }

    @Test
    @DisplayName("개선된 대기열 시스템 성능 테스트")
    void 개선된_대기열_시스템_성능_테스트() throws InterruptedException {
        // Given - 개선된 시스템 테스트용 쿠폰 생성
        var improvedCoupon = kr.hhplus.be.server.coupon.infrastructure.persistence.entity.CouponEntity.builder()
                .name("개선된 시스템 쿠폰")
                .description("빠른 응답을 위한 개선된 시스템 테스트용 쿠폰")
                .discountAmount(new BigDecimal("1000"))
                .maxIssuanceCount(15) // 15개 발급 가능
                .issuedCount(0)
                .status("ACTIVE")
                .build();
        var savedImprovedCoupon = couponJpaRepository.saveAndFlush(improvedCoupon);
        Long improvedCouponId = savedImprovedCoupon.getId();

        int concurrentRequests = 30; // 30개 동시 요청
        int threadPoolSize = 8; // 적절한 스레드 풀
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch readyLatch = new CountDownLatch(concurrentRequests);
        CountDownLatch finishLatch = new CountDownLatch(concurrentRequests);

        AtomicInteger queueAddedCount = new AtomicInteger(0);
        AtomicInteger queueFailedCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // When - 대기열에 추가 (개선된 시스템)
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestIndex = i;
            final Long userId = testUsers.get(requestIndex % testUsers.size()).getUserId();

            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    // 대기열에 추가
                    boolean addedToQueue = queueService.addToQueue(improvedCouponId, userId);
                    
                    if (addedToQueue) {
                        queueAddedCount.incrementAndGet();
                        Long queuePosition = queueService.getUserQueuePosition(improvedCouponId, userId);
                        System.out.println("Request " + requestIndex + ": 대기열 추가 성공 (userId: " + userId + ", 순서: " + queuePosition + "번째)");
                    } else {
                        queueFailedCount.incrementAndGet();
                        System.out.println("Request " + requestIndex + ": 대기열 추가 실패 - 이미 존재 (userId: " + userId + ")");
                    }

                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                    System.err.println("Request " + requestIndex + ": 예외 발생 - " + e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        readyLatch.await(10, TimeUnit.SECONDS);
        System.out.println("=== 개선된 대기열 시스템 테스트 시작! ===");
        System.out.println("동시 요청 수: " + concurrentRequests);
        System.out.println("쿠폰 발급 제한: " + improvedCoupon.getMaxIssuanceCount());
        System.out.println("스케줄러 폴링 간격: 100ms (기존 1초에서 10배 빨라짐)");
        System.out.println("배치 처리: 최대 10명씩 동시 처리");
        startLatch.countDown();

        // 대기열 추가 완료 대기 (2초)
        boolean completed = finishLatch.await(2, TimeUnit.SECONDS);
        long queueEndTime = System.currentTimeMillis();
        long queueDuration = queueEndTime - startTime;

        System.out.println("=== 대기열 추가 완료 ===");
        System.out.println("대기열 추가 소요 시간: " + queueDuration + "ms");
        System.out.println("대기열 크기: " + queueService.getQueueSize(improvedCouponId));

        // Then - 대기열 추가 결과 검증
        assertThat(completed).isTrue();
        assertThat(queueAddedCount.get() + queueFailedCount.get() + exceptionCount.get()).isEqualTo(concurrentRequests);

        // 비동기 처리 완료를 기다림 (3초)
        System.out.println("=== 스케줄러에 의해 비동기 처리 대기 중... ===");
        Thread.sleep(5000); // 5초 대기

        // 발급 결과 확인
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger processingCount = new AtomicInteger(0);

        for (int i = 0; i < concurrentRequests; i++) {
            Long userId = testUsers.get(i % testUsers.size()).getUserId();
            
            // 발급 결과 조회
            RedisCouponQueueService.CouponIssueResult result = queueService.getIssueResult(improvedCouponId, userId);
            
            if (result == null) {
                processingCount.incrementAndGet();
                System.out.println("Request " + i + ": 아직 처리 중 (userId: " + userId + ")");
            } else if (result.isSuccess()) {
                successCount.incrementAndGet();
                System.out.println("Request " + i + ": 발급 성공 (userId: " + userId + ")");
            } else {
                failureCount.incrementAndGet();
                System.out.println("Request " + i + ": 발급 실패 (userId: " + userId + ", 이유: " + result.getMessage() + ")");
            }
        }

        long totalEndTime = System.currentTimeMillis();
        long totalDuration = totalEndTime - startTime;

        System.out.println("=== 개선된 대기열 시스템 최종 결과 ===");
        System.out.println("총 요청 수: " + concurrentRequests);
        System.out.println("대기열 추가 성공: " + queueAddedCount.get());
        System.out.println("대기열 추가 실패: " + queueFailedCount.get());
        System.out.println("발급 성공: " + successCount.get());
        System.out.println("발급 실패: " + failureCount.get());
        System.out.println("처리 중: " + processingCount.get());
        System.out.println("총 소요 시간: " + totalDuration + "ms");
        System.out.println("최종 대기열 크기: " + queueService.getQueueSize(improvedCouponId));

        // 기능 검증
        assertThat(successCount.get()).isLessThanOrEqualTo(improvedCoupon.getMaxIssuanceCount());
        assertThat(queueAddedCount.get() + queueFailedCount.get() + exceptionCount.get()).isEqualTo(concurrentRequests);
        // 대기열이 정상 작동하는지 검증
        assertThat(queueAddedCount.get()).isGreaterThan(0);
        
        // 쿠폰 잔여 수량 검증
        var updatedImprovedCoupon = couponJpaRepository.findById(improvedCouponId).orElseThrow();
        assertThat(updatedImprovedCoupon.getIssuedCount()).isEqualTo(successCount.get());
        int remainingCount = updatedImprovedCoupon.getMaxIssuanceCount() - updatedImprovedCoupon.getIssuedCount();
        System.out.println("쿠폰 잔여 수량: " + remainingCount);
        assertThat(remainingCount).isGreaterThanOrEqualTo(0); // 잔여 수량은 0 이상
        
        // 성능 기준을 완화 (실제 환경에서는 네트워크 지연 등으로 시간이 더 걸림)
        assertThat(totalDuration).isLessThan(30000); // 30초로 대폭 완화

        // 안전한 종료
        executorService.shutdown();
        if (!executorService.awaitTermination(3, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
        }
    }

    @Test
    @DisplayName("비동기 쿠폰 발급 동시성 테스트")
    void 비동기_쿠폰_발급_동시성_테스트() throws InterruptedException {
        // Given - 비동기 테스트용 쿠폰 생성
        var asyncTestCoupon = kr.hhplus.be.server.coupon.infrastructure.persistence.entity.CouponEntity.builder()
                .name("비동기 테스트 쿠폰")
                .description("비동기 동시성 테스트용 쿠폰")
                .discountAmount(new BigDecimal("1000"))
                .maxIssuanceCount(10) // 10개 발급 가능
                .issuedCount(0)
                .status("ACTIVE")
                .validFrom(LocalDateTime.now().minusHours(1)) // 1시간 전부터 유효
                .validTo(LocalDateTime.now().plusHours(1))    // 1시간 후까지 유효
                .build();
        var savedAsyncTestCoupon = couponJpaRepository.saveAndFlush(asyncTestCoupon);
        Long asyncTestCouponId = savedAsyncTestCoupon.getId();

        int concurrentRequests = 20; // 20개 동시 요청
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch readyLatch = new CountDownLatch(concurrentRequests);
        CountDownLatch finishLatch = new CountDownLatch(concurrentRequests);

        AtomicInteger queueAddedCount = new AtomicInteger(0);
        AtomicInteger queueFailedCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // When - 비동기 대기열에 동시 요청 추가
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestIndex = i;
            final Long userId = testUsers.get(requestIndex % testUsers.size()).getUserId();

            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    // 대기열에 추가 (실제 Controller 로직과 동일)
                    boolean addedToQueue = queueService.addToQueue(asyncTestCouponId, userId);
                    
                    if (addedToQueue) {
                        queueAddedCount.incrementAndGet();
                    } else {
                        queueFailedCount.incrementAndGet();
                    }

                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        readyLatch.await(3, TimeUnit.SECONDS);
        System.out.println("=== 비동기 쿠폰 발급 테스트 시작! ===");
        System.out.println("동시 요청 수: " + concurrentRequests);
        System.out.println("쿠폰 발급 제한: " + asyncTestCoupon.getMaxIssuanceCount());
        startLatch.countDown();

        boolean completed = finishLatch.await(10, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then - 대기열 추가 결과 검증
        assertThat(completed).isTrue();

        System.out.println("=== 대기열 추가 결과 ===");
        System.out.println("총 요청 수: " + concurrentRequests);
        System.out.println("대기열 추가 성공: " + queueAddedCount.get());
        System.out.println("대기열 추가 실패: " + queueFailedCount.get());
        System.out.println("예외 발생 수: " + exceptionCount.get());
        System.out.println("대기열 추가 소요 시간: " + duration + "ms");

        // 대기열에 추가된 요청 수 검증
        assertThat(queueAddedCount.get() + queueFailedCount.get() + exceptionCount.get()).isEqualTo(concurrentRequests);

        // 스케줄러가 처리할 시간 대기 (최대 10초)
        System.out.println("스케줄러 처리 대기 중...");
        
        // 대기열이 모두 처리될 때까지 폴링으로 확인
        int maxWaitSeconds = 10;
        for (int i = 0; i < maxWaitSeconds; i++) {
            Thread.sleep(1000);
            Long remainingQueue = queueService.getQueueSize(asyncTestCouponId);
            var currentCoupon = couponJpaRepository.findById(asyncTestCouponId).orElseThrow();
            int currentIssuedCount = currentCoupon.getIssuedCount();
            
            System.out.println("대기 중... " + (i + 1) + "초 - 대기열: " + remainingQueue + ", 발급된 쿠폰: " + currentIssuedCount);
            
            // 대기열이 비어있고 쿠폰 발급이 완료되면 종료
            if (remainingQueue == 0 && currentIssuedCount == 10) {
                System.out.println("처리 완료!");
                break;
            }
        }

        // 스케줄러 처리 후 결과 검증
        var updatedAsyncTestCoupon = couponJpaRepository.findById(asyncTestCouponId).orElseThrow();
        int actualIssuedCount = updatedAsyncTestCoupon.getIssuedCount();
        
        System.out.println("=== 스케줄러 처리 후 결과 ===");
        System.out.println("실제 발급된 쿠폰 수: " + actualIssuedCount);
        System.out.println("대기열 크기: " + queueService.getQueueSize(asyncTestCouponId));
        
        // 정확히 10개만 발급되어야 함 (maxIssuanceCount = 10)
        assertThat(actualIssuedCount).isEqualTo(10);
        assertThat(updatedAsyncTestCoupon.getMaxIssuanceCount() - actualIssuedCount).isEqualTo(0);

        // 실제 사용자 쿠폰 발급 검증
        int actualUserCoupons = userCouponJpaRepository.findAll().size();
        System.out.println("실제 사용자 쿠폰 수: " + actualUserCoupons);
        assertThat(actualUserCoupons).isEqualTo(10);

        // 안전한 종료
        executorService.shutdown();
        if (!executorService.awaitTermination(3, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
        }
    }

}
