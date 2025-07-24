package kr.hhplus.be.server.balance.application;

import kr.hhplus.be.server.balance.application.facade.BalanceFacade;
import kr.hhplus.be.server.balance.application.port.in.GetBalanceUseCase;
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
    private BalanceFacade balanceFacade;

    private GetBalanceService getBalanceService;

    @BeforeEach
    void setUp() {
        getBalanceService = new GetBalanceService(balanceFacade);
    }

    @Test
    @DisplayName("잔액 조회 성공")
    void getBalance_Success() {
        // given
        Long userId = 1L;
        BigDecimal balance = new BigDecimal("50000");
        
        GetBalanceUseCase.GetBalanceCommand command = new GetBalanceUseCase.GetBalanceCommand(userId);
        GetBalanceUseCase.GetBalanceResult expectedResult = new GetBalanceUseCase.GetBalanceResult(userId, balance);
        
        when(balanceFacade.getBalance(command)).thenReturn(Optional.of(expectedResult));

        // when
        Optional<GetBalanceUseCase.GetBalanceResult> result = getBalanceService.getBalance(command);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(userId);
        assertThat(result.get().getBalance()).isEqualTo(balance);
        
        verify(balanceFacade).getBalance(command);
    }

    @Test
    @DisplayName("잔액 조회 실패 - 결과가 없는 경우")
    void getBalance_Failure_NoResult() {
        // given
        Long userId = 999L;
        GetBalanceUseCase.GetBalanceCommand command = new GetBalanceUseCase.GetBalanceCommand(userId);
        
        when(balanceFacade.getBalance(command)).thenReturn(Optional.empty());

        // when
        Optional<GetBalanceUseCase.GetBalanceResult> result = getBalanceService.getBalance(command);

        // then
        assertThat(result).isEmpty();
        verify(balanceFacade).getBalance(command);
    }
} 