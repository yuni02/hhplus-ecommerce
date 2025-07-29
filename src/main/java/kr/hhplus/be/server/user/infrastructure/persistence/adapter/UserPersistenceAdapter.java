package kr.hhplus.be.server.user.infrastructure.persistence.adapter;

import kr.hhplus.be.server.user.application.port.out.LoadUserPort;
import kr.hhplus.be.server.user.infrastructure.persistence.entity.UserEntity;
import kr.hhplus.be.server.user.infrastructure.persistence.repository.UserJpaRepository;

import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * User 인프라스트럭처 영속성 Adapter
 * User 도메인 전용 데이터 접근
 */
@Component("userUserPersistenceAdapter")
public class UserPersistenceAdapter implements LoadUserPort {

    private final UserJpaRepository userJpaRepository;

    public UserPersistenceAdapter(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public Optional<LoadUserPort.UserInfo> loadUserById(Long userId) {
        return userJpaRepository.findById(userId)
                .map(this::mapToUserInfo);
    }

    @Override
    public boolean existsById(Long userId) {
        return userJpaRepository.existsById(userId);
    }

    /**
     * UserEntity를 UserInfo로 변환
     */
    private LoadUserPort.UserInfo mapToUserInfo(UserEntity entity) {
        return new LoadUserPort.UserInfo(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPhoneNumber(),
                entity.getStatus()
        );
    }
}