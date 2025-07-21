package kr.hhplus.be.server.balance.application;

import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.domain.BalanceDomainService;
import kr.hhplus.be.server.balance.domain.BalanceRepository;
import kr.hhplus.be.server.balance.domain.BalanceTransaction;
import kr.hhplus.be.server.balance.domain.BalanceTransactionRepository;
import kr.hhplus.be.server.user.domain.UserRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 잔액 충전 UseCase
 * 외부 의존성 없이 도메인 서비스만 호출
 */
@Component
public class ChargeBalanceUseCase {

    private final BalanceRepository balanceRepository;
    private final BalanceTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public ChargeBalanceUseCase(BalanceRepository balanceRepository,
                               BalanceTransactionRepository transactionRepository,
                               UserRepository userRepository) {
        this.balanceRepository = balanceRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    public Output execute(Input input) {
        // 사용자 존재 확인
        if (!userRepository.existsById(input.userId)) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        // 잔액 조회 또는 생성
        Balance balance = balanceRepository.findActiveBalanceByUserId(input.userId)
                .orElseGet(() -> {
                    Balance newBalance = new Balance(input.userId);
                    return balanceRepository.save(newBalance);
                });

        // 도메인 서비스를 통한 잔액 충전
        balance = BalanceDomainService.chargeBalance(balance, input.amount);

        // 거래 기록 생성
        BalanceTransaction transaction = BalanceDomainService.createTransaction(
                input.userId, input.amount, BalanceTransaction.TransactionType.CHARGE, "잔액 충전");
        transaction = transactionRepository.save(transaction);

        try {
            // 잔액 저장
            balance = balanceRepository.save(balance);

            // 거래 완료 처리
            transaction = BalanceDomainService.completeTransaction(transaction);
            transactionRepository.save(transaction);

            return new Output(balance.getUserId(), balance.getAmount(), transaction.getId());
        } catch (Exception e) {
            // 실패 시 거래 실패 처리
            transaction = BalanceDomainService.failTransaction(transaction);
            transactionRepository.save(transaction);
            throw e;
        }
    }

    public static class Input {
        private final Long userId;
        private final BigDecimal amount;

        public Input(Long userId, BigDecimal amount) {
            this.userId = userId;
            this.amount = amount;
        }

        public Long getUserId() {
            return userId;
        }

        public BigDecimal getAmount() {
            return amount;
        }
    }

    public static class Output {
        private final Long userId;
        private final BigDecimal balance;
        private final Long transactionId;

        public Output(Long userId, BigDecimal balance, Long transactionId) {
            this.userId = userId;
            this.balance = balance;
            this.transactionId = transactionId;
        }

        public Long getUserId() {
            return userId;
        }

        public BigDecimal getBalance() {
            return balance;
        }

        public Long getTransactionId() {
            return transactionId;
        }
    }
} 