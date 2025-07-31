package kr.hhplus.be.server.balance.application.port.in;

import java.math.BigDecimal;

/**
 * 잔액 충전 Incoming Port (Use Case)
 */
public interface ChargeBalanceUseCase {
    
    /**
     * 잔액 충전 실행
     */
    ChargeBalanceResult chargeBalance(ChargeBalanceCommand command);
    
    /**
     * 잔액 충전 명령
     */
    class ChargeBalanceCommand {
        private final Long userId;
        private final BigDecimal amount;
        
        public ChargeBalanceCommand(Long userId, BigDecimal amount) {
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
    
    /**
     * 잔액 충전 결과
     */
    class ChargeBalanceResult {
        private final boolean success;
        private final Long userId;
        private final BigDecimal newBalance;
        private final Long transactionId;
        private final String errorMessage;
        
        private ChargeBalanceResult(boolean success, Long userId, BigDecimal newBalance, 
                                  Long transactionId, String errorMessage) {
            this.success = success;
            this.userId = userId;
            this.newBalance = newBalance;
            this.transactionId = transactionId;
            this.errorMessage = errorMessage;
        }
        
        public static ChargeBalanceResult success(Long userId, BigDecimal newBalance, Long transactionId) {
            return new ChargeBalanceResult(true, userId, newBalance, transactionId, null);
        }
        
        public static ChargeBalanceResult failure(String errorMessage) {
            return new ChargeBalanceResult(false, null, null, null, errorMessage);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public BigDecimal getNewBalance() {
            return newBalance;
        }
        
        public Long getTransactionId() {
            return transactionId;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }

        public Object getChargeAmount() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getChargeAmount'");
        }
    }
} 