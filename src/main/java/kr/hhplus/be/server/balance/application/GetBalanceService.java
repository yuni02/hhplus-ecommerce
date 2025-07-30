package kr.hhplus.be.server.balance.application;

import kr.hhplus.be.server.balance.application.port.in.GetBalanceUseCase;
import kr.hhplus.be.server.balance.application.port.out.LoadBalancePort;
import kr.hhplus.be.server.balance.application.port.out.LoadUserPort;
import kr.hhplus.be.server.balance.domain.Balance;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 잔액 조회 Application 서비스
 */
@Service
public class GetBalanceService implements GetBalanceUseCase {

    private final LoadUserPort loadUserPort;
    private final LoadBalancePort loadBalancePort;

    public GetBalanceService(@Qualifier("balanceUserPersistenceAdapter") LoadUserPort loadUserPort, 
                           LoadBalancePort loadBalancePort) {
        this.loadUserPort = loadUserPort;
        this.loadBalancePort = loadBalancePort;
    }

    @Override
    public Optional<GetBalanceResult> getBalance(GetBalanceCommand command) {
        try {
            // 1. 사용자 존재 확인
            if (!loadUserPort.existsById(command.getUserId())) {
                return Optional.empty();
            }

            // 2. 잔액 조회
            Optional<Balance> balanceOpt = loadBalancePort.loadActiveBalanceByUserId(command.getUserId());
            
            if (balanceOpt.isEmpty()) {
                return Optional.empty();
            }

            Balance balance = balanceOpt.get();
            return Optional.of(new GetBalanceResult(command.getUserId(), balance.getAmount()));

        } catch (Exception e) {
            return Optional.empty();
        }
    }
} 