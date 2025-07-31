package kr.hhplus.be.server.coupon.adapter.in.web;

import kr.hhplus.be.server.coupon.application.port.in.IssueCouponUseCase;
import kr.hhplus.be.server.coupon.application.port.in.GetUserCouponsUseCase;
import kr.hhplus.be.server.coupon.adapter.in.dto.CouponResponse;
import kr.hhplus.be.server.coupon.adapter.in.dto.UserCouponResponse;
import kr.hhplus.be.server.shared.response.ErrorResponse;       
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/coupons")
public class CouponController implements CouponApiDocumentation {

    private final IssueCouponUseCase issueCouponUseCase;
    private final GetUserCouponsUseCase getUserCouponsUseCase;

    public CouponController(IssueCouponUseCase issueCouponUseCase,
                          GetUserCouponsUseCase getUserCouponsUseCase) {
        this.issueCouponUseCase = issueCouponUseCase;
        this.getUserCouponsUseCase = getUserCouponsUseCase;
    }

    @Override
    public ResponseEntity<?> issueCoupon(
            @PathVariable(name = "id") Long id,
            @RequestParam(name = "userId", required = true) Long userId) {
        
        IssueCouponUseCase.IssueCouponCommand command = new IssueCouponUseCase.IssueCouponCommand(userId, id);
        IssueCouponUseCase.IssueCouponResult result = issueCouponUseCase.issueCoupon(command);
        
        if (!result.isSuccess()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(result.getErrorMessage()));
        }
        
        CouponResponse response = new CouponResponse(
                result.getUserCouponId(),
                result.getCouponId(),
                result.getCouponName(),
                result.getDiscountAmount(),
                result.getStatus(),
                result.getIssuedAt()
        );
        
        return ResponseEntity.ok(response);
    }

    @Override
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