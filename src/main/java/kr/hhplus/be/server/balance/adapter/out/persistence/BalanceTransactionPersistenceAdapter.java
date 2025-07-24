package kr.hhplus.be.server.balance.adapter.out.persistence;

import kr.hhplus.be.server.balance.application.port.out.SaveBalanceTransactionPort;
import kr.hhplus.be.server.balance.domain.BalanceTransaction;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 잔액 거래 영속성 Adapter (Outgoing)
 */
@Component
public class BalanceTransactionPersistenceAdapter implements SaveBalanceTransactionPort {

    private final Map<Long, BalanceTransaction> transactions = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public BalanceTransaction saveBalanceTransaction(BalanceTransaction transaction) {
        if (transaction.getId() == null) {
            transaction.setId(idGenerator.getAndIncrement());
        }
        transactions.put(transaction.getId(), transaction);
        return transaction;
    }
} 