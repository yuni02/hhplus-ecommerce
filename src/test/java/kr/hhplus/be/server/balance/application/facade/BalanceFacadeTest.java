package kr.hhplus.be.server.balance.application.facade;

import kr.hhplus.be.server.balance.application.port.in.ChargeBalanceUseCase;
import kr.hhplus.be.server.balance.application.port.in.GetBalanceUseCase;
import kr.hhplus.be.server.balance.application.port.out.LoadBalancePort;
import kr.hhplus.be.server.balance.application.port.out.LoadUserPort;
import kr.hhplus.be.server.balance.application.port.out.SaveBalanceTransactionPort;
import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.domain.BalanceTransaction;
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
class BalanceFacadeTest {

    @Mock
    private LoadUserPort loadUserPort;
    
    @Mock
    private LoadBalancePort loadBalancePort;
    
    @Mock
    private SaveBalanceTransactionPort saveBalanceTransactionPort;

    private BalanceFacade balanceFacade;

    @BeforeEach
    void setUp() {
        balanceFacade = new BalanceFacade(loadUserPort, loadBalancePort, saveBalanceTransactionPort);
    }

    @Test
    @DisplayName("잔액 조회 성공 - 기존 잔액이 있는 경우")
    void getBalance_Success_WithExistingBalance() {
        // given
        Long userId = 1L;
        BigDecimal existingBalance = new BigDecimal("50000");
        
        GetBalanceUseCase.GetBalanceCommand command = new GetBalanceUseCase.GetBalanceCommand(userId);
        Balance balance = new Balance(userId);
        balance.setAmount(existingBalance);
        
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadBalancePort.loadActiveBalanceByUserId(userId)).thenReturn(Optional.of(balance));

        // when
        Optional<GetBalanceUseCase.GetBalanceResult> result = balanceFacade.getBalance(command);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(userId);
        assertThat(result.get().getBalance()).isEqualTo(existingBalance);
        
