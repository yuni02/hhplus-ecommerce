package kr.hhplus.be.server.order.adapter.out.persistence;

import org.springframework.stereotype.Component;

import kr.hhplus.be.server.order.application.port.out.DeductBalancePort;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 잔액 영속성 Adapter (Outgoing)
 */
@Component("orderBalancePersistenceAdapter")
public class BalancePersistenceAdapter implements DeductBalancePort {

    private final Map<Long, BalanceData> balances = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public BalancePersistenceAdapter() {
        // 더미 데이터 초기화
        initializeDummyData();
    }

    private void initializeDummyData() {
        // 사용자별 잔액 초기화
        for (long userId = 1; userId <= 3; userId++) {
            BalanceData balance = new BalanceData(userId, BigDecimal.valueOf(50000)); // 초기 잔액 50,000원
            balances.put(userId, balance);
        }
    }

    @Override
    public boolean deductBalance(Long userId, BigDecimal amount) {
        BalanceData balance = balances.get(userId);
        if (balance == null || balance.getAmount().compareTo(amount) < 0) {
            return false;
        }
        
        balance.setAmount(balance.getAmount().subtract(amount));
        return true;
    }

    /**
     * 잔액 데이터 내부 클래스
     */
    private static class BalanceData {
        private Long userId;
        private BigDecimal amount;

        public BalanceData(Long userId, BigDecimal amount) {
            this.userId = userId;
            this.amount = amount;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }
} 