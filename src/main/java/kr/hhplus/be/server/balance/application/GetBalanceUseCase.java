package kr.hhplus.be.server.balance.application;

import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.domain.BalanceRepository;
import kr.hhplus.be.server.user.domain.UserRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 잔액 조회 UseCase
 * 외부 의존성 없이 도메인 서비스만 호출
 */
@Component
public class GetBalanceUseCase {

    private final BalanceRepository balanceRepository;
    private final UserRepository userRepository;

    public GetBalanceUseCase(BalanceRepository balanceRepository, UserRepository userRepository) {
        this.balanceRepository = balanceRepository;
        this.userRepository = userRepository;
    }

    public Optional<Output> execute(Input input) {
        // 사용자 존재 확인
        if (!userRepository.existsById(input.userId)) {
            return Optional.empty();
        }
        
        return balanceRepository.findActiveBalanceByUserId(input.userId)
                .map(balance -> new Output(balance.getUserId(), balance.getAmount()));
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