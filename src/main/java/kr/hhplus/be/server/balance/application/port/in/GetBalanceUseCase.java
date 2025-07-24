package kr.hhplus.be.server.balance.application.port.in;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 잔액 조회 Incoming Port (Use Case)
 */
public interface GetBalanceUseCase {
    
    /**
     * 잔액 조회 실행
     */
    Optional<GetBalanceResult> getBalance(GetBalanceCommand command);
    
    /**
     * 잔액 조회 명령
     */
    class GetBalanceCommand {
        private final Long userId;
        
        public GetBalanceCommand(Long userId) {
            this.userId = userId;
        }
        
        public Long getUserId() {
            return userId;
        }
    }
    
    /**
     * 잔액 조회 결과
     */
    class GetBalanceResult {
        private final Long userId;
        private final BigDecimal balance;
        
        public GetBalanceResult(Long userId, BigDecimal balance) {
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