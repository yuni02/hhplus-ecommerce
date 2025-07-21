package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.domain.UserCoupon;
import kr.hhplus.be.server.coupon.domain.UserCouponRepository;
import kr.hhplus.be.server.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetUserCouponsUseCase 단위 테스트")
class GetUserCouponsUseCaseTest {

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private UserRepository userRepository;

    private GetUserCouponsUseCase getUserCouponsUseCase;

    @BeforeEach
    void setUp() {
        getUserCouponsUseCase = new GetUserCouponsUseCase(userCouponRepository, userRepository);
    }

    @Test
    @DisplayName("사용자 쿠폰 목록 조회")
    void getUserCoupons_ExistingUser_ReturnsCoupons() {
        // given
        Long userId = 1L;
        
        UserCoupon userCoupon1 = new UserCoupon(userId, 1L);
        userCoupon1.setId(1L);
        userCoupon1.setStatus(UserCoupon.UserCouponStatus.AVAILABLE);
        userCoupon1.setIssuedAt(LocalDateTime.now().minusDays(1));

        UserCoupon userCoupon2 = new UserCoupon(userId, 2L);
        userCoupon2.setId(2L);
        userCoupon2.setStatus(UserCoupon.UserCouponStatus.USED);
        userCoupon2.setIssuedAt(LocalDateTime.now().minusDays(2));
        userCoupon2.setUsedAt(LocalDateTime.now().minusDays(1));

        List<UserCoupon> userCoupons = Arrays.asList(userCoupon1, userCoupon2);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(userCouponRepository.findByUserId(userId)).thenReturn(userCoupons);

        // when
        GetUserCouponsUseCase.Output result = getUserCouponsUseCase.execute(
                new GetUserCouponsUseCase.Input(userId));

        // then
        assertThat(result.getUserCoupons()).hasSize(2);
        
        GetUserCouponsUseCase.UserCouponOutput firstCoupon = result.getUserCoupons().get(0);
        assertThat(firstCoupon.getUserCouponId()).isEqualTo(1L);
        assertThat(firstCoupon.getCouponId()).isEqualTo(1L);
        assertThat(firstCoupon.getStatus()).isEqualTo("AVAILABLE");
        
        GetUserCouponsUseCase.UserCouponOutput secondCoupon = result.getUserCoupons().get(1);
        assertThat(secondCoupon.getUserCouponId()).isEqualTo(2L);
        assertThat(secondCoupon.getCouponId()).isEqualTo(2L);
        assertThat(secondCoupon.getStatus()).isEqualTo("USED");
        
        verify(userRepository).existsById(userId);
        verify(userCouponRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 조회 시 빈 결과 반환")
    void getUserCoupons_NonExistentUser_ReturnsEmpty() {
        // given
        Long userId = 999L;
        when(userRepository.existsById(userId)).thenReturn(false);

        // when
        GetUserCouponsUseCase.Output result = getUserCouponsUseCase.execute(
                new GetUserCouponsUseCase.Input(userId));

        // then
        assertThat(result.getUserCoupons()).isEmpty();
        verify(userRepository).existsById(userId);
        verify(userCouponRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("사용자는 존재하지만 쿠폰이 없는 경우")
    void getUserCoupons_UserExistsButNoCoupons_ReturnsEmpty() {
        // given
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(true);
        when(userCouponRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        // when
        GetUserCouponsUseCase.Output result = getUserCouponsUseCase.execute(
                new GetUserCouponsUseCase.Input(userId));

        // then
        assertThat(result.getUserCoupons()).isEmpty();
        verify(userRepository).existsById(userId);
        verify(userCouponRepository).findByUserId(userId);
    }
} 