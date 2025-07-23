package kr.hhplus.be.server.balance.application;

import kr.hhplus.be.server.balance.application.facade.BalanceFacade;
import kr.hhplus.be.server.balance.application.port.in.ChargeBalanceUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChargeBalanceServiceTest {

    @Mock
    private BalanceFacade balanceFacade;

    private ChargeBalanceService chargeBalanceService;

    @BeforeEach
    void setUp() {
        chargeBalanceService = new ChargeBalanceService(balanceFacade);
    }

    @Test
    @DisplayName("잔액 충전 성공")
    void chargeBalance_Success() {
        // given
        Long userId = 1L;
        BigDecimal chargeAmount = new BigDecimal("10000");
        BigDecimal newBalance = new BigDecimal("60000");
        Long transactionId = 1L;
        
        ChargeBalanceUseCase.ChargeBalanceCommand command = 
            new ChargeBalanceUseCase.ChargeBalanceCommand(userId, chargeAmount);
        
        ChargeBalanceUseCase.ChargeBalanceResult expectedResult = 
            ChargeBalanceUseCase.ChargeBalanceResult.success(userId, newBalance, transactionId);
        
        when(balanceFacade.chargeBalance(command)).thenReturn(expectedResult);

        // when
        ChargeBalanceUseCase.ChargeBalanceResult result = chargeBalanceService.chargeBalance(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getNewBalance()).isEqualTo(newBalance);
        assertThat(result.getTransactionId()).isEqualTo(transactionId);
        assertThat(result.getErrorMessage()).isNull();
        
        verify(balanceFacade).chargeBalance(command);
    }

    @Test
    @DisplayName("잔액 충전 실패")
    void chargeBalance_Failure() {
        // given
        Long userId = 999L;
        BigDecimal chargeAmount = new BigDecimal("10000");
        String errorMessage = "사용자를 찾을 수 없습니다.";
        
        ChargeBalanceUseCase.ChargeBalanceCommand command = 
            new ChargeBalanceUseCase.ChargeBalanceCommand(userId, chargeAmount);
        
        ChargeBalanceUseCase.ChargeBalanceResult expectedResult = 
            ChargeBalanceUseCase.ChargeBalanceResult.failure(errorMessage);
        
        when(balanceFacade.chargeBalance(command)).thenReturn(expectedResult);

        // when
        ChargeBalanceUseCase.ChargeBalanceResult result = chargeBalanceService.chargeBalance(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo(errorMessage);
        assertThat(result.getUserId()).isNull();
        assertThat(result.getNewBalance()).isNull();
        assertThat(result.getTransactionId()).isNull();
        
        verify(balanceFacade).chargeBalance(command);
    }
} 