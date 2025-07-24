package kr.hhplus.be.server.balance.application;

import kr.hhplus.be.server.balance.application.port.in.GetBalanceUseCase;
import kr.hhplus.be.server.balance.application.facade.BalanceFacade;

import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 잔액 조회 Application 서비스 (Facade 패턴 적용)
 */
@Service
public class GetBalanceService implements GetBalanceUseCase {

    private final BalanceFacade balanceFacade;

    public GetBalanceService(BalanceFacade balanceFacade) {
        this.balanceFacade = balanceFacade;
    }

    @Override
    public Optional<GetBalanceResult> getBalance(GetBalanceCommand command) {
        return balanceFacade.getBalance(command);
    }
} 