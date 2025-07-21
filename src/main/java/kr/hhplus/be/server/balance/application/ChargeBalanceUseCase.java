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

    public Balance execute(Long userId, BigDecimal amount) {
        // 사용자 존재 확인
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        // 잔액 조회 또는 생성
        Balance balance = balanceRepository.findActiveBalanceByUserId(userId)
                .orElseGet(() -> {
                    Balance newBalance = new Balance(userId);
                    return balanceRepository.save(newBalance);
                });

        // 도메인 서비스를 통한 잔액 충전
        balance = BalanceDomainService.chargeBalance(balance, amount);

        // 거래 기록 생성
        BalanceTransaction transaction = BalanceDomainService.createTransaction(
                userId, amount, BalanceTransaction.TransactionType.CHARGE, "잔액 충전");
        transaction = transactionRepository.save(transaction);

        try {
            // 잔액 저장
            balance = balanceRepository.save(balance);

            // 거래 완료 처리
            transaction = BalanceDomainService.completeTransaction(transaction);
            transactionRepository.save(transaction);

            return balance;
        } catch (Exception e) {
            // 실패 시 거래 실패 처리
            transaction = BalanceDomainService.failTransaction(transaction);
            transactionRepository.save(transaction);
            throw e;
        }
    }
} 