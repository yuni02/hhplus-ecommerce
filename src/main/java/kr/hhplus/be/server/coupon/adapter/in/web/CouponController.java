package kr.hhplus.be.server.coupon.adapter.in.web;

import kr.hhplus.be.server.coupon.application.port.in.IssueCouponUseCase;
import kr.hhplus.be.server.coupon.application.port.in.GetUserCouponsUseCase;
import kr.hhplus.be.server.coupon.application.CachedCouponService;
import kr.hhplus.be.server.coupon.application.RedisCouponQueueService;
import kr.hhplus.be.server.coupon.adapter.in.dto.CouponResponse;
import kr.hhplus.be.server.coupon.adapter.in.dto.UserCouponResponse;
import kr.hhplus.be.server.coupon.adapter.in.dto.CouponQueueResponse;
import kr.hhplus.be.server.coupon.adapter.in.dto.CouponQueueStatusResponse;
import kr.hhplus.be.server.shared.response.ErrorResponse;       
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/coupons")
public class CouponController implements CouponApiDocumentation {

    private static final Logger log = LoggerFactory.getLogger(CouponController.class);

    private final IssueCouponUseCase issueCouponUseCase;
    private final GetUserCouponsUseCase getUserCouponsUseCase;
    private final CachedCouponService cachedCouponService;  // 캐시 서비스
    private final RedisCouponQueueService queueService;  // 대기열 서비스

    public CouponController(IssueCouponUseCase issueCouponUseCase,
                          GetUserCouponsUseCase getUserCouponsUseCase,
                          CachedCouponService cachedCouponService,  // 캐시 서비스
                          RedisCouponQueueService queueService) {  // 대기열 서비스
        this.issueCouponUseCase = issueCouponUseCase;
        this.getUserCouponsUseCase = getUserCouponsUseCase;
        this.cachedCouponService = cachedCouponService;
        this.queueService = queueService;
    }

    @PostMapping("/{id}/issue")
    public ResponseEntity<?> issueCoupon(
            @PathVariable(name = "id") Long id,
            @RequestParam(name = "userId", required = true) Long userId,
            @RequestParam(name = "immediate", required = false, defaultValue = "false") boolean immediate) {
        
        // 1. 대기열에 추가 시도
        boolean addedToQueue = queueService.addToQueue(id, userId);
        
        if (!addedToQueue) {
            return ResponseEntity.badRequest().body(new ErrorResponse("이미 대기열에 등록되어 있습니다."));
        }
        
        // 2. 대기열 순서 조회
        Long queuePosition = queueService.getUserQueuePosition(id, userId);
        
        // 3. 즉시 처리 옵션이 활성화된 경우 처리 시도
        if (immediate) {
            // 즉시 처리 시도 (스케줄러가 처리하도록 함)
            try {
                // 스케줄러가 100ms마다 폴링하므로 빠른 처리 가능
                log.info("즉시 처리 요청 - couponId: {}, userId: {}", id, userId);
            } catch (Exception e) {
                log.warn("즉시 처리 실패 - couponId: {}, userId: {}", id, userId, e);
            }
        }
        
        // 4. 즉시 응답 (비동기 처리)
        return ResponseEntity.accepted().body(new CouponQueueResponse(
            immediate ? "쿠폰 발급 요청이 등록되었습니다. 즉시 처리 중입니다." : "쿠폰 발급 요청이 대기열에 등록되었습니다.",
            queuePosition,
            queueService.getQueueSize(id)
        ));
    }

    @GetMapping("/{id}/issue/status")
    public ResponseEntity<?> getIssueStatus(
            @PathVariable(name = "id") Long id,
            @RequestParam(name = "userId", required = true) Long userId) {
        
        // 발급 결과 조회
        RedisCouponQueueService.CouponIssueResult result = queueService.getIssueResult(id, userId);
        
        if (result == null) {
            // 아직 처리되지 않음
            Long queuePosition = queueService.getUserQueuePosition(id, userId);
            return ResponseEntity.ok(new CouponQueueStatusResponse(
                "PROCESSING",
                "처리 중입니다.",
                queuePosition,
                queueService.getQueueSize(id)
            ));
        }
        
        // 처리 완료
        return ResponseEntity.ok(new CouponQueueStatusResponse(
            result.isSuccess() ? "SUCCESS" : "FAILED",
            result.getMessage(),
            null,
            null
        ));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserCoupons(
            @PathVariable(name = "userId") Long userId) {
        
        GetUserCouponsUseCase.GetUserCouponsCommand command = new GetUserCouponsUseCase.GetUserCouponsCommand(userId);
        GetUserCouponsUseCase.GetUserCouponsResult result = getUserCouponsUseCase.getUserCoupons(command);
        
        List<UserCouponResponse> responses = result.getUserCoupons().stream()
                .map(userCoupon -> new UserCouponResponse(
                        userCoupon.getUserCouponId(),
                        userCoupon.getCouponId(),
                        userCoupon.getCouponName(),
                        userCoupon.getDiscountAmount(),
                        userCoupon.getStatus(),
                        userCoupon.getIssuedAt(),
                        userCoupon.getUsedAt()
                ))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
} 