package kr.hhplus.be.server.coupon.infrastructure.persistence.adapter;

import kr.hhplus.be.server.coupon.application.port.out.LoadUserPort;
import kr.hhplus.be.server.user.infrastructure.persistence.entity.UserEntity;
import kr.hhplus.be.server.user.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Component;

/**
 * 사용자 영속성 Adapter (Outgoing) - Coupon 도메인용
 * 실제 데이터베이스를 사용하여 사용자 정보 조회
 */
@Component("couponUserPersistenceAdapter")
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