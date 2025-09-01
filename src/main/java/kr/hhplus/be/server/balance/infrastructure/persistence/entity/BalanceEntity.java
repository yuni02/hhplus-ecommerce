package kr.hhplus.be.server.balance.infrastructure.persistence.entity;

import jakarta.persistence.*;
import kr.hhplus.be.server.shared.domain.BaseEntity;
import kr.hhplus.be.server.user.infrastructure.persistence.entity.UserEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 잔액 전용 엔티티
 * 동시성 제어를 위해 별도 테이블로 분리
 */
@Slf4j
@Entity
@Table(name = "balances")
@Getter
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", 
                foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private UserEntity user;

    @Column(name = "user_id", unique = true, nullable = false, insertable = false, updatable = false)
    private Long userId;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "ACTIVE";

    @Version
    @Column(name = "version")
    private Long version;

    // 잔액 관련 비즈니스 메서드들
    public void updateAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void updateStatus(String status) {
        this.status = status;
    }

    // 잔액 차감 비즈니스 메서드
    public boolean deductAmount(BigDecimal amount) {
        log.debug("BalanceEntity.deductAmount() 호출 - 현재 잔액: {}, 차감 금액: {}", this.amount, amount);
        
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("차감 금액이 유효하지 않습니다: {}", amount);
            throw new IllegalArgumentException("차감 금액은 0보다 커야 합니다.");
        }
        if (this.amount.compareTo(amount) < 0) {
            log.debug("잔액 부족 - 현재: {}, 필요: {}", this.amount, amount);
            return false; // 잔액 부족
        }
        
        BigDecimal oldAmount = this.amount;
        this.amount = this.amount.subtract(amount);
        
        log.debug("잔액 차감 완료 - 이전: {}, 차감: {}, 이후: {}", oldAmount, amount, this.amount);
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