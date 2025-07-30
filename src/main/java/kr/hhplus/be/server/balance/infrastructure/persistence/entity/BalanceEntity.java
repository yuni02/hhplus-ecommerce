package kr.hhplus.be.server.balance.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Balance 인프라스트럭처 엔티티
 * Balance 도메인 전용 JPA 매핑 엔티티
 * 외래키 제약조건 없이 느슨한 결합으로 설계
 */
@Entity
@Table(name = "users")
@Getter
@Setter(AccessLevel.PRIVATE) // setter는 private으로 제한
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceEntity {
    
    @Id
    @Column(name = "id")
    private Long id;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "amount")
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;
    
    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // enum 대신 varchar

    // 필요한 경우에만 public setter 제공
    public void updateAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void updateStatus(String status) {
        this.status = status;
    }

    // 잔액 차감 비즈니스 메서드
    public boolean deductAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("차감 금액은 0보다 커야 합니다.");
        }
        if (this.amount.compareTo(amount) < 0) {
            return false; // 잔액 부족
        }
        this.amount = this.amount.subtract(amount);
        return true;
    }

    // 잔액 충전 비즈니스 메서드
    public void chargeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }
        this.amount = this.amount.add(amount);
    }
}