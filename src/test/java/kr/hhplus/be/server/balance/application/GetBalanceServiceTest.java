package kr.hhplus.be.server.balance.application;

import kr.hhplus.be.server.balance.application.port.in.GetBalanceUseCase;
import kr.hhplus.be.server.balance.application.port.out.LoadBalancePort;
import kr.hhplus.be.server.balance.application.port.out.LoadUserPort;
import kr.hhplus.be.server.balance.domain.Balance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetBalanceServiceTest {

    @Mock
    private LoadUserPort loadUserPort;
    
    @Mock
    private LoadBalancePort loadBalancePort;

    private GetBalanceService getBalanceService;

    @BeforeEach
    void setUp() {
        getBalanceService = new GetBalanceService(loadUserPort, loadBalancePort);
    }

    @Test
    @DisplayName("잔액 조회 성공")
    void getBalance_Success() {
        // given
        Long userId = 1L;
        BigDecimal balance = BigDecimal.valueOf(50000);
        GetBalanceUseCase.GetBalanceCommand command = new GetBalanceUseCase.GetBalanceCommand(userId);

        Balance existingBalance = Balance.builder().userId(userId).amount(balance).build();

        when(loadUserPort.existsByUserId(userId)).thenReturn(true);
        when(loadBalancePort.loadActiveBalanceByUserId(userId)).thenReturn(Optional.of(existingBalance));

        // when
        Optional<GetBalanceUseCase.GetBalanceResult> result = getBalanceService.getBalance(command);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(userId);
        assertThat(result.get().getBalance()).isEqualTo(balance);
        
        verify(loadUserPort).existsByUserId(userId);
        verify(loadBalancePort).loadActiveBalanceByUserId(userId);
    }

    @Test
    @DisplayName("잔액 조회 실패 - 사용자가 존재하지 않는 경우")
    void getBalance_Failure_UserNotFound() {
        // given
        Long userId = 999L;
        GetBalanceUseCase.GetBalanceCommand command = new GetBalanceUseCase.GetBalanceCommand(userId);

        when(loadUserPort.existsByUserId(userId)).thenReturn(false);

        // when
        Optional<GetBalanceUseCase.GetBalanceResult> result = getBalanceService.getBalance(command);

        // then
        assertThat(result).isEmpty();
        
        verify(loadUserPort).existsByUserId(userId);
        verify(loadBalancePort, never()).loadActiveBalanceByUserId(any());
    }
} 