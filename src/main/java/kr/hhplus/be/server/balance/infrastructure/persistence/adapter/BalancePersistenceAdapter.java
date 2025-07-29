package kr.hhplus.be.server.balance.infrastructure.persistence.adapter;

import kr.hhplus.be.server.balance.application.port.out.LoadBalancePort;
import kr.hhplus.be.server.balance.application.port.out.SaveBalanceTransactionPort;
import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.domain.BalanceTransaction;
import kr.hhplus.be.server.balance.infrastructure.persistence.entity.BalanceEntity;
import kr.hhplus.be.server.balance.infrastructure.persistence.entity.BalanceTransactionEntity;
import kr.hhplus.be.server.balance.infrastructure.persistence.repository.BalanceJpaRepository;
import kr.hhplus.be.server.balance.infrastructure.persistence.repository.BalanceTransactionJpaRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Balance 인프라스트럭처 영속성 Adapter
 * Balance 도메인 전용 데이터 접근
 */
@Component
public class BalancePersistenceAdapter implements LoadBalancePort, SaveBalanceTransactionPort {

    private final BalanceJpaRepository balanceJpaRepository;
    private final BalanceTransactionJpaRepository balanceTransactionJpaRepository;

    public BalancePersistenceAdapter(BalanceJpaRepository balanceJpaRepository,
                                   BalanceTransactionJpaRepository balanceTransactionJpaRepository) {
        this.balanceJpaRepository = balanceJpaRepository;
        this.balanceTransactionJpaRepository = balanceTransactionJpaRepository;
    }

    @Override
    public Optional<Balance> loadActiveBalanceByUserId(Long userId) {
        return balanceJpaRepository.findByUserIdAndStatus(userId, "ACTIVE")
                .map(this::mapToBalance);
    }

    @Override
    public Balance saveBalance(Balance balance) {
        BalanceEntity entity = mapToBalanceEntity(balance);
        BalanceEntity savedEntity = balanceJpaRepository.save(entity);
        return mapToBalance(savedEntity);
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
        return new BalanceEntity(
                balance.getId(),
                balance.getUserId(),
                balance.getAmount(),
                balance.getStatus().name()
        );
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
        BalanceTransactionEntity entity = new BalanceTransactionEntity();
        entity.setId(transaction.getId());
        entity.setUserId(transaction.getUserId());
        entity.setAmount(transaction.getAmount());
        entity.setType(transaction.getType().name());
        entity.setStatus(transaction.getStatus().name());
        entity.setDescription(transaction.getDescription());
        entity.setReferenceId(transaction.getReferenceId());
        entity.setCreatedAt(transaction.getCreatedAt());
        entity.setUpdatedAt(transaction.getUpdatedAt());
        return entity;
    }
}