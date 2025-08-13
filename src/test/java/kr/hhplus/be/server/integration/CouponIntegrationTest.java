package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.TestcontainersConfiguration;
import kr.hhplus.be.server.coupon.application.IssueCouponService;
import kr.hhplus.be.server.coupon.application.port.in.IssueCouponUseCase;
import kr.hhplus.be.server.coupon.application.CachedCouponService;
import kr.hhplus.be.server.coupon.application.port.out.LoadUserCouponPort;
import kr.hhplus.be.server.coupon.infrastructure.persistence.entity.CouponEntity;
import kr.hhplus.be.server.coupon.infrastructure.persistence.entity.UserCouponEntity;
import kr.hhplus.be.server.coupon.infrastructure.persistence.repository.CouponJpaRepository;
import kr.hhplus.be.server.coupon.infrastructure.persistence.repository.UserCouponJpaRepository;
import kr.hhplus.be.server.user.infrastructure.persistence.entity.UserEntity;
import kr.hhplus.be.server.user.infrastructure.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Coupon 도메인 통합테스트")
class CouponIntegrationTest {

    @Autowired
    private IssueCouponService issueCouponService;
    
    @Autowired
    private CachedCouponService cachedCouponService;

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
                .name("testuser")
                .email("test@example.com")
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
        Long userId = testUser.getUserId() != null ? testUser.getUserId() : testUser.getId();
        Long couponId = testCoupon.getId();

        // when
        IssueCouponUseCase.IssueCouponCommand command = new IssueCouponUseCase.IssueCouponCommand(userId, couponId);
        IssueCouponUseCase.IssueCouponResult result = issueCouponService.issueCoupon(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getCouponId()).isEqualTo(couponId);
        assertThat(result.getCouponName()).isEqualTo("테스트 쿠폰");
        assertThat(result.getDiscountAmount()).isEqualTo(1000);
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
        Long userId = testUser.getUserId() != null ? testUser.getUserId() : testUser.getId();
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
    @DisplayName("전체 사용자 쿠폰 목록 조회 성공 - CachedCouponService")
    void 전체_사용자_쿠폰_목록_조회_성공_Cached() {
        // given
        Long userId = testUser.getUserId() != null ? testUser.getUserId() : testUser.getId();
        Long couponId = testCoupon.getId();

        // 쿠폰 발급
        IssueCouponUseCase.IssueCouponCommand issueCommand = new IssueCouponUseCase.IssueCouponCommand(userId, couponId);
        issueCouponService.issueCoupon(issueCommand);

        // when
        List<LoadUserCouponPort.UserCouponInfo> result = cachedCouponService.getAllUserCoupons(userId);

        // then
        assertThat(result).hasSize(1);
        LoadUserCouponPort.UserCouponInfo userCouponInfo = result.get(0);
        assertThat(userCouponInfo.getCouponId()).isEqualTo(couponId);
        assertThat(userCouponInfo.getStatus()).isEqualTo("AVAILABLE");
    }
    
    @Test
    @DisplayName("사용 가능한 쿠폰 목록 조회 성공 - CachedCouponService")
    void 사용가능한_쿠폰_목록_조회_성공_Cached() {
        // given
        Long userId = testUser.getUserId() != null ? testUser.getUserId() : testUser.getId();
        Long couponId = testCoupon.getId();

        // 쿠폰 발급
        IssueCouponUseCase.IssueCouponCommand issueCommand = new IssueCouponUseCase.IssueCouponCommand(userId, couponId);
        issueCouponService.issueCoupon(issueCommand);

        // when
        List<LoadUserCouponPort.UserCouponInfo> result = cachedCouponService.getAvailableUserCoupons(userId);

        // then
        assertThat(result).hasSize(1);
        LoadUserCouponPort.UserCouponInfo userCouponInfo = result.get(0);
        assertThat(userCouponInfo.getCouponId()).isEqualTo(couponId);
        assertThat(userCouponInfo.getStatus()).isEqualTo("AVAILABLE");
    }

    @Test
    @DisplayName("사용자 쿠폰 목록 조회 실패 - 존재하지 않는 사용자")
    void 사용자_쿠폰_목록_조회_실패_존재하지_않는_사용자() {
        // given
        Long nonExistentUserId = 9999L;

        // when - 캐시된 서비스로 조회
        List<LoadUserCouponPort.UserCouponInfo> result = cachedCouponService.getAvailableUserCoupons(nonExistentUserId);

        // then
        assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("사용 가능한 쿠폰 캐시 무효화 테스트")
    void 사용가능한_쿠폰_캐시_무효화_테스트() {
        // given
        Long userId = testUser.getUserId() != null ? testUser.getUserId() : testUser.getId();
        Long couponId = testCoupon.getId();

        // 쿠폰 발급 후 캐시 생성 (AOP에 의해 자동 처리)
        IssueCouponUseCase.IssueCouponCommand issueCommand = new IssueCouponUseCase.IssueCouponCommand(userId, couponId);
        issueCouponService.issueCoupon(issueCommand);
        
        // 첫 번째 조회로 캐시 생성 (AOP @Cacheable에 의해 자동 캐시)
        List<LoadUserCouponPort.UserCouponInfo> firstResult = cachedCouponService.getAvailableUserCoupons(userId);
        assertThat(firstResult).hasSize(1);

        // when - 캐시 무효화 (AOP @CacheEvict에 의해 자동 처리)
        cachedCouponService.evictAvailableUserCouponsCache(userId);
        cachedCouponService.evictAllUserCouponsCache(userId);
        
        // then - 캐시 무효화 후에도 데이터를 올바르게 조회
        List<LoadUserCouponPort.UserCouponInfo> secondResult = cachedCouponService.getAvailableUserCoupons(userId);
        assertThat(secondResult).hasSize(1);
        assertThat(secondResult.get(0).getStatus()).isEqualTo("AVAILABLE");
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 쿠폰 소진")
    void 쿠폰_발급_실패_쿠폰_소진() {
        // given
        Long userId = testUser.getUserId() != null ? testUser.getUserId() : testUser.getId();
        Long couponId = testCoupon.getId();

        // 쿠폰을 최대 발급 수량까지 발급
        for (int i = 0; i < 100; i++) {
            UserCouponEntity userCoupon = UserCouponEntity.builder()
                    .user(testUser)  // user 관계 설정
                    .coupon(testCoupon)  // coupon 관계 설정
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
        assertThat(result.getErrorMessage()).contains("발급할 수 없는 쿠폰입니다.");
    }
} 