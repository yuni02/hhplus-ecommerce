package kr.hhplus.be.server.coupon.application.port.in;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 활성 쿠폰 조회 Incoming Port (Use Case)
 */
public interface GetActiveCouponsUseCase {
    
    /**
     * 활성 쿠폰 조회 실행
     */
    GetActiveCouponsResult getActiveCoupons(GetActiveCouponsCommand command);
    
    /**
     * 활성 쿠폰 조회 명령
     */
    class GetActiveCouponsCommand {
        // 현재는 파라미터 없이 모든 활성 쿠폰 조회
        public GetActiveCouponsCommand() {
        }
    }
    
    /**
     * 활성 쿠폰 조회 결과
     */
    class GetActiveCouponsResult {
        private final List<ActiveCouponInfo> activeCoupons;
        
        public GetActiveCouponsResult(List<ActiveCouponInfo> activeCoupons) {
            this.activeCoupons = activeCoupons;
        }
        
        public List<ActiveCouponInfo> getActiveCoupons() {
            return activeCoupons;
        }
    }
    
    /**
     * 활성 쿠폰 정보
     */
    class ActiveCouponInfo {
        private final Long couponId;
        private final String couponName;
        private final Integer maxIssuanceCount;
        private final LocalDateTime validFrom;
        private final LocalDateTime validTo;
        private final String status;
        
        public ActiveCouponInfo(Long couponId, String couponName, Integer maxIssuanceCount,
                              LocalDateTime validFrom, LocalDateTime validTo, String status) {
            this.couponId = couponId;
            this.couponName = couponName;
            this.maxIssuanceCount = maxIssuanceCount;
            this.validFrom = validFrom;
            this.validTo = validTo;
            this.status = status;
        }
        
        public Long getCouponId() {
            return couponId;
        }
        
        public String getCouponName() {
            return couponName;
        }
        
        public Integer getMaxIssuanceCount() {
            return maxIssuanceCount;
        }
        
        public LocalDateTime getValidFrom() {
            return validFrom;
        }
        
        public LocalDateTime getValidTo() {
            return validTo;
        }
        
        public String getStatus() {
            return status;
        }
    }
}
