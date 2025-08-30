package kr.hhplus.be.server.unit.coupon.application;

import kr.hhplus.be.server.coupon.application.IssueCouponService;
import kr.hhplus.be.server.coupon.application.port.in.IssueCouponUseCase;
import kr.hhplus.be.server.coupon.application.port.out.LoadUserPort;
import kr.hhplus.be.server.coupon.application.port.out.LoadCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.SaveUserCouponPort;
import kr.hhplus.be.server.coupon.application.RedisCouponService;
import kr.hhplus.be.server.coupon.domain.UserCoupon;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueCouponServiceTest {

    @Mock
    private LoadUserPort loadUserPort;
    
    @Mock
    private LoadCouponPort loadCouponPort;
    
    @Mock
    private SaveUserCouponPort saveUserCouponPort;

    @Mock
    private RedisCouponService redisCouponService;

    private IssueCouponService issueCouponService;

    @BeforeEach
    void setUp() {
        issueCouponService = new IssueCouponService(loadUserPort, loadCouponPort, saveUserCouponPort, redisCouponService);   // 생성자 주입 방식으로 변경            
    }

    @Test
    @DisplayName("쿠폰 발급 성공")
    void issueCoupon_Success() {
        // given
        Long userId = 1L;   
        Long couponId = 1L;
        IssueCouponUseCase.IssueCouponCommand command = 
            new IssueCouponUseCase.IssueCouponCommand(userId, couponId);

        LoadCouponPort.CouponInfo couponInfo = new LoadCouponPort.CouponInfo(
            couponId, "신규 가입 쿠폰", "신규 회원 할인", 1000, 100, 50, "ACTIVE", 
            LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(7));

        UserCoupon savedUserCoupon = UserCoupon.builder()
            .id(1L)  // id 추가
            .userId(userId)
            .couponId(couponId)
            .discountAmount(1000)
            .issuedAt(LocalDateTime.now())
            .build();

        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadCouponPort.loadCouponByIdWithLock(couponId)).thenReturn(Optional.of(couponInfo));
        when(loadCouponPort.incrementIssuedCount(couponId)).thenReturn(true);
        when(saveUserCouponPort.saveUserCoupon(any(UserCoupon.class))).thenReturn(savedUserCoupon);
        
        // RedisCouponService Mock 설정
        when(redisCouponService.getCouponInfoFromCache(couponId)).thenReturn(Optional.empty()); // Redis에서 캐시 없음
        when(redisCouponService.checkAndIssueCoupon(eq(couponId), eq(userId), eq(100)))
            .thenReturn(RedisCouponService.CouponIssueResult.success());
        doNothing().when(redisCouponService).cacheCouponInfo(any(), any(), any(), any(), any(), any(), any(), any(), any());
        doNothing().when(redisCouponService).updateCouponIssuedCount(couponId, 51);
        
        // AOP 기반 @DistributedLock 사용으로 Redis 분산락 Mock 불필요

        // when
        IssueCouponUseCase.IssueCouponResult result = issueCouponService.issueCoupon(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCouponId()).isEqualTo(couponId);
        assertThat(result.getCouponName()).isEqualTo("신규 가입 쿠폰");
        assertThat(result.getDiscountAmount()).isEqualTo(1000);
        assertThat(result.getStatus()).isEqualTo("AVAILABLE");
        
        verify(loadUserPort).existsById(userId);
        verify(loadCouponPort).loadCouponByIdWithLock(couponId);
        verify(loadCouponPort).incrementIssuedCount(couponId);
        verify(saveUserCouponPort).saveUserCoupon(any(UserCoupon.class));
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 잘못된 사용자 ID")
    void issueCoupon_Failure_InvalidUserId() {
        // given
        Long userId = null;
        Long couponId = 1L;
        IssueCouponUseCase.IssueCouponCommand command = 
            new IssueCouponUseCase.IssueCouponCommand(userId, couponId);

        // when
        IssueCouponUseCase.IssueCouponResult result = issueCouponService.issueCoupon(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("잘못된 사용자 ID입니다.");
        
        verify(loadUserPort, never()).existsById(any());
        verify(loadCouponPort, never()).loadCouponByIdWithLock(any());
        verify(saveUserCouponPort, never()).saveUserCoupon(any());
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 사용자가 존재하지 않음")
    void issueCoupon_Failure_UserNotFound() {
        // given
        Long userId = 999L;
        Long couponId = 1L;
        IssueCouponUseCase.IssueCouponCommand command = 
            new IssueCouponUseCase.IssueCouponCommand(userId, couponId);

        when(loadUserPort.existsById(userId)).thenReturn(false);

        // when
        IssueCouponUseCase.IssueCouponResult result = issueCouponService.issueCoupon(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("사용자를 찾을 수 없습니다.");
        
        verify(loadUserPort).existsById(userId);
        verify(loadCouponPort, never()).loadCouponByIdWithLock(any());
        verify(saveUserCouponPort, never()).saveUserCoupon(any());
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 쿠폰이 존재하지 않음")
    void issueCoupon_Failure_CouponNotFound() {
        // given
        Long userId = 1L;
        Long couponId = 999L;
        IssueCouponUseCase.IssueCouponCommand command = 
            new IssueCouponUseCase.IssueCouponCommand(userId, couponId);

        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadCouponPort.loadCouponByIdWithLock(couponId)).thenReturn(Optional.empty());

        // when
        IssueCouponUseCase.IssueCouponResult result = issueCouponService.issueCoupon(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("존재하지 않는 쿠폰입니다.");
        
        verify(loadUserPort).existsById(userId);
        verify(loadCouponPort).loadCouponByIdWithLock(couponId);
        verify(loadCouponPort, never()).incrementIssuedCount(any());
        verify(saveUserCouponPort, never()).saveUserCoupon(any());
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 비활성 상태 쿠폰")
    void issueCoupon_Failure_InactiveCoupon() {
        // given
        Long userId = 1L;
        Long couponId = 1L;
        IssueCouponUseCase.IssueCouponCommand command = 
            new IssueCouponUseCase.IssueCouponCommand(userId, couponId);

        LoadCouponPort.CouponInfo couponInfo = new LoadCouponPort.CouponInfo(
            couponId, "만료된 쿠폰", "만료된 쿠폰", 1000, 100, 50, "INACTIVE", LocalDateTime.now(), LocalDateTime.now());

        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadCouponPort.loadCouponByIdWithLock(couponId)).thenReturn(Optional.of(couponInfo));

        // when
        IssueCouponUseCase.IssueCouponResult result = issueCouponService.issueCoupon(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("발급할 수 없는 쿠폰입니다.");
        
        verify(loadUserPort).existsById(userId);
        verify(loadCouponPort).loadCouponByIdWithLock(couponId);
        verify(loadCouponPort, never()).incrementIssuedCount(any());
        verify(saveUserCouponPort, never()).saveUserCoupon(any());
    }

    // @Test
    // @DisplayName("쿠폰 발급 실패 - 재고 소진")
    // void issueCoupon_Failure_OutOfStock() {
    //     // given
    //     Long userId = 1L;
    //     Long couponId = 1L;
    //     IssueCouponUseCase.IssueCouponCommand command = 
    //         new IssueCouponUseCase.IssueCouponCommand(userId, couponId);

    //     LoadCouponPort.CouponInfo couponInfo = new LoadCouponPort.CouponInfo(
    //         couponId, "인기 쿠폰", "인기 쿠폰", 1000, 100, 100, "ACTIVE"); // 이미 최대 발급

    //     when(loadUserPort.existsById(userId)).thenReturn(true);
    //     when(loadCouponPort.loadCouponByIdWithLock(couponId)).thenReturn(Optional.of(couponInfo));
    //     when(loadCouponPort.incrementIssuedCount(couponId)).thenReturn(false);

    //     // when
    //     IssueCouponUseCase.IssueCouponResult result = issueCouponService.issueCoupon(command);

    //     // then
    //     assertThat(result.isSuccess()).isFalse();
    //     assertThat(result.getErrorMessage()).isEqualTo("쿠폰이 모두 소진되었습니다. 선착순 발급에 실패했습니다.");
        
    //     verify(loadUserPort).existsById(userId);
    //     verify(loadCouponPort).loadCouponByIdWithLock(couponId);
    //     verify(loadCouponPort).incrementIssuedCount(couponId);
    //     verify(saveUserCouponPort, never()).saveUserCoupon(any());
    // }
} 