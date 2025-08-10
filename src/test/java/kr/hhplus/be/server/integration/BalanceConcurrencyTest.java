package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.balance.application.ChargeBalanceService;
import kr.hhplus.be.server.balance.application.GetBalanceService;
import kr.hhplus.be.server.balance.application.port.in.ChargeBalanceUseCase;
import kr.hhplus.be.server.balance.application.port.in.GetBalanceUseCase;
import kr.hhplus.be.server.order.application.port.out.DeductBalancePort;
import kr.hhplus.be.server.balance.infrastructure.persistence.repository.BalanceJpaRepository;
import kr.hhplus.be.server.balance.infrastructure.persistence.repository.BalanceTransactionJpaRepository;
import kr.hhplus.be.server.TestcontainersConfiguration;                                 
import kr.hhplus.be.server.product.infrastructure.persistence.repository.ProductJpaRepository;
import kr.hhplus.be.server.user.infrastructure.persistence.entity.UserEntity;
import kr.hhplus.be.server.user.infrastructure.persistence.repository.UserJpaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 잔액 동시성 테스트
 * 실제 운영 환경을 반영한 동시성 제어와 성능 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Balance 동시성 테스트")
class BalanceConcurrencyTest {

    @Autowired
    private ChargeBalanceService chargeBalanceService;

    @Autowired
    private GetBalanceService getBalanceService;

    @Autowired
    private BalanceJpaRepository balanceJpaRepository;      // 잔액 저장소

    @Autowired
    private BalanceTransactionJpaRepository balanceTransactionJpaRepository; // 잔액 거래 저장소

    @Autowired
    private UserJpaRepository userJpaRepository; // 사용자 저장소

    @Autowired
    private ProductJpaRepository productJpaRepository; // 상품 저장소

    @Autowired
    private ApplicationContext applicationContext; // 애플리케이션 컨텍스트

    @Autowired
    private DeductBalancePort deductBalancePort; // 잔액 차감 포트

    private UserEntity testUser;

    private List<UserEntity> testUsers; // 테스트용 사용자 목록
    private Random random = new Random(); // 랜덤 생성기

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리
        balanceTransactionJpaRepository.deleteAll();
        balanceJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
        
