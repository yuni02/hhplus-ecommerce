package kr.hhplus.be.server.user.infrastructure;

import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.domain.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryUserRepository implements UserRepository {

    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public InMemoryUserRepository() {
        // 더미 데이터 초기화
        initializeDummyData();
    }

    private void initializeDummyData() {
        // 사용자 1
        User user1 = new User("김철수", "kim@example.com");
        user1.setId(idGenerator.getAndIncrement());
        users.put(user1.getId(), user1);

        // 사용자 2
        User user2 = new User("이영희", "lee@example.com");
        user2.setId(idGenerator.getAndIncrement());
        users.put(user2.getId(), user2);

        // 사용자 3
        User user3 = new User("박민수", "park@example.com");
        user3.setId(idGenerator.getAndIncrement());
        users.put(user3.getId(), user3);
    }

    @Override
    public Optional<User> findByIdAndStatus(Long id, User.UserStatus status) {
        User user = users.get(id);
        if (user != null && user.getStatus() == status) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return users.values().stream()
                .filter(user -> user.getEmail().equals(email))
                .findFirst();
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            user.setId(idGenerator.getAndIncrement());
        }
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public boolean existsById(Long id) {
        return users.containsKey(id);
    }
} 