package kr.hhplus.be.server.unit.balance.domain;

import kr.hhplus.be.server.balance.domain.BalanceTransaction;
import kr.hhplus.be.server.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BalanceTransactionTest {

    @Test
    @DisplayName("BalanceTransaction 생성 성공 - 충전")
    void createBalanceTransaction_Charge() {
        // given
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("10000");
        BalanceTransaction.TransactionType type = BalanceTransaction.TransactionType.CHARGE;
        String description = "잔액 충전";

        // when
        BalanceTransaction transaction = BalanceTransaction.builder()
            .userId(userId)
            .amount(amount)
            .type(type)
            .description(description)
            .build();

        // then
        assertThat(transaction.getUserId()).isEqualTo(userId);
        assertThat(transaction.getAmount()).isEqualTo(amount);
        assertThat(transaction.getType()).isEqualTo(type);
        assertThat(transaction.getDescription()).isEqualTo(description);
        assertThat(transaction.getStatus()).isEqualTo(BalanceTransaction.TransactionStatus.COMPLETED);
    }

    @Test
    @DisplayName("BalanceTransaction 생성 성공 - 차감")
    void createBalanceTransaction_Deduct() {
        // given
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("5000");
        BalanceTransaction.TransactionType type = BalanceTransaction.TransactionType.PAYMENT;
        String description = "주문 결제";

        // when
        BalanceTransaction transaction = BalanceTransaction.create(userId, amount, type, description);

        // then
        assertThat(transaction.getUserId()).isEqualTo(userId);
        assertThat(transaction.getAmount()).isEqualTo(amount);
        assertThat(transaction.getType()).isEqualTo(type);
        assertThat(transaction.getDescription()).isEqualTo(description);
        assertThat(transaction.getStatus()).isEqualTo(BalanceTransaction.TransactionStatus.COMPLETED);
    }

    @Test
    @DisplayName("BalanceTransaction 정적 팩토리 메서드 사용 - DEPOSIT")
    void createBalanceTransaction_StaticFactory_Deposit() {
        // given
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("50000");
        BalanceTransaction.TransactionType type = BalanceTransaction.TransactionType.DEPOSIT;
        String description = "예금 입금";

        // when
        BalanceTransaction transaction = BalanceTransaction.create(userId, amount, type, description);

        // then
        assertThat(transaction.getUserId()).isEqualTo(userId);
        assertThat(transaction.getAmount()).isEqualTo(amount);
        assertThat(transaction.getType()).isEqualTo(type);
        assertThat(transaction.getDescription()).isEqualTo(description);
        assertThat(transaction.getStatus()).isEqualTo(BalanceTransaction.TransactionStatus.COMPLETED);
        assertThat(transaction.getCreatedAt()).isNotNull();
        assertThat(transaction.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("BalanceTransaction 정적 팩토리 메서드 사용 - REFUND")
    void createBalanceTransaction_StaticFactory_Refund() {
        // given
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("15000");
        BalanceTransaction.TransactionType type = BalanceTransaction.TransactionType.REFUND;
        String description = "주문 취소 환불";

        // when
        BalanceTransaction transaction = BalanceTransaction.create(userId, amount, type, description);

        // then
        assertThat(transaction.getUserId()).isEqualTo(userId);
        assertThat(transaction.getAmount()).isEqualTo(amount);
        assertThat(transaction.getType()).isEqualTo(type);
        assertThat(transaction.getDescription()).isEqualTo(description);
        assertThat(transaction.getStatus()).isEqualTo(BalanceTransaction.TransactionStatus.COMPLETED);
    }

    @Test
    @DisplayName("BalanceTransaction Builder로 참조 ID 설정")
    void createBalanceTransaction_WithReferenceId() {
        // given
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("25000");
        BalanceTransaction.TransactionType type = BalanceTransaction.TransactionType.PAYMENT;
        String description = "주문 결제";
        Long referenceId = 100L; // 주문 ID

        // when
        BalanceTransaction transaction = BalanceTransaction.builder()
                .userId(userId)
                .amount(amount)
                .type(type)
                .description(description)
                .referenceId(referenceId)
                .build();

        // then
        assertThat(transaction.getUserId()).isEqualTo(userId);
        assertThat(transaction.getAmount()).isEqualTo(amount);
        assertThat(transaction.getType()).isEqualTo(type);
        assertThat(transaction.getDescription()).isEqualTo(description);
        assertThat(transaction.getReferenceId()).isEqualTo(referenceId);
        assertThat(transaction.getStatus()).isEqualTo(BalanceTransaction.TransactionStatus.COMPLETED);
    }

    @Test
    @DisplayName("BalanceTransaction User 연관 설정")
    void createBalanceTransaction_WithUser() {
        // given
        User user = User.builder()
                .id(1L)
                .name("테스트 사용자")
                .build();
        
        BigDecimal amount = new BigDecimal("10000");
        BalanceTransaction.TransactionType type = BalanceTransaction.TransactionType.CHARGE;
        String description = "포인트 충전";

        // when
        BalanceTransaction transaction = BalanceTransaction.builder()
                .userId(user.getId())
                .amount(amount)
                .type(type)
                .description(description)
                .user(user)
                .build();

        // then
        assertThat(transaction.getUser()).isEqualTo(user);
        assertThat(transaction.getUserId()).isEqualTo(user.getId());
        assertThat(transaction.getAmount()).isEqualTo(amount);
        assertThat(transaction.getType()).isEqualTo(type);
        assertThat(transaction.getDescription()).isEqualTo(description);
    }

    @Test
    @DisplayName("TransactionType enum 검증")
    void transactionTypeEnum() {
        // then
        assertThat(BalanceTransaction.TransactionType.values()).containsExactly(
                BalanceTransaction.TransactionType.DEPOSIT,
                BalanceTransaction.TransactionType.PAYMENT,
                BalanceTransaction.TransactionType.REFUND,
                BalanceTransaction.TransactionType.CHARGE
        );
    }

    @Test
    @DisplayName("TransactionStatus enum 검증")
    void transactionStatusEnum() {
        // then
        assertThat(BalanceTransaction.TransactionStatus.values()).containsExactly(
                BalanceTransaction.TransactionStatus.PENDING,
                BalanceTransaction.TransactionStatus.PROCESSING,
                BalanceTransaction.TransactionStatus.COMPLETED,
                BalanceTransaction.TransactionStatus.FAILED
        );
    }

    @Test
    @DisplayName("BalanceTransaction 다양한 상태로 생성")
    void createBalanceTransaction_DifferentStatuses() {
        // given & when
        BalanceTransaction pendingTransaction = BalanceTransaction.builder()
                .userId(1L)
                .amount(new BigDecimal("10000"))
                .type(BalanceTransaction.TransactionType.CHARGE)
                .description("수동 충전")
                .status(BalanceTransaction.TransactionStatus.PENDING)
                .build();

        BalanceTransaction processingTransaction = BalanceTransaction.builder()
                .userId(1L)
                .amount(new BigDecimal("5000"))
                .type(BalanceTransaction.TransactionType.PAYMENT)
                .description("결제 처리 중")
                .status(BalanceTransaction.TransactionStatus.PROCESSING)
                .build();

        BalanceTransaction failedTransaction = BalanceTransaction.builder()
                .userId(1L)
                .amount(new BigDecimal("3000"))
                .type(BalanceTransaction.TransactionType.REFUND)
                .description("환불 실패")
                .status(BalanceTransaction.TransactionStatus.FAILED)
                .build();

        // then
        assertThat(pendingTransaction.getStatus()).isEqualTo(BalanceTransaction.TransactionStatus.PENDING);
        assertThat(processingTransaction.getStatus()).isEqualTo(BalanceTransaction.TransactionStatus.PROCESSING);
        assertThat(failedTransaction.getStatus()).isEqualTo(BalanceTransaction.TransactionStatus.FAILED);
    }
} 