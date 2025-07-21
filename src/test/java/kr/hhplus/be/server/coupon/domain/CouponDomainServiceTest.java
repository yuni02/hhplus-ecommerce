package kr.hhplus.be.server.coupon.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CouponDomainService 단위 테스트")
class CouponDomainServiceTest {

    @Test
    @DisplayName("발급 가능한 쿠폰 확인")
    void canIssueCoupon_ValidCoupon_ReturnsTrue() {
        // given
        Coupon coupon = new Coupon("10% 할인 쿠폰", "모든 상품 10% 할인", 
                BigDecimal.valueOf(1000), 100, 
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30));
        coupon.setStatus(Coupon.CouponStatus.ACTIVE);
        coupon.setIssuedCount(50);

        // when
        boolean result = CouponDomainService.canIssueCoupon(coupon);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("품절된 쿠폰 발급 불가 확인")
    void canIssueCoupon_SoldOutCoupon_ReturnsFalse() {
        // given
        Coupon coupon = new Coupon("10% 할인 쿠폰", "모든 상품 10% 할인", 
                BigDecimal.valueOf(1000), 100, 
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30));
        coupon.setStatus(Coupon.CouponStatus.SOLD_OUT);
        coupon.setIssuedCount(100);

        // when
        boolean result = CouponDomainService.canIssueCoupon(coupon);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("만료된 쿠폰 발급 불가 확인")
    void canIssueCoupon_ExpiredCoupon_ReturnsFalse() {
        // given
        Coupon coupon = new Coupon("10% 할인 쿠폰", "모든 상품 10% 할인", 
                BigDecimal.valueOf(1000), 100, 
                LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(1));
        coupon.setStatus(Coupon.CouponStatus.ACTIVE);

        // when
        boolean result = CouponDomainService.canIssueCoupon(coupon);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("정상적인 쿠폰 발급 처리")
    void issueCoupon_ValidCoupon_Success() {
        // given
        Coupon coupon = new Coupon("10% 할인 쿠폰", "모든 상품 10% 할인", 
                BigDecimal.valueOf(1000), 100, 
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30));
        coupon.setStatus(Coupon.CouponStatus.ACTIVE);
        coupon.setIssuedCount(50);

        // when
        Coupon result = CouponDomainService.issueCoupon(coupon);

        // then
        assertThat(result.getIssuedCount()).isEqualTo(51);
        assertThat(result.getStatus()).isEqualTo(Coupon.CouponStatus.ACTIVE);
    }

    @Test
    @DisplayName("마지막 쿠폰 발급 시 품절 상태로 변경")
    void issueCoupon_LastCoupon_SoldOutStatus() {
        // given
        Coupon coupon = new Coupon("10% 할인 쿠폰", "모든 상품 10% 할인", 
                BigDecimal.valueOf(1000), 100, 
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30));
        coupon.setStatus(Coupon.CouponStatus.ACTIVE);
        coupon.setIssuedCount(99); // 마지막 1개

        // when
        Coupon result = CouponDomainService.issueCoupon(coupon);

