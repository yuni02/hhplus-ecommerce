package kr.hhplus.be.server.order.adapter.out.persistence;

import org.springframework.stereotype.Component;

import kr.hhplus.be.server.order.application.port.out.LoadUserPort;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 사용자 영속성 Adapter (Outgoing)
 */
@Component("orderUserPersistenceAdapter")
public class UserPersistenceAdapter implements LoadUserPort {

    private final Map<Long, Object> users = new ConcurrentHashMap<>();

    public UserPersistenceAdapter() {
        // 더미 데이터 초기화
        initializeDummyData();
    }

    private void initializeDummyData() {
        // 사용자 데이터 초기화
        for (long userId = 1; userId <= 3; userId++) {
            users.put(userId, new Object()); // 실제로는 User 엔티티
        }
    }

    @Override
    public boolean existsById(Long userId) {
        return users.containsKey(userId);
    }
} 