package kr.hhplus.be.server.balance.infrastructure.persistence.adapter;

import kr.hhplus.be.server.balance.application.port.out.LoadUserPort;
import kr.hhplus.be.server.user.infrastructure.persistence.repository.UserJpaRepository;

import org.springframework.stereotype.Component;

/**
 * User 영속성 Adapter (Balance 도메인용)
 * Balance 도메인에서 사용자 존재 여부 확인을 위한 어댑터
 */
@Component("balanceUserPersistenceAdapter")
public class UserPersistenceAdapter implements LoadUserPort {

    private final UserJpaRepository userJpaRepository;

    public UserPersistenceAdapter(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public boolean existsById(Long userId) {
        // userId로 조회하도록 수정 (기본 키가 아닌 userId 필드로 조회)
        return userJpaRepository.findByUserIdAndStatus(userId, "ACTIVE").isPresent();
    }
}