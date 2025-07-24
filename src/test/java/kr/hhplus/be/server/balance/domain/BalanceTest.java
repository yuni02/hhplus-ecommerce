package kr.hhplus.be.server.balance.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BalanceTest {

    @Test
    @DisplayName("Balance 생성 성공")
    void createBalance_Success() {
        // given
        Long userId = 1L;
        BigDecimal initialAmount = new BigDecimal("50000");

        // when
        Balance balance = new Balance(userId);
        balance.setAmount(initialAmount);

        // then
        assertThat(balance.getUserId()).isEqualTo(userId);
        assertThat(balance.getAmount()).isEqualTo(initialAmount);
        assertThat(balance.getStatus()).isEqualTo(Balance.BalanceStatus.ACTIVE);
    }

    @Test
    @DisplayName("Balance 생성 - 기본값 확인")
    void createBalance_DefaultValues() {
        // given
        Long userId = 1L;

        // when
        Balance balance = new Balance(userId);

        // then
        assertThat(balance.getUserId()).isEqualTo(userId);
        assertThat(balance.getAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(balance.getStatus()).isEqualTo(Balance.BalanceStatus.ACTIVE);
    }

    @Test
    @DisplayName("Balance 충전 성공")
    void chargeBalance_Success() {
        // given
        Long userId = 1L;
        Balance balance = new Balance(userId);
        balance.setAmount(new BigDecimal("50000"));
        
        BigDecimal chargeAmount = new BigDecimal("10000");

        // when
        balance.charge(chargeAmount);

        // then
        assertThat(balance.getAmount()).isEqualTo(new BigDecimal("60000"));
    }

    @Test
    @DisplayName("Balance 충전 - 음수 금액")
    void chargeBalance_NegativeAmount() {
        // given
        Long userId = 1L;
        Balance balance = new Balance(userId);
        balance.setAmount(new BigDecimal("50000"));
        
        BigDecimal negativeAmount = new BigDecimal("-1000");

        // when & then
        assertThatThrownBy(() -> balance.charge(negativeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("Balance 충전 - 0원")
    void chargeBalance_ZeroAmount() {
        // given
        Long userId = 1L;
        Balance balance = new Balance(userId);
        balance.setAmount(new BigDecimal("50000"));
        
        BigDecimal zeroAmount = BigDecimal.ZERO;

        // when & then
        assertThatThrownBy(() -> balance.charge(zeroAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("Balance 차감 성공")
    void deductBalance_Success() {
        // given
        Long userId = 1L;
        Balance balance = new Balance(userId);
        balance.setAmount(new BigDecimal("50000"));
        
        BigDecimal deductAmount = new BigDecimal("20000");

        // when
        balance.deduct(deductAmount);

        // then
        assertThat(balance.getAmount()).isEqualTo(new BigDecimal("30000"));
    }

    @Test
    @DisplayName("Balance 차감 실패 - 잔액 부족")
    void deductBalance_InsufficientBalance() {
        // given
        Long userId = 1L;
        Balance balance = new Balance(userId);
        balance.setAmount(new BigDecimal("50000"));
        
        BigDecimal deductAmount = new BigDecimal("60000");

        // when & then
        assertThatThrownBy(() -> balance.deduct(deductAmount))
                .isInstanceOf(Balance.InsufficientBalanceException.class)
                .hasMessage("잔액이 부족합니다.");
        assertThat(balance.getAmount()).isEqualTo(new BigDecimal("50000")); // 잔액 변경 없음
    }

    @Test
    @DisplayName("Balance 차감 - 음수 금액")
    void deductBalance_NegativeAmount() {
        // given
        Long userId = 1L;
        Balance balance = new Balance(userId);
        balance.setAmount(new BigDecimal("50000"));
        
        BigDecimal negativeAmount = new BigDecimal("-1000");

        // when & then
        assertThatThrownBy(() -> balance.deduct(negativeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("차감 금액은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("Balance 차감 - 0원")
    void deductBalance_ZeroAmount() {
        // given
        Long userId = 1L;
        Balance balance = new Balance(userId);
        balance.setAmount(new BigDecimal("50000"));
        
        BigDecimal zeroAmount = BigDecimal.ZERO;

        // when & then
        assertThatThrownBy(() -> balance.deduct(zeroAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("차감 금액은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("Balance 상태 변경")
    void changeBalanceStatus() {
        // given
        Long userId = 1L;
        Balance balance = new Balance(userId);

        // when
        balance.setStatus(Balance.BalanceStatus.INACTIVE);

        // then
        assertThat(balance.getStatus()).isEqualTo(Balance.BalanceStatus.INACTIVE);
    }

    @Test
    @DisplayName("Balance ID 설정")
    void setBalanceId() {
        // given
        Long userId = 1L;
        Long balanceId = 1L;
        Balance balance = new Balance(userId);

        // when
        balance.setId(balanceId);

        // then
        assertThat(balance.getId()).isEqualTo(balanceId);
    }

    @Test
    @DisplayName("Balance 충전 금액 검증")
    void chargeBalance_Validation() {
        // given
        Long userId = 1L;
        Balance balance = new Balance(userId);
        balance.setAmount(new BigDecimal("50000"));
        
        BigDecimal chargeAmount = new BigDecimal("10000");

        // when
        balance.charge(chargeAmount);

        // then
        assertThat(balance.getAmount()).isEqualTo(new BigDecimal("60000"));
        assertThat(balance.hasSufficientBalance(new BigDecimal("60000"))).isTrue();
        assertThat(balance.hasSufficientBalance(new BigDecimal("70000"))).isFalse();
    }
} 