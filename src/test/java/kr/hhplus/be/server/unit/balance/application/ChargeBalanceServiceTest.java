package kr.hhplus.be.server.unit.balance.application;

import kr.hhplus.be.server.balance.application.ChargeBalanceService;
import kr.hhplus.be.server.balance.application.port.in.ChargeBalanceUseCase;
import kr.hhplus.be.server.balance.application.port.out.LoadBalancePort;
import kr.hhplus.be.server.balance.application.port.out.LoadUserPort;
import kr.hhplus.be.server.balance.application.port.out.SaveBalanceTransactionPort;
import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.domain.BalanceTransaction;
import kr.hhplus.be.server.shared.service.DistributedLockService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChargeBalanceServiceTest {

    @Mock
    private LoadUserPort loadUserPort;
    
    @Mock
    private LoadBalancePort loadBalancePort;
    
    @Mock
    private SaveBalanceTransactionPort saveBalanceTransactionPort;

    @Mock
    private DistributedLockService distributedLockService;

    private ChargeBalanceService chargeBalanceService;

    @BeforeEach
    void setUp() {
        chargeBalanceService = new ChargeBalanceService(loadUserPort, loadBalancePort, saveBalanceTransactionPort, distributedLockService);
    }

    @Test
    @DisplayName("잔액 충전 성공")
    void chargeBalance_Success() {
        // given
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(10000);
        ChargeBalanceUseCase.ChargeBalanceCommand command = 
            new ChargeBalanceUseCase.ChargeBalanceCommand(userId, amount);

        Balance existingBalance = Balance.builder().userId(userId).amount(BigDecimal.valueOf(5000)).build();
        
        Balance savedBalance = Balance.builder().userId(userId).build();
        savedBalance.setId(1L);
        savedBalance.setAmount(BigDecimal.valueOf(15000));
        
        BalanceTransaction savedTransaction = BalanceTransaction.create(userId, amount, 
            BalanceTransaction.TransactionType.CHARGE, "잔액 충전");
        savedTransaction.setId(1L);

        when(loadUserPort.existsByUserId(userId)).thenReturn(true);
        when(loadBalancePort.loadActiveBalanceByUserId(userId)).thenReturn(Optional.of(existingBalance));
        when(loadBalancePort.saveBalanceWithConcurrencyControl(any(Balance.class))).thenReturn(savedBalance);
        when(saveBalanceTransactionPort.saveBalanceTransaction(any(BalanceTransaction.class)))
            .thenReturn(savedTransaction);

        // when
        ChargeBalanceUseCase.ChargeBalanceResult result = chargeBalanceService.chargeBalance(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getNewBalance()).isEqualTo(BigDecimal.valueOf(15000));
        assertThat(result.getTransactionId()).isEqualTo(1L);
        
        verify(loadUserPort).existsByUserId(userId);
        verify(loadBalancePort).loadActiveBalanceByUserId(userId);
        verify(loadBalancePort).saveBalanceWithConcurrencyControl(any(Balance.class));
        verify(saveBalanceTransactionPort).saveBalanceTransaction(any(BalanceTransaction.class));
    }

    @Test
    @DisplayName("잔액 충전 실패 - 사용자가 존재하지 않는 경우")
    void chargeBalance_Failure_UserNotFound() {
        // given
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(10000);
        ChargeBalanceUseCase.ChargeBalanceCommand command = 
            new ChargeBalanceUseCase.ChargeBalanceCommand(userId, amount);

        when(loadUserPort.existsByUserId(userId)).thenReturn(false);                        

        // when
        ChargeBalanceUseCase.ChargeBalanceResult result = chargeBalanceService.chargeBalance(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("사용자를 찾을 수 없습니다.");
        
        verify(loadUserPort).existsByUserId(userId);
        verify(loadBalancePort, never()).loadActiveBalanceByUserId(any());
        verify(loadBalancePort, never()).saveBalanceWithConcurrencyControl(any());
        verify(saveBalanceTransactionPort, never()).saveBalanceTransaction(any());
    }
} 