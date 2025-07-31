package kr.hhplus.be.server.balance.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 잔액 도메인 엔티티
 * 순수한 비즈니스 로직만 포함 (JPA 어노테이션 없음)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Balance {
    
    private Long id;
    private Long userId;
    
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;
    
    @Builder.Default
    private BalanceStatus status = BalanceStatus.ACTIVE;

    // 비즈니스 로직 메서드들
    public void charge(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }
        this.amount = this.amount.add(amount);
    }

    public void deduct(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("차감 금액은 0보다 커야 합니다.");
        }
        if (this.amount.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("잔액이 부족합니다.");
        }
        this.amount = this.amount.subtract(amount);
    }

    public boolean hasSufficientBalance(BigDecimal amount) {
        return this.amount.compareTo(amount) >= 0;
    }

    public enum BalanceStatus {
        ACTIVE, INACTIVE
    }

    public static class InsufficientBalanceException extends RuntimeException {
        public InsufficientBalanceException(String message) {
            super(message);
        }
    }
} 