        // then
        assertThat(result.getIssuedCount()).isEqualTo(100);
        assertThat(result.getStatus()).isEqualTo(Coupon.CouponStatus.SOLD_OUT);
    }

    @Test
    @DisplayName("발급 불가능한 쿠폰 발급 시 예외 발생")
    void issueCoupon_InvalidCoupon_ThrowsException() {
        // given
        Coupon coupon = new Coupon("10% 할인 쿠폰", "모든 상품 10% 할인", 
                BigDecimal.valueOf(1000), 100, 
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30));
        coupon.setStatus(Coupon.CouponStatus.SOLD_OUT);

        // when & then
        assertThatThrownBy(() -> CouponDomainService.issueCoupon(coupon))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("쿠폰을 발급할 수 없습니다.");
    }

    @Test
    @DisplayName("사용자 쿠폰 생성")
    void createUserCoupon_ValidData_Success() {
        // given
        Long userId = 1L;
        Long couponId = 1L;

        // when
        UserCoupon result = CouponDomainService.createUserCoupon(userId, couponId);

        // then
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getCouponId()).isEqualTo(couponId);
        assertThat(result.getStatus()).isEqualTo(UserCoupon.UserCouponStatus.AVAILABLE);
        assertThat(result.getIssuedAt()).isNotNull();
    }

    @Test
    @DisplayName("사용 가능한 쿠폰 확인")
    void canUseCoupon_AvailableCoupon_ReturnsTrue() {
        // given
        UserCoupon userCoupon = new UserCoupon(1L, 1L);
        userCoupon.setStatus(UserCoupon.UserCouponStatus.AVAILABLE);

        // when
        boolean result = CouponDomainService.canUseCoupon(userCoupon);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("이미 사용된 쿠폰 사용 불가 확인")
    void canUseCoupon_UsedCoupon_ReturnsFalse() {
        // given
        UserCoupon userCoupon = new UserCoupon(1L, 1L);
        userCoupon.setStatus(UserCoupon.UserCouponStatus.USED);

        // when
        boolean result = CouponDomainService.canUseCoupon(userCoupon);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("정상적인 쿠폰 사용 처리")
    void useCoupon_ValidCoupon_Success() {
        // given
        UserCoupon userCoupon = new UserCoupon(1L, 1L);
        userCoupon.setStatus(UserCoupon.UserCouponStatus.AVAILABLE);
        Long orderId = 1L;

        // when
        UserCoupon result = CouponDomainService.useCoupon(userCoupon, orderId);

        // then
        assertThat(result.getStatus()).isEqualTo(UserCoupon.UserCouponStatus.USED);
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("사용 불가능한 쿠폰 사용 시 예외 발생")
    void useCoupon_InvalidCoupon_ThrowsException() {
        // given
        UserCoupon userCoupon = new UserCoupon(1L, 1L);
        userCoupon.setStatus(UserCoupon.UserCouponStatus.USED);
        Long orderId = 1L;

        // when & then
        assertThatThrownBy(() -> CouponDomainService.useCoupon(userCoupon, orderId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("사용할 수 없는 쿠폰입니다.");
    }

    @Test
    @DisplayName("유효한 쿠폰 확인")
    void isValidCoupon_ValidCoupon_ReturnsTrue() {
        // given
        Coupon coupon = new Coupon("10% 할인 쿠폰", "모든 상품 10% 할인", 
                BigDecimal.valueOf(1000), 100, 
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30));
        coupon.setStatus(Coupon.CouponStatus.ACTIVE);

        // when
        boolean result = CouponDomainService.isValidCoupon(coupon);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("무효한 쿠폰 확인")
    void isValidCoupon_InvalidCoupon_ReturnsFalse() {
        // given
        Coupon coupon = new Coupon("10% 할인 쿠폰", "모든 상품 10% 할인", 
                BigDecimal.valueOf(1000), 100, 
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30));
        coupon.setStatus(Coupon.CouponStatus.INACTIVE);

        // when
        boolean result = CouponDomainService.isValidCoupon(coupon);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("중복 발급 확인 - 이미 발급된 경우")
    void isAlreadyIssued_AlreadyIssued_ReturnsTrue() {
        // given
        Long userId = 1L;
        Long couponId = 1L;
        boolean existsByUserIdAndCouponId = true;

        // when
        boolean result = CouponDomainService.isAlreadyIssued(userId, couponId, existsByUserIdAndCouponId);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("중복 발급 확인 - 발급되지 않은 경우")
    void isAlreadyIssued_NotIssued_ReturnsFalse() {
        // given
        Long userId = 1L;
        Long couponId = 1L;
        boolean existsByUserIdAndCouponId = false;

        // when
        boolean result = CouponDomainService.isAlreadyIssued(userId, couponId, existsByUserIdAndCouponId);

        // then
        assertThat(result).isFalse();
    }
} 