        verify(loadUserPort).existsById(userId);
        verify(loadBalancePort).loadActiveBalanceByUserId(userId);
    }

    @Test
    @DisplayName("잔액 조회 실패 - 사용자가 존재하지 않는 경우")
    void getBalance_Failure_UserNotFound() {
        // given
        Long userId = 999L;
        GetBalanceUseCase.GetBalanceCommand command = new GetBalanceUseCase.GetBalanceCommand(userId);
        
        when(loadUserPort.existsById(userId)).thenReturn(false);

        // when
        Optional<GetBalanceUseCase.GetBalanceResult> result = balanceFacade.getBalance(command);

        // then
        assertThat(result).isEmpty();
        verify(loadUserPort).existsById(userId);
        verify(loadBalancePort, never()).loadActiveBalanceByUserId(any());
    }

    @Test
    @DisplayName("잔액 조회 실패 - 잔액이 존재하지 않는 경우")
    void getBalance_Failure_BalanceNotFound() {
        // given
        Long userId = 1L;
        GetBalanceUseCase.GetBalanceCommand command = new GetBalanceUseCase.GetBalanceCommand(userId);
        
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadBalancePort.loadActiveBalanceByUserId(userId)).thenReturn(Optional.empty());

        // when
        Optional<GetBalanceUseCase.GetBalanceResult> result = balanceFacade.getBalance(command);

        // then
        assertThat(result).isEmpty();
        verify(loadUserPort).existsById(userId);
        verify(loadBalancePort).loadActiveBalanceByUserId(userId);
    }

    @Test
    @DisplayName("잔액 충전 성공 - 기존 잔액이 있는 경우")
    void chargeBalance_Success_WithExistingBalance() {
        // given
        Long userId = 1L;
        BigDecimal chargeAmount = new BigDecimal("10000");
        BigDecimal existingBalance = new BigDecimal("50000");
        Long transactionId = 1L;
        
        ChargeBalanceUseCase.ChargeBalanceCommand command = 
            new ChargeBalanceUseCase.ChargeBalanceCommand(userId, chargeAmount);
        
        Balance existingBalanceEntity = new Balance(userId);
        existingBalanceEntity.setAmount(existingBalance);
        
        Balance savedBalance = new Balance(userId);
        savedBalance.setAmount(existingBalance.add(chargeAmount));
        
        BalanceTransaction transaction = new BalanceTransaction(userId, chargeAmount, 
            BalanceTransaction.TransactionType.CHARGE, "잔액 충전");
        transaction.setId(transactionId);
        
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadBalancePort.loadActiveBalanceByUserId(userId)).thenReturn(Optional.of(existingBalanceEntity));
        when(loadBalancePort.saveBalance(any(Balance.class))).thenReturn(savedBalance);
        when(saveBalanceTransactionPort.saveBalanceTransaction(any(BalanceTransaction.class))).thenReturn(transaction);

        // when
        ChargeBalanceUseCase.ChargeBalanceResult result = balanceFacade.chargeBalance(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getNewBalance()).isEqualTo(existingBalance.add(chargeAmount));
        assertThat(result.getTransactionId()).isEqualTo(transactionId);
        assertThat(result.getErrorMessage()).isNull();
        
        verify(loadUserPort).existsById(userId);
        verify(loadBalancePort).loadActiveBalanceByUserId(userId);
        verify(loadBalancePort).saveBalance(any(Balance.class));
        verify(saveBalanceTransactionPort).saveBalanceTransaction(any(BalanceTransaction.class));
    }

    @Test
    @DisplayName("잔액 충전 성공 - 기존 잔액이 없는 경우")
    void chargeBalance_Success_WithoutExistingBalance() {
        // given
        Long userId = 1L;
        BigDecimal chargeAmount = new BigDecimal("10000");
        Long transactionId = 1L;
        
        ChargeBalanceUseCase.ChargeBalanceCommand command = 
            new ChargeBalanceUseCase.ChargeBalanceCommand(userId, chargeAmount);
        
        Balance newBalance = new Balance(userId);
        newBalance.setAmount(chargeAmount);
        
        BalanceTransaction transaction = new BalanceTransaction(userId, chargeAmount, 
            BalanceTransaction.TransactionType.CHARGE, "잔액 충전");
        transaction.setId(transactionId);
        
        when(loadUserPort.existsById(userId)).thenReturn(true);
        when(loadBalancePort.loadActiveBalanceByUserId(userId)).thenReturn(Optional.empty());
        when(loadBalancePort.saveBalance(any(Balance.class))).thenReturn(newBalance);
        when(saveBalanceTransactionPort.saveBalanceTransaction(any(BalanceTransaction.class))).thenReturn(transaction);

        // when
        ChargeBalanceUseCase.ChargeBalanceResult result = balanceFacade.chargeBalance(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getNewBalance()).isEqualTo(chargeAmount);
        assertThat(result.getTransactionId()).isEqualTo(transactionId);
        
        verify(loadUserPort).existsById(userId);
        verify(loadBalancePort).loadActiveBalanceByUserId(userId);
        verify(loadBalancePort).saveBalance(any(Balance.class));
        verify(saveBalanceTransactionPort).saveBalanceTransaction(any(BalanceTransaction.class));
    }

    @Test
    @DisplayName("잔액 충전 실패 - 사용자가 존재하지 않는 경우")
    void chargeBalance_Failure_UserNotFound() {
        // given
        Long userId = 999L;
        BigDecimal chargeAmount = new BigDecimal("10000");
        
        ChargeBalanceUseCase.ChargeBalanceCommand command = 
            new ChargeBalanceUseCase.ChargeBalanceCommand(userId, chargeAmount);
        
        when(loadUserPort.existsById(userId)).thenReturn(false);

        // when
        ChargeBalanceUseCase.ChargeBalanceResult result = balanceFacade.chargeBalance(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("사용자를 찾을 수 없습니다.");
        assertThat(result.getUserId()).isNull();
        assertThat(result.getNewBalance()).isNull();
        assertThat(result.getTransactionId()).isNull();
        
        verify(loadUserPort).existsById(userId);
        verify(loadBalancePort, never()).loadActiveBalanceByUserId(any());
        verify(loadBalancePort, never()).saveBalance(any());
        verify(saveBalanceTransactionPort, never()).saveBalanceTransaction(any());
    }

    @Test
    @DisplayName("잔액 충전 실패 - 예외 발생 시")
    void chargeBalance_Failure_Exception() {
        // given
        Long userId = 1L;
        BigDecimal chargeAmount = new BigDecimal("10000");
        
        ChargeBalanceUseCase.ChargeBalanceCommand command = 
            new ChargeBalanceUseCase.ChargeBalanceCommand(userId, chargeAmount);
        
        when(loadUserPort.existsById(userId)).thenThrow(new RuntimeException("데이터베이스 오류"));

        // when
        ChargeBalanceUseCase.ChargeBalanceResult result = balanceFacade.chargeBalance(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("잔액 충전 중 오류가 발생했습니다");
        assertThat(result.getUserId()).isNull();
        assertThat(result.getNewBalance()).isNull();
        assertThat(result.getTransactionId()).isNull();
        
        verify(loadUserPort).existsById(userId);
        verify(loadBalancePort, never()).loadActiveBalanceByUserId(any());
    }
} 