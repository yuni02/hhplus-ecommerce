package kr.hhplus.be.server.unit.coupon.adapter.in.web;

import kr.hhplus.be.server.coupon.adapter.in.web.CouponController;
import kr.hhplus.be.server.coupon.application.port.in.IssueCouponUseCase;
import kr.hhplus.be.server.coupon.application.port.in.GetUserCouponsUseCase;
import kr.hhplus.be.server.coupon.application.CachedCouponService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ExtendWith(MockitoExtension.class)
class CouponControllerTest {

    @Mock
    private IssueCouponUseCase issueCouponUseCase;
    
    @Mock
    private GetUserCouponsUseCase getUserCouponsUseCase;
    
    @Mock
    private CachedCouponService cachedCouponService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(
                new CouponController(issueCouponUseCase, getUserCouponsUseCase, cachedCouponService))
                .setControllerAdvice(new GlobalExceptionHandler())
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
    @DisplayName("선착순 쿠폰 발급 - 정상 발급")
    void issueCoupon_FirstComeFirstServed_Success() throws Exception {
        // given
        Long couponId = 1L;
        Long userId = 1L;
        IssueCouponUseCase.IssueCouponResult result = IssueCouponUseCase.IssueCouponResult.success(
                1L, couponId, "신규 가입 쿠폰", 1000, "AVAILABLE", LocalDateTime.now());

        when(issueCouponUseCase.issueCoupon(any(IssueCouponUseCase.IssueCouponCommand.class)))
                .thenReturn(result);

        // when & then
        mockMvc.perform(post("/api/coupons/{id}/issue", couponId)
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userCouponId").value(1L))
                .andExpect(jsonPath("$.couponName").value("신규 가입 쿠폰"));

        verify(issueCouponUseCase).issueCoupon(any(IssueCouponUseCase.IssueCouponCommand.class));
    }

    @Test
    @DisplayName("선착순 쿠폰 발급 - 재고 소진으로 실패")
    void issueCoupon_OutOfStock_Failure() throws Exception {
        // given
        Long couponId = 1L;
        Long userId = 1L;
        IssueCouponUseCase.IssueCouponResult result = IssueCouponUseCase.IssueCouponResult
                .failure("쿠폰이 모두 소진되었습니다. 선착순 발급에 실패했습니다.");

        when(issueCouponUseCase.issueCoupon(any(IssueCouponUseCase.IssueCouponCommand.class)))
                .thenReturn(result);

        // when & then
        mockMvc.perform(post("/api/coupons/{id}/issue", couponId)
                        .param("userId", userId.toString()))
                .andExpect(status().isBadRequest());

        verify(issueCouponUseCase).issueCoupon(any(IssueCouponUseCase.IssueCouponCommand.class));
    }

    @Test
    @DisplayName("선착순 쿠폰 발급 - 동시 요청 시뮬레이션")
    void issueCoupon_ConcurrentRequests_Simulation() throws Exception {
        // given
        Long couponId = 1L;
        int numberOfThreads = 4;
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        // 성공 응답 설정
        when(issueCouponUseCase.issueCoupon(any(IssueCouponUseCase.IssueCouponCommand.class)))
                .thenReturn(IssueCouponUseCase.IssueCouponResult.success(
                        1L, couponId, "신규 가입 쿠폰", 1000, "AVAILABLE", LocalDateTime.now()));

        // when
        for (int i = 0; i < numberOfThreads; i++) {
            final int userId = i + 1;
            executorService.submit(() -> {
                try {
                    mockMvc.perform(post("/api/coupons/{id}/issue", couponId)
                                    .param("userId", String.valueOf(userId)))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    // 예외 무시 (테스트에서는 동시성만 확인)
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        verify(issueCouponUseCase, times(4)).issueCoupon(any(IssueCouponUseCase.IssueCouponCommand.class));
    }
} 