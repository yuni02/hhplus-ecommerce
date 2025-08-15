package kr.hhplus.be.server.balance.infrastructure.persistence.adapter;

import kr.hhplus.be.server.balance.application.port.out.LoadBalancePort;
import kr.hhplus.be.server.balance.application.port.out.SaveBalanceTransactionPort;
import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.domain.BalanceTransaction;
import kr.hhplus.be.server.balance.infrastructure.persistence.entity.BalanceEntity;
import kr.hhplus.be.server.balance.infrastructure.persistence.entity.BalanceTransactionEntity;
import kr.hhplus.be.server.balance.infrastructure.persistence.repository.BalanceJpaRepository;
import kr.hhplus.be.server.balance.infrastructure.persistence.repository.BalanceTransactionJpaRepository;
import kr.hhplus.be.server.user.infrastructure.persistence.entity.UserEntity;
import kr.hhplus.be.server.user.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Balance 인프라스트럭처 영속성 Adapter
 * 잔액 전용 데이터 접근
 */
@Component
public class BalancePersistenceAdapter implements LoadBalancePort, SaveBalanceTransactionPort {

    private final BalanceJpaRepository balanceJpaRepository;
    private final BalanceTransactionJpaRepository balanceTransactionJpaRepository;
    private final UserJpaRepository userJpaRepository;

    public BalancePersistenceAdapter(BalanceJpaRepository balanceJpaRepository,
                                   BalanceTransactionJpaRepository balanceTransactionJpaRepository,
                                   UserJpaRepository userJpaRepository) {
        this.balanceJpaRepository = balanceJpaRepository;
        this.balanceTransactionJpaRepository = balanceTransactionJpaRepository;
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public Optional<Balance> loadActiveBalanceByUserId(Long userId) {
        return balanceJpaRepository.findByUserIdAndStatus(userId, "ACTIVE")
                .map(this::mapToBalance);
    }

    /**
     * 동시성 제어를 위한 잔액 조회 (Pessimistic Lock 사용)
     */
    @Override
    @Transactional
    public Optional<Balance> loadActiveBalanceByUserIdWithLock(Long userId) {
        return balanceJpaRepository.findByUserIdAndStatusWithLock(userId, "ACTIVE")
                .map(this::mapToBalance);
    }

    @Override
    @Transactional
    public Balance saveBalance(Balance balance) {
        // 기존 잔액이 있는지 확인 (락 없이)
        Optional<BalanceEntity> existingEntity = balanceJpaRepository.findByUserIdAndStatus(balance.getUserId(), "ACTIVE");
        
        BalanceEntity entity;
        if (existingEntity.isPresent()) {
            // 기존 엔티티 업데이트
            entity = existingEntity.get();
            entity.updateAmount(balance.getAmount());
            entity = balanceJpaRepository.save(entity);
        } else {
            // 새로운 엔티티 생성
            entity = mapToBalanceEntity(balance);
            entity = balanceJpaRepository.save(entity);
        }
        
        return mapToBalance(entity);
    }

    @Override
    @Transactional
    public BalanceTransaction saveBalanceTransaction(BalanceTransaction transaction) {
        BalanceTransactionEntity entity = mapToBalanceTransactionEntity(transaction);
        BalanceTransactionEntity savedEntity = balanceTransactionJpaRepository.save(entity);
        return mapToBalanceTransaction(savedEntity);
    }

    /**
     * BalanceEntity를 Balance 도메인 객체로 변환
     */
    private Balance mapToBalance(BalanceEntity entity) {
        return new Balance(
                entity.getId(),
                entity.getUserId(),
                entity.getAmount(),
                Balance.BalanceStatus.valueOf(entity.getStatus())
        );
    }

    /**
     * Balance 도메인 객체를 BalanceEntity로 변환
     */
    private BalanceEntity mapToBalanceEntity(Balance balance) {
        // UserEntity 조회
        UserEntity userEntity = userJpaRepository.findByUserIdAndStatus(balance.getUserId(), "ACTIVE")
                .orElseThrow(() -> new IllegalArgumentException("User not found with userId: " + balance.getUserId()));
        
        return BalanceEntity.builder()
                .user(userEntity)  // user 관계를 통해 userId 설정
                .amount(balance.getAmount())
                .status(balance.getStatus().name())
                .build();
    }

    /**
     * BalanceTransactionEntity를 BalanceTransaction 도메인 객체로 변환
     */
    private BalanceTransaction mapToBalanceTransaction(BalanceTransactionEntity entity) {
        BalanceTransaction transaction = new BalanceTransaction();
        transaction.setId(entity.getId());
        transaction.setUserId(entity.getUserId());
        transaction.setAmount(entity.getAmount());
        transaction.setType(BalanceTransaction.TransactionType.valueOf(entity.getType()));
        transaction.setStatus(BalanceTransaction.TransactionStatus.valueOf(entity.getStatus()));
        transaction.setDescription(entity.getDescription());
        transaction.setReferenceId(entity.getReferenceId());
        transaction.setCreatedAt(entity.getCreatedAt());
        transaction.setUpdatedAt(entity.getUpdatedAt());
        return transaction;
    }

    /**
     * BalanceTransaction 도메인 객체를 BalanceTransactionEntity로 변환
     */
    private BalanceTransactionEntity mapToBalanceTransactionEntity(BalanceTransaction transaction) {
        // UserEntity 조회
        UserEntity userEntity = userJpaRepository.findByUserIdAndStatus(transaction.getUserId(), "ACTIVE")
                .orElseThrow(() -> new IllegalArgumentException("User not found with userId: " + transaction.getUserId()));
        
        return BalanceTransactionEntity.builder()
                .user(userEntity)  // user 관계를 통해 userId 설정
                .amount(transaction.getAmount())
                .type(transaction.getType().name())
                .status(transaction.getStatus().name())
                .description(transaction.getDescription())
                .referenceId(transaction.getReferenceId())
                .build();
    }
}