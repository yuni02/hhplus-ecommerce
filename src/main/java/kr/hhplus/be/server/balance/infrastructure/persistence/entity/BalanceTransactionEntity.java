package kr.hhplus.be.server.balance.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * BalanceTransaction 인프라스트럭처 엔티티
 * BalanceTransaction 도메인 전용 JPA 매핑 엔티티
 * 외래키 제약조건 없이 느슨한 결합으로 설계
 */
@Entity
@Table(name = "user_balance_tx")
@Getter
@Setter(AccessLevel.PRIVATE) // setter는 private으로 제한
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;    

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "tx_type", nullable = false, length = 20)
    private String type; // enum 대신 varchar

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "COMPLETED"; // enum 대신 varchar

    @Column(name = "memo")
    private String description;

    @Column(name = "related_order_id")
    private Long referenceId; // 주문 ID, 쿠폰 ID 등 참조

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}