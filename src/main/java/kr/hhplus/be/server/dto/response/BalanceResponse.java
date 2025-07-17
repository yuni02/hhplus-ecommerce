package kr.hhplus.be.server.dto.response;

public class BalanceResponse {
    private Long userId;
    private Integer balance;
    private Long transactionId;

    public BalanceResponse() {
    }

    public BalanceResponse(Long userId, Integer balance) {
        this.userId = userId;
        this.balance = balance;
    }

    public BalanceResponse(Long userId, Integer balance, Long transactionId) {
        this.userId = userId;
        this.balance = balance;
        this.transactionId = transactionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getBalance() {
        return balance;
    }

    public void setBalance(Integer balance) {
        this.balance = balance;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }
}
