package kr.hhplus.be.server.balance.application;

import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.domain.BalanceRepository;
import kr.hhplus.be.server.user.domain.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 잔액 조회 UseCase
 * 외부 의존성 없이 도메인 서비스만 호출
 */
@Component
public class GetBalanceUseCase {

    private final BalanceRepository balanceRepository;
    private final UserRepository userRepository;

    public GetBalanceUseCase(BalanceRepository balanceRepository, UserRepository userRepository) {
        this.balanceRepository = balanceRepository;
        this.userRepository = userRepository;
    }

    public Optional<Balance> execute(Long userId) {
        // 사용자 존재 확인
        if (!userRepository.existsById(userId)) {
            return Optional.empty();
        }
        
        return balanceRepository.findActiveBalanceByUserId(userId);
    }
} 