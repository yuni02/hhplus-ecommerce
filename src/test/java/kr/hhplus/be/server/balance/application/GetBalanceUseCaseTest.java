package kr.hhplus.be.server.balance.application;

import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.domain.BalanceRepository;
import kr.hhplus.be.server.user.domain.UserRepository;
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
@DisplayName("GetBalanceUseCase 단위 테스트")
class GetBalanceUseCaseTest {

    @Mock
    private BalanceRepository balanceRepository;

    @Mock
    private UserRepository userRepository;

    private GetBalanceUseCase getBalanceUseCase;

    @BeforeEach
    void setUp() {
        getBalanceUseCase = new GetBalanceUseCase(balanceRepository, userRepository);
    }

    @Test
    @DisplayName("존재하는 사용자의 잔액 조회")
    void getBalance_ExistingUser_ReturnsBalance() {
        // given
        Long userId = 1L;
        Balance balance = new Balance(userId);
        balance.setId(1L);
        balance.setAmount(BigDecimal.valueOf(15000));

        when(userRepository.existsById(userId)).thenReturn(true);
        when(balanceRepository.findActiveBalanceByUserId(userId))
                .thenReturn(Optional.of(balance));

        // when
        Optional<Balance> result = getBalanceUseCase.execute(userId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getAmount()).isEqualTo(BigDecimal.valueOf(15000));
        verify(userRepository).existsById(userId);
        verify(balanceRepository).findActiveBalanceByUserId(userId);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 잔액 조회 시 빈 결과 반환")
    void getBalance_NonExistentUser_ReturnsEmpty() {
        // given
        Long userId = 999L;

        when(userRepository.existsById(userId)).thenReturn(false);

        // when
        Optional<Balance> result = getBalanceUseCase.execute(userId);

        // then
        assertThat(result).isEmpty();
        verify(userRepository).existsById(userId);
        verifyNoInteractions(balanceRepository);
    }

    @Test
    @DisplayName("사용자는 존재하지만 잔액이 없는 경우")
    void getBalance_UserExistsButNoBalance_ReturnsEmpty() {
        // given
        Long userId = 1L;

        when(userRepository.existsById(userId)).thenReturn(true);
        when(balanceRepository.findActiveBalanceByUserId(userId))
                .thenReturn(Optional.empty());

        // when
        Optional<Balance> result = getBalanceUseCase.execute(userId);

        // then
        assertThat(result).isEmpty();
        verify(userRepository).existsById(userId);
        verify(balanceRepository).findActiveBalanceByUserId(userId);
    }
} 