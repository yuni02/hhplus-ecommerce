package kr.hhplus.be.server.order.infrastructure.persistence.adapter;

import kr.hhplus.be.server.order.application.port.out.LoadUserPort;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.infrastructure.persistence.entity.UserEntity;
import kr.hhplus.be.server.user.infrastructure.persistence.repository.UserJpaRepository;

import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * User 영속성 Adapter (Order 도메인용)
 * Order 도메인에서 사용자 존재 여부 확인을 위한 어댑터
 */
@Component("orderUserPersistenceAdapter")
public class UserPersistenceAdapter implements LoadUserPort {

    private final UserJpaRepository userJpaRepository;

    public UserPersistenceAdapter(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public boolean existsById(Long userId) {
        return userJpaRepository.existsById(userId);
    }

    @Override
    public Optional<User> loadUserById(Long userId) {
        return userJpaRepository.findById(userId)
                .map(this::mapToDomain);
    }

    /**
     * UserEntity를 User 도메인 객체로 변환
     */
    private User mapToDomain(UserEntity entity) {
        User user = User.builder()
            .userId(entity.getUserId())
            .username(entity.getUsername())
            .status(User.UserStatus.valueOf(entity.getStatus()))
            .build();
        user.setId(entity.getId());
        user.setCreatedAt(entity.getCreatedAt());
        user.setUpdatedAt(entity.getUpdatedAt());
        return user;
    }
}