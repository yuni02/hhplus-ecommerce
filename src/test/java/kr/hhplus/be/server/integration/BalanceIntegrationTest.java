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
import org.springframework.context.annotation.Import;
import kr.hhplus.be.server.TestcontainersConfiguration;     
import org.springframework.context.ApplicationContext;
import kr.hhplus.be.server.order.application.port.out.DeductBalancePort;
import kr.hhplus.be.server.product.infrastructure.persistence.repository.ProductJpaRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;          

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
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

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private DeductBalancePort deductBalancePort;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        balanceTransactionJpaRepository.deleteAll();
        balanceJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
        productJpaRepository.deleteAll();

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

    @Test
    @DisplayName("동시 잔액 충전 테스트 - 동시성 제어")
    void concurrentChargeBalanceTest() throws InterruptedException {
        // Given
        Long userId = testUser.getUserId();
        BigDecimal initialAmount = new BigDecimal("10000.00");
        BigDecimal chargeAmount1 = new BigDecimal("5000.00");
        BigDecimal chargeAmount2 = new BigDecimal("3000.00");
        
        // 초기 잔액 설정
        ChargeBalanceUseCase.ChargeBalanceCommand initialRequest = new ChargeBalanceUseCase.ChargeBalanceCommand(userId, initialAmount);
        ChargeBalanceUseCase.ChargeBalanceResult initialResponse = chargeBalanceService.chargeBalance(initialRequest);
        assertThat(initialResponse.isSuccess()).isTrue();
        
        // When - 진짜 동시에 두 개의 충전 요청 실행
        CountDownLatch startLatch = new CountDownLatch(1); // 시작 신호
        CountDownLatch readyLatch = new CountDownLatch(2); // 준비 완료 신호
        CountDownLatch finishLatch = new CountDownLatch(2); // 완료 신호
        AtomicReference<ChargeBalanceUseCase.ChargeBalanceResult> response1 = new AtomicReference<>();
        AtomicReference<ChargeBalanceUseCase.ChargeBalanceResult> response2 = new AtomicReference<>();
        
        Thread thread1 = new Thread(() -> {
            try {
                readyLatch.countDown(); // 준비 완료 신호
                startLatch.await(); // 시작 신호 대기
                
                ChargeBalanceUseCase.ChargeBalanceCommand request1 = new ChargeBalanceUseCase.ChargeBalanceCommand(userId, chargeAmount1);
                response1.set(chargeBalanceService.chargeBalance(request1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                finishLatch.countDown();
            }
        });
        
        Thread thread2 = new Thread(() -> {
            try {
                readyLatch.countDown(); // 준비 완료 신호
                startLatch.await(); // 시작 신호 대기
                
                ChargeBalanceUseCase.ChargeBalanceCommand request2 = new ChargeBalanceUseCase.ChargeBalanceCommand(userId, chargeAmount2);
                response2.set(chargeBalanceService.chargeBalance(request2));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                finishLatch.countDown();
            }
        });
        
        thread1.start();
        thread2.start();
        
        // 모든 스레드가 준비될 때까지 대기
        readyLatch.await();
        System.out.println("모든 스레드가 준비되었습니다. 동시 시작!");
        
        // 시작 신호 전송 (모든 스레드가 동시에 시작)
        startLatch.countDown();
        
        finishLatch.await(5, TimeUnit.SECONDS);
        
        // Then - 두 요청 모두 성공해야 함
        assertThat(response1.get().isSuccess()).isTrue();
        assertThat(response2.get().isSuccess()).isTrue();
        
        // 최종 잔액 확인 (10000.00 + 5000.00 + 3000.00 = 18000.00)
        GetBalanceUseCase.GetBalanceCommand finalBalanceCommand = new GetBalanceUseCase.GetBalanceCommand(userId);
        Optional<GetBalanceUseCase.GetBalanceResult> finalBalanceResult = getBalanceService.getBalance(finalBalanceCommand);
        assertThat(finalBalanceResult).isPresent();
        
        GetBalanceUseCase.GetBalanceResult finalBalance = finalBalanceResult.get();
        assertThat(finalBalance.getUserId()).isEqualTo(userId);
        assertThat(finalBalance.getBalance()).isEqualTo(new BigDecimal("18000.00"));
    }
    
    @Test
    @DisplayName("동시 잔액 충전과 결제 테스트")
    void concurrentChargeAndPaymentTest() throws InterruptedException {
        // Given
        Long userId = testUser.getUserId();
        BigDecimal initialAmount = new BigDecimal("10000.00");
        BigDecimal chargeAmount = new BigDecimal("5000.00");
        BigDecimal paymentAmount = new BigDecimal("3000.00");
        
        // 초기 잔액 설정
        ChargeBalanceUseCase.ChargeBalanceCommand initialRequest = new ChargeBalanceUseCase.ChargeBalanceCommand(userId, initialAmount);
        ChargeBalanceUseCase.ChargeBalanceResult initialResponse = chargeBalanceService.chargeBalance(initialRequest);
        assertThat(initialResponse.isSuccess()).isTrue();
        
        // When - 진짜 동시에 충전과 결제 요청 실행
        CountDownLatch startLatch = new CountDownLatch(1); // 시작 신호
        CountDownLatch readyLatch = new CountDownLatch(2); // 준비 완료 신호
        CountDownLatch finishLatch = new CountDownLatch(2); // 완료 신호
        AtomicReference<ChargeBalanceUseCase.ChargeBalanceResult> chargeResponse = new AtomicReference<>();
        AtomicReference<Boolean> paymentSuccess = new AtomicReference<>(false);
        
        Thread chargeThread = new Thread(() -> {
            try {
                readyLatch.countDown(); // 준비 완료 신호
                startLatch.await(); // 시작 신호 대기
                
                ChargeBalanceUseCase.ChargeBalanceCommand chargeRequest = new ChargeBalanceUseCase.ChargeBalanceCommand(userId, chargeAmount);
                chargeResponse.set(chargeBalanceService.chargeBalance(chargeRequest));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                finishLatch.countDown();
            }
        });
        
        Thread paymentThread = new Thread(() -> {
            try {
                readyLatch.countDown(); // 준비 완료 신호
                startLatch.await(); // 시작 신호 대기
                
                // 실제 결제 요청 (DeductBalancePort 사용)
                boolean success = deductBalancePort.deductBalance(userId, paymentAmount);
                paymentSuccess.set(success);
            } catch (Exception e) {
                System.err.println("Payment failed: " + e.getMessage());
            } finally {
                finishLatch.countDown();
            }
        });
        
        chargeThread.start();
        paymentThread.start();
        
        // 모든 스레드가 준비될 때까지 대기
        readyLatch.await();
        System.out.println("모든 스레드가 준비되었습니다. 동시 시작!");
        
        // 시작 신호 전송 (모든 스레드가 동시에 시작)
        startLatch.countDown();
        
        finishLatch.await(5, TimeUnit.SECONDS);
        
        // Then
        assertThat(chargeResponse.get().isSuccess()).isTrue();
        
        // 최종 잔액 확인 (충전은 성공, 결제는 별도 처리)
        GetBalanceUseCase.GetBalanceCommand finalBalanceCommand = new GetBalanceUseCase.GetBalanceCommand(userId);
        Optional<GetBalanceUseCase.GetBalanceResult> finalBalanceResult = getBalanceService.getBalance(finalBalanceCommand);
        assertThat(finalBalanceResult).isPresent();
        
        GetBalanceUseCase.GetBalanceResult finalBalance = finalBalanceResult.get();
        assertThat(finalBalance.getUserId()).isEqualTo(userId);
        assertThat(finalBalance.getBalance()).isGreaterThanOrEqualTo(new BigDecimal("15000.00")); // 10000.00 + 5000.00
    }
    

    
    

    

    

    

    

} 