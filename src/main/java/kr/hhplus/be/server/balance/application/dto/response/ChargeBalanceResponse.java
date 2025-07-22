package kr.hhplus.be.server.balance.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "잔액 충전 응답")
public class ChargeBalanceResponse {

    @Schema(description = "응답 메시지", example = "잔액 충전이 완료되었습니다.")
    private String message;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "충전 후 잔액", example = "50000")
    private BigDecimal balance;

    @Schema(description = "거래 ID", example = "12345")
    private Long transactionId;

    public ChargeBalanceResponse() {}

    public ChargeBalanceResponse(String message, Long userId, BigDecimal balance, Long transactionId) {
        this.message = message;
        this.userId = userId;
        this.balance = balance;
        this.transactionId = transactionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }
} 