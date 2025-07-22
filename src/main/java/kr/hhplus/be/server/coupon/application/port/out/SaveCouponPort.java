package kr.hhplus.be.server.coupon.application.port.out;

/**
 * 쿠폰 저장 Outgoing Port
 */
public interface SaveCouponPort {
    
    /**
     * 쿠폰 저장
     */
    CouponInfo saveCoupon(CouponInfo couponInfo);
    
    /**
     * 쿠폰 정보
     */
    class CouponInfo {
        private final Long id;
        private final String name;
        private final String description;
        private final Integer discountAmount;
        private final Integer maxIssuanceCount;
        private final Integer issuedCount;
        private final String status;
        
        public CouponInfo(Long id, String name, String description, Integer discountAmount,
                         Integer maxIssuanceCount, Integer issuedCount, String status) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.discountAmount = discountAmount;
            this.maxIssuanceCount = maxIssuanceCount;
            this.issuedCount = issuedCount;
            this.status = status;
        }
        
        public Long getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public Integer getDiscountAmount() {
            return discountAmount;
        }
        
        public Integer getMaxIssuanceCount() {
            return maxIssuanceCount;
        }
        
        public Integer getIssuedCount() {
            return issuedCount;
        }
        
        public String getStatus() {
            return status;
        }
    }
} 