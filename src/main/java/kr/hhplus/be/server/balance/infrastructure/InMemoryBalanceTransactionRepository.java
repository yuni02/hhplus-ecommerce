package kr.hhplus.be.server.balance.infrastructure;

import kr.hhplus.be.server.balance.domain.BalanceTransaction;
import kr.hhplus.be.server.balance.domain.BalanceTransactionRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryBalanceTransactionRepository implements BalanceTransactionRepository {

    private final Map<Long, BalanceTransaction> transactions = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public List<BalanceTransaction> findByUserIdOrderByCreatedAtDesc(Long userId) {
        return transactions.values().stream()
                .filter(transaction -> transaction.getUserId().equals(userId))
                .sorted(Comparator.comparing(BalanceTransaction::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<BalanceTransaction> findCompletedTransactionsByUserId(Long userId) {
        return transactions.values().stream()
                .filter(transaction -> transaction.getUserId().equals(userId) 
                        && transaction.getStatus() == BalanceTransaction.TransactionStatus.COMPLETED)
                .sorted(Comparator.comparing(BalanceTransaction::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public BalanceTransaction save(BalanceTransaction transaction) {
        if (transaction.getId() == null) {
            transaction.setId(idGenerator.getAndIncrement());
        }
        transactions.put(transaction.getId(), transaction);
        return transaction;
    }

    @Override
    public Optional<BalanceTransaction> findById(Long id) {
        return Optional.ofNullable(transactions.get(id));
    }
} 