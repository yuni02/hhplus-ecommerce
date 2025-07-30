package kr.hhplus.be.server.user.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 사용자 도메인 엔티티
 * 순수한 비즈니스 로직만 포함 (JPA 어노테이션 없음)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    private Long id;
    private String username;
    private String name;
    private String email;
    private String phoneNumber;
    
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;
    
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 비즈니스 로직 메서드들
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.status = UserStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 잔액 충전
     */
    public void chargeBalance(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("충전 금액은 양수여야 합니다.");
        }
        this.balance = this.balance.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 잔액 차감
     */
    public void deductBalance(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("차감 금액은 양수여야 합니다.");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("잔액이 부족합니다. 현재 잔액: " + this.balance);
        }
        this.balance = this.balance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 잔액 확인
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        return this.balance.compareTo(amount) >= 0;
    }

    public enum UserStatus {
        ACTIVE, INACTIVE, SUSPENDED
    }

    public static class InsufficientBalanceException extends RuntimeException {
        public InsufficientBalanceException(String message) {
            super(message);
        }
    }
} 