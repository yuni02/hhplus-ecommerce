package kr.hhplus.be.server.balance.infrastructure;

import kr.hhplus.be.server.balance.application.ChargeBalanceUseCase;
import kr.hhplus.be.server.balance.application.GetBalanceUseCase;
import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.dto.request.BalanceChargeRequest;
import kr.hhplus.be.server.dto.response.BalanceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BalanceController 단위 테스트")
class BalanceControllerTest {

    @Mock
    private GetBalanceUseCase getBalanceUseCase;

    @Mock
    private ChargeBalanceUseCase chargeBalanceUseCase;

    private BalanceController balanceController;

    @BeforeEach
    void setUp() {
        balanceController = new BalanceController(getBalanceUseCase, chargeBalanceUseCase);
    }

    @Test
    @DisplayName("정상적인 잔액 조회")
    void getBalance_ValidUserId_ReturnsBalance() {
        // given
        Long userId = 1L;
        Balance balance = new Balance(userId);
        balance.setAmount(BigDecimal.valueOf(15000));

        when(getBalanceUseCase.execute(userId)).thenReturn(Optional.of(balance));

        // when
        ResponseEntity<?> response = balanceController.getBalance(userId);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(BalanceResponse.class);
        
        BalanceResponse responseBody = (BalanceResponse) response.getBody();
        assertThat(responseBody.getUserId()).isEqualTo(userId);
        assertThat(responseBody.getBalance()).isEqualTo(15000);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 잔액 조회")
    void getBalance_NonExistentUser_ReturnsBadRequest() {
        // given
        Long userId = 999L;

        when(getBalanceUseCase.execute(userId)).thenReturn(Optional.empty());

        // when
        ResponseEntity<?> response = balanceController.getBalance(userId);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertThat(responseBody.get("message")).isEqualTo("사용자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("잘못된 사용자 ID로 잔액 조회")
    void getBalance_InvalidUserId_ReturnsBadRequest() {
        // given
        Long userId = -1L;

        // when
        ResponseEntity<?> response = balanceController.getBalance(userId);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertThat(responseBody.get("message")).isEqualTo("유효하지 않은 사용자 ID입니다.");
    }

    @Test
    @DisplayName("정상적인 잔액 충전")
    void chargeBalance_ValidRequest_Success() {
        // given
        Long userId = 1L;
        Integer amount = 10000;
        
        BalanceChargeRequest request = new BalanceChargeRequest(userId, amount);
        
        Balance balance = new Balance(userId);
        balance.setAmount(BigDecimal.valueOf(25000));

        when(chargeBalanceUseCase.execute(eq(userId), any(BigDecimal.class))).thenReturn(balance);

        // when
        ResponseEntity<?> response = balanceController.chargeBalance(request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertThat(responseBody.get("message")).isEqualTo("잔액 충전이 완료되었습니다.");
        
        BalanceResponse balanceData = (BalanceResponse) responseBody.get("balance");
        assertThat(balanceData.getUserId()).isEqualTo(userId);
        assertThat(balanceData.getBalance()).isEqualTo(25000);
    }

    @Test
    @DisplayName("잘못된 사용자 ID로 잔액 충전")
    void chargeBalance_InvalidUserId_ReturnsBadRequest() {
        // given
        Long userId = -1L;
        Integer amount = 10000;
        
        BalanceChargeRequest request = new BalanceChargeRequest(userId, amount);

        // when
        ResponseEntity<?> response = balanceController.chargeBalance(request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertThat(responseBody.get("message")).isEqualTo("유효하지 않은 사용자 ID입니다.");
    }

    @Test
    @DisplayName("잘못된 충전 금액으로 잔액 충전")
    void chargeBalance_InvalidAmount_ReturnsBadRequest() {
        // given
        Long userId = 1L;
        Integer amount = -1000;
        
        BalanceChargeRequest request = new BalanceChargeRequest(userId, amount);

        // when
        ResponseEntity<?> response = balanceController.chargeBalance(request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertThat(responseBody.get("message")).isEqualTo("충전 금액은 양수여야 합니다.");
    }

    @Test
    @DisplayName("최대 충전 금액 초과로 잔액 충전")
    void chargeBalance_ExceedMaxAmount_ReturnsBadRequest() {
        // given
        Long userId = 1L;
        Integer amount = 1000001;
        
        BalanceChargeRequest request = new BalanceChargeRequest(userId, amount);

        // when
        ResponseEntity<?> response = balanceController.chargeBalance(request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertThat(responseBody.get("message")).isEqualTo("1회 최대 충전 금액은 1,000,000원입니다.");
    }

    @Test
    @DisplayName("잔액 충전 중 예외 발생")
    void chargeBalance_ExceptionOccurs_ReturnsInternalServerError() {
        // given
        Long userId = 1L;
        Integer amount = 10000;
        
        BalanceChargeRequest request = new BalanceChargeRequest(userId, amount);

        when(chargeBalanceUseCase.execute(eq(userId), any(BigDecimal.class)))
                .thenThrow(new RuntimeException("Database error"));

        // when
        ResponseEntity<?> response = balanceController.chargeBalance(request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertThat(responseBody.get("message")).isEqualTo("잔액 충전 중 오류가 발생했습니다.");
    }
} 