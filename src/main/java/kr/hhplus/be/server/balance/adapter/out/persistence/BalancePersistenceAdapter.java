package kr.hhplus.be.server.balance.adapter.out.persistence;

import kr.hhplus.be.server.balance.application.port.out.LoadBalancePort;
import kr.hhplus.be.server.balance.domain.Balance;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 잔액 영속성 Adapter (Outgoing)
 */
@Component
public class BalancePersistenceAdapter implements LoadBalancePort {

    private final Map<Long, Balance> balances = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public BalancePersistenceAdapter() {
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
    public Optional<Balance> loadActiveBalanceByUserId(Long userId) {
        return balances.values().stream()
                .filter(balance -> balance.getUserId().equals(userId) && balance.getStatus() == Balance.BalanceStatus.ACTIVE)
                .findFirst();
    }

    @Override
    public Balance saveBalance(Balance balance) {
        if (balance.getId() == null) {
            balance.setId(idGenerator.getAndIncrement());
        }
        balances.put(balance.getId(), balance);
        return balance;
    }
} 