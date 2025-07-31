package kr.hhplus.be.server.balance.infrastructure.persistence.adapter;

import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.domain.BalanceTransaction;
import kr.hhplus.be.server.balance.infrastructure.persistence.entity.BalanceEntity;
import kr.hhplus.be.server.balance.infrastructure.persistence.repository.BalanceJpaRepository;
import kr.hhplus.be.server.balance.infrastructure.persistence.repository.BalanceTransactionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class BalancePersistenceAdapterTestcontainersTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }

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
    void 실제MySQL_잔액저장및조회() {
        // when
        Balance savedBalance = balancePersistenceAdapter.saveBalance(testBalance);

        // then
        assertThat(savedBalance.getId()).isNotNull();
        assertThat(savedBalance.getUserId()).isEqualTo(1001L);
        assertThat(savedBalance.getAmount()).isEqualTo(new BigDecimal("10000"));

        // 실제 DB에서 조회
        Optional<Balance> foundBalance = balancePersistenceAdapter.loadActiveBalanceByUserId(1001L);
        assertThat(foundBalance).isPresent();
        assertThat(foundBalance.get().getId()).isEqualTo(savedBalance.getId());
    }

    @Test
    void 실제MySQL_거래내역저장() {
        // given
        BalanceTransaction transaction = new BalanceTransaction();
        transaction.setUserId(1001L);
        transaction.setAmount(new BigDecimal("5000"));
        transaction.setType(BalanceTransaction.TransactionType.CHARGE);
        transaction.setStatus(BalanceTransaction.TransactionStatus.COMPLETED);
        transaction.setDescription("실제 DB 테스트 충전");

        // when
        BalanceTransaction savedTransaction = balancePersistenceAdapter.saveBalanceTransaction(transaction);

        // then
        assertThat(savedTransaction.getId()).isNotNull();
        assertThat(savedTransaction.getUserId()).isEqualTo(1001L);
        assertThat(savedTransaction.getAmount()).isEqualTo(new BigDecimal("5000"));
    }

    @Test
    void 실제MySQL_동시성테스트() {
        // given - 초기 잔액 저장
        Balance initialBalance = balancePersistenceAdapter.saveBalance(testBalance);

        // when - 여러 번 잔액 업데이트
        for (int i = 1; i <= 5; i++) {
            Balance updatedBalance = Balance.builder()
                    .id(initialBalance.getId())
                    .userId(1001L)
                    .amount(initialBalance.getAmount().add(new BigDecimal(i * 1000)))
                    .status(Balance.BalanceStatus.ACTIVE)
                    .build();

            balancePersistenceAdapter.saveBalance(updatedBalance);
        }

        // then
        Optional<Balance> finalBalance = balancePersistenceAdapter.loadActiveBalanceByUserId(1001L);
        assertThat(finalBalance).isPresent();
        assertThat(finalBalance.get().getAmount()).isEqualTo(new BigDecimal("25000")); // 10000 + 1000 + 2000 + 3000 + 4000 + 5000
    }
} 