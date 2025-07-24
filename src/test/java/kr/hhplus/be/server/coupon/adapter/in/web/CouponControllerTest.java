package kr.hhplus.be.server.coupon.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.coupon.application.facade.CouponFacade;
import kr.hhplus.be.server.coupon.application.port.in.GetUserCouponsUseCase;
import kr.hhplus.be.server.coupon.application.port.in.IssueCouponUseCase;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CouponControllerTest {

    @Mock
    private CouponFacade couponFacade;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CouponController(couponFacade))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("쿠폰 발급 성공")
    void issueCoupon_Success() throws Exception {
        // given
        Long couponId = 1L;
        Long userId = 1L;
        Long userCouponId = 1L;
        String couponName = "테스트 쿠폰";
        Integer discountAmount = 1000;
        String status = "AVAILABLE";
        LocalDateTime issuedAt = LocalDateTime.now();
        
        IssueCouponUseCase.IssueCouponCommand command = 
            new IssueCouponUseCase.IssueCouponCommand(userId, couponId);
        IssueCouponUseCase.IssueCouponResult result = 
            IssueCouponUseCase.IssueCouponResult.success(userCouponId, couponId, couponName, discountAmount, status, issuedAt);
        
        // Mock 설정을 더 구체적으로
        when(couponFacade.issueCoupon(any(IssueCouponUseCase.IssueCouponCommand.class)))
            .thenReturn(result);

        // when & then
        String response = mockMvc.perform(post("/api/coupons/{id}/issue", couponId)
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()) // 응답 내용 출력
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userCouponId").value(userCouponId))
                .andExpect(jsonPath("$.couponId").value(couponId))
                .andExpect(jsonPath("$.couponName").value(couponName))
                .andExpect(jsonPath("$.discountAmount").value(discountAmount))
                .andExpect(jsonPath("$.status").value(status))
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        System.out.println("Response: " + response);
        
        // Mock이 실제로 호출되었는지 확인
        verify(couponFacade).issueCoupon(any(IssueCouponUseCase.IssueCouponCommand.class));
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 사용자가 존재하지 않는 경우")
    void issueCoupon_Failure_UserNotFound() throws Exception {
        // given
        Long couponId = 1L;
        Long userId = 999L;
        
        IssueCouponUseCase.IssueCouponCommand command = 
            new IssueCouponUseCase.IssueCouponCommand(userId, couponId);
        IssueCouponUseCase.IssueCouponResult result = 
            IssueCouponUseCase.IssueCouponResult.failure("사용자를 찾을 수 없습니다.");
        
        // Mock 설정을 더 구체적으로
        when(couponFacade.issueCoupon(any(IssueCouponUseCase.IssueCouponCommand.class)))
            .thenReturn(result);

        // when & then
        String response = mockMvc.perform(post("/api/coupons/{id}/issue", couponId)
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
        verify(couponFacade).issueCoupon(any(IssueCouponUseCase.IssueCouponCommand.class));
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 쿠폰이 존재하지 않는 경우")
    void issueCoupon_Failure_CouponNotFound() throws Exception {
        // given
        Long couponId = 999L;
        Long userId = 1L;
        
        IssueCouponUseCase.IssueCouponCommand command = 
            new IssueCouponUseCase.IssueCouponCommand(userId, couponId);
        IssueCouponUseCase.IssueCouponResult result = 
            IssueCouponUseCase.IssueCouponResult.failure("쿠폰을 찾을 수 없습니다.");
        
        // Mock 설정을 더 구체적으로
        when(couponFacade.issueCoupon(any(IssueCouponUseCase.IssueCouponCommand.class)))
            .thenReturn(result);

        // when & then
        String response = mockMvc.perform(post("/api/coupons/{id}/issue", couponId)
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()) // 응답 내용 출력
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("쿠폰을 찾을 수 없습니다."))
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        System.out.println("Response: " + response);
        
        // Mock이 실제로 호출되었는지 확인
        verify(couponFacade).issueCoupon(any(IssueCouponUseCase.IssueCouponCommand.class));
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 잘못된 요청")
    void issueCoupon_Failure_InvalidRequest() throws Exception {
        // given
        Long couponId = 1L;
        Long userId = 1L;
        
        IssueCouponUseCase.IssueCouponCommand command = 
            new IssueCouponUseCase.IssueCouponCommand(userId, couponId);
        IssueCouponUseCase.IssueCouponResult result = 
            IssueCouponUseCase.IssueCouponResult.failure("잘못된 요청입니다.");
        
        // Mock 설정을 더 구체적으로
        when(couponFacade.issueCoupon(any(IssueCouponUseCase.IssueCouponCommand.class)))
            .thenReturn(result);

        // when & then
        String response = mockMvc.perform(post("/api/coupons/{id}/issue", couponId)
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()) // 응답 내용 출력
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        System.out.println("Response: " + response);
        
        // Mock이 실제로 호출되었는지 확인
        verify(couponFacade).issueCoupon(any(IssueCouponUseCase.IssueCouponCommand.class));
    }

    @Test
    @DisplayName("사용자 쿠폰 조회 성공")
    void getUserCoupons_Success() throws Exception {
        // given
        Long userId = 1L;
        GetUserCouponsUseCase.GetUserCouponsCommand command = 
            new GetUserCouponsUseCase.GetUserCouponsCommand(userId);
        GetUserCouponsUseCase.GetUserCouponsResult result = 
            new GetUserCouponsUseCase.GetUserCouponsResult(List.of());
        
        // Mock 설정을 더 구체적으로
        when(couponFacade.getUserCoupons(any(GetUserCouponsUseCase.GetUserCouponsCommand.class)))
            .thenReturn(result);

        // when & then
        String response = mockMvc.perform(get("/api/coupons/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()) // 응답 내용 출력
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        System.out.println("Response: " + response);
        
        // Mock이 실제로 호출되었는지 확인
        verify(couponFacade).getUserCoupons(any(GetUserCouponsUseCase.GetUserCouponsCommand.class));
    }

    @Test
    @DisplayName("사용자 쿠폰 조회 성공 - 빈 결과")
    void getUserCoupons_Success_EmptyResult() throws Exception {
        // given
        Long userId = 1L;
        GetUserCouponsUseCase.GetUserCouponsCommand command = 
            new GetUserCouponsUseCase.GetUserCouponsCommand(userId);
        GetUserCouponsUseCase.GetUserCouponsResult result = 
            new GetUserCouponsUseCase.GetUserCouponsResult(List.of());
        
        // Mock 설정을 더 구체적으로
        when(couponFacade.getUserCoupons(any(GetUserCouponsUseCase.GetUserCouponsCommand.class)))
            .thenReturn(result);

        // when & then
        String response = mockMvc.perform(get("/api/coupons/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()) // 응답 내용 출력
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        System.out.println("Response: " + response);
        
        // Mock이 실제로 호출되었는지 확인
        verify(couponFacade).getUserCoupons(any(GetUserCouponsUseCase.GetUserCouponsCommand.class));
    }

    @Test
    @DisplayName("사용자 쿠폰 조회 실패 - 잘못된 요청")
    void getUserCoupons_Failure_InvalidRequest() throws Exception {
        // given
        Long userId = 1L;
        
        GetUserCouponsUseCase.GetUserCouponsCommand command = 
            new GetUserCouponsUseCase.GetUserCouponsCommand(userId);
        
        // Mock 설정을 더 구체적으로
        when(couponFacade.getUserCoupons(any(GetUserCouponsUseCase.GetUserCouponsCommand.class)))
            .thenThrow(new IllegalArgumentException("잘못된 사용자 ID입니다."));

        // when & then
        String response = mockMvc.perform(get("/api/coupons/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()) // 응답 내용 출력
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("잘못된 사용자 ID입니다."))
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        System.out.println("Response: " + response);
        
        // Mock이 실제로 호출되었는지 확인
        verify(couponFacade).getUserCoupons(any(GetUserCouponsUseCase.GetUserCouponsCommand.class));
    }

    @Test
    @DisplayName("사용자 쿠폰 조회 실패 - 서버 오류")
    void getUserCoupons_Failure_ServerError() throws Exception {
        // given
        when(couponFacade.getUserCoupons(any())).thenThrow(new RuntimeException("데이터베이스 오류"));

        // when & then
        String response = mockMvc.perform(get("/api/coupons/users/{userId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()) // 응답 내용 출력
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다: 데이터베이스 오류"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        System.out.println("Response: " + response);
        
        // Mock이 실제로 호출되었는지 확인
        verify(couponFacade).getUserCoupons(any(GetUserCouponsUseCase.GetUserCouponsCommand.class));
    }
} 