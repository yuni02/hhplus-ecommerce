package kr.hhplus.be.server.balance.application;

import kr.hhplus.be.server.balance.application.port.in.ChargeBalanceUseCase;
import kr.hhplus.be.server.balance.application.port.out.LoadBalancePort;
import kr.hhplus.be.server.balance.application.port.out.LoadUserPort;
import kr.hhplus.be.server.balance.application.port.out.SaveBalanceTransactionPort;
import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.domain.BalanceTransaction;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 잔액 충전 Application 서비스
 */
@Service
public class ChargeBalanceService implements ChargeBalanceUseCase {

    private final LoadUserPort loadUserPort;
    private final LoadBalancePort loadBalancePort;
    private final SaveBalanceTransactionPort saveBalanceTransactionPort;

    public ChargeBalanceService(LoadUserPort loadUserPort, 
                               LoadBalancePort loadBalancePort,
                               SaveBalanceTransactionPort saveBalanceTransactionPort) {
        this.loadUserPort = loadUserPort;
        this.loadBalancePort = loadBalancePort;
        this.saveBalanceTransactionPort = saveBalanceTransactionPort;
    }

    @Override
    @Transactional
    public ChargeBalanceResult chargeBalance(ChargeBalanceCommand command) {
        try {
            // 1. 입력값 검증
            if (command.getAmount() == null || command.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return ChargeBalanceResult.failure("충전 금액은 0보다 커야 합니다.");
            }
            
            // 2. 사용자 존재 확인
            if (!loadUserPort.existsByUserId(command.getUserId())) {
                return ChargeBalanceResult.failure("사용자를 찾을 수 없습니다.");
            }

            // 3. 기존 잔액 조회 또는 새로 생성
            Balance balance = loadBalancePort.loadActiveBalanceByUserId(command.getUserId())
                    .orElse(Balance.builder().userId(command.getUserId()).build());

            // 4. 잔액 충전 (도메인 로직)
            balance.charge(command.getAmount());
            Balance savedBalance = loadBalancePort.saveBalance(balance);

            // 5. 거래 내역 생성
            BalanceTransaction transaction = BalanceTransaction.create(
                    command.getUserId(), 
                    command.getAmount(), 
                    BalanceTransaction.TransactionType.CHARGE,  // 잔액 충전 타입
                    "잔액 충전"
            );
            BalanceTransaction savedTransaction = saveBalanceTransactionPort.saveBalanceTransaction(transaction);

            return ChargeBalanceResult.success(
                    command.getUserId(),
                    savedBalance.getAmount(),
                    savedTransaction.getId()
            );

        } catch (Exception e) {
            return ChargeBalanceResult.failure("잔액 충전 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
} 