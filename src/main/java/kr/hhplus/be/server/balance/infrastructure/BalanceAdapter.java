package kr.hhplus.be.server.balance.infrastructure;

import kr.hhplus.be.server.balance.application.ChargeBalanceUseCase;
import kr.hhplus.be.server.balance.application.GetBalanceUseCase;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class BalanceAdapter {

    public ChargeBalanceUseCase.Input adaptChargeRequest(Map<String, Object> request) {
        // 입력값 검증
        Long userId = Long.valueOf(request.get("userId").toString());
        Integer amount = Integer.valueOf(request.get("amount").toString());
        
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 양수여야 합니다.");
        }
        if (amount > 1000000) {
            throw new IllegalArgumentException("1회 최대 충전 금액은 1,000,000원입니다.");
        }

        return new ChargeBalanceUseCase.Input(userId, BigDecimal.valueOf(amount));
    }

    public Map<String, Object> adaptChargeResponse(ChargeBalanceUseCase.Output output) {
        return Map.of(
                "message", "잔액 충전이 완료되었습니다.",
                "userId", output.getUserId(),
                "balance", output.getBalance(),
                "transactionId", output.getTransactionId());
    }

    public GetBalanceUseCase.Input adaptGetBalanceRequest(Long userId) {
        // 입력값 검증
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }

        return new GetBalanceUseCase.Input(userId);
    }

    public Map<String, Object> adaptGetBalanceResponse(GetBalanceUseCase.Output output) {
        return Map.of(
                "userId", output.getUserId(),
                "balance", output.getBalance());
    }
} 