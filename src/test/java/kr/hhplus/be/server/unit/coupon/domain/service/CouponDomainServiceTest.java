package kr.hhplus.be.server.unit.coupon.domain.service;

import kr.hhplus.be.server.coupon.domain.service.CouponDomainService;
import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.order.domain.CouponRestorationCompletedEvent;
import kr.hhplus.be.server.order.domain.CouponRestorationRequestedEvent;
import kr.hhplus.be.server.order.domain.CouponUsageCompletedEvent;
import kr.hhplus.be.server.order.domain.CouponUsageRequestedEvent;
import kr.hhplus.be.server.shared.event.SynchronousEventProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponDomainServiceTest {

    @Mock
    private SynchronousEventProcessor synchronousEventProcessor;

    @Mock
    private CouponUsageCompletedEvent mockSuccessEvent;
    
    @Mock
    private CouponUsageCompletedEvent mockFailureEvent;
    
    @Mock
    private CouponRestorationCompletedEvent mockRestorationSuccessEvent;
    
    @Mock
    private CouponRestorationCompletedEvent mockRestorationFailureEvent;

    private CouponDomainService couponDomainService;

    @BeforeEach
    void setUp() {
        couponDomainService = new CouponDomainService(synchronousEventProcessor);
    }

    @Test
    @DisplayName("쿠폰 처리 성공 - 쿠폰 미사용")
    void processCouponDiscount_Success_NoCoupon() {
        // given
        Long userId = 1L;
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(), null); // 쿠폰 없음
        BigDecimal totalAmount = BigDecimal.valueOf(20000);

        // when
        CouponDomainService.CouponProcessResult result = 
            couponDomainService.processCouponDiscount(command, totalAmount);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDiscountedAmount()).isEqualTo(BigDecimal.valueOf(20000));
        assertThat(result.getDiscountAmount()).isEqualTo(0);
        assertThat(result.getErrorMessage()).isNull();
        
        // 쿠폰을 사용하지 않으므로 이벤트 처리되지 않음
        verify(synchronousEventProcessor, never()).publishAndWaitForResponse(any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("쿠폰 처리 성공 - 쿠폰 사용")
    void processCouponDiscount_Success_WithCoupon()  {
        // given
        Long userId = 1L;
        Long userCouponId = 1L;
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(), userCouponId);
        BigDecimal totalAmount = BigDecimal.valueOf(20000);

        when(mockSuccessEvent.isSuccess()).thenReturn(true);
        when(mockSuccessEvent.getDiscountedAmount()).thenReturn(BigDecimal.valueOf(18000));
        when(mockSuccessEvent.getDiscountAmount()).thenReturn(2000);

        when(synchronousEventProcessor.publishAndWaitForResponse(
            any(CouponUsageRequestedEvent.class), 
            any(String.class), 
            eq(CouponUsageCompletedEvent.class), 
            eq(5)))
            .thenReturn(mockSuccessEvent);

        // when
        CouponDomainService.CouponProcessResult result = 
            couponDomainService.processCouponDiscount(command, totalAmount);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDiscountedAmount()).isEqualTo(BigDecimal.valueOf(18000));
        assertThat(result.getDiscountAmount()).isEqualTo(2000);
        assertThat(result.getErrorMessage()).isNull();
        
        verify(synchronousEventProcessor).publishAndWaitForResponse(
            any(CouponUsageRequestedEvent.class), any(String.class), eq(CouponUsageCompletedEvent.class), eq(5));
    }

    @Test
    @DisplayName("쿠폰 처리 실패 - 쿠폰 사용 불가")
    void processCouponDiscount_Failure_CouponNotUsable() {
        // given
        Long userId = 1L;
        Long userCouponId = 1L;
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(), userCouponId);
        BigDecimal totalAmount = BigDecimal.valueOf(20000);

        when(mockFailureEvent.isSuccess()).thenReturn(false);
        when(mockFailureEvent.getErrorMessage()).thenReturn("쿠폰이 이미 사용되었습니다.");

        when(synchronousEventProcessor.publishAndWaitForResponse(
            any(CouponUsageRequestedEvent.class), any(String.class), eq(CouponUsageCompletedEvent.class), eq(5)))
            .thenReturn(mockFailureEvent);

        // when
        CouponDomainService.CouponProcessResult result = 
            couponDomainService.processCouponDiscount(command, totalAmount);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("쿠폰이 이미 사용되었습니다.");
        assertThat(result.getDiscountedAmount()).isNull();
        assertThat(result.getDiscountAmount()).isNull();
        
        verify(synchronousEventProcessor).publishAndWaitForResponse(
            any(CouponUsageRequestedEvent.class), any(String.class), eq(CouponUsageCompletedEvent.class), eq(5));
    }

    @Test
    @DisplayName("쿠폰 처리 실패 - 이벤트 처리 중 예외 발생")
    void processCouponDiscount_Failure_EventProcessingException() throws Exception {
        // given
        Long userId = 1L;
        Long userCouponId = 1L;
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(), userCouponId);
        BigDecimal totalAmount = BigDecimal.valueOf(20000);

        when(synchronousEventProcessor.publishAndWaitForResponse(
            any(CouponUsageRequestedEvent.class), any(String.class), eq(CouponUsageCompletedEvent.class), eq(5)))
            .thenThrow(new RuntimeException("쿠폰 서비스 연결 실패"));

        // when
        CouponDomainService.CouponProcessResult result = 
            couponDomainService.processCouponDiscount(command, totalAmount);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("쿠폰 처리 중 오류가 발생했습니다");
        
        verify(synchronousEventProcessor).publishAndWaitForResponse(
            any(CouponUsageRequestedEvent.class), any(String.class), eq(CouponUsageCompletedEvent.class), eq(5));
    }

    @Test
    @DisplayName("쿠폰 복원 성공 - 쿠폰 사용하지 않은 경우")
    void rollbackCouponUsage_Success_NoCouponUsed() {
        // given
        Long userId = 1L;
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(), null); // 쿠폰 없음
        String reason = "잔액 차감 실패";

        // when
        couponDomainService.rollbackCouponUsage(command, reason);

        // then - 쿠폰을 사용하지 않았으므로 복원할 것이 없음
        verify(synchronousEventProcessor, never()).publishAndWaitForResponse(any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("쿠폰 복원 성공 - 쿠폰 사용한 경우")
    void rollbackCouponUsage_Success_WithCouponUsed() {
        // given
        Long userId = 1L;
        Long userCouponId = 1L;
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(), userCouponId);
        String reason = "잔액 차감 실패";

        when(mockRestorationSuccessEvent.isSuccess()).thenReturn(true);

        when(synchronousEventProcessor.publishAndWaitForResponse(
            any(CouponRestorationRequestedEvent.class), 
            any(String.class), 
            eq(CouponRestorationCompletedEvent.class), 
            eq(3)))
            .thenReturn(mockRestorationSuccessEvent);

        // when
        couponDomainService.rollbackCouponUsage(command, reason);

        // then
        verify(synchronousEventProcessor).publishAndWaitForResponse(
            any(CouponRestorationRequestedEvent.class), any(String.class), eq(CouponRestorationCompletedEvent.class), eq(3));
    }

    @Test
    @DisplayName("쿠폰 복원 실패")
    void rollbackCouponUsage_Failure() throws Exception {
        // given
        Long userId = 1L;
        Long userCouponId = 1L;
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(), userCouponId);
        String reason = "주문 저장 실패";

        when(mockRestorationFailureEvent.isSuccess()).thenReturn(false);
        when(mockRestorationFailureEvent.getErrorMessage()).thenReturn("쿠폰 복원에 실패했습니다.");

        when(synchronousEventProcessor.publishAndWaitForResponse(
            any(CouponRestorationRequestedEvent.class), any(String.class), eq(CouponRestorationCompletedEvent.class), eq(3)))
            .thenReturn(mockRestorationFailureEvent);

        // when
        couponDomainService.rollbackCouponUsage(command, reason);

        // then - 실패해도 예외가 발생하지 않고 로그만 남김
        verify(synchronousEventProcessor).publishAndWaitForResponse(
            any(CouponRestorationRequestedEvent.class), any(String.class), eq(CouponRestorationCompletedEvent.class), eq(3));
    }

    @Test
    @DisplayName("쿠폰 처리 성공 - 높은 할인율")
    void processCouponDiscount_Success_HighDiscount() {
        // given
        Long userId = 1L;
        Long userCouponId = 1L;
        CreateOrderUseCase.CreateOrderCommand command = 
            new CreateOrderUseCase.CreateOrderCommand(userId, List.of(), userCouponId);
        BigDecimal totalAmount = BigDecimal.valueOf(50000);

        when(mockSuccessEvent.isSuccess()).thenReturn(true);
        when(mockSuccessEvent.getDiscountedAmount()).thenReturn(BigDecimal.valueOf(40000));
        when(mockSuccessEvent.getDiscountAmount()).thenReturn(10000);

        when(synchronousEventProcessor.publishAndWaitForResponse(
            any(CouponUsageRequestedEvent.class), any(String.class), eq(CouponUsageCompletedEvent.class), eq(5)))
            .thenReturn(mockSuccessEvent);

        // when
        CouponDomainService.CouponProcessResult result = 
            couponDomainService.processCouponDiscount(command, totalAmount);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDiscountedAmount()).isEqualTo(BigDecimal.valueOf(40000));
        assertThat(result.getDiscountAmount()).isEqualTo(10000);
        
        verify(synchronousEventProcessor).publishAndWaitForResponse(
            any(CouponUsageRequestedEvent.class), any(String.class), eq(CouponUsageCompletedEvent.class), eq(5));
    }
}