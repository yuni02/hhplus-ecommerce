package kr.hhplus.be.server.balance.infrastructure.persistence.adapter;

import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.domain.BalanceTransaction;
import kr.hhplus.be.server.balance.infrastructure.persistence.entity.BalanceEntity;
import kr.hhplus.be.server.balance.infrastructure.persistence.repository.BalanceJpaRepository;
import kr.hhplus.be.server.balance.infrastructure.persistence.repository.BalanceTransactionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(BalancePersistenceAdapter.class)
class BalancePersistenceAdapterIntegrationTest {

    @Autowired
    private BalancePersistenceAdapter balancePersistenceAdapter;

    @Autowired
    private BalanceJpaRepository balanceJpaRepository;

    @Autowired
    private BalanceTransactionJpaRepository balanceTransactionJpaRepository;

    private Balance testBalance;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        balanceTransactionJpaRepository.deleteAll();
        balanceJpaRepository.deleteAll();

        testBalance = Balance.builder()
                .userId(1001L)
                .amount(new BigDecimal("10000"))
                .status(Balance.BalanceStatus.ACTIVE)
                .build();
    }

    @Test
    void saveBalance_새로운잔액저장() {
        // when
        Balance savedBalance = balancePersistenceAdapter.saveBalance(testBalance);

        // then
        assertThat(savedBalance.getId()).isNotNull();
        assertThat(savedBalance.getUserId()).isEqualTo(1001L);
        assertThat(savedBalance.getAmount()).isEqualTo(new BigDecimal("10000"));
        assertThat(savedBalance.getStatus()).isEqualTo(Balance.BalanceStatus.ACTIVE);

        // DB에서 직접 확인
        Optional<BalanceEntity> foundEntity = balanceJpaRepository.findById(savedBalance.getId());
        assertThat(foundEntity).isPresent();
        assertThat(foundEntity.get().getUserId()).isEqualTo(1001L);
    }

    @Test
    void loadActiveBalanceByUserId_존재하는잔액조회() {
        // given
        Balance savedBalance = balancePersistenceAdapter.saveBalance(testBalance);

        // when
        Optional<Balance> foundBalance = balancePersistenceAdapter.loadActiveBalanceByUserId(1001L);

        // then
        assertThat(foundBalance).isPresent();
        assertThat(foundBalance.get().getId()).isEqualTo(savedBalance.getId());
        assertThat(foundBalance.get().getAmount()).isEqualTo(new BigDecimal("10000"));
    }

    @Test
    void loadActiveBalanceByUserId_존재하지않는사용자() {
        // when
        Optional<Balance> foundBalance = balancePersistenceAdapter.loadActiveBalanceByUserId(9999L);

        // then
        assertThat(foundBalance).isEmpty();
    }

    @Test
    void saveBalanceTransaction_거래내역저장() {
        // given
        BalanceTransaction transaction = new BalanceTransaction();
        transaction.setUserId(1001L);
        transaction.setAmount(new BigDecimal("5000"));
        transaction.setType(BalanceTransaction.TransactionType.CHARGE);
        transaction.setStatus(BalanceTransaction.TransactionStatus.COMPLETED);
        transaction.setDescription("테스트 충전");

        // when
        BalanceTransaction savedTransaction = balancePersistenceAdapter.saveBalanceTransaction(transaction);

        // then
        assertThat(savedTransaction.getId()).isNotNull();
        assertThat(savedTransaction.getUserId()).isEqualTo(1001L);
        assertThat(savedTransaction.getAmount()).isEqualTo(new BigDecimal("5000"));
        assertThat(savedTransaction.getType()).isEqualTo(BalanceTransaction.TransactionType.CHARGE);
    }

    @Test
    void 잔액충전_전체플로우() {
        // given - 초기 잔액 저장
        Balance initialBalance = balancePersistenceAdapter.saveBalance(testBalance);

        // when - 잔액 충전
        Balance chargedBalance = Balance.builder()
                .id(initialBalance.getId())
                .userId(1001L)
                .amount(initialBalance.getAmount().add(new BigDecimal("5000")))
                .status(Balance.BalanceStatus.ACTIVE)
                .build();

        Balance savedChargedBalance = balancePersistenceAdapter.saveBalance(chargedBalance);

        // then
        assertThat(savedChargedBalance.getAmount()).isEqualTo(new BigDecimal("15000"));

        // DB에서 직접 확인
        Optional<Balance> foundBalance = balancePersistenceAdapter.loadActiveBalanceByUserId(1001L);
        assertThat(foundBalance).isPresent();
        assertThat(foundBalance.get().getAmount()).isEqualTo(new BigDecimal("15000"));
    }
} 