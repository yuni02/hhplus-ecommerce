package kr.hhplus.be.server.balance.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.balance.adapter.in.dto.ChargeBalanceRequest;
import kr.hhplus.be.server.balance.application.facade.BalanceFacade;
import kr.hhplus.be.server.balance.application.port.in.ChargeBalanceUseCase;
import kr.hhplus.be.server.balance.application.port.in.GetBalanceUseCase;
import kr.hhplus.be.server.balance.application.response.BalanceResponse;
import kr.hhplus.be.server.balance.application.response.ChargeBalanceResponse;
import kr.hhplus.be.server.balance.application.response.ErrorResponse;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BalanceControllerTest {

    @Mock
    private BalanceFacade balanceFacade;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BalanceController(balanceFacade)).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("잔액 조회 성공")
    void getBalance_Success() throws Exception {
        // given
        Long userId = 1L;
        BigDecimal balance = new BigDecimal("50000");
        
        GetBalanceUseCase.GetBalanceCommand command = new GetBalanceUseCase.GetBalanceCommand(userId);
        GetBalanceUseCase.GetBalanceResult result = new GetBalanceUseCase.GetBalanceResult(userId, balance);
        
        when(balanceFacade.getBalance(command)).thenReturn(Optional.of(result));

        // when & then
        mockMvc.perform(get("/api/users/balance")
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.balance").value(50000));
    }

    @Test
    @DisplayName("잔액 조회 실패 - 사용자가 존재하지 않는 경우")
    void getBalance_Failure_UserNotFound() throws Exception {
        // given
        Long userId = 999L;
        GetBalanceUseCase.GetBalanceCommand command = new GetBalanceUseCase.GetBalanceCommand(userId);
        
        when(balanceFacade.getBalance(command)).thenReturn(Optional.empty());

        // when & then
        mockMvc.perform(get("/api/users/balance")
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("잔액 조회 실패 - 잘못된 요청")
    void getBalance_Failure_InvalidRequest() throws Exception {
        // given
        when(balanceFacade.getBalance(any())).thenThrow(new IllegalArgumentException("잘못된 사용자 ID입니다."));

        // when & then
        mockMvc.perform(get("/api/users/balance")
                        .param("userId", "-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("잘못된 사용자 ID입니다."));
    }

    @Test
    @DisplayName("잔액 충전 성공")
    void chargeBalance_Success() throws Exception {
        // given
        Long userId = 1L;
        Integer amount = 10000;
        BigDecimal newBalance = new BigDecimal("60000");
        Long transactionId = 1L;
        
        ChargeBalanceRequest request = new ChargeBalanceRequest();
        request.setUserId(userId);
        request.setAmount(amount);
        
        ChargeBalanceUseCase.ChargeBalanceCommand command = 
            new ChargeBalanceUseCase.ChargeBalanceCommand(userId, BigDecimal.valueOf(amount));
        ChargeBalanceUseCase.ChargeBalanceResult result = 
            ChargeBalanceUseCase.ChargeBalanceResult.success(userId, newBalance, transactionId);
        
        when(balanceFacade.chargeBalance(command)).thenReturn(result);

        // when & then
        mockMvc.perform(post("/api/users/balance/charge")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.chargeAmount").value(amount))
                .andExpect(jsonPath("$.newBalance").value(60000));
    }

    @Test
    @DisplayName("잔액 충전 실패 - 사용자가 존재하지 않는 경우")
    void chargeBalance_Failure_UserNotFound() throws Exception {
        // given
        Long userId = 999L;
        Integer amount = 10000;
        
        ChargeBalanceRequest request = new ChargeBalanceRequest();
        request.setUserId(userId);
        request.setAmount(amount);
        
        ChargeBalanceUseCase.ChargeBalanceCommand command = 
            new ChargeBalanceUseCase.ChargeBalanceCommand(userId, BigDecimal.valueOf(amount));
        ChargeBalanceUseCase.ChargeBalanceResult result = 
            ChargeBalanceUseCase.ChargeBalanceResult.failure("사용자를 찾을 수 없습니다.");
        
        when(balanceFacade.chargeBalance(command)).thenReturn(result);

        // when & then
        mockMvc.perform(post("/api/users/balance/charge")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("잔액 충전 실패 - 잘못된 요청")
    void chargeBalance_Failure_InvalidRequest() throws Exception {
        // given
        ChargeBalanceRequest request = new ChargeBalanceRequest();
        request.setUserId(1L);
        request.setAmount(-1000); // 음수 금액
        
        when(balanceFacade.chargeBalance(any())).thenThrow(new IllegalArgumentException("충전 금액은 0보다 커야 합니다."));

        // when & then
        mockMvc.perform(post("/api/users/balance/charge")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("충전 금액은 0보다 커야 합니다."));
    }

    @Test
    @DisplayName("잔액 충전 실패 - 서버 오류")
    void chargeBalance_Failure_ServerError() throws Exception {
        // given
        ChargeBalanceRequest request = new ChargeBalanceRequest();
        request.setUserId(1L);
        request.setAmount(10000);
        
        when(balanceFacade.chargeBalance(any())).thenThrow(new RuntimeException("데이터베이스 연결 오류"));

        // when & then
        mockMvc.perform(post("/api/users/balance/charge")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("잔액 충전 중 오류가 발생했습니다."));
    }
} 