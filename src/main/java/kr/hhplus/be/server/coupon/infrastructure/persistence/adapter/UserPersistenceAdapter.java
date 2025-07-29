package kr.hhplus.be.server.coupon.infrastructure.persistence.adapter;

import kr.hhplus.be.server.coupon.application.port.out.LoadUserPort;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 사용자 영속성 Adapter (Outgoing) - Coupon 도메인용
 */
@Component("couponUserPersistenceAdapter")
public class UserPersistenceAdapter implements LoadUserPort {

    private final Map<Long, UserData> users = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public UserPersistenceAdapter() {
        // 더미 데이터 초기화
        initializeDummyData();
    }

    private void initializeDummyData() {
        // 사용자 데이터 초기화
        for (long userId = 1; userId <= 3; userId++) {
            UserData user = new UserData(
                    userId,
                    "사용자 " + userId,
                    "user" + userId + "@example.com",
                    "010-1234-567" + userId,
                    "ACTIVE"
            );
            users.put(userId, user);
        }
    }

    @Override
    public boolean existsById(Long userId) {
        return users.containsKey(userId);
    }

    /**
     * 사용자 데이터 내부 클래스
     */
    private static class UserData {
        private Long id;
        private String name;
        private String email;
        private String phoneNumber;
        private String status;

        public UserData(Long id, String name, String email, String phoneNumber, String status) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.phoneNumber = phoneNumber;
            this.status = status;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
} 