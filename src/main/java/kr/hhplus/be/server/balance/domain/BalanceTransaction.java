package kr.hhplus.be.server.balance.domain;

import kr.hhplus.be.server.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 사용자 잔액 거래 내역 도메인 엔티티
 * 순수한 비즈니스 로직만 포함 (JPA 어노테이션 없음)
 * 로그성 테이블 (INSERT ONLY, 감사 추적)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceTransaction {

    // 기존 복잡한 생성자 제거
    // 정적 팩토리 메서드 제공
    public static BalanceTransaction create(Long userId, BigDecimal amount, TransactionType type, String description) {
        return BalanceTransaction.builder()
                .userId(userId)
                .amount(amount)
                .type(type)
                .description(description)
                .status(TransactionStatus.COMPLETED)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
    }

    private Long id;    
    private Long userId;
    private BigDecimal amount;
    private TransactionType type;
    
    @Builder.Default
    private TransactionStatus status = TransactionStatus.COMPLETED;
    
    private String description;
    private Long referenceId; // 주문 ID, 쿠폰 ID 등 참조
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private User user;

    public enum TransactionType {
        DEPOSIT, PAYMENT, REFUND, CHARGE
    }

    public enum TransactionStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }
} 