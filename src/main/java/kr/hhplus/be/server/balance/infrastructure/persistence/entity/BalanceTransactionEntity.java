package kr.hhplus.be.server.balance.infrastructure.persistence.entity;

import jakarta.persistence.*;
import kr.hhplus.be.server.shared.domain.BaseEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 잔액 거래 내역 전용 엔티티
 * INSERT ONLY 테이블로 설계 (감사 추적용)
 */
@Entity
@Table(name = "user_balance_tx")
@Getter
@Setter(AccessLevel.PRIVATE) // setter는 private으로 제한
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceTransactionEntity extends BaseEntity {

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
}