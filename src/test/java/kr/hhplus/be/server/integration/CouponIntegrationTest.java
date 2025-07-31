package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.coupon.application.IssueCouponService;
import kr.hhplus.be.server.coupon.application.GetUserCouponsService;
import kr.hhplus.be.server.coupon.application.port.in.IssueCouponUseCase;
import kr.hhplus.be.server.coupon.application.port.in.GetUserCouponsUseCase;
import kr.hhplus.be.server.coupon.domain.UserCoupon;
import kr.hhplus.be.server.coupon.infrastructure.persistence.adapter.CouponPersistenceAdapter;
import kr.hhplus.be.server.coupon.infrastructure.persistence.adapter.UserCouponPersistenceAdapter;
import kr.hhplus.be.server.coupon.infrastructure.persistence.entity.CouponEntity;
import kr.hhplus.be.server.coupon.infrastructure.persistence.entity.UserCouponEntity;
import kr.hhplus.be.server.coupon.infrastructure.persistence.repository.CouponJpaRepository;
import kr.hhplus.be.server.coupon.infrastructure.persistence.repository.UserCouponJpaRepository;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.infrastructure.persistence.adapter.UserPersistenceAdapter;
import kr.hhplus.be.server.user.infrastructure.persistence.entity.UserEntity;
import kr.hhplus.be.server.user.infrastructure.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Coupon 도메인 통합테스트")
class CouponIntegrationTest {

    @Autowired
    private IssueCouponService issueCouponService;

    @Autowired
    private GetUserCouponsService getUserCouponsService;

    @Autowired
    private CouponJpaRepository couponJpaRepository;

    @Autowired
    private UserCouponJpaRepository userCouponJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    private UserEntity testUser;
    private CouponEntity testCoupon;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        userCouponJpaRepository.deleteAll();
        couponJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        // 테스트용 사용자 생성
        testUser = UserEntity.builder()
                .userId(1L)
                .username("testuser")
                .status("ACTIVE")
                .build();
        testUser = userJpaRepository.saveAndFlush(testUser);

