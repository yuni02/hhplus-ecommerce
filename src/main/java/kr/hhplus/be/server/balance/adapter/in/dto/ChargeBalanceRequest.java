package kr.hhplus.be.server.balance.adapter.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;

@Schema(description = "잔액 충전 요청")
public class ChargeBalanceRequest {

    @Schema(description = "사용자 ID", example = "1", required = true)
    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;

    @Schema(description = "충전 금액 (최대 1,000,000원)", example = "10000", required = true)
    @NotNull(message = "충전 금액은 필수입니다.")
    @Positive(message = "충전 금액은 양수여야 합니다.")
    @Max(value = 1000000, message = "1회 최대 충전 금액은 1,000,000원입니다.")
    private Integer amount;

    public ChargeBalanceRequest() {}

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