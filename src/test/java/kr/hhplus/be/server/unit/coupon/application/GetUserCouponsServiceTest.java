package kr.hhplus.be.server.unit.coupon.application;

import kr.hhplus.be.server.coupon.application.GetUserCouponsService;
import kr.hhplus.be.server.coupon.application.port.in.GetUserCouponsUseCase;
import kr.hhplus.be.server.coupon.application.port.out.LoadUserPort;
import kr.hhplus.be.server.coupon.application.port.out.LoadUserCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.LoadCouponPort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetUserCouponsServiceTest {

    @Mock
    private LoadUserPort loadUserPort;
    
    @Mock
    private LoadUserCouponPort loadUserCouponPort;
    
    @Mock
    private LoadCouponPort loadCouponPort;

    private GetUserCouponsService getUserCouponsService;

    @BeforeEach
    void setUp() {
        getUserCouponsService = new GetUserCouponsService(loadUserPort, loadUserCouponPort, loadCouponPort);
    }

    @Test
    @DisplayName("사용자 쿠폰 조회 성공")
    void getUserCoupons_Success() {
        // given
        Long userId = 1L;
        GetUserCouponsUseCase.GetUserCouponsCommand command = 
            new GetUserCouponsUseCase.GetUserCouponsCommand(userId);

        String issuedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        LoadUserCouponPort.UserCouponInfo userCouponInfo = new LoadUserCouponPort.UserCouponInfo(
            1L, userId, 1L, "AVAILABLE", issuedAt, null, null);
        
        LoadCouponPort.CouponInfo couponInfo = new LoadCouponPort.CouponInfo(
            1L, "신규 가입 쿠폰", "신규 회원 할인", 1000, 100, 50, "ACTIVE");

        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadUserCouponPort.loadUserCouponsByUserId(userId)).thenReturn(List.of(userCouponInfo));
        when(loadCouponPort.loadCouponById(1L)).thenReturn(Optional.of(couponInfo));

        // when
        GetUserCouponsUseCase.GetUserCouponsResult result = getUserCouponsService.getUserCoupons(command);

        // then
        assertThat(result.getUserCoupons()).hasSize(1);
        GetUserCouponsUseCase.UserCouponInfo resultCoupon = result.getUserCoupons().get(0);
        assertThat(resultCoupon.getUserCouponId()).isEqualTo(1L);
        assertThat(resultCoupon.getCouponId()).isEqualTo(1L);
        assertThat(resultCoupon.getCouponName()).isEqualTo("신규 가입 쿠폰");
        assertThat(resultCoupon.getDiscountAmount()).isEqualTo(1000);
        assertThat(resultCoupon.getStatus()).isEqualTo("AVAILABLE");
        
        verify(loadUserPort).existsById(userId);
        verify(loadUserCouponPort).loadUserCouponsByUserId(userId);
        verify(loadCouponPort).loadCouponById(1L);
    }

    @Test
    @DisplayName("사용자 쿠폰 조회 성공 - 빈 결과")
    void getUserCoupons_Success_EmptyResult() {
        // given
        Long userId = 1L;
        GetUserCouponsUseCase.GetUserCouponsCommand command = 
            new GetUserCouponsUseCase.GetUserCouponsCommand(userId);

        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadUserCouponPort.loadUserCouponsByUserId(userId)).thenReturn(List.of());

        // when
        GetUserCouponsUseCase.GetUserCouponsResult result = getUserCouponsService.getUserCoupons(command);

        // then
        assertThat(result.getUserCoupons()).isEmpty();
        
        verify(loadUserPort).existsById(userId);
        verify(loadUserCouponPort).loadUserCouponsByUserId(userId);
        verify(loadCouponPort, never()).loadCouponById(any());
    }

    @Test
    @DisplayName("사용자 쿠폰 조회 실패 - 잘못된 사용자 ID")
    void getUserCoupons_Failure_InvalidUserId() {
        // given
        Long userId = null;
        GetUserCouponsUseCase.GetUserCouponsCommand command = 
            new GetUserCouponsUseCase.GetUserCouponsCommand(userId);

        // when
        GetUserCouponsUseCase.GetUserCouponsResult result = getUserCouponsService.getUserCoupons(command);

        // then
        assertThat(result.getUserCoupons()).isEmpty();
        
        verify(loadUserPort, never()).existsById(any());
        verify(loadUserCouponPort, never()).loadUserCouponsByUserId(any());
    }

    @Test
    @DisplayName("사용자 쿠폰 조회 실패 - 사용자가 존재하지 않음")
    void getUserCoupons_Failure_UserNotFound() {
        // given
        Long userId = 999L;
        GetUserCouponsUseCase.GetUserCouponsCommand command = 
            new GetUserCouponsUseCase.GetUserCouponsCommand(userId);

        when(loadUserPort.existsById(userId)).thenReturn(false);

        // when
        GetUserCouponsUseCase.GetUserCouponsResult result = getUserCouponsService.getUserCoupons(command);

        // then
        assertThat(result.getUserCoupons()).isEmpty();
        
        verify(loadUserPort).existsById(userId);
        verify(loadUserCouponPort, never()).loadUserCouponsByUserId(any());
    }

    @Test
    @DisplayName("사용자 쿠폰 조회 성공 - 쿠폰 정보가 없는 경우")
    void getUserCoupons_Success_WithMissingCouponInfo() {
        // given
        Long userId = 1L;
        GetUserCouponsUseCase.GetUserCouponsCommand command = 
            new GetUserCouponsUseCase.GetUserCouponsCommand(userId);

        String issuedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        LoadUserCouponPort.UserCouponInfo userCouponInfo = new LoadUserCouponPort.UserCouponInfo(
            1L, userId, 999L, "AVAILABLE", issuedAt, null, null);

        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadUserCouponPort.loadUserCouponsByUserId(userId)).thenReturn(List.of(userCouponInfo));
        when(loadCouponPort.loadCouponById(999L)).thenReturn(Optional.empty());

        // when
        GetUserCouponsUseCase.GetUserCouponsResult result = getUserCouponsService.getUserCoupons(command);

        // then
        assertThat(result.getUserCoupons()).hasSize(1);
        GetUserCouponsUseCase.UserCouponInfo resultCoupon = result.getUserCoupons().get(0);
        assertThat(resultCoupon.getUserCouponId()).isEqualTo(1L);
        assertThat(resultCoupon.getCouponId()).isEqualTo(999L);
        assertThat(resultCoupon.getCouponName()).isEqualTo("알 수 없는 쿠폰");
        assertThat(resultCoupon.getDiscountAmount()).isEqualTo(0);
        assertThat(resultCoupon.getStatus()).isEqualTo("AVAILABLE");
        
        verify(loadUserPort).existsById(userId);
        verify(loadUserCouponPort).loadUserCouponsByUserId(userId);
        verify(loadCouponPort).loadCouponById(999L);
    }

    @Test
    @DisplayName("사용자 쿠폰 조회 성공 - 여러 쿠폰")
    void getUserCoupons_Success_MultipleCoupons() {
        // given
        Long userId = 1L;
        GetUserCouponsUseCase.GetUserCouponsCommand command = 
            new GetUserCouponsUseCase.GetUserCouponsCommand(userId);

        String issuedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        LoadUserCouponPort.UserCouponInfo userCoupon1 = new LoadUserCouponPort.UserCouponInfo(
            1L, userId, 1L, "AVAILABLE", issuedAt, null, null);
        LoadUserCouponPort.UserCouponInfo userCoupon2 = new LoadUserCouponPort.UserCouponInfo(
            2L, userId, 2L, "USED", issuedAt, issuedAt, 1L);
        
        LoadCouponPort.CouponInfo coupon1 = new LoadCouponPort.CouponInfo(
            1L, "신규 가입 쿠폰", "신규 회원 할인", 1000, 100, 50, "ACTIVE");
        LoadCouponPort.CouponInfo coupon2 = new LoadCouponPort.CouponInfo(
            2L, "생일 축하 쿠폰", "생일 축하 할인", 2000, 50, 30, "ACTIVE");

        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadUserCouponPort.loadUserCouponsByUserId(userId)).thenReturn(List.of(userCoupon1, userCoupon2));
        when(loadCouponPort.loadCouponById(1L)).thenReturn(Optional.of(coupon1));
        when(loadCouponPort.loadCouponById(2L)).thenReturn(Optional.of(coupon2));

        // when
        GetUserCouponsUseCase.GetUserCouponsResult result = getUserCouponsService.getUserCoupons(command);

        // then
        assertThat(result.getUserCoupons()).hasSize(2);
        
        GetUserCouponsUseCase.UserCouponInfo resultCoupon1 = result.getUserCoupons().get(0);
        assertThat(resultCoupon1.getCouponName()).isEqualTo("신규 가입 쿠폰");
        assertThat(resultCoupon1.getDiscountAmount()).isEqualTo(1000);
        assertThat(resultCoupon1.getStatus()).isEqualTo("AVAILABLE");
        
        GetUserCouponsUseCase.UserCouponInfo resultCoupon2 = result.getUserCoupons().get(1);
        assertThat(resultCoupon2.getCouponName()).isEqualTo("생일 축하 쿠폰");
        assertThat(resultCoupon2.getDiscountAmount()).isEqualTo(2000);
        assertThat(resultCoupon2.getStatus()).isEqualTo("USED");
        
        verify(loadUserPort).existsById(userId);
        verify(loadUserCouponPort).loadUserCouponsByUserId(userId);
        verify(loadCouponPort).loadCouponById(1L);
        verify(loadCouponPort).loadCouponById(2L);
    }
} 