package kr.hhplus.be.server.balance.domain;

import kr.hhplus.be.server.user.domain.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class BalanceServiceImpl implements BalanceService {

    private final BalanceRepository balanceRepository;
    private final BalanceTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public BalanceServiceImpl(BalanceRepository balanceRepository,
                             BalanceTransactionRepository transactionRepository,
                             UserRepository userRepository) {
        this.balanceRepository = balanceRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Override
    public BalanceChargeResult chargeBalance(Long userId, BigDecimal amount) {
        try {
            // 사용자 존재 확인
            if (!userRepository.existsById(userId)) {
                return BalanceChargeResult.failure("사용자를 찾을 수 없습니다.");
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

            // 잔액 저장
            balance = balanceRepository.save(balance);

            // 거래 완료 처리
            transaction = BalanceDomainService.completeTransaction(transaction);
            transactionRepository.save(transaction);

            return BalanceChargeResult.success(balance.getUserId(), balance.getAmount(), transaction.getId());
        } catch (Exception e) {
            return BalanceChargeResult.failure("잔액 충전 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Override
    public BalanceDeductResult deductBalance(Long userId, BigDecimal amount) {
        try {
            // 사용자 존재 확인
            if (!userRepository.existsById(userId)) {
                return BalanceDeductResult.failure("사용자를 찾을 수 없습니다.");
            }

            // 잔액 조회
            Balance balance = balanceRepository.findActiveBalanceByUserId(userId)
                    .orElse(null);

            if (balance == null) {
                return BalanceDeductResult.failure("잔액 정보를 찾을 수 없습니다.");
            }

            // 잔액 충분 여부 확인
            if (!BalanceDomainService.hasSufficientBalance(balance, amount)) {
                return BalanceDeductResult.failure("잔액이 부족합니다.");
            }

            // 도메인 서비스를 통한 잔액 차감
            balance = BalanceDomainService.deductBalance(balance, amount);

            // 거래 기록 생성
            BalanceTransaction transaction = BalanceDomainService.createTransaction(
                    userId, amount, BalanceTransaction.TransactionType.DEDUCT, "주문 결제");
            transaction = transactionRepository.save(transaction);

            // 잔액 저장
            balance = balanceRepository.save(balance);

            // 거래 완료 처리
            transaction = BalanceDomainService.completeTransaction(transaction);
            transactionRepository.save(transaction);

            return BalanceDeductResult.success(balance.getUserId(), balance.getAmount(), transaction.getId());
        } catch (Exception e) {
            return BalanceDeductResult.failure("잔액 차감 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Override
    public BalanceQueryResult getBalance(Long userId) {
        try {
            // 사용자 존재 확인
            if (!userRepository.existsById(userId)) {
                return BalanceQueryResult.notFound("사용자를 찾을 수 없습니다.");
            }

            // 잔액 조회
            return balanceRepository.findActiveBalanceByUserId(userId)
                    .map(balance -> BalanceQueryResult.found(balance.getUserId(), balance.getAmount()))
                    .orElse(BalanceQueryResult.notFound("잔액 정보를 찾을 수 없습니다."));
        } catch (Exception e) {
            return BalanceQueryResult.notFound("잔액 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
} 