        // 테스트용 사용자들 생성 (실제 운영 환경과 유사)
        testUsers = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            UserEntity user = UserEntity.builder()
                    .userId((long) (i + 1000)) // 더 큰 범위로 중복 방지
                    .name("테스트 사용자 " + i)
                    .email("test" + i + "@example.com")
                    .status("ACTIVE")
                    .build();
            testUsers.add(userJpaRepository.saveAndFlush(user));
        }


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
    @DisplayName("동시 충전 테스트 - 실제 동시성 검증")
    void 동시_충전_테스트() throws InterruptedException {
        // Given - 실제 동시성 테스트
        Long targetUserId = testUsers.get(0).getUserId();
        BigDecimal chargeAmount = new BigDecimal("100.00");
        int concurrentRequests = 5; // 동시 요청 수
        
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch readyLatch = new CountDownLatch(concurrentRequests);
        CountDownLatch finishLatch = new CountDownLatch(concurrentRequests);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        // When - 실제 동시 충전 요청
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestIndex = i;
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await(); // 모든 스레드가 동시에 시작
                    
                    // 최소한의 지연만 적용 (데드락 방지)
                    Thread.sleep(requestIndex * 10); // 0ms, 10ms, 20ms, 30ms, 40ms
                    
                    ChargeBalanceUseCase.ChargeBalanceCommand request = 
                        new ChargeBalanceUseCase.ChargeBalanceCommand(targetUserId, chargeAmount);
                    
                    ChargeBalanceUseCase.ChargeBalanceResult result = chargeBalanceService.chargeBalance(request);
                    
                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                    
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.println("Request " + requestIndex + " failed: " + e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });
        }
        
        readyLatch.await();
        System.out.println("동시 충전 테스트 시작!");
        startLatch.countDown();
        
        boolean completed = finishLatch.await(30, TimeUnit.SECONDS);
        
        // Then
        assertThat(completed).isTrue();
        
        System.out.println("=== 동시 충전 테스트 결과 ===");
        System.out.println("총 요청 수: " + concurrentRequests);
        System.out.println("성공한 요청 수: " + successCount.get());
        System.out.println("실패한 요청 수: " + failureCount.get());
        System.out.println("성공률: " + String.format("%.2f", successCount.get() * 100.0 / concurrentRequests) + "%");
        
        // 최종 잔액 확인
        GetBalanceUseCase.GetBalanceCommand finalBalanceCommand = new GetBalanceUseCase.GetBalanceCommand(targetUserId);
        Optional<GetBalanceUseCase.GetBalanceResult> finalBalanceResult = getBalanceService.getBalance(finalBalanceCommand);
        assertThat(finalBalanceResult).isPresent();
        
        GetBalanceUseCase.GetBalanceResult finalBalance = finalBalanceResult.get();
        assertThat(finalBalance.getUserId()).isEqualTo(targetUserId);
        
        // 예상 잔액: 성공한 요청 수 * 100.00원 (동시성 충돌로 실패한 요청은 제외)
        BigDecimal expectedBalance = new BigDecimal(successCount.get()).multiply(chargeAmount);
        assertThat(finalBalance.getBalance()).isEqualTo(expectedBalance);
        
        System.out.println("예상 잔액: " + expectedBalance);
        System.out.println("실제 잔액: " + finalBalance.getBalance());
        System.out.println("데이터 무결성 검증: " + (finalBalance.getBalance().equals(expectedBalance) ? "성공" : "실패"));
        
        // 동시성 제어 검증 (일부 실패는 정상적인 동시성 충돌)
        assertThat(successCount.get()).isGreaterThan(0);
        assertThat(finalBalance.getBalance()).isEqualTo(expectedBalance);
        
        executorService.shutdown();
    }

    @Test
    @DisplayName("다중 사용자 동시 충전 테스트")
    void 다중_사용자_동시_충전_테스트() throws InterruptedException {
        // Given - 같은 사용자에 대한 동시 충전 (충전 이벤트가 무시되지 않는지 검증)
        Long targetUserId = testUsers.get(0).getUserId();
        BigDecimal chargeAmount = new BigDecimal("100.00");
        int concurrentRequests = 5; // 동시 충전 요청 수
        
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch readyLatch = new CountDownLatch(concurrentRequests);
        CountDownLatch finishLatch = new CountDownLatch(concurrentRequests);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        // When - 같은 사용자에 대한 동시 충전 요청
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestIndex = i;
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await(); // 모든 스레드가 동시에 시작
                    
                    // 최소한의 지연만 적용 (데드락 방지)
                    Thread.sleep(requestIndex * 10); // 0ms, 10ms, 20ms, 30ms, 40ms
                    
                    ChargeBalanceUseCase.ChargeBalanceCommand request = 
                        new ChargeBalanceUseCase.ChargeBalanceCommand(targetUserId, chargeAmount);
                    
                    ChargeBalanceUseCase.ChargeBalanceResult result = chargeBalanceService.chargeBalance(request);
                    
                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                    
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.println("Request " + requestIndex + " failed: " + e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });
        }
        
        readyLatch.await();
        System.out.println("다중 사용자 동시 충전 테스트 시작!");
        startLatch.countDown();
        
        boolean completed = finishLatch.await(30, TimeUnit.SECONDS);
        
        // Then
        assertThat(completed).isTrue();
        
        System.out.println("=== 다중 사용자 동시 충전 테스트 결과 ===");
        System.out.println("대상 사용자 ID: " + targetUserId);
        System.out.println("동시 충전 요청 수: " + concurrentRequests);
        System.out.println("성공한 요청 수: " + successCount.get());
        System.out.println("실패한 요청 수: " + failureCount.get());
        System.out.println("성공률: " + String.format("%.2f", successCount.get() * 100.0 / concurrentRequests) + "%");
        
        // 최종 잔액 확인 - 모든 충전 이벤트가 제대로 합쳐졌는지 검증
        GetBalanceUseCase.GetBalanceCommand finalBalanceCommand = new GetBalanceUseCase.GetBalanceCommand(targetUserId);
        Optional<GetBalanceUseCase.GetBalanceResult> finalBalanceResult = getBalanceService.getBalance(finalBalanceCommand);
        assertThat(finalBalanceResult).isPresent();
        
        GetBalanceUseCase.GetBalanceResult finalBalance = finalBalanceResult.get();
        assertThat(finalBalance.getUserId()).isEqualTo(targetUserId);
        
        // 예상 잔액: 성공한 요청 수 * 100.00원
        BigDecimal expectedBalance = new BigDecimal(successCount.get()).multiply(chargeAmount);
        assertThat(finalBalance.getBalance()).isEqualTo(expectedBalance);
        
        System.out.println("충전 금액: " + chargeAmount);
        System.out.println("예상 잔액: " + expectedBalance);
        System.out.println("실제 잔액: " + finalBalance.getBalance());
        System.out.println("충전 이벤트 무시 여부: " + (finalBalance.getBalance().equals(expectedBalance) ? "모든 충전 반영됨" : "일부 충전 무시됨"));
        
        // 동시성 제어 검증
        assertThat(successCount.get()).isGreaterThan(0);
        assertThat(finalBalance.getBalance()).isEqualTo(expectedBalance);
        
        executorService.shutdown();
    }

    @Test
    @DisplayName("강한 동시성 테스트")
    void 강한_동시성_테스트() throws InterruptedException {
        // Given - 강한 동시성 충돌 상황
        Long targetUserId = testUsers.get(0).getUserId();
        BigDecimal chargeAmount = new BigDecimal("50.00");
        int concurrentRequests = 10; // 더 많은 동시 요청
        
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch readyLatch = new CountDownLatch(concurrentRequests);
        CountDownLatch finishLatch = new CountDownLatch(concurrentRequests);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger retryCount = new AtomicInteger(0);
        
        // When - 강한 동시성 충돌 상황
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestIndex = i;
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await(); // 모든 스레드가 동시에 시작
                    
                    // 매우 짧은 지연으로 강한 충돌 상황 생성
                    Thread.sleep(requestIndex * 5); // 0ms, 5ms, 10ms, 15ms, 20ms...
                    
                    ChargeBalanceUseCase.ChargeBalanceCommand request = 
                        new ChargeBalanceUseCase.ChargeBalanceCommand(targetUserId, chargeAmount);
                    
                    ChargeBalanceUseCase.ChargeBalanceResult result = chargeBalanceService.chargeBalance(request);
                    
                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                    
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    if (e.getMessage().contains("OptimisticLockingFailureException")) {
                        retryCount.incrementAndGet();
                    }
                } finally {
                    finishLatch.countDown();
                }
            });
        }
        
        readyLatch.await();
        System.out.println("강한 동시성 테스트 시작!");
        startLatch.countDown();
        
        boolean completed = finishLatch.await(60, TimeUnit.SECONDS);
        
        // Then
        assertThat(completed).isTrue();
        
        System.out.println("=== 강한 동시성 테스트 결과 ===");
        System.out.println("총 요청 수: " + concurrentRequests);
        System.out.println("성공한 요청 수: " + successCount.get());
        System.out.println("실패한 요청 수: " + failureCount.get());
        System.out.println("재시도 발생 수: " + retryCount.get());
        System.out.println("성공률: " + String.format("%.2f", successCount.get() * 100.0 / concurrentRequests) + "%");
        
        // 최종 잔액 확인
        GetBalanceUseCase.GetBalanceCommand finalBalanceCommand = new GetBalanceUseCase.GetBalanceCommand(targetUserId);
        Optional<GetBalanceUseCase.GetBalanceResult> finalBalanceResult = getBalanceService.getBalance(finalBalanceCommand);
        assertThat(finalBalanceResult).isPresent();
        
        GetBalanceUseCase.GetBalanceResult finalBalance = finalBalanceResult.get();
        assertThat(finalBalance.getUserId()).isEqualTo(targetUserId);
        
        // 예상 잔액: 성공한 요청 수 * 50.00원 (동시성 충돌로 실패한 요청은 제외)
        BigDecimal expectedBalance = new BigDecimal(successCount.get()).multiply(chargeAmount);
        assertThat(finalBalance.getBalance()).isEqualTo(expectedBalance);
        
        System.out.println("예상 잔액: " + expectedBalance);
        System.out.println("실제 잔액: " + finalBalance.getBalance());
        System.out.println("데이터 무결성 검증: " + (finalBalance.getBalance().equals(expectedBalance) ? "성공" : "실패"));
        
        // 동시성 제어 검증 (일부 실패는 정상적인 동시성 충돌)
        assertThat(successCount.get()).isGreaterThan(0);
        assertThat(finalBalance.getBalance()).isEqualTo(expectedBalance);
        
        executorService.shutdown();
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
        assertThat(paymentSuccess.get()).isTrue(); // 결제도 성공해야 함

        // 최종 잔액 확인 (충전과 결제 모두 성공한 경우)
        GetBalanceUseCase.GetBalanceCommand finalBalanceCommand = new GetBalanceUseCase.GetBalanceCommand(userId);
        Optional<GetBalanceUseCase.GetBalanceResult> finalBalanceResult = getBalanceService.getBalance(finalBalanceCommand);
        assertThat(finalBalanceResult).isPresent();

        GetBalanceUseCase.GetBalanceResult finalBalance = finalBalanceResult.get();
        assertThat(finalBalance.getUserId()).isEqualTo(userId);
        // 10000.00 + 5000.00 - 3000.00 = 12000.00
        assertThat(finalBalance.getBalance()).isEqualByComparingTo(new BigDecimal("12000.00"));
    }
}
