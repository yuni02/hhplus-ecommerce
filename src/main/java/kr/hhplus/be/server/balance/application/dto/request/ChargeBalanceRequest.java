package kr.hhplus.be.server.balance.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "잔액 충전 요청")
public class ChargeBalanceRequest {

    @Schema(description = "사용자 ID", example = "1", required = true)
    private Long userId;

    @Schema(description = "충전 금액 (원)", example = "10000", required = true, minimum = "1", maximum = "1000000")
    private Integer amount;

    public ChargeBalanceRequest() {
    }

    public ChargeBalanceRequest(Long userId, Integer amount) {
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