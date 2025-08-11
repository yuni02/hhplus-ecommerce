package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.balance.application.port.in.ChargeBalanceUseCase;
import kr.hhplus.be.server.coupon.application.port.in.IssueCouponUseCase;
import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.shared.service.DistributedLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class DistributedLockIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private DistributedLockService distributedLockService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ChargeBalanceUseCase chargeBalanceUseCase;

    @Autowired
    private IssueCouponUseCase issueCouponUseCase;

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @BeforeEach
    void setUp() {
        // Redis 데이터 초기화
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    // ==================== 1. Simple Lock 테스트 ====================
    
    @Test
    void 분산락_기본_동작_테스트() {
        // Given
        String lockKey = "test:lock:1";
        
        // When & Then - Redisson은 재진입 가능한 락이므로 같은 스레드에서 중복 획득 가능
        assertThat(distributedLockService.acquireLock(lockKey)).isTrue();
        assertThat(distributedLockService.acquireLock(lockKey)).isTrue(); // 재진입 가능한 락이므로 성공
        assertThat(distributedLockService.releaseLock(lockKey)).isTrue();
        assertThat(distributedLockService.releaseLock(lockKey)).isTrue(); // 두 번 해제
        assertThat(distributedLockService.acquireLock(lockKey)).isTrue(); // 락 해제 후 다시 획득 가능
    }

    @Test
    void 락_타임아웃_테스트() {
        // Given
        String lockKey = "test:timeout:1";
        
        // When - 짧은 타임아웃으로 락 획득
        boolean acquired = distributedLockService.acquireLock(lockKey, 1); // 1초 타임아웃
        
        // Then
        assertThat(acquired).isTrue();
        
        // 2초 후 락이 자동으로 해제되는지 확인
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 락이 해제되었으므로 다시 획득 가능
        assertThat(distributedLockService.acquireLock(lockKey)).isTrue();
    }

    // ==================== 2. Multi Lock 테스트 ====================
    
    @Test
    void 멀티락_기본_동작_테스트() {
        // Given
        List<String> lockKeys = Arrays.asList("test:multi:1", "test:multi:2", "test:multi:3");
        
        // When & Then
        assertThat(distributedLockService.acquireMultiLock(lockKeys, 10)).isTrue();
        
        // 다른 키로 멀티락 시도 (성공해야 함)
        List<String> otherLockKeys = Arrays.asList("test:multi:4", "test:multi:5");
        assertThat(distributedLockService.acquireMultiLock(otherLockKeys, 10)).isTrue();
        
        // 기존 키와 겹치는 멀티락 시도 (재진입 가능한 락이므로 성공할 수 있음)
        List<String> overlappingKeys = Arrays.asList("test:multi:1", "test:multi:6");
        assertThat(distributedLockService.acquireMultiLock(overlappingKeys, 10)).isTrue();
        
        // 락 해제
        assertThat(distributedLockService.releaseMultiLock(overlappingKeys)).isTrue();
        assertThat(distributedLockService.releaseMultiLock(otherLockKeys)).isTrue();
        assertThat(distributedLockService.releaseMultiLock(lockKeys)).isTrue();
    }

    @Test
    void 멀티락_동시성_테스트() throws InterruptedException {
        // Given
        List<String> lockKeys = Arrays.asList("test:concurrent:1", "test:concurrent:2");
        int concurrentRequests = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(concurrentRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When - 동시에 멀티락 획득 시도 (서로 다른 스레드)
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestIndex = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    Thread.sleep(requestIndex * 10);
                    
                    boolean acquired = distributedLockService.acquireMultiLock(lockKeys, 5);
                    if (acquired) {
                        successCount.incrementAndGet();
                        Thread.sleep(100); // 락 보유 시간
                        distributedLockService.releaseMultiLock(lockKeys);
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

        startLatch.countDown();
        finishLatch.await();

        // Then - 서로 다른 스레드이므로 하나만 성공해야 함
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(concurrentRequests - 1);
        
        executorService.shutdown();
    }

    // ==================== 3. Read-Write Lock 테스트 ====================
    
    @Test
    void ReadWrite_락_기본_동작_테스트() {
        // Given
        String lockKey = "test:rw:1";
        
        // When & Then - 읽기 락은 여러 개 동시 획득 가능
        assertThat(distributedLockService.acquireReadLock(lockKey, 10)).isTrue();
        assertThat(distributedLockService.acquireReadLock(lockKey, 10)).isTrue(); // 읽기 락은 중복 획득 가능
        
        // 읽기 락 해제
        assertThat(distributedLockService.releaseReadLock(lockKey)).isTrue();
        assertThat(distributedLockService.releaseReadLock(lockKey)).isTrue();
        
        // 쓰기 락 획득
        assertThat(distributedLockService.acquireWriteLock(lockKey, 10)).isTrue();
        assertThat(distributedLockService.releaseWriteLock(lockKey)).isTrue();
    }

    @Test
    void ReadWrite_락_동시성_테스트() throws InterruptedException {
        // Given
        String lockKey = "test:rw:concurrent";
        int concurrentRequests = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(concurrentRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When - 동시에 읽기 락 획득 시도
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestIndex = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    Thread.sleep(requestIndex * 10);
                    
                    boolean acquired = distributedLockService.acquireReadLock(lockKey, 5);
                    if (acquired) {
                        successCount.incrementAndGet();
                        Thread.sleep(100);
                        distributedLockService.releaseReadLock(lockKey);
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

        startLatch.countDown();
        finishLatch.await();

        // Then - 읽기 락은 모두 성공해야 함
        assertThat(successCount.get()).isEqualTo(concurrentRequests);
        assertThat(failureCount.get()).isEqualTo(0);
        
        executorService.shutdown();
    }

    // ==================== 4. Fair Lock 테스트 ====================
    
    @Test
    void Fair_락_기본_동작_테스트() {
        // Given
        String lockKey = "test:fair:1";
        
        // When & Then
        assertThat(distributedLockService.acquireFairLock(lockKey, 10)).isTrue();
        
        // 다른 키로 Fair 락 시도 (성공해야 함)
        String otherLockKey = "test:fair:2";
        assertThat(distributedLockService.acquireFairLock(otherLockKey, 10)).isTrue();
        
        // 기존 키로 Fair 락 시도 (재진입 가능한 락이므로 성공)
        assertThat(distributedLockService.acquireFairLock(lockKey, 10)).isTrue();
        
        // 락 해제
        assertThat(distributedLockService.releaseFairLock(lockKey)).isTrue();
        assertThat(distributedLockService.releaseFairLock(lockKey)).isTrue(); // 두 번 해제
        assertThat(distributedLockService.releaseFairLock(otherLockKey)).isTrue();
    }

    @Test
    void Fair_락_동시성_테스트() throws InterruptedException {
        // Given
        String lockKey = "test:fair:concurrent";
        int concurrentRequests = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(concurrentRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When - 동시에 Fair 락 획득 시도 (서로 다른 스레드)
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestIndex = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    Thread.sleep(requestIndex * 10);
                    
                    boolean acquired = distributedLockService.acquireFairLock(lockKey, 5);
                    if (acquired) {
                        successCount.incrementAndGet();
                        Thread.sleep(100);
                        distributedLockService.releaseFairLock(lockKey);
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

        startLatch.countDown();
        finishLatch.await();

        // Then - 서로 다른 스레드이므로 하나만 성공해야 함 (FIFO 순서)
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(concurrentRequests - 1);
        
        executorService.shutdown();
    }

    // ==================== 5. 락 상태 확인 테스트 ====================
    
    @Test
    void 락_상태_확인_테스트() {
        // Given
        String lockKey = "test:status:1";
        
        // When & Then - 초기 상태
        assertThat(distributedLockService.isLocked(lockKey)).isFalse();
        assertThat(distributedLockService.isHeldByCurrentThread(lockKey)).isFalse();
        
        // 락 획득 후 상태
        assertThat(distributedLockService.acquireLock(lockKey, 10)).isTrue();
        assertThat(distributedLockService.isLocked(lockKey)).isTrue();
        assertThat(distributedLockService.isHeldByCurrentThread(lockKey)).isTrue();
        assertThat(distributedLockService.getHoldCount(lockKey)).isEqualTo(1);
        
        // 재진입 가능한 락 - 같은 스레드에서 다시 획득
        assertThat(distributedLockService.acquireLock(lockKey, 10)).isTrue();
        assertThat(distributedLockService.getHoldCount(lockKey)).isEqualTo(2);
        
        // 락 해제 후 상태
        assertThat(distributedLockService.releaseLock(lockKey)).isTrue();
        assertThat(distributedLockService.getHoldCount(lockKey)).isEqualTo(1);
        assertThat(distributedLockService.releaseLock(lockKey)).isTrue();
        assertThat(distributedLockService.isLocked(lockKey)).isFalse();
        assertThat(distributedLockService.isHeldByCurrentThread(lockKey)).isFalse();
    }

    // ==================== 6. 기존 비즈니스 로직 테스트 (Simple Lock 사용) ====================
    
    @Test
    void 잔액_충전_동시성_제어_테스트() throws InterruptedException {
        // Given
        Long userId = 1001L;
        BigDecimal chargeAmount = new BigDecimal("10000");
        int concurrentRequests = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(concurrentRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When - 동시에 잔액 충전 시도
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestIndex = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    Thread.sleep(requestIndex * 10); // 약간의 지연으로 동시성 시뮬레이션
                    
                    ChargeBalanceUseCase.ChargeBalanceCommand command = 
                        new ChargeBalanceUseCase.ChargeBalanceCommand(userId, chargeAmount);
                    
                    ChargeBalanceUseCase.ChargeBalanceResult result = chargeBalanceUseCase.chargeBalance(command);
                    
                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
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

        startLatch.countDown();
        finishLatch.await();

        // Then - 모든 요청이 성공해야 함 (분산락으로 동시성 제어)
        assertThat(successCount.get()).isEqualTo(concurrentRequests);
        assertThat(failureCount.get()).isEqualTo(0);
        
        executorService.shutdown();
    }

    @Test
    void 쿠폰_발급_동시성_제어_테스트() throws InterruptedException {
        // Given
        Long couponId = 1L;
        int concurrentRequests = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(concurrentRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When - 동시에 쿠폰 발급 시도
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestIndex = i;
            final Long userId = 1001L + requestIndex; // 각각 다른 사용자
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    Thread.sleep(requestIndex * 10);
                    
                    IssueCouponUseCase.IssueCouponCommand command = 
                        new IssueCouponUseCase.IssueCouponCommand(userId, couponId);
                    
                    IssueCouponUseCase.IssueCouponResult result = issueCouponUseCase.issueCoupon(command);
                    
                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
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

        startLatch.countDown();
        finishLatch.await();

        // Then - 모든 요청이 성공해야 함 (분산락으로 동시성 제어)
        assertThat(successCount.get()).isEqualTo(concurrentRequests);
        assertThat(failureCount.get()).isEqualTo(0);
        
        executorService.shutdown();
    }

    @Test
    void 주문_생성_동시성_제어_테스트() throws InterruptedException {
        // Given
        Long userId = 1001L;
        int concurrentRequests = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(concurrentRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When - 동시에 주문 생성 시도
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestIndex = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    Thread.sleep(requestIndex * 10);
                    
                    CreateOrderUseCase.CreateOrderCommand command = 
                        new CreateOrderUseCase.CreateOrderCommand(
                            userId,
                            List.of(new CreateOrderUseCase.OrderItemCommand(1L, 1)),
                            null
                        );
                    
                    CreateOrderUseCase.CreateOrderResult result = createOrderUseCase.createOrder(command);
                    
                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
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

        startLatch.countDown();
        finishLatch.await();

        // Then - 분산락으로 인해 일부만 성공해야 함 (동시 주문 방지)
        assertThat(successCount.get()).isEqualTo(1); // 첫 번째 요청만 성공
        assertThat(failureCount.get()).isEqualTo(concurrentRequests - 1);
        
        executorService.shutdown();
    }
}
