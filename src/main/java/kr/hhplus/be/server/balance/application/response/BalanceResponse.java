package kr.hhplus.be.server.balance.application.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "잔액 조회 응답")
public class BalanceResponse {

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "현재 잔액", example = "50000")
    private Integer balance;

    public BalanceResponse() {}

    public BalanceResponse(Long userId, Integer balance) {
        this.userId = userId;
        this.balance = balance;
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
} 