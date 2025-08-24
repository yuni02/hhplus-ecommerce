package kr.hhplus.be.server.unit.balance.adapter.in.web;

import kr.hhplus.be.server.balance.adapter.in.dto.ChargeBalanceRequest;
import kr.hhplus.be.server.balance.adapter.in.web.BalanceController;
import kr.hhplus.be.server.balance.application.port.in.ChargeBalanceUseCase;
import kr.hhplus.be.server.balance.application.port.in.GetBalanceUseCase;
import kr.hhplus.be.server.shared.exception.GlobalExceptionHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BalanceControllerTest {

    @Mock
    private GetBalanceUseCase getBalanceUseCase;
    
    @Mock
    private ChargeBalanceUseCase chargeBalanceUseCase;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(
                new BalanceController(getBalanceUseCase, chargeBalanceUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("잔액 조회 성공")
    void getBalance_Success() throws Exception {
        // given
        Long userId = 1L;
        BigDecimal balance = BigDecimal.valueOf(50000);
        GetBalanceUseCase.GetBalanceResult result = 
            new GetBalanceUseCase.GetBalanceResult(userId, balance);

        when(getBalanceUseCase.getBalance(any(GetBalanceUseCase.GetBalanceCommand.class)))
                .thenReturn(Optional.of(result));

        // when & then
        mockMvc.perform(get("/api/users/balance")
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.balance").value(50000));

        verify(getBalanceUseCase).getBalance(any(GetBalanceUseCase.GetBalanceCommand.class));
    }

    @Test
    @DisplayName("잔액 조회 실패 - 사용자가 존재하지 않는 경우")
    void getBalance_Failure_UserNotFound() throws Exception {
        // given
        Long userId = 999L;

        when(getBalanceUseCase.getBalance(any(GetBalanceUseCase.GetBalanceCommand.class)))
                .thenReturn(Optional.empty());

        // when & then
        mockMvc.perform(get("/api/users/balance")
                        .param("userId", userId.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));

        verify(getBalanceUseCase).getBalance(any(GetBalanceUseCase.GetBalanceCommand.class));
    }

    @Test
    @DisplayName("잔액 조회 실패 - 잘못된 요청")
    void getBalance_Failure_InvalidRequest() throws Exception {
        // when & then
        mockMvc.perform(get("/api/users/balance")
                        .param("userId", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("잔액 충전 성공")
    void chargeBalance_Success() throws Exception {
        // given
        Long userId = 1L;
        Integer amount = 10000;
        BigDecimal newBalance = BigDecimal.valueOf(60000);
        Long transactionId = 1L;

        ChargeBalanceRequest request = new ChargeBalanceRequest(userId, amount);
        ChargeBalanceUseCase.ChargeBalanceResult result = 
            ChargeBalanceUseCase.ChargeBalanceResult.success(userId, newBalance, transactionId, newBalance);

        when(chargeBalanceUseCase.chargeBalance(any(ChargeBalanceUseCase.ChargeBalanceCommand.class)))
                .thenReturn(result);

        // when & then
        mockMvc.perform(post("/api/users/balance/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.chargeAmount").value(amount))
                .andExpect(jsonPath("$.balanceAfterCharge").value(60000));

        verify(chargeBalanceUseCase).chargeBalance(any(ChargeBalanceUseCase.ChargeBalanceCommand.class));
    }

    @Test
    @DisplayName("잔액 충전 실패 - 사용자가 존재하지 않는 경우")
    void chargeBalance_Failure_UserNotFound() throws Exception {
        // given
        Long userId = 999L;
        Integer amount = 10000;
        String errorMessage = "사용자를 찾을 수 없습니다.";

        ChargeBalanceRequest request = new ChargeBalanceRequest(userId, amount);
        ChargeBalanceUseCase.ChargeBalanceResult result = 
            ChargeBalanceUseCase.ChargeBalanceResult.failure(errorMessage);

        when(chargeBalanceUseCase.chargeBalance(any(ChargeBalanceUseCase.ChargeBalanceCommand.class)))
                .thenReturn(result);

        // when & then
        mockMvc.perform(post("/api/users/balance/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(errorMessage));

        verify(chargeBalanceUseCase).chargeBalance(any(ChargeBalanceUseCase.ChargeBalanceCommand.class));
    }

    @Test
    @DisplayName("잔액 충전 실패 - 잘못된 요청")
    void chargeBalance_Failure_InvalidRequest() throws Exception {
        // given
        String invalidJson = "{\"userId\": \"invalid\", \"amount\": -1000}";

        // when & then
        mockMvc.perform(post("/api/users/balance/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("잔액 충전 실패 - 서버 오류")
    void chargeBalance_Failure_ServerError() throws Exception {
        // given
        Long userId = 1L;
        Integer amount = 10000;
        String errorMessage = "잔액 충전 중 오류가 발생했습니다.";

        ChargeBalanceRequest request = new ChargeBalanceRequest(userId, amount);
        ChargeBalanceUseCase.ChargeBalanceResult result = 
            ChargeBalanceUseCase.ChargeBalanceResult.failure(errorMessage);

        when(chargeBalanceUseCase.chargeBalance(any(ChargeBalanceUseCase.ChargeBalanceCommand.class)))
                .thenReturn(result);

        // when & then
        mockMvc.perform(post("/api/users/balance/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(errorMessage));

        verify(chargeBalanceUseCase).chargeBalance(any(ChargeBalanceUseCase.ChargeBalanceCommand.class));
    }
} 