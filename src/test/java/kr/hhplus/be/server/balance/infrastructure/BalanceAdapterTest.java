package kr.hhplus.be.server.balance.infrastructure;

import kr.hhplus.be.server.balance.application.ChargeBalanceUseCase;
import kr.hhplus.be.server.balance.application.GetBalanceUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("BalanceAdapter 단위 테스트")
class BalanceAdapterTest {

    private BalanceAdapter balanceAdapter;

    @BeforeEach
    void setUp() {
        balanceAdapter = new BalanceAdapter();
    }

    @Test
    @DisplayName("잔액 조회 요청 변환 - 유효한 userId")
    void adaptGetBalanceRequest_ValidUserId_ReturnsInput() {
        // given
        Long userId = 1L;

        // when
        GetBalanceUseCase.Input result = balanceAdapter.adaptGetBalanceRequest(userId);

        // then
        assertThat(result.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("잔액 조회 요청 변환 - null userId")
    void adaptGetBalanceRequest_NullUserId_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> balanceAdapter.adaptGetBalanceRequest(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 사용자 ID입니다.");
    }

    @Test
    @DisplayName("잔액 조회 요청 변환 - 음수 userId")
    void adaptGetBalanceRequest_NegativeUserId_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> balanceAdapter.adaptGetBalanceRequest(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 사용자 ID입니다.");
    }

    @Test
    @DisplayName("잔액 조회 응답 변환")
    void adaptGetBalanceResponse_ValidOutput_ReturnsMap() {
        // given
        GetBalanceUseCase.Output output = new GetBalanceUseCase.Output(1L, BigDecimal.valueOf(15000));

        // when
        Map<String, Object> result = balanceAdapter.adaptGetBalanceResponse(output);

        // then
        assertThat(result.get("userId")).isEqualTo(1L);
        assertThat(result.get("balance")).isEqualTo(BigDecimal.valueOf(15000));
    }

    @Test
    @DisplayName("잔액 충전 요청 변환 - 유효한 요청")
    void adaptChargeRequest_ValidRequest_ReturnsInput() {
        // given
        Map<String, Object> request = Map.of(
                "userId", 1L,
                "amount", 10000
        );

        // when
        ChargeBalanceUseCase.Input result = balanceAdapter.adaptChargeRequest(request);

        // then
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getAmount()).isEqualTo(BigDecimal.valueOf(10000));
    }

    @Test
    @DisplayName("잔액 충전 요청 변환 - null userId")
    void adaptChargeRequest_NullUserId_ThrowsException() {
        // given
        Map<String, Object> request = Map.of("amount", 10000);

        // when & then
        assertThatThrownBy(() -> balanceAdapter.adaptChargeRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자 ID는 필수입니다.");
    }

    @Test
    @DisplayName("잔액 충전 요청 변환 - null amount")
    void adaptChargeRequest_NullAmount_ThrowsException() {
        // given
        Map<String, Object> request = Map.of("userId", 1L);

        // when & then
        assertThatThrownBy(() -> balanceAdapter.adaptChargeRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액은 필수입니다.");
    }

    @Test
    @DisplayName("잔액 충전 요청 변환 - 음수 amount")
    void adaptChargeRequest_NegativeAmount_ThrowsException() {
        // given
        Map<String, Object> request = Map.of(
                "userId", 1L,
                "amount", -1000
        );

        // when & then
        assertThatThrownBy(() -> balanceAdapter.adaptChargeRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액은 양수여야 합니다.");
    }

    @Test
    @DisplayName("잔액 충전 요청 변환 - 최대 금액 초과")
    void adaptChargeRequest_ExceedMaxAmount_ThrowsException() {
        // given
        Map<String, Object> request = Map.of(
                "userId", 1L,
                "amount", 1000001
        );

        // when & then
        assertThatThrownBy(() -> balanceAdapter.adaptChargeRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("1회 최대 충전 금액은 1,000,000원입니다.");
    }

    @Test
    @DisplayName("잔액 충전 응답 변환")
    void adaptChargeResponse_ValidOutput_ReturnsMap() {
        // given
        ChargeBalanceUseCase.Output output = new ChargeBalanceUseCase.Output(
                1L, BigDecimal.valueOf(15000), 1L);

        // when
        Map<String, Object> result = balanceAdapter.adaptChargeResponse(output);

        // then
        assertThat(result).containsKey("message");
        assertThat(result.get("userId")).isEqualTo(1L);
        assertThat(result.get("balance")).isEqualTo(BigDecimal.valueOf(15000));
        assertThat(result.get("transactionId")).isEqualTo(1L);
    }
} 