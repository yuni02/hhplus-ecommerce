package kr.hhplus.be.server.balance.adapter.out.persistence;

import kr.hhplus.be.server.balance.domain.BalanceTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * BalanceTransaction 엔티티 JPA Repository
 */
@Repository
public interface BalanceTransactionJpaRepository extends JpaRepository<BalanceTransaction, Long> {

    /**
     * 사용자별 거래 내역 조회
     */
    List<BalanceTransaction> findByUserId(Long userId);

    /**
     * 사용자별 특정 거래 타입 조회
     */
    List<BalanceTransaction> findByUserIdAndType(Long userId, BalanceTransaction.TransactionType type);

    /**
     * 사용자별 특정 상태의 거래 조회
     */
    List<BalanceTransaction> findByUserIdAndStatus(Long userId, BalanceTransaction.TransactionStatus status);

    /**
     * 특정 기간 내 거래 내역 조회
     */
    List<BalanceTransaction> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long userId, 
                                                                                LocalDateTime startDate, 
                                                                                LocalDateTime endDate);

    /**
     * 주문 관련 거래 조회
     */
    List<BalanceTransaction> findByReferenceIdAndType(Long orderId, BalanceTransaction.TransactionType type);

    /**
     * 사용자의 최근 거래 내역 조회
     */
    List<BalanceTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
} 