package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.balance.application.ChargeBalanceService;
import kr.hhplus.be.server.balance.application.GetBalanceService;
import kr.hhplus.be.server.balance.application.port.in.ChargeBalanceUseCase;
import kr.hhplus.be.server.balance.application.port.in.GetBalanceUseCase;
import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.domain.BalanceTransaction;
import kr.hhplus.be.server.balance.infrastructure.persistence.adapter.BalancePersistenceAdapter;
import kr.hhplus.be.server.balance.infrastructure.persistence.entity.BalanceEntity;
import kr.hhplus.be.server.balance.infrastructure.persistence.entity.BalanceTransactionEntity;
import kr.hhplus.be.server.balance.infrastructure.persistence.repository.BalanceJpaRepository;
import kr.hhplus.be.server.balance.infrastructure.persistence.repository.BalanceTransactionJpaRepository;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.infrastructure.persistence.adapter.UserPersistenceAdapter;
import kr.hhplus.be.server.user.infrastructure.persistence.entity.UserEntity;
import kr.hhplus.be.server.user.infrastructure.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Balance 도메인 통합테스트")
class BalanceIntegrationTest {

    @Autowired
    private ChargeBalanceService chargeBalanceService;

    @Autowired
    private GetBalanceService getBalanceService;

    @Autowired
    private BalanceJpaRepository balanceJpaRepository;

    @Autowired
    private BalanceTransactionJpaRepository balanceTransactionJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        balanceTransactionJpaRepository.deleteAll();
        balanceJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        // 테스트용 사용자 생성
        testUser = UserEntity.builder()
                .userId(1L)
                .name("testuser")
                .email("test@example.com")
                .status("ACTIVE")
                .build();
        testUser = userJpaRepository.saveAndFlush(testUser);
    }

    @Test
    @DisplayName("잔액 충전 시 사용자의 잔액이 증가하고 이력이 기록된다")
    void 잔액_충전_검증() {
        // given
        Long userId = testUser.getUserId();
        BigDecimal originalAmount = new BigDecimal("5000.00");
        BigDecimal chargeAmount = new BigDecimal("1000.00");

        // 기존 잔액 생성
        BalanceEntity balanceEntity = BalanceEntity.builder()
                .userId(userId)
                .amount(originalAmount)
                .status("ACTIVE")
                .build();
        balanceJpaRepository.saveAndFlush(balanceEntity);

        // when
        ChargeBalanceUseCase.ChargeBalanceCommand command = new ChargeBalanceUseCase.ChargeBalanceCommand(userId, chargeAmount);
        ChargeBalanceUseCase.ChargeBalanceResult result = chargeBalanceService.chargeBalance(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getNewBalance()).isEqualTo(originalAmount.add(chargeAmount));

        // 잔액이 올바르게 업데이트되었는지 확인
        Optional<BalanceEntity> updatedBalance = balanceJpaRepository.findByUserIdAndStatus(userId, "ACTIVE");
        assertThat(updatedBalance).isPresent();
        assertThat(updatedBalance.get().getAmount()).isEqualTo(originalAmount.add(chargeAmount));

        // 거래 내역이 올바르게 기록되었는지 확인
        assertThat(balanceTransactionJpaRepository.findAll())
                .hasSize(1)
                .allSatisfy(transaction -> {
                    assertThat(transaction.getUserId()).isEqualTo(userId);
                    assertThat(transaction.getAmount()).isEqualTo(chargeAmount);
                    assertThat(transaction.getType()).isEqualTo("CHARGE");
                    assertThat(transaction.getStatus()).isEqualTo("COMPLETED");
                });
    }

    @Test
    @DisplayName("잔액 조회 성공")
    void 잔액_조회_성공() {
        // given
        Long userId = testUser.getUserId();
        BigDecimal balanceAmount = new BigDecimal("10000.00");

        BalanceEntity balanceEntity = BalanceEntity.builder()
                .userId(userId)
                .amount(balanceAmount)
                .status("ACTIVE")
                .build();
        balanceJpaRepository.saveAndFlush(balanceEntity);

        // when
        GetBalanceUseCase.GetBalanceCommand command = new GetBalanceUseCase.GetBalanceCommand(userId);
        Optional<GetBalanceUseCase.GetBalanceResult> result = getBalanceService.getBalance(command);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(userId);
        assertThat(result.get().getBalance()).isEqualTo(balanceAmount);
    }

    @Test
    @DisplayName("잔액 조회 실패 - 존재하지 않는 사용자")
    void 잔액_조회_실패_존재하지_않는_사용자() {
        // given
        Long nonExistentUserId = 9999L;

        // when
        GetBalanceUseCase.GetBalanceCommand command = new GetBalanceUseCase.GetBalanceCommand(nonExistentUserId);
        Optional<GetBalanceUseCase.GetBalanceResult> result = getBalanceService.getBalance(command);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("잔액 충전 실패 - 잘못된 금액")
    void 잔액_충전_실패_잘못된_금액() {
        // given
        Long userId = testUser.getUserId();
        BigDecimal invalidAmount = new BigDecimal("-1000");

        // when
        ChargeBalanceUseCase.ChargeBalanceCommand command = new ChargeBalanceUseCase.ChargeBalanceCommand(userId, invalidAmount);
        ChargeBalanceUseCase.ChargeBalanceResult result = chargeBalanceService.chargeBalance(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("충전 금액은 0보다 커야 합니다");
    }

    @Test
    @DisplayName("잔액 충전 실패 - 존재하지 않는 사용자")
    void 잔액_충전_실패_존재하지_않는_사용자() {
        // given
        Long nonExistentUserId = 9999L;
        BigDecimal chargeAmount = new BigDecimal("1000");

        // when
        ChargeBalanceUseCase.ChargeBalanceCommand command = new ChargeBalanceUseCase.ChargeBalanceCommand(nonExistentUserId, chargeAmount);
        ChargeBalanceUseCase.ChargeBalanceResult result = chargeBalanceService.chargeBalance(command);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("잔액 충전 시 기존 잔액이 없으면 새로 생성된다")
    void 잔액_충전_새로운_잔액_생성() {
        // given
        Long userId = testUser.getUserId();
        BigDecimal chargeAmount = new BigDecimal("5000.00");

        // when
        ChargeBalanceUseCase.ChargeBalanceCommand command = new ChargeBalanceUseCase.ChargeBalanceCommand(userId, chargeAmount);
        ChargeBalanceUseCase.ChargeBalanceResult result = chargeBalanceService.chargeBalance(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getNewBalance()).isEqualTo(chargeAmount);

        // 새로운 잔액이 생성되었는지 확인
        Optional<BalanceEntity> newBalance = balanceJpaRepository.findByUserIdAndStatus(userId, "ACTIVE");
        assertThat(newBalance).isPresent();
        assertThat(newBalance.get().getAmount()).isEqualTo(chargeAmount);
    }
} 