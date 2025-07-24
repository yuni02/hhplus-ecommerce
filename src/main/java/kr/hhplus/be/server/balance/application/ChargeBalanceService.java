package kr.hhplus.be.server.balance.application;

import kr.hhplus.be.server.balance.application.port.in.ChargeBalanceUseCase;
import kr.hhplus.be.server.balance.application.facade.BalanceFacade;

import org.springframework.stereotype.Service;

/**
 * 잔액 충전 Application 서비스 (Facade 패턴 적용)
 */
@Service
public class ChargeBalanceService implements ChargeBalanceUseCase {

    private final BalanceFacade balanceFacade;

    public ChargeBalanceService(BalanceFacade balanceFacade) {
        this.balanceFacade = balanceFacade;
    }

    @Override
    public ChargeBalanceResult chargeBalance(ChargeBalanceCommand command) {
        return balanceFacade.chargeBalance(command);
    }
} 