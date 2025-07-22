package kr.hhplus.be.server.coupon.adapter.out.persistence;

import kr.hhplus.be.server.coupon.application.port.out.LoadUserCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.SaveUserCouponPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 사용자 쿠폰 영속성 Adapter (Outgoing)
 */
@Component
public class UserCouponPersistenceAdapter implements LoadUserCouponPort, SaveUserCouponPort {

    private final Map<Long, UserCouponData> userCoupons = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public UserCouponPersistenceAdapter() {
        // 더미 데이터 초기화
        initializeDummyData();
    }

    private void initializeDummyData() {
        // 사용자 쿠폰 데이터 초기화
        for (long userId = 1; userId <= 3; userId++) {
            for (long couponId = 1; couponId <= 2; couponId++) {
                UserCouponData userCoupon = new UserCouponData(
                        idGenerator.getAndIncrement(),
                        userId,
                        couponId,
                        "AVAILABLE",
                        "2024-01-01T10:00:00",
                        null,
                        null
                );
                userCoupons.put(userCoupon.getId(), userCoupon);
            }
        }
    }

    @Override
    public List<LoadUserCouponPort.UserCouponInfo> loadUserCouponsByUserId(Long userId) {
        return userCoupons.values().stream()
                .filter(uc -> uc.getUserId().equals(userId))
                .map(uc -> new LoadUserCouponPort.UserCouponInfo(
                        uc.getId(),
                        uc.getUserId(),
                        uc.getCouponId(),
                        uc.getStatus(),
                        uc.getIssuedAt(),
                        uc.getUsedAt(),
                        uc.getOrderId()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public SaveUserCouponPort.UserCouponInfo saveUserCoupon(SaveUserCouponPort.UserCouponInfo userCouponInfo) {
        UserCouponData userCoupon = new UserCouponData(
                userCouponInfo.getId(),
                userCouponInfo.getUserId(),
                userCouponInfo.getCouponId(),
                userCouponInfo.getStatus(),
                userCouponInfo.getIssuedAt(),
                userCouponInfo.getUsedAt(),
                userCouponInfo.getOrderId()
        );
        
        userCoupons.put(userCoupon.getId(), userCoupon);
        
        return new SaveUserCouponPort.UserCouponInfo(
                userCoupon.getId(),
                userCoupon.getUserId(),
                userCoupon.getCouponId(),
                userCoupon.getStatus(),
                userCoupon.getIssuedAt(),
                userCoupon.getUsedAt(),
                userCoupon.getOrderId()
        );
    }

    /**
     * 사용자 쿠폰 데이터 내부 클래스
     */
    private static class UserCouponData {
        private Long id;
        private Long userId;
        private Long couponId;
        private String status;
        private String issuedAt;
        private String usedAt;
        private Long orderId;

        public UserCouponData(Long id, Long userId, Long couponId, String status,
                            String issuedAt, String usedAt, Long orderId) {
            this.id = id;
            this.userId = userId;
            this.couponId = couponId;
            this.status = status;
            this.issuedAt = issuedAt;
            this.usedAt = usedAt;
            this.orderId = orderId;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getCouponId() { return couponId; }
        public void setCouponId(Long couponId) { this.couponId = couponId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getIssuedAt() { return issuedAt; }
        public void setIssuedAt(String issuedAt) { this.issuedAt = issuedAt; }
        public String getUsedAt() { return usedAt; }
        public void setUsedAt(String usedAt) { this.usedAt = usedAt; }
        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
    }
} 