package kr.hhplus.be.server.unit.coupon.domain;

import kr.hhplus.be.server.coupon.domain.Coupon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponTest {

    @Test
    @DisplayName("Coupon 생성 성공")
    void createCoupon_Success() {
        // given
        String name = "신규 가입 쿠폰";
        String description = "신규 가입 회원을 위한 할인 쿠폰";
        BigDecimal discountAmount = new BigDecimal("5000");
        Integer maxIssuanceCount = 100;
        LocalDateTime validFrom = LocalDateTime.now().minusDays(1);
        LocalDateTime validTo = LocalDateTime.now().plusDays(30);

        // when
        Coupon coupon = Coupon.builder()
                .name(name)
                .description(description)
                .discountAmount(discountAmount)
                .maxIssuanceCount(maxIssuanceCount)
                .validFrom(validFrom)
                .validTo(validTo)
                .build();

        // then
        assertThat(coupon.getName()).isEqualTo(name);
        assertThat(coupon.getDescription()).isEqualTo(description);
        assertThat(coupon.getDiscountAmount()).isEqualTo(discountAmount);
        assertThat(coupon.getMaxIssuanceCount()).isEqualTo(maxIssuanceCount);
        assertThat(coupon.getIssuedCount()).isEqualTo(0);
        assertThat(coupon.getStatus()).isEqualTo(Coupon.CouponStatus.ACTIVE);
        assertThat(coupon.getValidFrom()).isEqualTo(validFrom);
        assertThat(coupon.getValidTo()).isEqualTo(validTo);
    }

    @Test
    @DisplayName("쿠폰 발급 가능 여부 확인 - 정상 발급 가능")
    void canIssue_Success() {
        // given
        Coupon coupon = Coupon.builder()
                .name("테스트 쿠폰")
                .discountAmount(new BigDecimal("3000"))
                .maxIssuanceCount(10)
                .issuedCount(5)
                .status(Coupon.CouponStatus.ACTIVE)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(1))
                .build();

        // when
        boolean canIssue = coupon.canIssue();

        // then
        assertThat(canIssue).isTrue();
    }

    @Test
    @DisplayName("쿠폰 발급 가능 여부 확인 - 수량 초과로 발급 불가")
    void canIssue_Failed_MaxCountReached() {
        // given
        Coupon coupon = Coupon.builder()
                .name("테스트 쿠폰")
                .discountAmount(new BigDecimal("3000"))
                .maxIssuanceCount(10)
                .issuedCount(10)
                .status(Coupon.CouponStatus.ACTIVE)
                .build();

        // when
        boolean canIssue = coupon.canIssue();

        // then
        assertThat(canIssue).isFalse();
    }

    @Test
    @DisplayName("쿠폰 발급 가능 여부 확인 - 비활성 상태로 발급 불가")
    void canIssue_Failed_InactiveStatus() {
        // given
        Coupon coupon = Coupon.builder()
                .name("테스트 쿠폰")
                .discountAmount(new BigDecimal("3000"))
                .maxIssuanceCount(10)
                .issuedCount(5)
                .status(Coupon.CouponStatus.INACTIVE)
                .build();

        // when
        boolean canIssue = coupon.canIssue();

        // then
        assertThat(canIssue).isFalse();
    }

    @Test
    @DisplayName("쿠폰 발급 가능 여부 확인 - 유효기간 만료로 발급 불가")
    void canIssue_Failed_Expired() {
        // given
        Coupon coupon = Coupon.builder()
                .name("테스트 쿠폰")
                .discountAmount(new BigDecimal("3000"))
                .maxIssuanceCount(10)
                .issuedCount(5)
                .status(Coupon.CouponStatus.ACTIVE)
                .validFrom(LocalDateTime.now().minusDays(10))
                .validTo(LocalDateTime.now().minusDays(1))
                .build();

        // when
        boolean canIssue = coupon.canIssue();

        // then
        assertThat(canIssue).isFalse();
    }

    @Test
    @DisplayName("쿠폰 발급 가능 여부 확인 - 유효기간 시작 전 발급 불가")
    void canIssue_Failed_NotStarted() {
        // given
        Coupon coupon = Coupon.builder()
                .name("테스트 쿠폰")
                .discountAmount(new BigDecimal("3000"))
                .maxIssuanceCount(10)
                .issuedCount(5)
                .status(Coupon.CouponStatus.ACTIVE)
                .validFrom(LocalDateTime.now().plusDays(1))
                .validTo(LocalDateTime.now().plusDays(10))
                .build();

        // when
        boolean canIssue = coupon.canIssue();

        // then
        assertThat(canIssue).isFalse();
    }

    @Test
    @DisplayName("쿠폰 발급 수량 증가 성공")
    void incrementIssuedCount_Success() {
        // given
        Coupon coupon = Coupon.builder()
                .name("테스트 쿠폰")
                .discountAmount(new BigDecimal("3000"))
                .maxIssuanceCount(10)
                .issuedCount(5)
                .status(Coupon.CouponStatus.ACTIVE)
                .build();

        // when
        coupon.incrementIssuedCount();

        // then
        assertThat(coupon.getIssuedCount()).isEqualTo(6);
        assertThat(coupon.getStatus()).isEqualTo(Coupon.CouponStatus.ACTIVE);
        assertThat(coupon.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("쿠폰 발급 수량 증가 - 최대 수량 도달 시 SOLD_OUT 상태로 변경")
    void incrementIssuedCount_ChangesToSoldOut() {
        // given
        Coupon coupon = Coupon.builder()
                .name("테스트 쿠폰")
                .discountAmount(new BigDecimal("3000"))
                .maxIssuanceCount(10)
                .issuedCount(9)
                .status(Coupon.CouponStatus.ACTIVE)
                .build();

        // when
        coupon.incrementIssuedCount();

        // then
        assertThat(coupon.getIssuedCount()).isEqualTo(10);
        assertThat(coupon.getStatus()).isEqualTo(Coupon.CouponStatus.SOLD_OUT);
        assertThat(coupon.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("쿠폰 발급 수량 증가 실패 - 발급 불가능한 상태")
    void incrementIssuedCount_Failed_CannotIssue() {
        // given
        Coupon coupon = Coupon.builder()
                .name("테스트 쿠폰")
                .discountAmount(new BigDecimal("3000"))
                .maxIssuanceCount(10)
                .issuedCount(10)
                .status(Coupon.CouponStatus.SOLD_OUT)
                .build();

        // when & then
        assertThatThrownBy(() -> coupon.incrementIssuedCount())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("쿠폰을 발급할 수 없습니다.");
    }

    @Test
    @DisplayName("쿠폰 상태 enum 검증")
    void couponStatusEnum() {
        // then
        assertThat(Coupon.CouponStatus.values()).containsExactly(
                Coupon.CouponStatus.ACTIVE,
                Coupon.CouponStatus.INACTIVE,
                Coupon.CouponStatus.SOLD_OUT,
                Coupon.CouponStatus.EXPIRED
        );
    }
}