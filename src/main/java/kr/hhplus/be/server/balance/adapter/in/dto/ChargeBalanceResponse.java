package kr.hhplus.be.server.balance.adapter.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "잔액 충전 응답")
public class ChargeBalanceResponse {
    
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "충전 금액", example = "10000")
    private Integer chargeAmount;

    @Schema(description = "충전 후 잔액", example = "60000")
    private Integer balanceAfterCharge;

    public ChargeBalanceResponse() {}

    public ChargeBalanceResponse(Long userId, Integer chargeAmount, Integer balanceAfterCharge) {
        this.userId = userId;
        this.chargeAmount = chargeAmount;
        this.balanceAfterCharge = balanceAfterCharge;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getChargeAmount() {
        return chargeAmount;
    }

    public void setChargeAmount(Integer chargeAmount) {
        this.chargeAmount = chargeAmount;
    }

    public Integer getBalanceAfterCharge() {
        return balanceAfterCharge;
    }

    public void setBalanceAfterCharge(Integer balanceAfterCharge) {
        this.balanceAfterCharge = balanceAfterCharge;
    }
} 