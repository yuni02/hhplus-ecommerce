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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
        couponFacade = new CouponFacade(loadUserPort, loadCouponPort, saveCouponPort,
                loadUserCouponPort, saveUserCouponPort, updateUserCouponPort);
    }

    @Test
    @DisplayName("쿠폰 발급 성공")
    void issueCoupon_Success() {
        // given
        Long userId = 1L;
        Long couponId = 1L;
        Integer discountAmount = 1000;
        
        IssueCouponUseCase.IssueCouponCommand command = 
            new IssueCouponUseCase.IssueCouponCommand(userId, couponId);
        
        LoadCouponPort.CouponInfo couponInfo = new LoadCouponPort.CouponInfo(
                couponId, "테스트 쿠폰", "테스트 쿠폰 설명", discountAmount, 100, 50, "ACTIVE");
        
        UserCoupon userCoupon = new UserCoupon(userId, couponId, discountAmount);
        userCoupon.setId(1L);
        userCoupon.setStatus(UserCoupon.UserCouponStatus.AVAILABLE);
        
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadCouponPort.loadCouponById(couponId)).thenReturn(Optional.of(couponInfo));
        when(saveCouponPort.saveCoupon(any(SaveCouponPort.CouponInfo.class))).thenReturn(
            new SaveCouponPort.CouponInfo(couponId, "테스트 쿠폰", "테스트 쿠폰 설명", 
                discountAmount, 100, 51, "ACTIVE"));
        when(saveUserCouponPort.saveUserCoupon(any(UserCoupon.class))).thenReturn(userCoupon);

        // when
        IssueCouponUseCase.IssueCouponResult result = couponFacade.issueCoupon(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getUserCouponId()).isEqualTo(1L);
        assertThat(result.getCouponId()).isEqualTo(couponId);
        assertThat(result.getCouponName()).isEqualTo("테스트 쿠폰");
        assertThat(result.getDiscountAmount()).isEqualTo(discountAmount);
        assertThat(result.getStatus()).isEqualTo("AVAILABLE");
        assertThat(result.getErrorMessage()).isNull();
        
        verify(loadUserPort).existsById(userId);
        verify(loadCouponPort).loadCouponById(couponId);
        verify(saveCouponPort).saveCoupon(any(SaveCouponPort.CouponInfo.class));
        verify(saveUserCouponPort).saveUserCoupon(any(UserCoupon.class));
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 사용자가 존재하지 않는 경우")
    void issueCoupon_Failure_UserNotFound() {
        // given
        Long userId = 999L;
        Long couponId = 1L;
        
        IssueCouponUseCase.IssueCouponCommand command = 
            new IssueCouponUseCase.IssueCouponCommand(userId, couponId);
        
        when(loadUserPort.existsById(userId)).thenReturn(false);

        // when
        IssueCouponUseCase.IssueCouponResult result = couponFacade.issueCoupon(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("사용자를 찾을 수 없습니다.");
        
        verify(loadUserPort).existsById(userId);
        verify(loadCouponPort, never()).loadCouponById(any());
        verify(saveCouponPort, never()).saveCoupon(any());
        verify(saveUserCouponPort, never()).saveUserCoupon(any());
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 쿠폰이 존재하지 않는 경우")
    void issueCoupon_Failure_CouponNotFound() {
        // given
        Long userId = 1L;
        Long couponId = 999L;
        
        IssueCouponUseCase.IssueCouponCommand command = 
            new IssueCouponUseCase.IssueCouponCommand(userId, couponId);
        
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadCouponPort.loadCouponById(couponId)).thenReturn(Optional.empty());

        // when
        IssueCouponUseCase.IssueCouponResult result = couponFacade.issueCoupon(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("존재하지 않는 쿠폰입니다.");
        
        verify(loadUserPort).existsById(userId);
        verify(loadCouponPort).loadCouponById(couponId);
        verify(saveCouponPort, never()).saveCoupon(any());
        verify(saveUserCouponPort, never()).saveUserCoupon(any());
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 발급할 수 없는 쿠폰인 경우")
    void issueCoupon_Failure_CannotIssueCoupon() {
        // given
        Long userId = 1L;
        Long couponId = 1L;
        
        IssueCouponUseCase.IssueCouponCommand command = 
            new IssueCouponUseCase.IssueCouponCommand(userId, couponId);
        
        LoadCouponPort.CouponInfo couponInfo = new LoadCouponPort.CouponInfo(
                couponId, "테스트 쿠폰", "테스트 쿠폰 설명", 1000, 100, 100, "SOLD_OUT");
        
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadCouponPort.loadCouponById(couponId)).thenReturn(Optional.of(couponInfo));

        // when
        IssueCouponUseCase.IssueCouponResult result = couponFacade.issueCoupon(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("발급할 수 없는 쿠폰입니다.");
        
        verify(loadUserPort).existsById(userId);
        verify(loadCouponPort).loadCouponById(couponId);
        verify(saveCouponPort, never()).saveCoupon(any());
        verify(saveUserCouponPort, never()).saveUserCoupon(any());
    }

    @Test
    @DisplayName("사용자 쿠폰 조회 성공")
    void getUserCoupons_Success() {
        // given
        Long userId = 1L;
        
        GetUserCouponsUseCase.GetUserCouponsCommand command = 
            new GetUserCouponsUseCase.GetUserCouponsCommand(userId);
        
        LoadUserCouponPort.UserCouponInfo userCouponInfo = new LoadUserCouponPort.UserCouponInfo(
                1L, userId, 1L, "AVAILABLE", "2024-01-01T10:00:00", null, null);
        
        LoadCouponPort.CouponInfo couponInfo = new LoadCouponPort.CouponInfo(
                1L, "테스트 쿠폰", "테스트 쿠폰 설명", 1000, 100, 50, "ACTIVE");
        
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadUserCouponPort.loadUserCouponsByUserId(userId)).thenReturn(List.of(userCouponInfo));
        when(loadCouponPort.loadCouponById(1L)).thenReturn(Optional.of(couponInfo));

        // when
        GetUserCouponsUseCase.GetUserCouponsResult result = couponFacade.getUserCoupons(command);

        // then
        assertThat(result.getUserCoupons()).hasSize(1);
        assertThat(result.getUserCoupons().get(0).getUserCouponId()).isEqualTo(1L);
        assertThat(result.getUserCoupons().get(0).getCouponId()).isEqualTo(1L);
        assertThat(result.getUserCoupons().get(0).getCouponName()).isEqualTo("테스트 쿠폰");
        assertThat(result.getUserCoupons().get(0).getDiscountAmount()).isEqualTo(1000);
        assertThat(result.getUserCoupons().get(0).getStatus()).isEqualTo("AVAILABLE");
        
        verify(loadUserPort).existsById(userId);
        verify(loadUserCouponPort).loadUserCouponsByUserId(userId);
        verify(loadCouponPort).loadCouponById(1L);
    }

    @Test
    @DisplayName("사용자 쿠폰 조회 실패 - 사용자가 존재하지 않는 경우")
    void getUserCoupons_Failure_UserNotFound() {
        // given
        Long userId = 999L;
        
        GetUserCouponsUseCase.GetUserCouponsCommand command = 
            new GetUserCouponsUseCase.GetUserCouponsCommand(userId);
        
        when(loadUserPort.existsById(userId)).thenReturn(false);

        // when
        GetUserCouponsUseCase.GetUserCouponsResult result = couponFacade.getUserCoupons(command);

        // then
        assertThat(result.getUserCoupons()).isEmpty();
        
        verify(loadUserPort).existsById(userId);
        verify(loadUserCouponPort, never()).loadUserCouponsByUserId(any());
    }

    @Test
    @DisplayName("쿠폰 사용 성공")
    void useCoupon_Success() {
        // given
        Long userId = 1L;
        Long userCouponId = 1L;
        BigDecimal orderAmount = new BigDecimal("5000");
        Integer discountAmount = 1000;
        
        UseCouponUseCase.UseCouponCommand command = 
            new UseCouponUseCase.UseCouponCommand(userId, userCouponId, orderAmount);
        
        UserCoupon userCoupon = new UserCoupon(userId, 1L, discountAmount);
        userCoupon.setId(userCouponId);
        userCoupon.setStatus(UserCoupon.UserCouponStatus.AVAILABLE);
        
        when(loadUserCouponPort.loadUserCoupon(userCouponId)).thenReturn(Optional.of(userCoupon));
        doNothing().when(updateUserCouponPort).updateUserCoupon(any(UserCoupon.class));

        // when
        UseCouponUseCase.UseCouponResult result = couponFacade.useCoupon(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDiscountedAmount()).isEqualTo(new BigDecimal("4000"));
        assertThat(result.getDiscountAmount()).isEqualTo(discountAmount);
        assertThat(result.getErrorMessage()).isNull();
        
        verify(loadUserCouponPort).loadUserCoupon(userCouponId);
        verify(updateUserCouponPort).updateUserCoupon(any(UserCoupon.class));
    }

    @Test
    @DisplayName("쿠폰 사용 실패 - 쿠폰을 찾을 수 없는 경우")
    void useCoupon_Failure_CouponNotFound() {
        // given
        Long userId = 1L;
        Long userCouponId = 999L;
        BigDecimal orderAmount = new BigDecimal("5000");
        
        UseCouponUseCase.UseCouponCommand command = 
            new UseCouponUseCase.UseCouponCommand(userId, userCouponId, orderAmount);
        
        when(loadUserCouponPort.loadUserCoupon(userCouponId)).thenReturn(Optional.empty());

        // when
        UseCouponUseCase.UseCouponResult result = couponFacade.useCoupon(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("쿠폰을 찾을 수 없습니다.");
        assertThat(result.getDiscountedAmount()).isNull();
        assertThat(result.getDiscountAmount()).isNull();
        
        verify(loadUserCouponPort).loadUserCoupon(userCouponId);
        verify(updateUserCouponPort, never()).updateUserCoupon(any());
    }

    @Test
    @DisplayName("쿠폰 사용 실패 - 쿠폰 소유자가 아닌 경우")
    void useCoupon_Failure_NotOwner() {
        // given
        Long userId = 1L;
        Long userCouponId = 1L;
        Long actualOwnerId = 2L;
        BigDecimal orderAmount = new BigDecimal("5000");
        
        UseCouponUseCase.UseCouponCommand command = 
            new UseCouponUseCase.UseCouponCommand(userId, userCouponId, orderAmount);
        
        UserCoupon userCoupon = new UserCoupon(actualOwnerId, 1L, 1000);
        userCoupon.setId(userCouponId);
        userCoupon.setStatus(UserCoupon.UserCouponStatus.AVAILABLE);
        
        when(loadUserCouponPort.loadUserCoupon(userCouponId)).thenReturn(Optional.of(userCoupon));

        // when
        UseCouponUseCase.UseCouponResult result = couponFacade.useCoupon(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("해당 쿠폰의 소유자가 아닙니다.");
        
        verify(loadUserCouponPort).loadUserCoupon(userCouponId);
        verify(updateUserCouponPort, never()).updateUserCoupon(any());
    }
} 