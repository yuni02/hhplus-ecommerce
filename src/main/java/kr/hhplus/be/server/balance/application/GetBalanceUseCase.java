package kr.hhplus.be.server.balance.application;

import kr.hhplus.be.server.balance.domain.BalanceService;
import kr.hhplus.be.server.balance.domain.BalanceQueryResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 잔액 조회 UseCase
 * 외부 의존성 없이 도메인 서비스만 호출
 */
@Component
public class GetBalanceUseCase {

    private final BalanceService balanceService;

    public GetBalanceUseCase(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    public Optional<Output> execute(Input input) {
        // BalanceService를 통한 잔액 조회
        BalanceQueryResult result = balanceService.getBalance(input.userId);
        
        if (!result.isFound()) {
            return Optional.empty();
        }
        
        return Optional.of(new Output(result.getUserId(), result.getBalance()));
    }

    public static class Input {
        private final Long userId;

        public Input(Long userId) {
            this.userId = userId;
        }

        public Long getUserId() {
            return userId;
        }
    }

    public static class Output {
        private final Long userId;
        private final BigDecimal balance;

        public Output(Long userId, BigDecimal balance) {
            this.userId = userId;
            this.balance = balance;
        }

        public Long getUserId() {
            return userId;
        }

        public BigDecimal getBalance() {
            return balance;
        }
    }
} 