package kr.hhplus.be.server.coupon.adapter.in.web;

import kr.hhplus.be.server.coupon.application.port.in.GetUserCouponsUseCase;
import kr.hhplus.be.server.coupon.application.port.in.IssueCouponUseCase;
import kr.hhplus.be.server.coupon.domain.service.RedisCouponQueueService;
import kr.hhplus.be.server.coupon.adapter.in.dto.UserCouponResponse;
import kr.hhplus.be.server.coupon.adapter.in.dto.CouponQueueResponse;
import kr.hhplus.be.server.coupon.adapter.in.dto.CouponQueueStatusResponse;
import kr.hhplus.be.server.shared.response.ErrorResponse;       
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
public class CouponController implements CouponApiDocumentation {

    private final GetUserCouponsUseCase getUserCouponsUseCase;
    private final IssueCouponUseCase issueCouponUseCase;
    private final RedisCouponQueueService queueService;

    public CouponController(GetUserCouponsUseCase getUserCouponsUseCase,
                          IssueCouponUseCase issueCouponUseCase,
                          RedisCouponQueueService queueService) {
        this.getUserCouponsUseCase = getUserCouponsUseCase;
        this.issueCouponUseCase = issueCouponUseCase;
        this.queueService = queueService;
    }

    @PostMapping("/{id}/issue")
    public ResponseEntity<?> issueCoupon(
            @PathVariable(name = "id") Long id,
            @RequestParam(name = "userId", required = true) Long userId) {
        
        // IssueCouponService를 통해 하이브리드 처리 (빠른 실패 + 비동기)
        IssueCouponUseCase.IssueCouponCommand command = 
            new IssueCouponUseCase.IssueCouponCommand(userId, id);
        
        IssueCouponUseCase.IssueCouponResult result = issueCouponUseCase.issueCoupon(command);
        
        // 빠른 실패 (즉시 응답)
        if (!result.isSuccess() && !"PROCESSING".equals(result.getStatus())) {
            return ResponseEntity.badRequest().body(new ErrorResponse(result.getErrorMessage()));
        }
        
        // 비동기 처리 중 (PROCESSING 상태)
        if ("PROCESSING".equals(result.getStatus())) {
            // 대기열 순서 조회
            Long queuePosition = queueService.getUserQueuePosition(id, userId);
            
            return ResponseEntity.accepted().body(new CouponQueueResponse(
                result.getErrorMessage() != null ? result.getErrorMessage() : "쿠폰 발급 요청이 처리 중입니다.",
                queuePosition,
                queueService.getQueueSize(id)
            ));
        }
        
        // 혹시나 하는 성공 케이스 (현재는 발생하지 않음)
        return ResponseEntity.ok().body(new CouponQueueResponse(
            "쿠폰 발급이 완료되었습니다.",
            null,
            null
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
                .toList();
        
        return ResponseEntity.ok(responses);
    }
} 