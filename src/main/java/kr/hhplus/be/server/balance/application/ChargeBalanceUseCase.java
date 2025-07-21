package kr.hhplus.be.server.balance.application;

import kr.hhplus.be.server.balance.domain.BalanceService;
import kr.hhplus.be.server.balance.domain.BalanceChargeResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 잔액 충전 UseCase
 * 외부 의존성 없이 도메인 서비스만 호출
 */
@Component
public class ChargeBalanceUseCase {

    private final BalanceService balanceService;

    public ChargeBalanceUseCase(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    public Output execute(Input input) {
        // BalanceService를 통한 잔액 충전
        BalanceChargeResult result = balanceService.chargeBalance(input.userId, input.amount);
        
        if (!result.isSuccess()) {
            throw new IllegalArgumentException(result.getErrorMessage());
        }
        
        return new Output(result.getUserId(), result.getNewBalance(), result.getTransactionId());
    }

    public static class Input {
        private final Long userId;
        private final BigDecimal amount;

        public Input(Long userId, BigDecimal amount) {
            this.userId = userId;
            this.amount = amount;
        }

        public Long getUserId() {
            return userId;
        }

        public BigDecimal getAmount() {
            return amount;
        }
    }

    public static class Output {
        private final Long userId;
        private final BigDecimal balance;
        private final Long transactionId;

        public Output(Long userId, BigDecimal balance, Long transactionId) {
            this.userId = userId;
            this.balance = balance;
            this.transactionId = transactionId;
        }

        public Long getUserId() {
            return userId;
        }

        public BigDecimal getBalance() {
            return balance;
        }

        public Long getTransactionId() {
            return transactionId;
        }
    }
} 