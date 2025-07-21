package kr.hhplus.be.server.balance.infrastructure;

import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.domain.BalanceRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryBalanceRepository implements BalanceRepository {

    private final Map<Long, Balance> balances = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public InMemoryBalanceRepository() {
        // 더미 데이터 초기화
        initializeDummyData();
    }

    private void initializeDummyData() {
        // 사용자별 잔액 초기화
        for (long userId = 1; userId <= 3; userId++) {
            Balance balance = new Balance(userId);
            balance.setId(idGenerator.getAndIncrement());
            balance.setAmount(BigDecimal.valueOf(10000)); // 초기 잔액 10,000원
            balances.put(balance.getId(), balance);
        }
    }

    @Override
    public Optional<Balance> findByUserIdAndStatus(Long userId, Balance.BalanceStatus status) {
        return balances.values().stream()
                .filter(balance -> balance.getUserId().equals(userId) && balance.getStatus() == status)
                .findFirst();
    }

    @Override
    public Optional<Balance> findActiveBalanceByUserId(Long userId) {
        return findByUserIdAndStatus(userId, Balance.BalanceStatus.ACTIVE);
    }

    @Override
    public Balance save(Balance balance) {
        if (balance.getId() == null) {
            balance.setId(idGenerator.getAndIncrement());
        }
        balances.put(balance.getId(), balance);
        return balance;
    }

    @Override
    public Optional<Balance> findById(Long id) {
        return Optional.ofNullable(balances.get(id));
    }
} 