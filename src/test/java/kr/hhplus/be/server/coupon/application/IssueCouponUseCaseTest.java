package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.CouponRepository;
import kr.hhplus.be.server.coupon.domain.UserCoupon;
import kr.hhplus.be.server.coupon.domain.UserCouponRepository;
import kr.hhplus.be.server.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IssueCouponUseCase 단위 테스트")
class IssueCouponUseCaseTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private UserRepository userRepository;

    private IssueCouponUseCase issueCouponUseCase;

    @BeforeEach
    void setUp() {
        issueCouponUseCase = new IssueCouponUseCase(
                couponRepository, userCouponRepository, userRepository);
    }

    @Test
    @DisplayName("정상적인 쿠폰 발급")
    void issueCoupon_ValidRequest_Success() {
        // given
        Long userId = 1L;
        Long couponId = 1L;

        Coupon coupon = new Coupon("10% 할인 쿠폰", "모든 상품 10% 할인", 
                BigDecimal.valueOf(1000), 100, 
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30));
        coupon.setId(couponId);
        coupon.setIssuedCount(50);

        UserCoupon userCoupon = new UserCoupon(userId, couponId);
        userCoupon.setId(1L);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
        when(userCouponRepository.existsByUserIdAndCouponId(userId, couponId)).thenReturn(false);
        when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);
        when(userCouponRepository.save(any(UserCoupon.class))).thenReturn(userCoupon);

        // when
        UserCoupon result = issueCouponUseCase.execute(userId, couponId);

        // then
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getCouponId()).isEqualTo(couponId);
        verify(userRepository).existsById(userId);
        verify(couponRepository).findById(couponId);
        verify(userCouponRepository).existsByUserIdAndCouponId(userId, couponId);
        verify(couponRepository).save(any(Coupon.class));
        verify(userCouponRepository).save(any(UserCoupon.class));
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 쿠폰 발급 시 예외 발생")
    void issueCoupon_NonExistentUser_ThrowsException() {
        // given
        Long userId = 999L;
        Long couponId = 1L;

        when(userRepository.existsById(userId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> issueCouponUseCase.execute(userId, couponId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");

        verify(userRepository).existsById(userId);
        verifyNoInteractions(couponRepository, userCouponRepository);
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰으로 발급 시 예외 발생")
    void issueCoupon_NonExistentCoupon_ThrowsException() {
        // given
        Long userId = 1L;
        Long couponId = 999L;

        when(userRepository.existsById(userId)).thenReturn(true);
        when(couponRepository.findById(couponId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> issueCouponUseCase.execute(userId, couponId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 쿠폰입니다.");

        verify(userRepository).existsById(userId);
        verify(couponRepository).findById(couponId);
        verifyNoInteractions(userCouponRepository);
    }

    @Test
    @DisplayName("이미 발급받은 쿠폰 재발급 시 예외 발생")
    void issueCoupon_AlreadyIssued_ThrowsException() {
        // given
        Long userId = 1L;
        Long couponId = 1L;

        Coupon coupon = new Coupon("10% 할인 쿠폰", "모든 상품 10% 할인", 
                BigDecimal.valueOf(1000), 100, 
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30));
        coupon.setId(couponId);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
        when(userCouponRepository.existsByUserIdAndCouponId(userId, couponId)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> issueCouponUseCase.execute(userId, couponId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 발급받은 쿠폰입니다.");

        verify(userRepository).existsById(userId);
        verify(couponRepository).findById(couponId);
        verify(userCouponRepository).existsByUserIdAndCouponId(userId, couponId);
        verifyNoMoreInteractions(couponRepository, userCouponRepository);
    }

    @Test
    @DisplayName("발급 마감된 쿠폰 발급 시 예외 발생")
    void issueCoupon_SoldOutCoupon_ThrowsException() {
        // given
        Long userId = 1L;
        Long couponId = 1L;

        Coupon coupon = new Coupon("10% 할인 쿠폰", "모든 상품 10% 할인", 
                BigDecimal.valueOf(1000), 100, 
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30));
        coupon.setId(couponId);
        coupon.setIssuedCount(100); // 최대 발급 수량에 도달
        coupon.setStatus(Coupon.CouponStatus.SOLD_OUT);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
        when(userCouponRepository.existsByUserIdAndCouponId(userId, couponId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> issueCouponUseCase.execute(userId, couponId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("쿠폰 발급이 마감되었습니다.");

        verify(userRepository).existsById(userId);
        verify(couponRepository).findById(couponId);
        verify(userCouponRepository).existsByUserIdAndCouponId(userId, couponId);
        verifyNoMoreInteractions(couponRepository, userCouponRepository);
    }
} 