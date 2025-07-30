package kr.hhplus.be.server.balance.domain;

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
    @DisplayName("BalanceTransaction ID 설정")
    void setBalanceTransactionId() {
        // given
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("10000");
        BalanceTransaction.TransactionType type = BalanceTransaction.TransactionType.CHARGE;
        String description = "잔액 충전";
        BalanceTransaction transaction = BalanceTransaction.create(userId, amount, type, description);
        
        Long transactionId = 1L;

        // when
        transaction.setId(transactionId);

        // then
        assertThat(transaction.getId()).isEqualTo(transactionId);
    }

    @Test
    @DisplayName("BalanceTransaction 상태 변경")
    void changeBalanceTransactionStatus() {
        // given
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("10000");
        BalanceTransaction.TransactionType type = BalanceTransaction.TransactionType.CHARGE;
        String description = "잔액 충전";
        BalanceTransaction transaction = BalanceTransaction.create(userId, amount, type, description);

        // when
        transaction.setStatus(BalanceTransaction.TransactionStatus.FAILED);

        // then
        assertThat(transaction.getStatus()).isEqualTo(BalanceTransaction.TransactionStatus.FAILED);
    }

    @Test
    @DisplayName("BalanceTransaction 생성 시간 설정")
    void setBalanceTransactionCreatedAt() {
        // given
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("10000");
        BalanceTransaction.TransactionType type = BalanceTransaction.TransactionType.CHARGE;
        String description = "잔액 충전";   
        BalanceTransaction transaction = BalanceTransaction.create(userId, amount, type, description);
        
        LocalDateTime now = LocalDateTime.now();

        // when
        transaction.setCreatedAt(now);

        // then
        assertThat(transaction.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("BalanceTransaction 기본 생성자")
    void createBalanceTransaction_DefaultConstructor() {
        // when
        BalanceTransaction transaction = new BalanceTransaction();

        // then
        assertThat(transaction.getUserId()).isNull();
        assertThat(transaction.getAmount()).isNull();
        assertThat(transaction.getType()).isNull();
        assertThat(transaction.getDescription()).isNull();
        assertThat(transaction.getStatus()).isEqualTo(BalanceTransaction.TransactionStatus.COMPLETED);
    }

    @Test
    @DisplayName("BalanceTransaction 모든 필드 설정")
    void setBalanceTransactionAllFields() {
        // given
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("10000");
        BalanceTransaction.TransactionType type = BalanceTransaction.TransactionType.CHARGE;
        String description = "잔액 충전";
        BalanceTransaction.TransactionStatus status = BalanceTransaction.TransactionStatus.COMPLETED;
        LocalDateTime createdAt = LocalDateTime.now();
        
        BalanceTransaction transaction = BalanceTransaction.builder()
            .userId(userId)
            .amount(amount)
            .type(type)
            .description(description)
            .status(status)
            .createdAt(createdAt)
            .build();

        // when
        transaction.setUserId(userId);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setDescription(description);
        transaction.setStatus(status);
        transaction.setCreatedAt(createdAt);

        // then
        assertThat(transaction.getUserId()).isEqualTo(userId);
        assertThat(transaction.getAmount()).isEqualTo(amount);
        assertThat(transaction.getType()).isEqualTo(type);
        assertThat(transaction.getDescription()).isEqualTo(description);
        assertThat(transaction.getStatus()).isEqualTo(status);
        assertThat(transaction.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("BalanceTransaction 타입별 검증")
    void validateBalanceTransactionTypes() {
        // given & when & then
        assertThat(BalanceTransaction.TransactionType.CHARGE).isNotNull();
        assertThat(BalanceTransaction.TransactionType.PAYMENT).isNotNull();
        assertThat(BalanceTransaction.TransactionType.REFUND).isNotNull();
        assertThat(BalanceTransaction.TransactionType.DEPOSIT).isNotNull();
        assertThat(BalanceTransaction.TransactionType.CHARGE).isNotNull();  // 잔액 충전 타입   
        
        assertThat(BalanceTransaction.TransactionStatus.COMPLETED).isNotNull();
        assertThat(BalanceTransaction.TransactionStatus.FAILED).isNotNull();
        assertThat(BalanceTransaction.TransactionStatus.PENDING).isNotNull();
    }
} 