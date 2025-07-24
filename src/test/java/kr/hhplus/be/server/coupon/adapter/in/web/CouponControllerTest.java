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
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
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
    @DisplayName("사용자 쿠폰 조회 성공")
    void getUserCoupons_Success() throws Exception {
        // given
        Long userId = 1L;
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

    @Test
    @DisplayName("선착순 쿠폰 발급 - 정상 발급")
    void issueCoupon_FirstComeFirstServed_Success() throws Exception {
        // given
        Long couponId = 1L;
        Long userId = 1L;
        
        IssueCouponUseCase.IssueCouponResult mockResult = IssueCouponUseCase.IssueCouponResult.success(
                1L, couponId, "테스트 쿠폰", 1000, "AVAILABLE", LocalDateTime.now());
        
        when(couponFacade.issueCoupon(any(IssueCouponUseCase.IssueCouponCommand.class)))
                .thenReturn(mockResult);

        // when & then
        mockMvc.perform(post("/api/coupons/{id}/issue", couponId)
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userCouponId").value(1))
                .andExpect(jsonPath("$.couponId").value(couponId))
                .andExpect(jsonPath("$.couponName").value("테스트 쿠폰"))
                .andExpect(jsonPath("$.discountAmount").value(1000))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
        
        verify(couponFacade).issueCoupon(any(IssueCouponUseCase.IssueCouponCommand.class));
    }

    @Test
    @DisplayName("선착순 쿠폰 발급 - 재고 소진으로 실패")
    void issueCoupon_OutOfStock_Failure() throws Exception {
        // given
        Long couponId = 1L;
        Long userId = 1L;
        
        IssueCouponUseCase.IssueCouponResult mockResult = IssueCouponUseCase.IssueCouponResult.failure(
                "쿠폰이 모두 소진되었습니다. 선착순 발급에 실패했습니다.");
        
        when(couponFacade.issueCoupon(any(IssueCouponUseCase.IssueCouponCommand.class)))
                .thenReturn(mockResult);

        // when & then
        mockMvc.perform(post("/api/coupons/{id}/issue", couponId)
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("쿠폰이 모두 소진되었습니다. 선착순 발급에 실패했습니다."));
        
        verify(couponFacade).issueCoupon(any(IssueCouponUseCase.IssueCouponCommand.class));
    }

    @Test
    @DisplayName("선착순 쿠폰 발급 - 동시 요청 시뮬레이션")
    void issueCoupon_ConcurrentRequests_Simulation() throws Exception {
        // given
        Long couponId = 1L;
        Long userId = 1L;
        int maxIssuanceCount = 3;
        
        // 첫 3번은 성공, 나머지는 실패
        AtomicInteger callCount = new AtomicInteger(0);
        when(couponFacade.issueCoupon(any(IssueCouponUseCase.IssueCouponCommand.class)))
                .thenAnswer(invocation -> {
                    int currentCall = callCount.incrementAndGet();
                    if (currentCall <= maxIssuanceCount) {
                        return IssueCouponUseCase.IssueCouponResult.success(
                                (long) currentCall, couponId, "테스트 쿠폰", 1000, "AVAILABLE", LocalDateTime.now());
                    } else {
                        return IssueCouponUseCase.IssueCouponResult.failure("쿠폰이 모두 소진되었습니다.");
                    }
                });

        // when & then - 첫 번째 요청 (성공)
        mockMvc.perform(post("/api/coupons/{id}/issue", couponId)
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userCouponId").value(1));
        
        // 두 번째 요청 (성공)
        mockMvc.perform(post("/api/coupons/{id}/issue", couponId)
                        .param("userId", Long.toString(userId + 1))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userCouponId").value(2));
        
        // 세 번째 요청 (성공)
        mockMvc.perform(post("/api/coupons/{id}/issue", couponId)
                        .param("userId", Long.toString(userId + 2))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userCouponId").value(3));
        
        // 네 번째 요청 (실패 - 재고 소진)
        mockMvc.perform(post("/api/coupons/{id}/issue", couponId)
                        .param("userId", Long.toString(userId + 3))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("쿠폰이 모두 소진되었습니다."));
        
        verify(couponFacade, times(4)).issueCoupon(any(IssueCouponUseCase.IssueCouponCommand.class));
    }
} 