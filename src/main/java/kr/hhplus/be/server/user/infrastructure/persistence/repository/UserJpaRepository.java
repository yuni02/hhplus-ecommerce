package kr.hhplus.be.server.user.infrastructure.persistence.repository;

import kr.hhplus.be.server.user.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User 엔티티 JPA Repository
 * User 도메인과 Balance 도메인을 통합한 데이터 접근 계층
 */
@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    
    /**
     * 사용자 ID와 상태로 조회 (잔액 조회용)
     */
    Optional<UserEntity> findByUserIdAndStatus(Long userId, String status);
}