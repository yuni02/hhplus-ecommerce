package kr.hhplus.be.server.balance.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class BalanceHistoryResponse {

    private final List<TransactionHistory> transactions;

    public BalanceHistoryResponse(List<TransactionHistory> transactions) {
        this.transactions = transactions;
    }

    public List<TransactionHistory> getTransactions() {
        return transactions;
    }

    public static class TransactionHistory {
        private final Long transactionId;
        private final String txType;
        private final Integer amount;
        private final String status;
        private final String memo;
        private final LocalDateTime timestamp;

        public TransactionHistory(Long transactionId, String txType, Integer amount,
                String status, String memo, LocalDateTime timestamp) {
            this.transactionId = transactionId;
            this.txType = txType;
            this.amount = amount;
            this.status = status;
            this.memo = memo;
            this.timestamp = timestamp;
        }

        public Long getTransactionId() {
            return transactionId;
        }

        public String getTxType() {
            return txType;
        }

        public Integer getAmount() {
            return amount;
        }

        public String getStatus() {
            return status;
        }

        public String getMemo() {
            return memo;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }
} 