package kr.hhplus.be.server.balance.infrastructure.persistence.adapter;

import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.domain.BalanceTransaction;
import kr.hhplus.be.server.balance.infrastructure.persistence.entity.BalanceEntity;
import kr.hhplus.be.server.balance.infrastructure.persistence.entity.BalanceTransactionEntity;
import kr.hhplus.be.server.balance.infrastructure.persistence.repository.BalanceJpaRepository;
import kr.hhplus.be.server.balance.infrastructure.persistence.repository.BalanceTransactionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalancePersistenceAdapterTest {

    @Mock
    private BalanceJpaRepository balanceJpaRepository;

    @Mock
    private BalanceTransactionJpaRepository balanceTransactionJpaRepository;

    @InjectMocks
    private BalancePersistenceAdapter balancePersistenceAdapter;

    private BalanceEntity testBalanceEntity;
    private Balance testBalance;

    @BeforeEach
    void setUp() {
        testBalanceEntity = BalanceEntity.builder()
                .id(1L)
                .userId(1001L)
                .amount(new BigDecimal("10000"))
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .version(0L)
                .build();

        testBalance = Balance.builder()
                .id(1L)
                .userId(1001L)
                .amount(new BigDecimal("10000"))
                .status(Balance.BalanceStatus.ACTIVE)
                .build();
    }

    @Test
    void loadActiveBalanceByUserId_성공() {
        // given
        Long userId = 1001L;
        when(balanceJpaRepository.findByUserIdAndStatus(userId, "ACTIVE"))
                .thenReturn(Optional.of(testBalanceEntity));

        // when
        Optional<Balance> result = balancePersistenceAdapter.loadActiveBalanceByUserId(userId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(userId);
        assertThat(result.get().getAmount()).isEqualTo(new BigDecimal("10000"));
    }

    @Test
    void loadActiveBalanceByUserId_사용자없음() {
        // given
        Long userId = 9999L;
        when(balanceJpaRepository.findByUserIdAndStatus(userId, "ACTIVE"))
                .thenReturn(Optional.empty());

        // when
        Optional<Balance> result = balancePersistenceAdapter.loadActiveBalanceByUserId(userId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void saveBalance_성공() {
        // given
        when(balanceJpaRepository.save(any(BalanceEntity.class)))
                .thenReturn(testBalanceEntity);

        // when
        Balance result = balancePersistenceAdapter.saveBalance(testBalance);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1001L);
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("10000"));
    }

    @Test
    void saveBalanceTransaction_성공() {
        // given
        BalanceTransaction transaction = new BalanceTransaction();
        transaction.setUserId(1001L);
        transaction.setAmount(new BigDecimal("5000"));
        transaction.setType(BalanceTransaction.TransactionType.CHARGE);
        transaction.setStatus(BalanceTransaction.TransactionStatus.COMPLETED);
        transaction.setDescription("테스트 충전");

        BalanceTransactionEntity transactionEntity = BalanceTransactionEntity.builder()
                .id(1L)
                .userId(1001L)
                .amount(new BigDecimal("5000"))
                .type("CHARGE")
                .status("COMPLETED")
                .description("테스트 충전")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(balanceTransactionJpaRepository.save(any(BalanceTransactionEntity.class)))
                .thenReturn(transactionEntity);

        // when
        BalanceTransaction result = balancePersistenceAdapter.saveBalanceTransaction(transaction);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1001L);
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("5000"));
    }
} 