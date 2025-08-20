package kr.hhplus.be.server.coupon.application.port.out;

import java.time.LocalDateTime;
import java.util.List;
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
     * 모든 쿠폰 조회
     */
    List<CouponInfo> loadAllCoupons();
    
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
        private final LocalDateTime validFrom;
        private final LocalDateTime validTo;
        
        public CouponInfo(Long id, String name, String description, Integer discountAmount,
                         Integer maxIssuanceCount, Integer issuedCount, String status,
                         LocalDateTime validFrom, LocalDateTime validTo) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.discountAmount = discountAmount;
            this.maxIssuanceCount = maxIssuanceCount;
            this.issuedCount = issuedCount;
            this.status = status;
            this.validFrom = validFrom;
            this.validTo = validTo;
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
        
        public LocalDateTime getValidFrom() {
            return validFrom;
        }
        
        public LocalDateTime getValidTo() {
            return validTo;
        }
        
        // 기존 생성자와의 호환성을 위한 getter
        public Long getCouponId() {
            return id;
        }
        
        public String getCouponName() {
            return name;
        }
    }
} 