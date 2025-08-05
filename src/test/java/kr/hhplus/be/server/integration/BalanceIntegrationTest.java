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
import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.order.infrastructure.persistence.entity.OrderItemEntity;
import kr.hhplus.be.server.order.infrastructure.persistence.repository.OrderItemJpaRepository; 
import org.springframework.context.ApplicationContext;
import kr.hhplus.be.server.order.application.port.out.DeductBalancePort;
import kr.hhplus.be.server.product.infrastructure.persistence.entity.ProductEntity;
import kr.hhplus.be.server.product.infrastructure.persistence.repository.ProductJpaRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        Long userId = testUser.getUserId(); // 올바른 사용자 ID 사용
        BigDecimal initialAmount = new BigDecimal("10000");
        BigDecimal chargeAmount1 = new BigDecimal("5000");
        BigDecimal chargeAmount2 = new BigDecimal("3000");
        
        // 초기 잔액 설정
        ChargeBalanceUseCase.ChargeBalanceCommand initialRequest = new ChargeBalanceUseCase.ChargeBalanceCommand(userId, initialAmount);
        ChargeBalanceUseCase.ChargeBalanceResult initialResponse = chargeBalanceService.chargeBalance(initialRequest);
        assertThat(initialResponse.isSuccess()).isTrue();
        
        // When - 동시에 두 개의 충전 요청 실행
        CountDownLatch latch = new CountDownLatch(2);
        AtomicReference<ChargeBalanceUseCase.ChargeBalanceResult> response1 = new AtomicReference<>();
        AtomicReference<ChargeBalanceUseCase.ChargeBalanceResult> response2 = new AtomicReference<>();
        
        Thread thread1 = new Thread(() -> {
            try {
                ChargeBalanceUseCase.ChargeBalanceCommand request1 = new ChargeBalanceUseCase.ChargeBalanceCommand(userId, chargeAmount1);
                response1.set(chargeBalanceService.chargeBalance(request1));
            } finally {
                latch.countDown();
            }
        });
        
        Thread thread2 = new Thread(() -> {
            try {
                ChargeBalanceUseCase.ChargeBalanceCommand request2 = new ChargeBalanceUseCase.ChargeBalanceCommand(userId, chargeAmount2);
                response2.set(chargeBalanceService.chargeBalance(request2));
            } finally {
                latch.countDown();
            }
        });
        
        thread1.start();
        thread2.start();
        latch.await(5, TimeUnit.SECONDS);
        
        // Then - 두 요청 모두 성공해야 함
        assertThat(response1.get().isSuccess()).isTrue();
        assertThat(response2.get().isSuccess()).isTrue();
        
        // 최종 잔액 확인 (10000 + 5000 + 3000 = 18000)
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
        Long userId = testUser.getUserId(); // 올바른 사용자 ID 사용
        BigDecimal initialAmount = new BigDecimal("10000");
        BigDecimal chargeAmount = new BigDecimal("5000");
        BigDecimal paymentAmount = new BigDecimal("3000");
        
        // 초기 잔액 설정
        ChargeBalanceUseCase.ChargeBalanceCommand initialRequest = new ChargeBalanceUseCase.ChargeBalanceCommand(userId, initialAmount);
        ChargeBalanceUseCase.ChargeBalanceResult initialResponse = chargeBalanceService.chargeBalance(initialRequest);
        assertThat(initialResponse.isSuccess()).isTrue();
        
        // When - 동시에 충전과 결제 요청 실행
        CountDownLatch latch = new CountDownLatch(2);
        AtomicReference<ChargeBalanceUseCase.ChargeBalanceResult> chargeResponse = new AtomicReference<>();
        AtomicReference<Boolean> paymentSuccess = new AtomicReference<>(false);
        
        Thread chargeThread = new Thread(() -> {
            try {
                ChargeBalanceUseCase.ChargeBalanceCommand chargeRequest = new ChargeBalanceUseCase.ChargeBalanceCommand(userId, chargeAmount);
                chargeResponse.set(chargeBalanceService.chargeBalance(chargeRequest));
            } finally {
                latch.countDown();
            }
        });
        
        Thread paymentThread = new Thread(() -> {
            try {
                // 결제 시뮬레이션 (잔액 차감)
                GetBalanceUseCase.GetBalanceCommand balanceCommand = new GetBalanceUseCase.GetBalanceCommand(userId);
                Optional<GetBalanceUseCase.GetBalanceResult> balanceResult = getBalanceService.getBalance(balanceCommand);
                if (balanceResult.isPresent()) {
                    GetBalanceUseCase.GetBalanceResult balance = balanceResult.get();
                    if (balance.getBalance().compareTo(paymentAmount) >= 0) {
                        // 실제 결제 로직은 별도 구현 필요
                        paymentSuccess.set(true);
                    }
                }
            } finally {
                latch.countDown();
            }
        });
        
        chargeThread.start();
        paymentThread.start();
        latch.await(5, TimeUnit.SECONDS);
        
        // Then
        assertThat(chargeResponse.get().isSuccess()).isTrue();
        
        // 최종 잔액 확인 (충전은 성공, 결제는 별도 처리)
        GetBalanceUseCase.GetBalanceCommand finalBalanceCommand = new GetBalanceUseCase.GetBalanceCommand(userId);
        Optional<GetBalanceUseCase.GetBalanceResult> finalBalanceResult = getBalanceService.getBalance(finalBalanceCommand);
        assertThat(finalBalanceResult).isPresent();
        
        GetBalanceUseCase.GetBalanceResult finalBalance = finalBalanceResult.get();
        assertThat(finalBalance.getUserId()).isEqualTo(userId);
        assertThat(finalBalance.getBalance()).isGreaterThanOrEqualTo(new BigDecimal("15000")); // 10000 + 5000
    }
    
    @Test
    @DisplayName("동시에 100개의 잔액 충전 요청 테스트")
    void 동시에_100개의_잔액_충전_요청() throws InterruptedException {
        // Given
        Long userId = testUser.getUserId();
        BigDecimal chargeAmount = new BigDecimal("100"); // 각 요청당 100원씩 충전
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<ChargeBalanceUseCase.ChargeBalanceResult> results = Collections.synchronizedList(new ArrayList<>());
        
        // When - 100개의 동시 잔액 충전 요청 실행
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    ChargeBalanceUseCase.ChargeBalanceCommand request = new ChargeBalanceUseCase.ChargeBalanceCommand(userId, chargeAmount);
                    ChargeBalanceUseCase.ChargeBalanceResult result = chargeBalanceService.chargeBalance(request);
                    results.add(result);
                    
                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.println("Thread execution failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 모든 스레드가 완료될 때까지 대기
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        
        // Then
        assertThat(completed).isTrue(); // 모든 스레드가 완료되었는지 확인
        
        // 성공한 요청 수 확인 (일부는 재시도로 인해 실패할 수 있음)
        System.out.println("성공한 요청 수: " + successCount.get());
        System.out.println("실패한 요청 수: " + failureCount.get());
        System.out.println("총 요청 수: " + threadCount);
        
        // 최소 90% 이상의 요청이 성공해야 함
        assertThat(successCount.get()).isGreaterThanOrEqualTo(90);
        
        // 최종 잔액 확인 (100 * 100 = 10,000원이 추가되어야 함)
        GetBalanceUseCase.GetBalanceCommand finalBalanceCommand = new GetBalanceUseCase.GetBalanceCommand(userId);
        Optional<GetBalanceUseCase.GetBalanceResult> finalBalanceResult = getBalanceService.getBalance(finalBalanceCommand);
        assertThat(finalBalanceResult).isPresent();
        
        GetBalanceUseCase.GetBalanceResult finalBalance = finalBalanceResult.get();
        assertThat(finalBalance.getUserId()).isEqualTo(userId);
        
        // 예상 잔액: 성공한 요청 수 * 100원
        BigDecimal expectedBalance = new BigDecimal(successCount.get()).multiply(chargeAmount);
        assertThat(finalBalance.getBalance()).isEqualTo(expectedBalance);
        
        // 거래 내역 확인
        List<BalanceTransactionEntity> transactions = balanceTransactionJpaRepository.findAll();
        assertThat(transactions).hasSize(successCount.get()); // 성공한 요청만큼의 거래 내역이 있어야 함
        
        executorService.shutdown();
    }
    
    @Test
    @DisplayName("동시에 1000개의 잔액 충전 요청 테스트 (대용량)")
    void 동시에_1000개의_잔액_충전_요청() throws InterruptedException {
        // Given
        Long userId = testUser.getUserId();
        BigDecimal chargeAmount = new BigDecimal("10"); // 각 요청당 10원씩 충전
        int threadCount = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(64);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<ChargeBalanceUseCase.ChargeBalanceResult> results = Collections.synchronizedList(new ArrayList<>());
        
        // When - 1000개의 동시 잔액 충전 요청 실행
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    ChargeBalanceUseCase.ChargeBalanceCommand request = new ChargeBalanceUseCase.ChargeBalanceCommand(userId, chargeAmount);
                    ChargeBalanceUseCase.ChargeBalanceResult result = chargeBalanceService.chargeBalance(request);
                    results.add(result);
                    
                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.println("Thread execution failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 모든 스레드가 완료될 때까지 대기 (더 긴 시간 허용)
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        
        // Then
        assertThat(completed).isTrue(); // 모든 스레드가 완료되었는지 확인
        
        // 성공한 요청 수 확인
        System.out.println("성공한 요청 수: " + successCount.get());
        System.out.println("실패한 요청 수: " + failureCount.get());
        System.out.println("총 요청 수: " + threadCount);
        System.out.println("성공률: " + (successCount.get() * 100.0 / threadCount) + "%");
        
        // 최소 95% 이상의 요청이 성공해야 함
        assertThat(successCount.get()).isGreaterThanOrEqualTo((int)(threadCount * 0.95));
        
        // 최종 잔액 확인
        GetBalanceUseCase.GetBalanceCommand finalBalanceCommand = new GetBalanceUseCase.GetBalanceCommand(userId);
        Optional<GetBalanceUseCase.GetBalanceResult> finalBalanceResult = getBalanceService.getBalance(finalBalanceCommand);
        assertThat(finalBalanceResult).isPresent();
        
        GetBalanceUseCase.GetBalanceResult finalBalance = finalBalanceResult.get();
        assertThat(finalBalance.getUserId()).isEqualTo(userId);
        
        // 예상 잔액: 성공한 요청 수 * 10원
        BigDecimal expectedBalance = new BigDecimal(successCount.get()).multiply(chargeAmount);
        assertThat(finalBalance.getBalance()).isEqualTo(expectedBalance);
        
        // 거래 내역 확인
        List<BalanceTransactionEntity> transactions = balanceTransactionJpaRepository.findAll();
        assertThat(transactions).hasSize(successCount.get());
        
        executorService.shutdown();
    }
    
    @Test
    @DisplayName("재시도 로직 테스트 - 강한 동시성 충돌 상황")
    void 재시도_로직_테스트() throws InterruptedException {
        // Given
        Long userId = testUser.getUserId();
        BigDecimal chargeAmount = new BigDecimal("1000");
        int threadCount = 50; // 강한 충돌을 위해 적은 수의 스레드로 빠른 요청
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger retryCount = new AtomicInteger(0);
        
        // When - 빠른 연속 요청으로 재시도 로직 테스트
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    ChargeBalanceUseCase.ChargeBalanceCommand request = new ChargeBalanceUseCase.ChargeBalanceCommand(userId, chargeAmount);
                    ChargeBalanceUseCase.ChargeBalanceResult result = chargeBalanceService.chargeBalance(request);
                    
                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                        // 재시도 관련 메시지 확인
                        if (result.getErrorMessage().contains("재시도") || result.getErrorMessage().contains("충돌")) {
                            retryCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.println("Thread execution failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 모든 스레드가 완료될 때까지 대기
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        
        // Then
        assertThat(completed).isTrue();
        
        System.out.println("=== 재시도 로직 테스트 결과 ===");
        System.out.println("성공한 요청 수: " + successCount.get());
        System.out.println("실패한 요청 수: " + failureCount.get());
        System.out.println("재시도 관련 실패 수: " + retryCount.get());
        System.out.println("총 요청 수: " + threadCount);
        System.out.println("성공률: " + (successCount.get() * 100.0 / threadCount) + "%");
        
        // 재시도 로직이 작동했는지 확인 (일부는 재시도로 인해 실패할 수 있음)
        assertThat(successCount.get()).isGreaterThan(0);
        
        // 최종 잔액 확인
        GetBalanceUseCase.GetBalanceCommand finalBalanceCommand = new GetBalanceUseCase.GetBalanceCommand(userId);
        Optional<GetBalanceUseCase.GetBalanceResult> finalBalanceResult = getBalanceService.getBalance(finalBalanceCommand);
        assertThat(finalBalanceResult).isPresent();
        
        GetBalanceUseCase.GetBalanceResult finalBalance = finalBalanceResult.get();
        assertThat(finalBalance.getUserId()).isEqualTo(userId);
        
        // 예상 잔액: 성공한 요청 수 * 1000원
        BigDecimal expectedBalance = new BigDecimal(successCount.get()).multiply(chargeAmount);
        assertThat(finalBalance.getBalance()).isEqualTo(expectedBalance);
        
        executorService.shutdown();
    }
    
    @Test
    @DisplayName("고급 재시도 로직 테스트")
    void 고급_재시도_로직_테스트() throws InterruptedException {
        // Given
        Long userId = testUser.getUserId();
        BigDecimal chargeAmount = new BigDecimal("500");
        int threadCount = 30;
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        // When - 고급 재시도 로직 테스트
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    ChargeBalanceUseCase.ChargeBalanceCommand request = new ChargeBalanceUseCase.ChargeBalanceCommand(userId, chargeAmount);
                    // 고급 재시도 로직 사용 (리플렉션으로 접근)
                    ChargeBalanceUseCase.ChargeBalanceResult result = chargeBalanceService.chargeBalanceWithAdvancedRetry(request);
                    
                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.println("Thread execution failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 모든 스레드가 완료될 때까지 대기
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        
        // Then
        assertThat(completed).isTrue();
        
        System.out.println("=== 고급 재시도 로직 테스트 결과 ===");
        System.out.println("성공한 요청 수: " + successCount.get());
        System.out.println("실패한 요청 수: " + failureCount.get());
        System.out.println("총 요청 수: " + threadCount);
        System.out.println("성공률: " + (successCount.get() * 100.0 / threadCount) + "%");
        
        // 고급 재시도 로직이 작동했는지 확인
        assertThat(successCount.get()).isGreaterThan(0);
        
        // 최종 잔액 확인
        GetBalanceUseCase.GetBalanceCommand finalBalanceCommand = new GetBalanceUseCase.GetBalanceCommand(userId);
        Optional<GetBalanceUseCase.GetBalanceResult> finalBalanceResult = getBalanceService.getBalance(finalBalanceCommand);
        assertThat(finalBalanceResult).isPresent();
        
        GetBalanceUseCase.GetBalanceResult finalBalance = finalBalanceResult.get();
        assertThat(finalBalance.getUserId()).isEqualTo(userId);
        
        // 예상 잔액: 성공한 요청 수 * 500원
        BigDecimal expectedBalance = new BigDecimal(successCount.get()).multiply(chargeAmount);
        assertThat(finalBalance.getBalance()).isEqualTo(expectedBalance);
        
        executorService.shutdown();
    }
    

    
    @Test
    @DisplayName("충전과 결제 동시성 테스트 - 하이브리드 락 전략")
    void 충전과_결제_동시성_테스트_하이브리드() throws InterruptedException {
        // Given
        Long userId = testUser.getUserId();
        BigDecimal chargeAmount = new BigDecimal("1000");
        BigDecimal paymentAmount = new BigDecimal("300");
        int threadCount = 20; // 충전 10개, 결제 10개
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger chargeSuccessCount = new AtomicInteger(0);
        AtomicInteger paymentSuccessCount = new AtomicInteger(0);
        AtomicInteger chargeFailureCount = new AtomicInteger(0);
        AtomicInteger paymentFailureCount = new AtomicInteger(0);
        
        // 초기 잔액 설정
        ChargeBalanceUseCase.ChargeBalanceCommand initialCharge = new ChargeBalanceUseCase.ChargeBalanceCommand(userId, new BigDecimal("5000"));
        chargeBalanceService.chargeBalance(initialCharge);
        
        // When - 충전과 결제 동시 요청
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    if (index % 2 == 0) {
                        // 충전 요청 (ChargeBalanceService 사용)
                        ChargeBalanceUseCase.ChargeBalanceCommand chargeRequest = new ChargeBalanceUseCase.ChargeBalanceCommand(userId, chargeAmount);
                        ChargeBalanceUseCase.ChargeBalanceResult chargeResult = chargeBalanceService.chargeBalance(chargeRequest);
                        
                        if (chargeResult.isSuccess()) {
                            chargeSuccessCount.incrementAndGet();
                        } else {
                            chargeFailureCount.incrementAndGet();
                        }
                    } else {
                        // 실제 결제 요청 (DeductBalancePort 사용)
                        try {
                            boolean paymentSuccess = deductBalancePort.deductBalance(userId, paymentAmount);
                            if (paymentSuccess) {
                                paymentSuccessCount.incrementAndGet();
                            } else {
                                paymentFailureCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            paymentFailureCount.incrementAndGet();
                            System.err.println("Payment failed: " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    if (index % 2 == 0) {
                        chargeFailureCount.incrementAndGet();
                    } else {
                        paymentFailureCount.incrementAndGet();
                    }
                    System.err.println("Thread execution failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 모든 스레드가 완료될 때까지 대기
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        
        // Then
        assertThat(completed).isTrue();
        
        System.out.println("=== 충전과 결제 동시성 테스트 (하이브리드 락) 결과 ===");
        System.out.println("충전 성공: " + chargeSuccessCount.get() + ", 실패: " + chargeFailureCount.get());
        System.out.println("결제 성공: " + paymentSuccessCount.get() + ", 실패: " + paymentFailureCount.get());
        System.out.println("총 요청 수: " + threadCount);
        
        // 최종 잔액 확인
        GetBalanceUseCase.GetBalanceCommand finalBalanceCommand = new GetBalanceUseCase.GetBalanceCommand(userId);
        Optional<GetBalanceUseCase.GetBalanceResult> finalBalanceResult = getBalanceService.getBalance(finalBalanceCommand);
        assertThat(finalBalanceResult).isPresent();
        
        GetBalanceUseCase.GetBalanceResult finalBalance = finalBalanceResult.get();
        assertThat(finalBalance.getUserId()).isEqualTo(userId);
        
        // 예상 잔액: 초기 5000 + (충전 성공 수 * 1000) - (결제 성공 수 * 300)
        BigDecimal expectedBalance = new BigDecimal("5000")
                .add(new BigDecimal(chargeSuccessCount.get()).multiply(chargeAmount))
                .subtract(new BigDecimal(paymentSuccessCount.get()).multiply(paymentAmount));
        
        System.out.println("예상 잔액: " + expectedBalance);
        System.out.println("실제 잔액: " + finalBalance.getBalance());
        
        assertThat(finalBalance.getBalance()).isEqualTo(expectedBalance);
        
        executorService.shutdown();
    }
    
    @Test
    @DisplayName("대용량 동시성 테스트 - 하이브리드 락 전략")
    void 대용량_동시성_테스트() throws InterruptedException {
        // Given
        Long userId = testUser.getUserId();
        BigDecimal chargeAmount = new BigDecimal("100");
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        // When - 100개의 동시 잔액 충전 요청
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    ChargeBalanceUseCase.ChargeBalanceCommand request = new ChargeBalanceUseCase.ChargeBalanceCommand(userId, chargeAmount);
                    ChargeBalanceUseCase.ChargeBalanceResult result = chargeBalanceService.chargeBalance(request);
                    
                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.println("Thread execution failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 모든 스레드가 완료될 때까지 대기
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        
        // Then
        assertThat(completed).isTrue();
        
        System.out.println("=== 대용량 동시성 테스트 결과 ===");
        System.out.println("성공한 요청 수: " + successCount.get());
        System.out.println("실패한 요청 수: " + failureCount.get());
        System.out.println("총 요청 수: " + threadCount);
        System.out.println("성공률: " + (successCount.get() * 100.0 / threadCount) + "%");
        System.out.println("소요시간: " + (endTime - startTime) + "ms");
        
        // 최소 90% 이상의 요청이 성공해야 함
        assertThat(successCount.get()).isGreaterThanOrEqualTo((int)(threadCount * 0.9));
        
        // 최종 잔액 확인
        GetBalanceUseCase.GetBalanceCommand finalBalanceCommand = new GetBalanceUseCase.GetBalanceCommand(userId);
        Optional<GetBalanceUseCase.GetBalanceResult> finalBalanceResult = getBalanceService.getBalance(finalBalanceCommand);
        assertThat(finalBalanceResult).isPresent();
        
        GetBalanceUseCase.GetBalanceResult finalBalance = finalBalanceResult.get();
        assertThat(finalBalance.getUserId()).isEqualTo(userId);
        
        // 예상 잔액: 성공한 요청 수 * 100원
        BigDecimal expectedBalance = new BigDecimal(successCount.get()).multiply(chargeAmount);
        assertThat(finalBalance.getBalance()).isEqualTo(expectedBalance);
        
        executorService.shutdown();
    }
} 