package kr.hhplus.be.server.balance.infrastructure.persistence.repository;

import kr.hhplus.be.server.balance.infrastructure.persistence.entity.BalanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Balance 엔티티 JPA Repository
 * Balance 도메인 전용 데이터 접근 계층
 */
@Repository
public interface BalanceJpaRepository extends JpaRepository<BalanceEntity, Long> {

    /**
     * 사용자 ID로 활성 잔액 조회
     */
    Optional<BalanceEntity> findByUserIdAndStatus(Long userId, String status);

    /**
     * 사용자 ID로 잔액 조회
     */
    Optional<BalanceEntity> findByUserId(Long userId);
}