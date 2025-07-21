package kr.hhplus.be.server.balance.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BalanceDomainService 단위 테스트")
class BalanceDomainServiceTest {

    @Test
    @DisplayName("정상적인 잔액 충전")
    void chargeBalance_ValidAmount_Success() {
        // given
        Balance balance = new Balance(1L);
        balance.setAmount(BigDecimal.valueOf(5000));
        BigDecimal chargeAmount = BigDecimal.valueOf(10000);

        // when
        Balance result = BalanceDomainService.chargeBalance(balance, chargeAmount);

        // then
        assertThat(result.getAmount()).isEqualTo(BigDecimal.valueOf(15000));
    }

    @Test
    @DisplayName("음수 금액으로 충전 시 예외 발생")
    void chargeBalance_NegativeAmount_ThrowsException() {
        // given
        Balance balance = new Balance(1L);
        balance.setAmount(BigDecimal.valueOf(5000));
        BigDecimal chargeAmount = BigDecimal.valueOf(-1000);

        // when & then
        assertThatThrownBy(() -> BalanceDomainService.chargeBalance(balance, chargeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액은 양수여야 합니다.");
    }

    @Test
    @DisplayName("0원으로 충전 시 예외 발생")
    void chargeBalance_ZeroAmount_ThrowsException() {
        // given
        Balance balance = new Balance(1L);
        balance.setAmount(BigDecimal.valueOf(5000));
        BigDecimal chargeAmount = BigDecimal.ZERO;

        // when & then
        assertThatThrownBy(() -> BalanceDomainService.chargeBalance(balance, chargeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액은 양수여야 합니다.");
    }

    @Test
    @DisplayName("최대 충전 금액 초과 시 예외 발생")
    void chargeBalance_ExceedMaxAmount_ThrowsException() {
        // given
        Balance balance = new Balance(1L);
        balance.setAmount(BigDecimal.valueOf(5000));
        BigDecimal chargeAmount = BigDecimal.valueOf(1000001);

        // when & then
        assertThatThrownBy(() -> BalanceDomainService.chargeBalance(balance, chargeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("1회 최대 충전 금액은 1,000,000원입니다.");
    }

    @Test
    @DisplayName("정상적인 잔액 차감")
    void deductBalance_ValidAmount_Success() {
        // given
        Balance balance = new Balance(1L);
        balance.setAmount(BigDecimal.valueOf(15000));
        BigDecimal deductAmount = BigDecimal.valueOf(5000);

        // when
        Balance result = BalanceDomainService.deductBalance(balance, deductAmount);

        // then
        assertThat(result.getAmount()).isEqualTo(BigDecimal.valueOf(10000));
    }

    @Test
    @DisplayName("잔액 부족으로 차감 시 예외 발생")
    void deductBalance_InsufficientBalance_ThrowsException() {
        // given
        Balance balance = new Balance(1L);
        balance.setAmount(BigDecimal.valueOf(5000));
        BigDecimal deductAmount = BigDecimal.valueOf(10000);

        // when & then
        assertThatThrownBy(() -> BalanceDomainService.deductBalance(balance, deductAmount))
                .isInstanceOf(Balance.InsufficientBalanceException.class)
                .hasMessage("잔액이 부족합니다.");
    }

    @Test
    @DisplayName("거래 기록 생성")
    void createTransaction_ValidData_Success() {
        // given
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(10000);
        BalanceTransaction.TransactionType type = BalanceTransaction.TransactionType.CHARGE;
        String description = "잔액 충전";

        // when
        BalanceTransaction result = BalanceDomainService.createTransaction(userId, amount, type, description);

        // then
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getType()).isEqualTo(type);
        assertThat(result.getDescription()).isEqualTo(description);
        assertThat(result.getStatus()).isEqualTo(BalanceTransaction.TransactionStatus.PENDING);
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("거래 완료 처리")
    void completeTransaction_Success() {
        // given
        BalanceTransaction transaction = new BalanceTransaction(1L, BigDecimal.valueOf(10000), 
                BalanceTransaction.TransactionType.CHARGE, "잔액 충전");

        // when
        BalanceTransaction result = BalanceDomainService.completeTransaction(transaction);

        // then
        assertThat(result.getStatus()).isEqualTo(BalanceTransaction.TransactionStatus.COMPLETED);
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("거래 실패 처리")
    void failTransaction_Success() {
        // given
        BalanceTransaction transaction = new BalanceTransaction(1L, BigDecimal.valueOf(10000), 
                BalanceTransaction.TransactionType.CHARGE, "잔액 충전");

        // when
        BalanceTransaction result = BalanceDomainService.failTransaction(transaction);

        // then
        assertThat(result.getStatus()).isEqualTo(BalanceTransaction.TransactionStatus.FAILED);
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("잔액 충분 여부 확인 - 충분한 경우")
    void hasSufficientBalance_EnoughBalance_ReturnsTrue() {
        // given
        Balance balance = new Balance(1L);
        balance.setAmount(BigDecimal.valueOf(15000));
        BigDecimal requiredAmount = BigDecimal.valueOf(10000);

        // when
        boolean result = BalanceDomainService.hasSufficientBalance(balance, requiredAmount);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("잔액 충분 여부 확인 - 부족한 경우")
    void hasSufficientBalance_InsufficientBalance_ReturnsFalse() {
        // given
        Balance balance = new Balance(1L);
        balance.setAmount(BigDecimal.valueOf(5000));
        BigDecimal requiredAmount = BigDecimal.valueOf(10000);

        // when
        boolean result = BalanceDomainService.hasSufficientBalance(balance, requiredAmount);

        // then
        assertThat(result).isFalse();
    }
} 