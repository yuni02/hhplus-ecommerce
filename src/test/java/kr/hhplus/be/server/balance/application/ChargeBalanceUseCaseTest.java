package kr.hhplus.be.server.balance.application;

import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.domain.BalanceRepository;
import kr.hhplus.be.server.balance.domain.BalanceService;
import kr.hhplus.be.server.balance.domain.BalanceTransaction;
import kr.hhplus.be.server.balance.domain.BalanceTransactionRepository;
import kr.hhplus.be.server.user.domain.UserRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChargeBalanceUseCase 단위 테스트")
class ChargeBalanceUseCaseTest {

    @Mock
    private BalanceRepository balanceRepository;

    @Mock
    private BalanceTransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BalanceService balanceService;

    private ChargeBalanceUseCase chargeBalanceUseCase;

    @BeforeEach
    void setUp() {
        chargeBalanceUseCase = new ChargeBalanceUseCase(balanceService);        
    }

    @Test
    @DisplayName("기존 잔액이 있는 사용자의 잔액 충전")
    void chargeBalance_ExistingBalance_Success() {
        // given
        Long userId = 1L;
        BigDecimal chargeAmount = BigDecimal.valueOf(10000);
        
        Balance existingBalance = new Balance(userId);
        existingBalance.setId(1L);
        existingBalance.setAmount(BigDecimal.valueOf(5000));
        
        BalanceTransaction transaction = new BalanceTransaction(userId, chargeAmount, 
                BalanceTransaction.TransactionType.CHARGE, "잔액 충전");
        transaction.setId(1L);
        transaction.setStatus(BalanceTransaction.TransactionStatus.COMPLETED);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(balanceRepository.findActiveBalanceByUserId(userId))
                .thenReturn(Optional.of(existingBalance));
        when(balanceRepository.save(any(Balance.class))).thenReturn(existingBalance);
        when(transactionRepository.save(any(BalanceTransaction.class))).thenReturn(transaction);

        // when
        ChargeBalanceUseCase.Output result = chargeBalanceUseCase.execute(new ChargeBalanceUseCase.Input(userId, chargeAmount));

        // then
        assertThat(result.getBalance()).isEqualTo(BigDecimal.valueOf(15000));
        verify(userRepository).existsById(userId);
        verify(balanceRepository).findActiveBalanceByUserId(userId);
        verify(balanceRepository).save(any(Balance.class));
        verify(transactionRepository, times(2)).save(any(BalanceTransaction.class));
    }

    @Test
    @DisplayName("기존 잔액이 없는 사용자의 잔액 충전")
    void chargeBalance_NoExistingBalance_CreateNewBalance() {
        // given
        Long userId = 1L;
        BigDecimal chargeAmount = BigDecimal.valueOf(10000);
        
        Balance newBalance = new Balance(userId);
        newBalance.setId(1L);
        newBalance.setAmount(chargeAmount);
        
        BalanceTransaction transaction = new BalanceTransaction(userId, chargeAmount, 
                BalanceTransaction.TransactionType.CHARGE, "잔액 충전");
        transaction.setId(1L);
        transaction.setStatus(BalanceTransaction.TransactionStatus.COMPLETED);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(balanceRepository.findActiveBalanceByUserId(userId))
                .thenReturn(Optional.empty());
        when(balanceRepository.save(any(Balance.class))).thenReturn(newBalance);
        when(transactionRepository.save(any(BalanceTransaction.class))).thenReturn(transaction);

        // when
        ChargeBalanceUseCase.Output result = chargeBalanceUseCase.execute(new ChargeBalanceUseCase.Input(userId, chargeAmount));

        // then
        assertThat(result.getBalance()).isEqualTo(chargeAmount);
        verify(userRepository).existsById(userId);
        verify(balanceRepository).findActiveBalanceByUserId(userId);
        verify(balanceRepository, times(2)).save(any(Balance.class));
        verify(transactionRepository, times(2)).save(any(BalanceTransaction.class));
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 잔액 충전 시 예외 발생")
    void chargeBalance_NonExistentUser_ThrowsException() {
        // given
        Long userId = 999L;
        BigDecimal chargeAmount = BigDecimal.valueOf(10000);

        when(userRepository.existsById(userId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> chargeBalanceUseCase.execute(new ChargeBalanceUseCase.Input(userId, chargeAmount)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");

        verify(userRepository).existsById(userId);
        verifyNoInteractions(balanceRepository, transactionRepository);
    }

    @Test
    @DisplayName("잔액 저장 실패 시 거래 실패 처리")
    void chargeBalance_SaveFailure_TransactionFailed() {
        // given
        Long userId = 1L;
        BigDecimal chargeAmount = BigDecimal.valueOf(10000);
        
        Balance existingBalance = new Balance(userId);
        existingBalance.setId(1L);
        existingBalance.setAmount(BigDecimal.valueOf(5000));
        
        BalanceTransaction transaction = new BalanceTransaction(userId, chargeAmount, 
                BalanceTransaction.TransactionType.CHARGE, "잔액 충전");
        transaction.setId(1L);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(balanceRepository.findActiveBalanceByUserId(userId))
                .thenReturn(Optional.of(existingBalance));
        when(balanceRepository.save(any(Balance.class)))
                .thenThrow(new RuntimeException("Database error"));
        when(transactionRepository.save(any(BalanceTransaction.class))).thenReturn(transaction);

        // when & then
        assertThatThrownBy(() -> chargeBalanceUseCase.execute(new ChargeBalanceUseCase.Input(userId, chargeAmount)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");

        verify(transactionRepository, times(2)).save(any(BalanceTransaction.class));
        verify(transactionRepository).save(argThat(t -> 
                t.getStatus() == BalanceTransaction.TransactionStatus.FAILED));
    }
} 