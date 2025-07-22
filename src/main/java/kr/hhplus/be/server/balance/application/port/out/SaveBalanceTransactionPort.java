package kr.hhplus.be.server.balance.application.port.out;

import kr.hhplus.be.server.balance.domain.BalanceTransaction;

/**
 * 잔액 거래 저장 Outgoing Port
 */
public interface SaveBalanceTransactionPort {
    
    /**
     * 잔액 거래 저장
     */
    BalanceTransaction saveBalanceTransaction(BalanceTransaction transaction);
} 