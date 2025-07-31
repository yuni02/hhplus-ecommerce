package kr.hhplus.be.server.balance.infrastructure.persistence.repository;

import kr.hhplus.be.server.balance.infrastructure.persistence.entity.BalanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

/**
 * Balance 엔티티 JPA Repository
 * 잔액 전용 데이터 접근 계층
 */
@Repository
public interface BalanceJpaRepository extends JpaRepository<BalanceEntity, Long> {

    /**
     * 사용자 ID로 활성 잔액 조회
     */
    Optional<BalanceEntity> findByUserIdAndStatus(Long userId, String status);

    /**
     * 사용자 ID로 잔액 조회 (락 없음)
     */
    Optional<BalanceEntity> findByUserId(Long userId);

    /**
     * 사용자 ID로 잔액 조회 (Pessimistic Lock)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BalanceEntity b WHERE b.userId = :userId AND b.status = :status")
    Optional<BalanceEntity> findByUserIdAndStatusWithLock(
        @Param("userId") Long userId, 
        @Param("status") String status
    );

    /**
     * 사용자 ID로 잔액 존재 여부 확인
     */
    boolean existsByUserIdAndStatus(Long userId, String status);
} 