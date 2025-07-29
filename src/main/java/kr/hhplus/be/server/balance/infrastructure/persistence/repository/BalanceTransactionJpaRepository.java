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
     * 사용자별 거래 내역 조회
     */
    List<BalanceTransactionEntity> findByUserId(Long userId);

    /**
     * 사용자별 특정 거래 타입 조회
     */
    List<BalanceTransactionEntity> findByUserIdAndType(Long userId, String type);

    /**
     * 사용자별 특정 상태의 거래 조회
     */
    List<BalanceTransactionEntity> findByUserIdAndStatus(Long userId, String status);

    /**
     * 특정 기간 내 거래 내역 조회
     */
    List<BalanceTransactionEntity> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long userId, 
                                                                                      LocalDateTime startDate, 
                                                                                      LocalDateTime endDate);

    /**
     * 주문 관련 거래 조회
     */
    List<BalanceTransactionEntity> findByReferenceIdAndType(Long orderId, String type);

    /**
     * 사용자의 최근 거래 내역 조회
     */
    List<BalanceTransactionEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}