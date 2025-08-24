package kr.hhplus.be.server.unit.coupon.adapter.in.web;

import kr.hhplus.be.server.coupon.application.CachedCouponService;
import kr.hhplus.be.server.coupon.application.AsyncCouponIssueWorker;
import kr.hhplus.be.server.coupon.application.port.in.IssueCouponUseCase;                   
import kr.hhplus.be.server.coupon.application.port.in.GetUserCouponsUseCase;
import kr.hhplus.be.server.coupon.adapter.in.web.CouponController;

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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import kr.hhplus.be.server.shared.exception.GlobalExceptionHandler;
import kr.hhplus.be.server.coupon.application.RedisCouponQueueService;

@ExtendWith(MockitoExtension.class)
class CouponControllerTest {

    @Mock
    private IssueCouponUseCase issueCouponUseCase;
    
    @Mock
    private GetUserCouponsUseCase getUserCouponsUseCase;
    
    @Mock
    private CachedCouponService cachedCouponService;        

    @Mock
    private RedisCouponQueueService queueService;

    @Mock
    private AsyncCouponIssueWorker asyncCouponIssueWorker;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(
                new CouponController( getUserCouponsUseCase,  queueService))  // 생성자 주입 방식으로 변경
                .setControllerAdvice(new GlobalExceptionHandler())  // 예외 처리 핸들러 설정    
                .build();           
    }

    @Test
    @DisplayName("사용자 쿠폰 조회 성공")
    void getUserCoupons_Success() throws Exception {
        // given
        Long userId = 1L;
        GetUserCouponsUseCase.UserCouponInfo userCouponInfo = new GetUserCouponsUseCase.UserCouponInfo(
                1L, 1L, "신규 가입 쿠폰", 1000, "AVAILABLE", LocalDateTime.now(), null);
        GetUserCouponsUseCase.GetUserCouponsResult result = 
            new GetUserCouponsUseCase.GetUserCouponsResult(List.of(userCouponInfo));

        when(getUserCouponsUseCase.getUserCoupons(any(GetUserCouponsUseCase.GetUserCouponsCommand.class)))
                .thenReturn(result);

        // when & then
        mockMvc.perform(get("/api/coupons/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].userCouponId").value(1L))
                .andExpect(jsonPath("$[0].couponName").value("신규 가입 쿠폰"));

        verify(getUserCouponsUseCase).getUserCoupons(any(GetUserCouponsUseCase.GetUserCouponsCommand.class));
    }

    @Test
    @DisplayName("사용자 쿠폰 조회 성공 - 빈 결과")
    void getUserCoupons_Success_EmptyResult() throws Exception {
        // given
        Long userId = 1L;
        GetUserCouponsUseCase.GetUserCouponsResult result = 
            new GetUserCouponsUseCase.GetUserCouponsResult(List.of());

        when(getUserCouponsUseCase.getUserCoupons(any(GetUserCouponsUseCase.GetUserCouponsCommand.class)))
                .thenReturn(result);

        // when & then
        mockMvc.perform(get("/api/coupons/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(getUserCouponsUseCase).getUserCoupons(any(GetUserCouponsUseCase.GetUserCouponsCommand.class));
    }

    @Test
    @DisplayName("사용자 쿠폰 조회 실패 - 잘못된 요청")
    void getUserCoupons_Failure_InvalidRequest() throws Exception {
        // when & then
        mockMvc.perform(get("/api/coupons/users/{userId}", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("사용자 쿠폰 조회 실패 - 서버 오류")
    void getUserCoupons_Failure_ServerError() throws Exception {
        // given
        Long userId = 1L;
        when(getUserCouponsUseCase.getUserCoupons(any())).thenThrow(new RuntimeException("데이터베이스 오류"));

        // when & then
        mockMvc.perform(get("/api/coupons/users/{userId}", userId))
                .andExpect(status().isInternalServerError());

        verify(getUserCouponsUseCase).getUserCoupons(any(GetUserCouponsUseCase.GetUserCouponsCommand.class));
    }

    @Test
    @DisplayName("쿠폰 발급 요청 - 대기열 등록 성공")
    void issueCoupon_QueueRegistration_Success() throws Exception {
        // given
        Long couponId = 1L;
        Long userId = 1L;

        when(queueService.addToQueue(couponId, userId)).thenReturn(true);
        when(queueService.getUserQueuePosition(couponId, userId)).thenReturn(1L);
        when(queueService.getQueueSize(couponId)).thenReturn(5L);

        // when & then
        mockMvc.perform(post("/api/coupons/{id}/issue", couponId)
                        .param("userId", userId.toString()))
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("쿠폰 발급 요청이 대기열에 등록되었습니다."))
                .andExpect(jsonPath("$.queuePosition").value(1))
                .andExpect(jsonPath("$.queueSize").value(5));

        verify(queueService).addToQueue(couponId, userId);
        verify(queueService).getUserQueuePosition(couponId, userId);
        verify(queueService).getQueueSize(couponId);
    }

    @Test
    @DisplayName("쿠폰 발급 요청 - 이미 대기열에 등록된 경우")
    void issueCoupon_AlreadyInQueue_Failure() throws Exception {
        // given
        Long couponId = 1L;
        Long userId = 1L;

        when(queueService.addToQueue(couponId, userId)).thenReturn(false);

        // when & then
        mockMvc.perform(post("/api/coupons/{id}/issue", couponId)
                        .param("userId", userId.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 대기열에 등록되어 있습니다."));

        verify(queueService).addToQueue(couponId, userId);
    }

    @Test
    @DisplayName("쿠폰 발급 상태 조회 - 처리 중")
    void getIssueStatus_Processing() throws Exception {
        // given
        Long couponId = 1L;
        Long userId = 1L;

        when(queueService.getIssueResult(couponId, userId)).thenReturn(null);
        when(queueService.getUserQueuePosition(couponId, userId)).thenReturn(3L);
        when(queueService.getQueueSize(couponId)).thenReturn(10L);

        // when & then
        mockMvc.perform(get("/api/coupons/{id}/issue/status", couponId)
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.message").value("처리 중입니다."))
                .andExpect(jsonPath("$.queuePosition").value(3))
                .andExpect(jsonPath("$.queueSize").value(10));

        verify(queueService).getIssueResult(couponId, userId);
        verify(queueService).getUserQueuePosition(couponId, userId);
        verify(queueService).getQueueSize(couponId);
    }

    @Test
    @DisplayName("쿠폰 발급 상태 조회 - 성공")
    void getIssueStatus_Success() throws Exception {
        // given
        Long couponId = 1L;
        Long userId = 1L;
        RedisCouponQueueService.CouponIssueResult result = 
            new RedisCouponQueueService.CouponIssueResult(true, "쿠폰 발급 성공", LocalDateTime.now());

        when(queueService.getIssueResult(couponId, userId)).thenReturn(result);

        // when & then
        mockMvc.perform(get("/api/coupons/{id}/issue/status", couponId)
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("쿠폰 발급 성공"));

        verify(queueService).getIssueResult(couponId, userId);
    }

    @Test
    @DisplayName("쿠폰 발급 상태 조회 - 실패")
    void getIssueStatus_Failed() throws Exception {
        // given
        Long couponId = 1L;
        Long userId = 1L;
        RedisCouponQueueService.CouponIssueResult result = 
            new RedisCouponQueueService.CouponIssueResult(false, "쿠폰이 모두 소진되었습니다.", LocalDateTime.now());

        when(queueService.getIssueResult(couponId, userId)).thenReturn(result);

        // when & then
        mockMvc.perform(get("/api/coupons/{id}/issue/status", couponId)
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("쿠폰이 모두 소진되었습니다."));

        verify(queueService).getIssueResult(couponId, userId);
    }
} 