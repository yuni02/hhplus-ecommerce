package kr.hhplus.be.server.unit.coupon.domain;

import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.UserCoupon;
import kr.hhplus.be.server.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserCouponTest {

    @Test
    @DisplayName("UserCoupon 생성 성공")
    void createUserCoupon_Success() {
        // given
        Long userId = 1L;
        Long couponId = 100L;
        Integer discountAmount = 5000;
        LocalDateTime issuedAt = LocalDateTime.now();

        // when
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(userId)
                .couponId(couponId)
                .discountAmount(discountAmount)
                .issuedAt(issuedAt)
                .build();

        // then
        assertThat(userCoupon.getUserId()).isEqualTo(userId);
        assertThat(userCoupon.getCouponId()).isEqualTo(couponId);
        assertThat(userCoupon.getDiscountAmount()).isEqualTo(discountAmount);
        assertThat(userCoupon.getStatus()).isEqualTo(UserCoupon.UserCouponStatus.AVAILABLE);
        assertThat(userCoupon.getIssuedAt()).isEqualTo(issuedAt);
        assertThat(userCoupon.getUsedAt()).isNull();
        assertThat(userCoupon.getOrderId()).isNull();
    }

    @Test
    @DisplayName("쿠폰 사용 가능 여부 확인 - 사용 가능")
    void isAvailable_True() {
        // given
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(1L)
                .couponId(100L)
                .discountAmount(3000)
                .status(UserCoupon.UserCouponStatus.AVAILABLE)
                .build();

        // when
        boolean isAvailable = userCoupon.isAvailable();

        // then
        assertThat(isAvailable).isTrue();
    }

    @Test
    @DisplayName("쿠폰 사용 가능 여부 확인 - 이미 사용됨")
    void isAvailable_False_Used() {
        // given
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(1L)
                .couponId(100L)
                .discountAmount(3000)
                .status(UserCoupon.UserCouponStatus.USED)
                .build();

        // when
        boolean isAvailable = userCoupon.isAvailable();

        // then
        assertThat(isAvailable).isFalse();
    }

    @Test
    @DisplayName("쿠폰 사용 가능 여부 확인 - 만료됨")
    void isAvailable_False_Expired() {
        // given
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(1L)
                .couponId(100L)
                .discountAmount(3000)
                .status(UserCoupon.UserCouponStatus.EXPIRED)
                .build();

        // when
        boolean isAvailable = userCoupon.isAvailable();

        // then
        assertThat(isAvailable).isFalse();
    }

    @Test
    @DisplayName("쿠폰 사용 성공 - orderId로 사용")
    void useCoupon_WithOrderId_Success() {
        // given
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(1L)
                .couponId(100L)
                .discountAmount(3000)
                .status(UserCoupon.UserCouponStatus.AVAILABLE)
                .build();
        Long orderId = 1000L;

        // when
        userCoupon.use(orderId);

        // then
        assertThat(userCoupon.getStatus()).isEqualTo(UserCoupon.UserCouponStatus.USED);
        assertThat(userCoupon.getOrderId()).isEqualTo(orderId);
        assertThat(userCoupon.getUsedAt()).isNotNull();
        assertThat(userCoupon.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("쿠폰 사용 성공 - 사용 시간 지정")
    void useCoupon_WithUsedAt_Success() {
        // given
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(1L)
                .couponId(100L)
                .discountAmount(3000)
                .status(UserCoupon.UserCouponStatus.AVAILABLE)
                .build();
        LocalDateTime usedAt = LocalDateTime.now();

        // when
        userCoupon.use(usedAt);

        // then
        assertThat(userCoupon.getStatus()).isEqualTo(UserCoupon.UserCouponStatus.USED);
        assertThat(userCoupon.getUsedAt()).isEqualTo(usedAt);
        assertThat(userCoupon.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("쿠폰 사용 실패 - 이미 사용된 쿠폰")
    void useCoupon_Failed_AlreadyUsed() {
        // given
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(1L)
                .couponId(100L)
                .discountAmount(3000)
                .status(UserCoupon.UserCouponStatus.USED)
                .build();
        Long orderId = 1000L;

        // when & then
        assertThatThrownBy(() -> userCoupon.use(orderId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("사용할 수 없는 쿠폰입니다.");
    }

    @Test
    @DisplayName("쿠폰 사용 실패 - 만료된 쿠폰")
    void useCoupon_Failed_Expired() {
        // given
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(1L)
                .couponId(100L)
                .discountAmount(3000)
                .status(UserCoupon.UserCouponStatus.EXPIRED)
                .build();
        LocalDateTime usedAt = LocalDateTime.now();

        // when & then
        assertThatThrownBy(() -> userCoupon.use(usedAt))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("사용할 수 없는 쿠폰입니다.");
    }

    @Test
    @DisplayName("쿠폰 복원 성공")
    void restoreCoupon_Success() {
        // given
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(1L)
                .couponId(100L)
                .discountAmount(3000)
                .status(UserCoupon.UserCouponStatus.USED)
                .orderId(1000L)
                .usedAt(LocalDateTime.now())
                .build();

        // when
        userCoupon.restore();

        // then
        assertThat(userCoupon.getStatus()).isEqualTo(UserCoupon.UserCouponStatus.AVAILABLE);
        assertThat(userCoupon.getUsedAt()).isNull();
        assertThat(userCoupon.getOrderId()).isNull();
        assertThat(userCoupon.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("쿠폰 복원 실패 - 사용되지 않은 쿠폰")
    void restoreCoupon_Failed_NotUsed() {
        // given
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(1L)
                .couponId(100L)
                .discountAmount(3000)
                .status(UserCoupon.UserCouponStatus.AVAILABLE)
                .build();

        // when & then
        assertThatThrownBy(() -> userCoupon.restore())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("사용된 쿠폰만 복원할 수 있습니다.");
    }

    @Test
    @DisplayName("쿠폰 복원 실패 - 만료된 쿠폰")
    void restoreCoupon_Failed_Expired() {
        // given
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(1L)
                .couponId(100L)
                .discountAmount(3000)
                .status(UserCoupon.UserCouponStatus.EXPIRED)
                .build();

        // when & then
        assertThatThrownBy(() -> userCoupon.restore())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("사용된 쿠폰만 복원할 수 있습니다.");
    }

    @Test
    @DisplayName("UserCoupon과 연관 엔티티 설정")
    void userCouponWithAssociations() {
        // given
        User user = User.builder()
                .id(1L)
                .name("테스트 유저")
                .build();
        
        Coupon coupon = Coupon.builder()
                .id(100L)
                .name("할인 쿠폰")
                .discountAmount(new BigDecimal("5000"))
                .build();

        // when
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(user.getId())
                .couponId(coupon.getId())
                .discountAmount(5000)
                .user(user)
                .coupon(coupon)
                .build();

        // then
        assertThat(userCoupon.getUser()).isEqualTo(user);
        assertThat(userCoupon.getCoupon()).isEqualTo(coupon);
        assertThat(userCoupon.getUserId()).isEqualTo(user.getId());
        assertThat(userCoupon.getCouponId()).isEqualTo(coupon.getId());
    }

    @Test
    @DisplayName("UserCouponStatus enum 검증")
    void userCouponStatusEnum() {
        // then
        assertThat(UserCoupon.UserCouponStatus.values()).containsExactly(
                UserCoupon.UserCouponStatus.AVAILABLE,
                UserCoupon.UserCouponStatus.USED,
                UserCoupon.UserCouponStatus.EXPIRED
        );
    }
}