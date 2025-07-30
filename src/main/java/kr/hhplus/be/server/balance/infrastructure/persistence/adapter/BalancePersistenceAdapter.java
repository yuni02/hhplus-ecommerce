package kr.hhplus.be.server.balance.infrastructure.persistence.adapter;

import kr.hhplus.be.server.balance.application.port.out.LoadBalancePort;
import kr.hhplus.be.server.balance.application.port.out.SaveBalanceTransactionPort;
import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.domain.BalanceTransaction;
import kr.hhplus.be.server.balance.infrastructure.persistence.entity.BalanceTransactionEntity;
import kr.hhplus.be.server.balance.infrastructure.persistence.repository.BalanceTransactionJpaRepository;
import kr.hhplus.be.server.user.infrastructure.persistence.entity.UserEntity;
import kr.hhplus.be.server.user.infrastructure.persistence.repository.UserJpaRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Balance 인프라스트럭처 영속성 Adapter
 * Balance 도메인 전용 데이터 접근
 */
@Component
public class BalancePersistenceAdapter implements LoadBalancePort, SaveBalanceTransactionPort {

    private final UserJpaRepository userJpaRepository;
    private final BalanceTransactionJpaRepository balanceTransactionJpaRepository;

    public BalancePersistenceAdapter(UserJpaRepository userJpaRepository,
                                   BalanceTransactionJpaRepository balanceTransactionJpaRepository) {
        this.userJpaRepository = userJpaRepository;
        this.balanceTransactionJpaRepository = balanceTransactionJpaRepository;
    }

    @Override
    public Optional<Balance> loadActiveBalanceByUserId(Long userId) {
        return userJpaRepository.findByUserIdAndStatus(userId, "ACTIVE")
                .map(this::mapToBalance);
    }

    @Override
    public Balance saveBalance(Balance balance) {
        UserEntity entity = mapToUserEntity(balance);
        UserEntity savedEntity = userJpaRepository.save(entity);
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
     * UserEntity를 Balance 도메인 객체로 변환
     */
    private Balance mapToBalance(UserEntity entity) {
        return new Balance(
                entity.getId(),
                entity.getUserId(),
                entity.getAmount(),
                Balance.BalanceStatus.valueOf(entity.getStatus())
        );
    }

    /**
     * Balance 도메인 객체를 UserEntity로 변환
     */
    private UserEntity mapToUserEntity(Balance balance) {
        String username = null;  // 기본값
        
        // 새로운 엔티티인 경우 기존 사용자 정보 조회
        // if (balance.getId() == null) {
            var existingUser = userJpaRepository.findByUserIdAndStatus(balance.getUserId(), "ACTIVE");
            if (existingUser.isPresent()) {
                username = existingUser.get().getUsername();
                System.out.println("Found existing user: " + username + " for userId: " + balance.getUserId());
            } else {
                System.out.println("No existing user found for userId: " + balance.getUserId());
            }
        // }
        
        return UserEntity.builder()
                .id(balance.getId())
                .userId(balance.getUserId())
                .username(username)
                .amount(balance.getAmount())
                .status(balance.getStatus().name())
                .createdAt(LocalDateTime.now())  // 항상 현재 시간으로 설정
                .updatedAt(LocalDateTime.now())
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
        return BalanceTransactionEntity.builder()
                .id(transaction.getId())
                .userId(transaction.getUserId())
                .amount(transaction.getAmount())
                .type(transaction.getType() != null ? transaction.getType().name() : null)
                .status(transaction.getStatus() != null ? transaction.getStatus().name() : null)
                .description(transaction.getDescription())
                .referenceId(transaction.getReferenceId())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
}