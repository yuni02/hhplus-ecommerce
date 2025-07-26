package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.application.port.in.UseCouponUseCase;
import kr.hhplus.be.server.coupon.application.port.out.LoadUserCouponPort;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UseCouponServiceTest {

    @Mock
    private LoadUserCouponPort loadUserCouponPort;
    
    @Mock
    private UpdateUserCouponPort updateUserCouponPort;

    private UseCouponService useCouponService;

    @BeforeEach
    void setUp() {
        useCouponService = new UseCouponService(loadUserCouponPort, updateUserCouponPort);
    }

    @Test
    @DisplayName("쿠폰 사용 성공")
    void useCoupon_Success() {
        // given
        Long userId = 1L;
        Long userCouponId = 1L;
        BigDecimal orderAmount = BigDecimal.valueOf(10000);
        
        UseCouponUseCase.UseCouponCommand command = 
            new UseCouponUseCase.UseCouponCommand(userId, userCouponId, orderAmount);

        UserCoupon userCoupon = new UserCoupon(userId, 1L, 2000);
        userCoupon.setId(userCouponId);
        userCoupon.setStatus(UserCoupon.UserCouponStatus.AVAILABLE);

        when(loadUserCouponPort.loadUserCoupon(userCouponId)).thenReturn(Optional.of(userCoupon));

        // when
        UseCouponUseCase.UseCouponResult result = useCouponService.useCoupon(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDiscountedAmount()).isEqualTo(BigDecimal.valueOf(8000)); // 10000 - 2000
        assertThat(result.getDiscountAmount()).isEqualTo(2000);
        
        verify(loadUserCouponPort).loadUserCoupon(userCouponId);
        verify(updateUserCouponPort).updateUserCoupon(any(UserCoupon.class));
        
        // 쿠폰이 사용됨 상태로 변경되었는지 확인
        assertThat(userCoupon.getStatus()).isEqualTo(UserCoupon.UserCouponStatus.USED);
        assertThat(userCoupon.getUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("쿠폰 사용 성공 - 할인 후 금액이 0원이 되는 경우")
    void useCoupon_Success_DiscountedAmountZero() {
        // given
        Long userId = 1L;
        Long userCouponId = 1L;
        BigDecimal orderAmount = BigDecimal.valueOf(1000);
        
        UseCouponUseCase.UseCouponCommand command = 
            new UseCouponUseCase.UseCouponCommand(userId, userCouponId, orderAmount);

        UserCoupon userCoupon = new UserCoupon(userId, 1L, 2000);
        userCoupon.setId(userCouponId);
        userCoupon.setStatus(UserCoupon.UserCouponStatus.AVAILABLE);

        when(loadUserCouponPort.loadUserCoupon(userCouponId)).thenReturn(Optional.of(userCoupon));

        // when
        UseCouponUseCase.UseCouponResult result = useCouponService.useCoupon(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDiscountedAmount()).isEqualTo(BigDecimal.ZERO); // 음수가 되지 않도록 0으로 설정
        assertThat(result.getDiscountAmount()).isEqualTo(2000);
        
        verify(loadUserCouponPort).loadUserCoupon(userCouponId);
        verify(updateUserCouponPort).updateUserCoupon(any(UserCoupon.class));
    }

    @Test
    @DisplayName("쿠폰 사용 실패 - 쿠폰을 찾을 수 없음")
    void useCoupon_Failure_CouponNotFound() {
        // given
        Long userId = 1L;
        Long userCouponId = 999L;
        BigDecimal orderAmount = BigDecimal.valueOf(10000);
        
        UseCouponUseCase.UseCouponCommand command = 
            new UseCouponUseCase.UseCouponCommand(userId, userCouponId, orderAmount);

        when(loadUserCouponPort.loadUserCoupon(userCouponId)).thenReturn(Optional.empty());

        // when
        UseCouponUseCase.UseCouponResult result = useCouponService.useCoupon(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("쿠폰을 찾을 수 없습니다.");
        
        verify(loadUserCouponPort).loadUserCoupon(userCouponId);
        verify(updateUserCouponPort, never()).updateUserCoupon(any());
    }

    @Test
    @DisplayName("쿠폰 사용 실패 - 쿠폰 소유자가 아님")
    void useCoupon_Failure_NotCouponOwner() {
        // given
        Long userId = 1L;
        Long userCouponId = 1L;
        BigDecimal orderAmount = BigDecimal.valueOf(10000);
        
        UseCouponUseCase.UseCouponCommand command = 
            new UseCouponUseCase.UseCouponCommand(userId, userCouponId, orderAmount);

        UserCoupon userCoupon = new UserCoupon(2L, 1L, 2000); // 다른 사용자의 쿠폰
        userCoupon.setId(userCouponId);
        userCoupon.setStatus(UserCoupon.UserCouponStatus.AVAILABLE);

        when(loadUserCouponPort.loadUserCoupon(userCouponId)).thenReturn(Optional.of(userCoupon));

        // when
        UseCouponUseCase.UseCouponResult result = useCouponService.useCoupon(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("해당 쿠폰의 소유자가 아닙니다.");
        
        verify(loadUserCouponPort).loadUserCoupon(userCouponId);
        verify(updateUserCouponPort, never()).updateUserCoupon(any());
    }

    @Test
    @DisplayName("쿠폰 사용 실패 - 이미 사용된 쿠폰")
    void useCoupon_Failure_AlreadyUsedCoupon() {
        // given
        Long userId = 1L;
        Long userCouponId = 1L;
        BigDecimal orderAmount = BigDecimal.valueOf(10000);
        
        UseCouponUseCase.UseCouponCommand command = 
            new UseCouponUseCase.UseCouponCommand(userId, userCouponId, orderAmount);

        UserCoupon userCoupon = new UserCoupon(userId, 1L, 2000);
        userCoupon.setId(userCouponId);
        userCoupon.setStatus(UserCoupon.UserCouponStatus.USED); // 이미 사용된 상태
        userCoupon.setUsedAt(LocalDateTime.now());

        when(loadUserCouponPort.loadUserCoupon(userCouponId)).thenReturn(Optional.of(userCoupon));

        // when
        UseCouponUseCase.UseCouponResult result = useCouponService.useCoupon(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("사용할 수 없는 쿠폰입니다.");
        
        verify(loadUserCouponPort).loadUserCoupon(userCouponId);
        verify(updateUserCouponPort, never()).updateUserCoupon(any());
    }

    @Test
    @DisplayName("쿠폰 사용 실패 - 만료된 쿠폰")
    void useCoupon_Failure_ExpiredCoupon() {
        // given
        Long userId = 1L;
        Long userCouponId = 1L;
        BigDecimal orderAmount = BigDecimal.valueOf(10000);
        
        UseCouponUseCase.UseCouponCommand command = 
            new UseCouponUseCase.UseCouponCommand(userId, userCouponId, orderAmount);

        UserCoupon userCoupon = new UserCoupon(userId, 1L, 2000);
        userCoupon.setId(userCouponId);
        userCoupon.setStatus(UserCoupon.UserCouponStatus.EXPIRED); // 만료된 상태

        when(loadUserCouponPort.loadUserCoupon(userCouponId)).thenReturn(Optional.of(userCoupon));

        // when
        UseCouponUseCase.UseCouponResult result = useCouponService.useCoupon(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("사용할 수 없는 쿠폰입니다.");
        
        verify(loadUserCouponPort).loadUserCoupon(userCouponId);
        verify(updateUserCouponPort, never()).updateUserCoupon(any());
    }

    @Test
    @DisplayName("쿠폰 사용 실패 - 예외 발생")
    void useCoupon_Failure_Exception() {
        // given
        Long userId = 1L;
        Long userCouponId = 1L;
        BigDecimal orderAmount = BigDecimal.valueOf(10000);
        
        UseCouponUseCase.UseCouponCommand command = 
            new UseCouponUseCase.UseCouponCommand(userId, userCouponId, orderAmount);

        when(loadUserCouponPort.loadUserCoupon(userCouponId))
            .thenThrow(new RuntimeException("데이터베이스 오류"));

        // when
        UseCouponUseCase.UseCouponResult result = useCouponService.useCoupon(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("쿠폰 사용 중 오류가 발생했습니다");
        
        verify(loadUserCouponPort).loadUserCoupon(userCouponId);
        verify(updateUserCouponPort, never()).updateUserCoupon(any());
    }
} 