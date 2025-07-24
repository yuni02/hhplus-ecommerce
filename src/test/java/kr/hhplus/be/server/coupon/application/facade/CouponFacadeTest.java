package kr.hhplus.be.server.coupon.application.facade;

import kr.hhplus.be.server.coupon.application.port.in.GetUserCouponsUseCase;
import kr.hhplus.be.server.coupon.application.port.in.IssueCouponUseCase;
import kr.hhplus.be.server.coupon.application.port.in.UseCouponUseCase;
import kr.hhplus.be.server.coupon.application.port.out.LoadCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.LoadUserCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.LoadUserPort;
import kr.hhplus.be.server.coupon.application.port.out.SaveCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.SaveUserCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.UpdateUserCouponPort;
import kr.hhplus.be.server.coupon.domain.UserCoupon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponFacadeTest {

    @Mock
    private LoadUserPort loadUserPort;
    
    @Mock
    private LoadCouponPort loadCouponPort;
    
    @Mock
    private SaveCouponPort saveCouponPort;
    
    @Mock
    private LoadUserCouponPort loadUserCouponPort;
    
    @Mock
    private SaveUserCouponPort saveUserCouponPort;
    
    @Mock
    private UpdateUserCouponPort updateUserCouponPort;

    private CouponFacade couponFacade;

    @BeforeEach
    void setUp() {
        couponFacade = new CouponFacade(
                loadUserPort,
                loadCouponPort,
                saveCouponPort,
                loadUserCouponPort,
                saveUserCouponPort,
                updateUserCouponPort
        );
    }

    @Test
    @DisplayName("선착순 쿠폰 발급 - 정상 발급")
    void issueCoupon_FirstComeFirstServed_Success() {
        // given
        Long userId = 1L;
        Long couponId = 1L;
        
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadCouponPort.loadCouponByIdWithLock(couponId))
                .thenReturn(Optional.of(new LoadCouponPort.CouponInfo(
                        couponId, "테스트 쿠폰", "테스트 쿠폰 설명", 
                        1000, 10, 5, "ACTIVE")));
        when(loadCouponPort.incrementIssuedCount(couponId)).thenReturn(true);
        when(saveUserCouponPort.saveUserCoupon(any(UserCoupon.class)))
                .thenAnswer(invocation -> {
                    UserCoupon userCoupon = invocation.getArgument(0);
                    userCoupon.setId(1L);
                    return userCoupon;
                });

        // when
        var result = couponFacade.issueCoupon(
                new IssueCouponUseCase.IssueCouponCommand(userId, couponId)
        );

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getUserCouponId()).isEqualTo(1L);
        assertThat(result.getCouponId()).isEqualTo(couponId);
        assertThat(result.getCouponName()).isEqualTo("테스트 쿠폰");
        assertThat(result.getDiscountAmount()).isEqualTo(1000);
        assertThat(result.getStatus()).isEqualTo("AVAILABLE");
        
        verify(loadCouponPort).loadCouponByIdWithLock(couponId);
        verify(loadCouponPort).incrementIssuedCount(couponId);
    }

    @Test
    @DisplayName("선착순 쿠폰 발급 - 재고 소진으로 실패")
    void issueCoupon_OutOfStock_Failure() {
        // given
        Long userId = 1L;
        Long couponId = 1L;
        
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadCouponPort.loadCouponByIdWithLock(couponId))
                .thenReturn(Optional.of(new LoadCouponPort.CouponInfo(
                        couponId, "테스트 쿠폰", "테스트 쿠폰 설명", 
                        1000, 10, 10, "ACTIVE"))); // 이미 최대 발급 수량에 도달

        // when
        var result = couponFacade.issueCoupon(
                new IssueCouponUseCase.IssueCouponCommand(userId, couponId)
        );

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("발급할 수 없는 쿠폰입니다.");
        
        verify(loadCouponPort).loadCouponByIdWithLock(couponId);
        verify(loadCouponPort, never()).incrementIssuedCount(couponId); // canIssueCoupon에서 실패하므로 호출되지 않음
        verify(saveUserCouponPort, never()).saveUserCoupon(any());
    }

    @Test
    @DisplayName("선착순 쿠폰 발급 - 동시 요청 처리")
    void issueCoupon_ConcurrentRequests_FirstComeFirstServed() throws InterruptedException {
        // given
        Long userId = 1L;
        Long couponId = 1L;
        int threadCount = 5;
        int maxIssuanceCount = 3; // 최대 3개만 발급 가능
        
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        // Mock 설정
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadCouponPort.loadCouponByIdWithLock(couponId))
                .thenReturn(Optional.of(new LoadCouponPort.CouponInfo(
                        couponId, "테스트 쿠폰", "테스트 쿠폰 설명", 
                        1000, maxIssuanceCount, 0, "ACTIVE")));
        
        // incrementIssuedCount를 호출할 때마다 다른 결과 반환 (선착순 시뮬레이션)
        AtomicInteger callCount = new AtomicInteger(0);
        when(loadCouponPort.incrementIssuedCount(couponId))
                .thenAnswer(invocation -> {
                    int currentCall = callCount.incrementAndGet();
                    return currentCall <= maxIssuanceCount; // 처음 3번만 성공
                });
        
        when(saveUserCouponPort.saveUserCoupon(any(UserCoupon.class)))
                .thenAnswer(invocation -> {
                    UserCoupon userCoupon = invocation.getArgument(0);
                    userCoupon.setId(successCount.get() + 1L);
                    return userCoupon;
                });

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    var result = couponFacade.issueCoupon(
                            new IssueCouponUseCase.IssueCouponCommand(userId, couponId)
                    );
                    
                    if (result.isSuccess()) {
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
        assertThat(loadCouponPort.incrementIssuedCount(couponId)).isFalse(); // 추가 호출 시 실패
    }

    @Test
    @DisplayName("선착순 쿠폰 발급 - 잘못된 사용자 ID")
    void issueCoupon_InvalidUserId_Failure() {
        // given
        Long userId = -1L;
        Long couponId = 1L;

        // when
        var result = couponFacade.issueCoupon(
                new IssueCouponUseCase.IssueCouponCommand(userId, couponId)
        );

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("잘못된 사용자 ID");
        
        verify(loadUserPort, never()).existsById(anyLong());
        verify(loadCouponPort, never()).loadCouponByIdWithLock(anyLong());
    }

    @Test
    @DisplayName("선착순 쿠폰 발급 - 존재하지 않는 사용자")
    void issueCoupon_UserNotFound_Failure() {
        // given
        Long userId = 1L;
        Long couponId = 1L;
        
        when(loadUserPort.existsById(userId)).thenReturn(false);

        // when
        var result = couponFacade.issueCoupon(
                new IssueCouponUseCase.IssueCouponCommand(userId, couponId)
        );

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("사용자를 찾을 수 없습니다");
        
        verify(loadUserPort).existsById(userId);
        verify(loadCouponPort, never()).loadCouponByIdWithLock(anyLong());
    }

    @Test
    @DisplayName("선착순 쿠폰 발급 - 존재하지 않는 쿠폰")
    void issueCoupon_CouponNotFound_Failure() {
        // given
        Long userId = 1L;
        Long couponId = 999L;
        
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadCouponPort.loadCouponByIdWithLock(couponId)).thenReturn(Optional.empty());

        // when
        var result = couponFacade.issueCoupon(
                new IssueCouponUseCase.IssueCouponCommand(userId, couponId)
        );

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("존재하지 않는 쿠폰입니다");
        
        verify(loadUserPort).existsById(userId);
        verify(loadCouponPort).loadCouponByIdWithLock(couponId);
        verify(loadCouponPort, never()).incrementIssuedCount(anyLong());
    }

    @Test
    @DisplayName("선착순 쿠폰 발급 - 비활성 쿠폰")
    void issueCoupon_InactiveCoupon_Failure() {
        // given
        Long userId = 1L;
        Long couponId = 1L;
        
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadCouponPort.loadCouponByIdWithLock(couponId))
                .thenReturn(Optional.of(new LoadCouponPort.CouponInfo(
                        couponId, "테스트 쿠폰", "테스트 쿠폰 설명", 
                        1000, 10, 5, "INACTIVE"))); // 비활성 상태

        // when
        var result = couponFacade.issueCoupon(
                new IssueCouponUseCase.IssueCouponCommand(userId, couponId)
        );

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("발급할 수 없는 쿠폰입니다");
        
        verify(loadUserPort).existsById(userId);
        verify(loadCouponPort).loadCouponByIdWithLock(couponId);
        verify(loadCouponPort, never()).incrementIssuedCount(anyLong());
    }

    @Test
    @DisplayName("선착순 쿠폰 발급 - 예외 발생 시 처리")
    void issueCoupon_ExceptionOccurs_Failure() {
        // given
        Long userId = 1L;
        Long couponId = 1L;
        
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadCouponPort.loadCouponByIdWithLock(couponId))
                .thenThrow(new RuntimeException("데이터베이스 연결 오류"));

        // when
        var result = couponFacade.issueCoupon(
                new IssueCouponUseCase.IssueCouponCommand(userId, couponId)
        );

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("쿠폰 발급 중 오류가 발생했습니다");
        assertThat(result.getErrorMessage()).contains("데이터베이스 연결 오류");
    }
} 