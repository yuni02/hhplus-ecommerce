package kr.hhplus.be.server.balance.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Balance 인프라스트럭처 엔티티
 * Balance 도메인 전용 JPA 매핑 엔티티
 * 외래키 제약조건 없이 느슨한 결합으로 설계
 */
@Entity
@Table(name = "users")
public class BalanceEntity {
    
    @Id
    @Column(name = "id")
    private Long id;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "amount")
    private BigDecimal amount = BigDecimal.ZERO;
    
    @Column(name = "status", length = 20)
    private String status = "ACTIVE"; // enum 대신 varchar

    public BalanceEntity() {}

    public BalanceEntity(Long userId) {
        this.userId = userId;
    }

    public BalanceEntity(Long id, Long userId, BigDecimal amount, String status) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}