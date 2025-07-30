package kr.hhplus.be.server.coupon.adapter.out.persistence;

import kr.hhplus.be.server.coupon.application.port.out.LoadCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.SaveCouponPort;
import kr.hhplus.be.server.coupon.infrastructure.persistence.adapter.CouponPersistenceAdapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CouponPersistenceAdapterTest {

    private CouponPersistenceAdapter couponPersistenceAdapter;

    @BeforeEach
    void setUp() {
        couponPersistenceAdapter = new CouponPersistenceAdapter(null);
    }

    @Test
    @DisplayName("쿠폰 조회 - 정상 조회")
    void loadCouponById_Success() {
        // given
        Long couponId = 1L;

        // when
        Optional<LoadCouponPort.CouponInfo> result = couponPersistenceAdapter.loadCouponById(couponId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(couponId);
        assertThat(result.get().getName()).isEqualTo("쿠폰 1");
        assertThat(result.get().getDiscountAmount()).isEqualTo(1000);
        assertThat(result.get().getMaxIssuanceCount()).isEqualTo(100);
        assertThat(result.get().getIssuedCount()).isEqualTo(0);
        assertThat(result.get().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("쿠폰 조회 - 존재하지 않는 쿠폰")
    void loadCouponById_NotFound() {
        // given
        Long couponId = 999L;

        // when
        Optional<LoadCouponPort.CouponInfo> result = couponPersistenceAdapter.loadCouponById(couponId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("락을 사용한 쿠폰 조회 - 정상 조회")
    void loadCouponByIdWithLock_Success() {
        // given
        Long couponId = 1L;

        // when
        Optional<LoadCouponPort.CouponInfo> result = couponPersistenceAdapter.loadCouponByIdWithLock(couponId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(couponId);
        assertThat(result.get().getName()).isEqualTo("쿠폰 1");
        assertThat(result.get().getDiscountAmount()).isEqualTo(1000);
        assertThat(result.get().getMaxIssuanceCount()).isEqualTo(100);
        assertThat(result.get().getIssuedCount()).isEqualTo(0);
        assertThat(result.get().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("락을 사용한 쿠폰 조회 - 존재하지 않는 쿠폰")
    void loadCouponByIdWithLock_NotFound() {
        // given
        Long couponId = 999L;

        // when
        Optional<LoadCouponPort.CouponInfo> result = couponPersistenceAdapter.loadCouponByIdWithLock(couponId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("쿠폰 발급 수량 증가 - 정상 증가")
    void incrementIssuedCount_Success() {
        // given
        Long couponId = 1L;

        // when
        boolean result = couponPersistenceAdapter.incrementIssuedCount(couponId);

        // then
        assertThat(result).isTrue();
        
        // 발급 수량이 증가했는지 확인
        Optional<LoadCouponPort.CouponInfo> couponInfo = couponPersistenceAdapter.loadCouponById(couponId);
        assertThat(couponInfo).isPresent();
        assertThat(couponInfo.get().getIssuedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("쿠폰 발급 수량 증가 - 최대 수량 도달")
    void incrementIssuedCount_MaxCountReached() {
        // given
        Long couponId = 1L;
        
        // 최대 발급 수량까지 증가
        for (int i = 0; i < 100; i++) {
            couponPersistenceAdapter.incrementIssuedCount(couponId);
        }

        // when
        boolean result = couponPersistenceAdapter.incrementIssuedCount(couponId);

        // then
        assertThat(result).isFalse();
        
        // 상태가 SOLD_OUT로 변경되었는지 확인
        Optional<LoadCouponPort.CouponInfo> couponInfo = couponPersistenceAdapter.loadCouponById(couponId);
        assertThat(couponInfo).isPresent();
        assertThat(couponInfo.get().getStatus()).isEqualTo("SOLD_OUT");
    }

    @Test
    @DisplayName("쿠폰 발급 수량 증가 - 존재하지 않는 쿠폰")
    void incrementIssuedCount_CouponNotFound() {
        // given
        Long couponId = 999L;

        // when
        boolean result = couponPersistenceAdapter.incrementIssuedCount(couponId);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("선착순 쿠폰 발급 - 동시 요청 처리")
    void incrementIssuedCount_ConcurrentRequests_FirstComeFirstServed() throws InterruptedException {
        // given
        Long couponId = 999L; // 테스트용 쿠폰 ID
        int threadCount = 10;
        int maxIssuanceCount = 5; // 최대 5개만 발급 가능
        
        // 테스트용 쿠폰 생성
        SaveCouponPort.CouponInfo testCoupon = new SaveCouponPort.CouponInfo(
                couponId, "테스트 쿠폰", "테스트 쿠폰 설명", 1000, maxIssuanceCount, 0, "ACTIVE");
        couponPersistenceAdapter.saveCoupon(testCoupon);
        
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    boolean result = couponPersistenceAdapter.incrementIssuedCount(couponId);
                    if (result) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(maxIssuanceCount);
        assertThat(failureCount.get()).isEqualTo(threadCount - maxIssuanceCount);
        
        // 추가 호출 시 실패하는지 확인
        assertThat(couponPersistenceAdapter.incrementIssuedCount(couponId)).isFalse();
        
        // 상태가 SOLD_OUT로 변경되었는지 확인
        Optional<LoadCouponPort.CouponInfo> couponInfo = couponPersistenceAdapter.loadCouponById(couponId);
        assertThat(couponInfo).isPresent();
        assertThat(couponInfo.get().getStatus()).isEqualTo("SOLD_OUT");
        assertThat(couponInfo.get().getIssuedCount()).isEqualTo(maxIssuanceCount);
    }

    @Test
    @DisplayName("선착순 쿠폰 발급 - 여러 쿠폰 동시 처리")
    void incrementIssuedCount_MultipleCoupons_Independent() throws InterruptedException {
        // given
        Long couponId1 = 1L;
        Long couponId2 = 2L;
        int threadCount = 5;
        
        CountDownLatch latch = new CountDownLatch(threadCount * 2);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount * 2);
        AtomicInteger successCount1 = new AtomicInteger(0);
        AtomicInteger successCount2 = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            // 쿠폰 1 발급 시도
            executor.submit(() -> {
                try {
                    if (couponPersistenceAdapter.incrementIssuedCount(couponId1)) {
                        successCount1.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
            
            // 쿠폰 2 발급 시도
            executor.submit(() -> {
                try {
                    if (couponPersistenceAdapter.incrementIssuedCount(couponId2)) {
                        successCount2.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();

        // then
        // 각 쿠폰은 독립적으로 처리되어야 함
        assertThat(successCount1.get()).isEqualTo(threadCount); // 쿠폰 1은 모두 성공
        assertThat(successCount2.get()).isEqualTo(threadCount); // 쿠폰 2도 모두 성공
        
        // 각 쿠폰의 발급 수량 확인
        Optional<LoadCouponPort.CouponInfo> couponInfo1 = couponPersistenceAdapter.loadCouponById(couponId1);
        Optional<LoadCouponPort.CouponInfo> couponInfo2 = couponPersistenceAdapter.loadCouponById(couponId2);
        
        assertThat(couponInfo1).isPresent();
        assertThat(couponInfo1.get().getIssuedCount()).isEqualTo(threadCount);
        
        assertThat(couponInfo2).isPresent();
        assertThat(couponInfo2.get().getIssuedCount()).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("쿠폰 저장 - 정상 저장")
    void saveCoupon_Success() {
        // given
        SaveCouponPort.CouponInfo couponInfo = new SaveCouponPort.CouponInfo(
                999L, "새 쿠폰", "새 쿠폰 설명", 2000, 50, 10, "ACTIVE");

        // when
        SaveCouponPort.CouponInfo savedCoupon = couponPersistenceAdapter.saveCoupon(couponInfo);

        // then
        assertThat(savedCoupon.getId()).isEqualTo(999L);
        assertThat(savedCoupon.getName()).isEqualTo("새 쿠폰");
        assertThat(savedCoupon.getDiscountAmount()).isEqualTo(2000);
        assertThat(savedCoupon.getMaxIssuanceCount()).isEqualTo(50);
        assertThat(savedCoupon.getIssuedCount()).isEqualTo(10);
        assertThat(savedCoupon.getStatus()).isEqualTo("ACTIVE");
        
        // 저장된 쿠폰이 조회되는지 확인
        Optional<LoadCouponPort.CouponInfo> foundCoupon = couponPersistenceAdapter.loadCouponById(999L);
        assertThat(foundCoupon).isPresent();
        assertThat(foundCoupon.get().getName()).isEqualTo("새 쿠폰");
    }
} 