        // 테스트용 쿠폰 생성
        testCoupon = CouponEntity.builder()
                .name("테스트 쿠폰")
                .discountAmount(new BigDecimal("1000.00"))
                .maxIssuanceCount(100)
                .issuedCount(0)
                .status("ACTIVE")
                .build();
        testCoupon = couponJpaRepository.saveAndFlush(testCoupon);
    }

    @Test
    @DisplayName("쿠폰 발급 성공")
    void 쿠폰_발급_성공() {
        // given
        Long userId = testUser.getUserId();
        Long couponId = testCoupon.getId();

        // when
        IssueCouponUseCase.IssueCouponCommand command = new IssueCouponUseCase.IssueCouponCommand(userId, couponId);
        IssueCouponUseCase.IssueCouponResult result = issueCouponService.issueCoupon(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getUserCouponId()).isNotNull();
        assertThat(result.getCouponId()).isEqualTo(couponId);
        assertThat(result.getCouponName()).isEqualTo("테스트 쿠폰");
        assertThat(result.getDiscountAmount()).isEqualTo(new BigDecimal("1000"));
        assertThat(result.getStatus()).isEqualTo("AVAILABLE");

        // 사용자 쿠폰이 올바르게 생성되었는지 확인
        List<UserCouponEntity> userCoupons = userCouponJpaRepository.findByUserId(userId);
        assertThat(userCoupons).hasSize(1);
        assertThat(userCoupons.get(0).getCouponId()).isEqualTo(couponId);
        assertThat(userCoupons.get(0).getStatus()).isEqualTo("AVAILABLE");

        // 쿠폰 발급 수량이 증가했는지 확인
        CouponEntity updatedCoupon = couponJpaRepository.findById(couponId).orElse(null);
        assertThat(updatedCoupon).isNotNull();
        assertThat(updatedCoupon.getIssuedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 존재하지 않는 쿠폰")
    void 쿠폰_발급_실패_존재하지_않는_쿠폰() {
        // given
        Long userId = testUser.getUserId();
        Long nonExistentCouponId = 9999L;

        // when
        IssueCouponUseCase.IssueCouponCommand command = new IssueCouponUseCase.IssueCouponCommand(userId, nonExistentCouponId);
        IssueCouponUseCase.IssueCouponResult result = issueCouponService.issueCoupon(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("존재하지 않는 쿠폰입니다");
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 존재하지 않는 사용자")
    void 쿠폰_발급_실패_존재하지_않는_사용자() {
        // given
        Long nonExistentUserId = 9999L;
        Long couponId = testCoupon.getId();

        // when
        IssueCouponUseCase.IssueCouponCommand command = new IssueCouponUseCase.IssueCouponCommand(nonExistentUserId, couponId);
        IssueCouponUseCase.IssueCouponResult result = issueCouponService.issueCoupon(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("사용자 쿠폰 목록 조회 성공")
    void 사용자_쿠폰_목록_조회_성공() {
        // given
        Long userId = testUser.getUserId();
        Long couponId = testCoupon.getId();

        // 쿠폰 발급
        IssueCouponUseCase.IssueCouponCommand issueCommand = new IssueCouponUseCase.IssueCouponCommand(userId, couponId);
        issueCouponService.issueCoupon(issueCommand);

        // when
        GetUserCouponsUseCase.GetUserCouponsCommand command = new GetUserCouponsUseCase.GetUserCouponsCommand(userId);
        GetUserCouponsUseCase.GetUserCouponsResult result = getUserCouponsService.getUserCoupons(command);

        // then
        assertThat(result.getUserCoupons()).hasSize(1);
        GetUserCouponsUseCase.UserCouponInfo userCouponInfo = result.getUserCoupons().get(0);
        assertThat(userCouponInfo.getCouponId()).isEqualTo(couponId);
        assertThat(userCouponInfo.getCouponName()).isEqualTo("테스트 쿠폰");
        assertThat(userCouponInfo.getDiscountAmount()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(userCouponInfo.getStatus()).isEqualTo("AVAILABLE");
    }

    @Test
    @DisplayName("사용자 쿠폰 목록 조회 실패 - 존재하지 않는 사용자")
    void 사용자_쿠폰_목록_조회_실패_존재하지_않는_사용자() {
        // given
        Long nonExistentUserId = 9999L;

        // when
        GetUserCouponsUseCase.GetUserCouponsCommand command = new GetUserCouponsUseCase.GetUserCouponsCommand(nonExistentUserId);
        GetUserCouponsUseCase.GetUserCouponsResult result = getUserCouponsService.getUserCoupons(command);

        // then
        assertThat(result.getUserCoupons()).isEmpty();
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 쿠폰 소진")
    void 쿠폰_발급_실패_쿠폰_소진() {
        // given
        Long userId = testUser.getUserId();
        Long couponId = testCoupon.getId();

        // 쿠폰을 최대 발급 수량까지 발급
        for (int i = 0; i < 100; i++) {
            UserCouponEntity userCoupon = UserCouponEntity.builder()
                    .userId(userId)
                    .couponId(couponId)
                    .discountAmount(1000)
                    .status("AVAILABLE")
                    .build();
            userCouponJpaRepository.save(userCoupon);
        }

        // 쿠폰 발급 수량을 최대로 설정
        testCoupon.incrementIssuedCount();
        for (int i = 1; i < 100; i++) {
            testCoupon.incrementIssuedCount();
        }
        couponJpaRepository.saveAndFlush(testCoupon);

        // when
        IssueCouponUseCase.IssueCouponCommand command = new IssueCouponUseCase.IssueCouponCommand(userId, couponId);
        IssueCouponUseCase.IssueCouponResult result = issueCouponService.issueCoupon(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("쿠폰이 모두 소진되었습니다");
    }
} 