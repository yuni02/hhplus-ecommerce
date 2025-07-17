package kr.hhplus.be.server.dto.request;

public class BalanceChargeRequest {
    private Long userId;
    private Integer amount;

    public BalanceChargeRequest() {
    }

    public BalanceChargeRequest(Long userId, Integer amount) {
        this.userId = userId;
        this.amount = amount;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }
}