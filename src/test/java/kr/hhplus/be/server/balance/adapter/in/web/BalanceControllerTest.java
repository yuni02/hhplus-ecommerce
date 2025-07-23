package kr.hhplus.be.server.balance.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.balance.adapter.in.dto.ChargeBalanceRequest;
import kr.hhplus.be.server.balance.application.facade.BalanceFacade;
import kr.hhplus.be.server.balance.application.port.in.ChargeBalanceUseCase;
import kr.hhplus.be.server.balance.application.port.in.GetBalanceUseCase;
import kr.hhplus.be.server.shared.exception.GlobalExceptionHandler;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BalanceControllerTest {

    @Mock
    private BalanceFacade balanceFacade;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BalanceController(balanceFacade))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("잔액 조회 성공")
    void getBalance_Success() throws Exception {
        // given
        Long userId = 1L;
        BigDecimal balance = BigDecimal.valueOf(50000);
        
        GetBalanceUseCase.GetBalanceCommand command = new GetBalanceUseCase.GetBalanceCommand(userId);
        GetBalanceUseCase.GetBalanceResult result = new GetBalanceUseCase.GetBalanceResult(userId, balance);
        
        // Mock 설정을 더 구체적으로
        when(balanceFacade.getBalance(any(GetBalanceUseCase.GetBalanceCommand.class)))
            .thenReturn(Optional.of(result));

        // when & then
        String response = mockMvc.perform(get("/api/users/balance")
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()) // 응답 내용 출력
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.balance").value(50000))
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        System.out.println("Response: " + response);
        
        // Mock이 실제로 호출되었는지 확인
        verify(balanceFacade).getBalance(any(GetBalanceUseCase.GetBalanceCommand.class));
    }

    @Test
    @DisplayName("잔액 조회 실패 - 사용자가 존재하지 않는 경우")
    void getBalance_Failure_UserNotFound() throws Exception {
        // given
        Long userId = 999L;
        
        GetBalanceUseCase.GetBalanceCommand command = new GetBalanceUseCase.GetBalanceCommand(userId);
        
        // Mock 설정을 더 구체적으로
        when(balanceFacade.getBalance(any(GetBalanceUseCase.GetBalanceCommand.class)))
            .thenReturn(Optional.empty());

        // when & then
        String response = mockMvc.perform(get("/api/users/balance")
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()) // 응답 내용 출력
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."))
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        System.out.println("Response: " + response);
        
        // Mock이 실제로 호출되었는지 확인
        verify(balanceFacade).getBalance(any(GetBalanceUseCase.GetBalanceCommand.class));
    }

    @Test
    @DisplayName("잔액 조회 실패 - 잘못된 요청")
    void getBalance_Failure_InvalidRequest() throws Exception {
        // given
        Long userId = 1L;
        
        GetBalanceUseCase.GetBalanceCommand command = new GetBalanceUseCase.GetBalanceCommand(userId);
        
        // Mock 설정을 더 구체적으로
        when(balanceFacade.getBalance(any(GetBalanceUseCase.GetBalanceCommand.class)))
            .thenThrow(new IllegalArgumentException("잘못된 사용자 ID입니다."));

        // when & then
        String response = mockMvc.perform(get("/api/users/balance")
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()) // 응답 내용 출력
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("잘못된 사용자 ID입니다."))
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        System.out.println("Response: " + response);
        
        // Mock이 실제로 호출되었는지 확인
        verify(balanceFacade).getBalance(any(GetBalanceUseCase.GetBalanceCommand.class));
    }

    @Test
    @DisplayName("잔액 충전 성공")
    void chargeBalance_Success() throws Exception {
        // given
        Long userId = 1L;
        int amount = 50000;
        BigDecimal newBalance = BigDecimal.valueOf(100000);
        Long transactionId = 1L;
        
        ChargeBalanceRequest request = new ChargeBalanceRequest(userId, amount);
        ChargeBalanceUseCase.ChargeBalanceCommand command = 
            new ChargeBalanceUseCase.ChargeBalanceCommand(userId, BigDecimal.valueOf(amount));
        ChargeBalanceUseCase.ChargeBalanceResult result = 
            ChargeBalanceUseCase.ChargeBalanceResult.success(userId, newBalance, transactionId);
        
        // Mock 설정을 더 구체적으로
        when(balanceFacade.chargeBalance(any(ChargeBalanceUseCase.ChargeBalanceCommand.class)))
            .thenReturn(result);

        // when & then
        String response = mockMvc.perform(post("/api/users/balance/charge")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()) // 응답 내용 출력
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.chargeAmount").value(amount))
                .andExpect(jsonPath("$.balanceAfterCharge").value(100000))
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        System.out.println("Response: " + response);
        
        // Mock이 실제로 호출되었는지 확인
        verify(balanceFacade).chargeBalance(any(ChargeBalanceUseCase.ChargeBalanceCommand.class));
    }

    @Test
    @DisplayName("잔액 충전 실패 - 사용자가 존재하지 않는 경우")
    void chargeBalance_Failure_UserNotFound() throws Exception {
        // given
        Long userId = 999L;
        int amount = 50000;
        
        ChargeBalanceRequest request = new ChargeBalanceRequest(userId, amount);
        ChargeBalanceUseCase.ChargeBalanceCommand command = 
            new ChargeBalanceUseCase.ChargeBalanceCommand(userId, BigDecimal.valueOf(amount));
        ChargeBalanceUseCase.ChargeBalanceResult result = 
            ChargeBalanceUseCase.ChargeBalanceResult.failure("사용자를 찾을 수 없습니다.");
        
        // Mock 설정을 더 구체적으로
        when(balanceFacade.chargeBalance(any(ChargeBalanceUseCase.ChargeBalanceCommand.class)))
            .thenReturn(result);

        // when & then
        String response = mockMvc.perform(post("/api/users/balance/charge")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()) // 응답 내용 출력
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."))
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        System.out.println("Response: " + response);
        
        // Mock이 실제로 호출되었는지 확인
        verify(balanceFacade).chargeBalance(any(ChargeBalanceUseCase.ChargeBalanceCommand.class));
    }

    @Test
    @DisplayName("잔액 충전 실패 - 잘못된 요청")
    void chargeBalance_Failure_InvalidRequest() throws Exception {
        // given
        Long userId = 1L;
        int amount = -1000; // 잘못된 금액
        
        ChargeBalanceRequest request = new ChargeBalanceRequest(userId, amount);
        ChargeBalanceUseCase.ChargeBalanceCommand command = 
            new ChargeBalanceUseCase.ChargeBalanceCommand(userId, BigDecimal.valueOf(amount));
        ChargeBalanceUseCase.ChargeBalanceResult result = 
            ChargeBalanceUseCase.ChargeBalanceResult.failure("충전 금액은 0보다 커야 합니다.");
        
        // Mock 설정을 더 구체적으로
        when(balanceFacade.chargeBalance(any(ChargeBalanceUseCase.ChargeBalanceCommand.class)))
            .thenReturn(result);

        // when & then
        String response = mockMvc.perform(post("/api/users/balance/charge")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()) // 응답 내용 출력
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("충전 금액은 0보다 커야 합니다."))
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        System.out.println("Response: " + response);
        
        // Mock이 실제로 호출되었는지 확인
        verify(balanceFacade).chargeBalance(any(ChargeBalanceUseCase.ChargeBalanceCommand.class));
    }

    @Test
    @DisplayName("잔액 충전 실패 - 서버 오류")
    void chargeBalance_Failure_ServerError() throws Exception {
        // given
        Long userId = 1L;
        int amount = 50000;
        
        ChargeBalanceRequest request = new ChargeBalanceRequest(userId, amount);
        
        // Mock 설정을 더 구체적으로
        when(balanceFacade.chargeBalance(any(ChargeBalanceUseCase.ChargeBalanceCommand.class)))
            .thenThrow(new RuntimeException("데이터베이스 오류"));

        // when & then
        String response = mockMvc.perform(post("/api/users/balance/charge")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()) // 응답 내용 출력
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다: 데이터베이스 오류"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        System.out.println("Response: " + response);
        
        // Mock이 실제로 호출되었는지 확인
        verify(balanceFacade).chargeBalance(any(ChargeBalanceUseCase.ChargeBalanceCommand.class));
    }
} 