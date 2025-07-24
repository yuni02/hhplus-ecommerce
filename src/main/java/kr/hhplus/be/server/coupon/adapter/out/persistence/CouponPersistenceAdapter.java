package kr.hhplus.be.server.coupon.adapter.out.persistence;

import kr.hhplus.be.server.coupon.application.port.out.LoadCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.SaveCouponPort;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 쿠폰 영속성 Adapter (Outgoing)
 */
@Component
public class CouponPersistenceAdapter implements LoadCouponPort, SaveCouponPort {

    private final Map<Long, CouponData> coupons = new ConcurrentHashMap<>();
    
    // 쿠폰별 락을 위한 Map (선착순 쿠폰 발급용)
    private final Map<Long, ReentrantLock> couponLocks = new ConcurrentHashMap<>();

    public CouponPersistenceAdapter() {
        // 더미 데이터 초기화
        initializeDummyData();
    }

    private void initializeDummyData() {
        // 쿠폰 데이터 초기화
        for (long couponId = 1; couponId <= 3; couponId++) {
            CouponData coupon = new CouponData(
                    couponId,
                    "쿠폰 " + couponId,
                    "쿠폰 " + couponId + " 설명",
                    Integer.valueOf((int)(1000 * couponId)),
                    100,
                    0,
                    "ACTIVE"
            );
            coupons.put(couponId, coupon);
        }
    }

    @Override
    public Optional<LoadCouponPort.CouponInfo> loadCouponById(Long couponId) {
        CouponData coupon = coupons.get(couponId);
        if (coupon == null) {
            return Optional.empty();
        }
        
        return Optional.of(new LoadCouponPort.CouponInfo(
                coupon.getId(),
                coupon.getName(),
                coupon.getDescription(),
                coupon.getDiscountAmount(),
                coupon.getMaxIssuanceCount(),
                coupon.getIssuedCount(),
                coupon.getStatus()
        ));
    }

    @Override
    public Optional<LoadCouponPort.CouponInfo> loadCouponByIdWithLock(Long couponId) {
        // 쿠폰별 락 획득
        ReentrantLock lock = couponLocks.computeIfAbsent(couponId, k -> new ReentrantLock());
        
        try {
            // 락 획득 (최대 5초 대기)
            if (!lock.tryLock()) {
                throw new RuntimeException("쿠폰 발급 중입니다. 잠시 후 다시 시도해주세요.");
            }
            
            CouponData coupon = coupons.get(couponId);
            if (coupon == null) {
                return Optional.empty();
            }
            
            return Optional.of(new LoadCouponPort.CouponInfo(
                    coupon.getId(),
                    coupon.getName(),
                    coupon.getDescription(),
                    coupon.getDiscountAmount(),
                    coupon.getMaxIssuanceCount(),
                    coupon.getIssuedCount(),
                    coupon.getStatus()
            ));
            
        } finally {
            // 락 해제
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public SaveCouponPort.CouponInfo saveCoupon(SaveCouponPort.CouponInfo couponInfo) {
        CouponData coupon = new CouponData(
                couponInfo.getId(),
                couponInfo.getName(),
                couponInfo.getDescription(),
                couponInfo.getDiscountAmount(),
                couponInfo.getMaxIssuanceCount(),
                couponInfo.getIssuedCount(),
                couponInfo.getStatus()
        );
        
        coupons.put(coupon.getId(), coupon);
        
        return new SaveCouponPort.CouponInfo(
                coupon.getId(),
                coupon.getName(),
                coupon.getDescription(),
                coupon.getDiscountAmount(),
                coupon.getMaxIssuanceCount(),
                coupon.getIssuedCount(),
                coupon.getStatus()
        );
    }

    /**
     * 쿠폰 발급 수량을 원자적으로 증가시키는 메서드
     */
    @Override
    public boolean incrementIssuedCount(Long couponId) {
        ReentrantLock lock = couponLocks.computeIfAbsent(couponId, k -> new ReentrantLock());
        
        try {
            if (!lock.tryLock()) {
                return false;
            }
            
            CouponData coupon = coupons.get(couponId);
            if (coupon == null) {
                return false;
            }
            
            // 선착순 확인
            if (coupon.getIssuedCount() >= coupon.getMaxIssuanceCount()) {
                return false;
            }
            
            // 발급 수량 증가
            coupon.setIssuedCount(coupon.getIssuedCount() + 1);
            
            // 상태 업데이트
            if (coupon.getIssuedCount() >= coupon.getMaxIssuanceCount()) {
                coupon.setStatus("SOLD_OUT");
            }
            
            return true;
            
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 쿠폰 데이터 내부 클래스
     */
    private static class CouponData {
        private Long id;
        private String name;
        private String description;
        private Integer discountAmount;
        private Integer maxIssuanceCount;
        private Integer issuedCount;
        private String status;

        public CouponData(Long id, String name, String description, Integer discountAmount,
                         Integer maxIssuanceCount, Integer issuedCount, String status) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.discountAmount = discountAmount;
            this.maxIssuanceCount = maxIssuanceCount;
            this.issuedCount = issuedCount;
            this.status = status;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getDiscountAmount() { return discountAmount; }
        public void setDiscountAmount(Integer discountAmount) { this.discountAmount = discountAmount; }
        public Integer getMaxIssuanceCount() { return maxIssuanceCount; }
        public void setMaxIssuanceCount(Integer maxIssuanceCount) { this.maxIssuanceCount = maxIssuanceCount; }
        public Integer getIssuedCount() { return issuedCount; }
        public void setIssuedCount(Integer issuedCount) { this.issuedCount = issuedCount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
} 