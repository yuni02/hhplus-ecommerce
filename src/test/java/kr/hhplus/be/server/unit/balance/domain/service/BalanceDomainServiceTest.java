package kr.hhplus.be.server.unit.balance.domain.service;

import kr.hhplus.be.server.order.domain.BalanceDeductionCompletedEvent;
import kr.hhplus.be.server.order.domain.BalanceDeductionRequestedEvent;
import kr.hhplus.be.server.shared.event.SynchronousEventProcessor;
import kr.hhplus.be.server.balance.domain.service.BalanceDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BalanceDomainServiceTest {

    @Mock
    private SynchronousEventProcessor synchronousEventProcessor;

    @Mock
    private BalanceDeductionCompletedEvent mockSuccessEvent;
    
    @Mock 
    private BalanceDeductionCompletedEvent mockFailureEvent;

    private BalanceDomainService balanceDomainService;

    @BeforeEach
    void setUp() {
        balanceDomainService = new BalanceDomainService(synchronousEventProcessor);
    }

    @Test
    @DisplayName("잔액 처리 성공")
    void processBalanceDeduction_Success() throws Exception {
        // given
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(20000);
        BigDecimal remainingBalance = BigDecimal.valueOf(80000);

        when(mockSuccessEvent.isSuccess()).thenReturn(true);
        when(mockSuccessEvent.getRemainingBalance()).thenReturn(remainingBalance);
        when(mockSuccessEvent.getErrorMessage()).thenReturn(null);

        when(synchronousEventProcessor.publishAndWaitForResponse(
            any(BalanceDeductionRequestedEvent.class),
            any(String.class), 
            eq(BalanceDeductionCompletedEvent.class), 
            eq(5)))
            .thenReturn(mockSuccessEvent);

        // when
        BalanceDomainService.BalanceProcessResult result = 
            balanceDomainService.processBalanceDeduction(userId, amount);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getRemainingBalance()).isEqualTo(remainingBalance);
        assertThat(result.getErrorMessage()).isNull();
        
        verify(synchronousEventProcessor).publishAndWaitForResponse(
            any(BalanceDeductionRequestedEvent.class), any(String.class), eq(BalanceDeductionCompletedEvent.class), eq(5));
    }

    @Test
    @DisplayName("잔액 처리 실패 - 잔액 부족")
    void processBalanceDeduction_Failure_InsufficientBalance() throws Exception {
        // given
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(100000);

        when(mockFailureEvent.isSuccess()).thenReturn(false);
        when(mockFailureEvent.getRemainingBalance()).thenReturn(null);
        when(mockFailureEvent.getErrorMessage()).thenReturn("잔액이 부족합니다.");

        when(synchronousEventProcessor.publishAndWaitForResponse(
            any(BalanceDeductionRequestedEvent.class), any(String.class), eq(BalanceDeductionCompletedEvent.class), eq(5)))
            .thenReturn(mockFailureEvent);

        // when
        BalanceDomainService.BalanceProcessResult result = 
            balanceDomainService.processBalanceDeduction(userId, amount);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getRemainingBalance()).isNull();
        assertThat(result.getErrorMessage()).isEqualTo("잔액이 부족합니다.");
        
        verify(synchronousEventProcessor).publishAndWaitForResponse(
            any(BalanceDeductionRequestedEvent.class), any(String.class), eq(BalanceDeductionCompletedEvent.class), eq(5));
    }

    @Test
    @DisplayName("잔액 처리 실패 - 이벤트 처리 중 예외 발생")
    void processBalanceDeduction_Failure_EventProcessingException() throws Exception {
        // given
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(20000);

        when(synchronousEventProcessor.publishAndWaitForResponse(
            any(BalanceDeductionRequestedEvent.class), any(String.class), eq(BalanceDeductionCompletedEvent.class), eq(5)))
            .thenThrow(new RuntimeException("잔액 서비스 연결 실패"));

        // when
        BalanceDomainService.BalanceProcessResult result = 
            balanceDomainService.processBalanceDeduction(userId, amount);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("잔액 처리 중 오류가 발생했습니다");
        
        verify(synchronousEventProcessor).publishAndWaitForResponse(
            any(BalanceDeductionRequestedEvent.class), any(String.class), eq(BalanceDeductionCompletedEvent.class), eq(5));
    }
}