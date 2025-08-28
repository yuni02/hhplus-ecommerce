package kr.hhplus.be.server.unit.coupon.application;

import kr.hhplus.be.server.coupon.application.CachedCouponService;
import kr.hhplus.be.server.coupon.application.port.out.LoadUserCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.LoadCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.UpdateUserCouponPort;

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
class CachedCouponServiceTest {

    @Mock
    private LoadUserCouponPort loadUserCouponPort;
    
    @Mock
    private LoadCouponPort loadCouponPort;
    
    @Mock
    private UpdateUserCouponPort updateUserCouponPort;

    private CachedCouponService cachedCouponService;

    @BeforeEach
    void setUp() {
        cachedCouponService = new CachedCouponService(loadCouponPort, loadUserCouponPort, updateUserCouponPort);
    }

    @Test
    @DisplayName("전체 사용자 쿠폰 조회 성공")
    void getAllUserCoupons_Success() {
        // given
        Long userId = 1L;
        String issuedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        LoadUserCouponPort.UserCouponInfo availableCoupon = new LoadUserCouponPort.UserCouponInfo(
            1L, userId, 1L, "AVAILABLE", issuedAt, null, null);
        LoadUserCouponPort.UserCouponInfo usedCoupon = new LoadUserCouponPort.UserCouponInfo(
            2L, userId, 2L, "USED", issuedAt, issuedAt, 1L);

        when(loadUserCouponPort.loadUserCouponsByUserId(userId))
            .thenReturn(List.of(availableCoupon, usedCoupon));

        // when
        List<LoadUserCouponPort.UserCouponInfo> result = cachedCouponService.getAllUserCoupons(userId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStatus()).isEqualTo("AVAILABLE");
        assertThat(result.get(1).getStatus()).isEqualTo("USED");
        
        verify(loadUserCouponPort).loadUserCouponsByUserId(userId);
    }



} 