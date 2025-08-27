package kr.hhplus.be.server.balance.infrastructure.persistence.repository;

import kr.hhplus.be.server.balance.infrastructure.persistence.entity.BalanceTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * BalanceTransaction 엔티티 JPA Repository
 * Balance 도메인 전용 데이터 접근 계층
 */
@Repository
public interface BalanceTransactionJpaRepository extends JpaRepository<BalanceTransactionEntity, Long> {


    /**
     * 사용자별 특정 상태의 거래 조회
     */
    List<BalanceTransactionEntity> findByUserIdAndStatus(Long userId, String status);

}