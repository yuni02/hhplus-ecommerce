package kr.hhplus.be.server.coupon.application.port.out;

import java.util.Optional;

/**
 * 쿠폰 조회 Outgoing Port
 */
public interface LoadCouponPort {
    
    /**
     * 쿠폰 ID로 조회
     */
    Optional<CouponInfo> loadCouponById(Long couponId);
    
    /**
     * 쿠폰 ID로 락을 사용하여 조회 (선착순 쿠폰 발급용)
     */
    Optional<CouponInfo> loadCouponByIdWithLock(Long couponId);
    
    /**
     * 쿠폰 발급 수량을 원자적으로 증가 (선착순 처리용)
     */
    boolean incrementIssuedCount(Long couponId);
    
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