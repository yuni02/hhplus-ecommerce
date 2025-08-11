package kr.hhplus.be.server.unit.coupon.application;

import kr.hhplus.be.server.coupon.application.IssueCouponService;
import kr.hhplus.be.server.coupon.application.port.in.IssueCouponUseCase;
import kr.hhplus.be.server.coupon.application.port.out.LoadUserPort;
import kr.hhplus.be.server.coupon.application.port.out.LoadCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.SaveUserCouponPort;
import kr.hhplus.be.server.coupon.domain.UserCoupon;
import kr.hhplus.be.server.shared.service.DistributedLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueCouponServiceTest {

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private LoadCouponPort loadCouponPort;

    @Mock
    private SaveUserCouponPort saveUserCouponPort;

    @Mock
    private DistributedLockService distributedLockService;

    private IssueCouponService issueCouponService;

    @BeforeEach
    void setUp() {
        issueCouponService = new IssueCouponService(loadUserPort, loadCouponPort, saveUserCouponPort, distributedLockService);
    }

    @Test
    void 쿠폰_발급_성공() {
        // Given
        Long userId = 1001L;
        Long couponId = 1L;
        IssueCouponUseCase.IssueCouponCommand command = 
            new IssueCouponUseCase.IssueCouponCommand(userId, couponId);

        LoadCouponPort.CouponInfo couponInfo = new LoadCouponPort.CouponInfo(
            couponId, "신규 가입 쿠폰", "1000원 할인", 1000, 100, 50, "ACTIVE"
        );

        UserCoupon userCoupon = UserCoupon.builder()
            .id(1L)
            .userId(userId)
            .couponId(couponId)
            .discountAmount(1000)
            .status(UserCoupon.UserCouponStatus.AVAILABLE)
            .issuedAt(LocalDateTime.now())
            .build();

        // Mock 설정
        when(distributedLockService.acquireLock(any(String.class), any(Long.class))).thenReturn(true);
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadCouponPort.loadCouponByIdWithLock(couponId)).thenReturn(Optional.of(couponInfo));
        when(loadCouponPort.incrementIssuedCount(couponId)).thenReturn(true);
        when(saveUserCouponPort.saveUserCoupon(any(UserCoupon.class))).thenReturn(userCoupon);

        // When
        IssueCouponUseCase.IssueCouponResult result = issueCouponService.issueCoupon(command);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCouponId()).isEqualTo(couponId);
        assertThat(result.getCouponName()).isEqualTo("신규 가입 쿠폰");
        assertThat(result.getDiscountAmount()).isEqualTo(1000);
    }

    @Test
    void 쿠폰_발급_실패_사용자_존재하지_않음() {
        // Given
        Long userId = 9999L;
        Long couponId = 1L;
        IssueCouponUseCase.IssueCouponCommand command = 
            new IssueCouponUseCase.IssueCouponCommand(userId, couponId);

        // Mock 설정
        when(distributedLockService.acquireLock(any(String.class), any(Long.class))).thenReturn(true);
        when(loadUserPort.existsById(userId)).thenReturn(false);

        // When
        IssueCouponUseCase.IssueCouponResult result = issueCouponService.issueCoupon(command);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("사용자를 찾을 수 없습니다");
    }

    @Test
    void 쿠폰_발급_실패_분산락_획득_실패() {
        // Given
        Long userId = 1001L;
        Long couponId = 1L;
        IssueCouponUseCase.IssueCouponCommand command = 
            new IssueCouponUseCase.IssueCouponCommand(userId, couponId);

        // Mock 설정
        when(distributedLockService.acquireLock(any(String.class), any(Long.class))).thenReturn(false);

        // When
        IssueCouponUseCase.IssueCouponResult result = issueCouponService.issueCoupon(command);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("쿠폰 발급 처리 중입니다");
    }

    @Test
    void 쿠폰_발급_실패_쿠폰_소진() {
        // Given
        Long userId = 1001L;
        Long couponId = 1L;
        IssueCouponUseCase.IssueCouponCommand command = 
            new IssueCouponUseCase.IssueCouponCommand(userId, couponId);

        LoadCouponPort.CouponInfo couponInfo = new LoadCouponPort.CouponInfo(
            couponId, "신규 가입 쿠폰", "1000원 할인", 1000, 100, 100, "ACTIVE"
        );

        // Mock 설정
        when(distributedLockService.acquireLock(any(String.class), any(Long.class))).thenReturn(true);
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadCouponPort.loadCouponByIdWithLock(couponId)).thenReturn(Optional.of(couponInfo));

        // When
        IssueCouponUseCase.IssueCouponResult result = issueCouponService.issueCoupon(command);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("발급할 수 없는 쿠폰입니다");
    }
} 