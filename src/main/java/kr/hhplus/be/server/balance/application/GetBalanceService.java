package kr.hhplus.be.server.balance.application;

import org.springframework.stereotype.Service;

import kr.hhplus.be.server.balance.application.port.in.GetBalanceUseCase;
import kr.hhplus.be.server.balance.application.port.out.LoadBalancePort;
import kr.hhplus.be.server.balance.application.port.out.LoadUserPort;

import java.util.Optional;

/**
 * 잔액 조회 Application 서비스
 */
@Service
public class GetBalanceService implements GetBalanceUseCase {

    private final LoadUserPort loadUserPort;
    private final LoadBalancePort loadBalancePort;

    public GetBalanceService(LoadUserPort loadUserPort, LoadBalancePort loadBalancePort) {
        this.loadUserPort = loadUserPort;
        this.loadBalancePort = loadBalancePort;
    }

    @Override
    public Optional<GetBalanceResult> getBalance(GetBalanceCommand command) {
        // 사용자 존재 확인
        if (!loadUserPort.existsById(command.getUserId())) {
            return Optional.empty();
        }

        // 잔액 조회
        return loadBalancePort.loadActiveBalanceByUserId(command.getUserId())
                .map(balance -> new GetBalanceResult(balance.getUserId(), balance.getAmount()));
    }
} 