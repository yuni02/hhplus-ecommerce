package kr.hhplus.be.server.coupon.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.coupon.application.facade.CouponFacade;
import kr.hhplus.be.server.coupon.application.port.in.GetUserCouponsUseCase;
import kr.hhplus.be.server.coupon.application.port.in.IssueCouponUseCase;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CouponControllerTest {

    @Mock
    private CouponFacade couponFacade;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CouponController(couponFacade)).build();
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
        
        when(couponFacade.issueCoupon(command)).thenReturn(result);

        // when & then
        mockMvc.perform(post("/api/coupons/{id}/issue", couponId)
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userCouponId").value(userCouponId))
                .andExpect(jsonPath("$.couponId").value(couponId))
                .andExpect(jsonPath("$.couponName").value(couponName))
                .andExpect(jsonPath("$.discountAmount").value(discountAmount))
                .andExpect(jsonPath("$.status").value(status));
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
        
        when(couponFacade.issueCoupon(command)).thenReturn(result);

        // when & then
        mockMvc.perform(post("/api/coupons/{id}/issue", couponId)
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));
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
            IssueCouponUseCase.IssueCouponResult.failure("존재하지 않는 쿠폰입니다.");
        
        when(couponFacade.issueCoupon(command)).thenReturn(result);

        // when & then
        mockMvc.perform(post("/api/coupons/{id}/issue", couponId)
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("존재하지 않는 쿠폰입니다."));
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 잘못된 요청")
    void issueCoupon_Failure_InvalidRequest() throws Exception {
        // given
        Long couponId = 1L;
        Long userId = -1L;
        
        when(couponFacade.issueCoupon(any())).thenThrow(new IllegalArgumentException("잘못된 사용자 ID입니다."));

        // when & then
        mockMvc.perform(post("/api/coupons/{id}/issue", couponId)
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("잘못된 사용자 ID입니다."));
    }

    @Test
    @DisplayName("사용자 쿠폰 조회 성공")
    void getUserCoupons_Success() throws Exception {
        // given
        Long userId = 1L;
        
        GetUserCouponsUseCase.GetUserCouponsCommand command = 
            new GetUserCouponsUseCase.GetUserCouponsCommand(userId);
        
        GetUserCouponsUseCase.UserCouponInfo userCouponInfo = 
            new GetUserCouponsUseCase.UserCouponInfo(1L, 1L, "테스트 쿠폰", 1000, "AVAILABLE", 
                LocalDateTime.now(), null);
        
        GetUserCouponsUseCase.GetUserCouponsResult result = 
            new GetUserCouponsUseCase.GetUserCouponsResult(List.of(userCouponInfo));
        
        when(couponFacade.getUserCoupons(command)).thenReturn(result);

        // when & then
        mockMvc.perform(get("/api/coupons/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userCouponId").value(1L))
                .andExpect(jsonPath("$[0].couponId").value(1L))
                .andExpect(jsonPath("$[0].couponName").value("테스트 쿠폰"))
                .andExpect(jsonPath("$[0].discountAmount").value(1000))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
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
        
        when(couponFacade.getUserCoupons(command)).thenReturn(result);

        // when & then
        mockMvc.perform(get("/api/coupons/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("사용자 쿠폰 조회 실패 - 잘못된 요청")
    void getUserCoupons_Failure_InvalidRequest() throws Exception {
        // given
        Long userId = -1L;
        
        when(couponFacade.getUserCoupons(any())).thenThrow(new IllegalArgumentException("잘못된 사용자 ID입니다."));

        // when & then
        mockMvc.perform(get("/api/coupons/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("잘못된 사용자 ID입니다."));
    }

    @Test
    @DisplayName("사용자 쿠폰 조회 실패 - 서버 오류")
    void getUserCoupons_Failure_ServerError() throws Exception {
        // given
        Long userId = 1L;
        
        when(couponFacade.getUserCoupons(any())).thenThrow(new RuntimeException("데이터베이스 연결 오류"));

        // when & then
        mockMvc.perform(get("/api/coupons/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("쿠폰 조회 중 오류가 발생했습니다: 데이터베이스 연결 오류"));
    }
} 