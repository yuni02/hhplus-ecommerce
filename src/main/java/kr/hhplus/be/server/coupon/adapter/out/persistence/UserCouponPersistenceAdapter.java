package kr.hhplus.be.server.coupon.adapter.out.persistence;

import kr.hhplus.be.server.coupon.application.port.out.LoadUserCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.SaveUserCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.UpdateUserCouponPort;
import kr.hhplus.be.server.coupon.domain.UserCoupon;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 사용자 쿠폰 영속성 Adapter (Outgoing)
 */
@Component("couponUserCouponPersistenceAdapter")
public class UserCouponPersistenceAdapter implements LoadUserCouponPort, SaveUserCouponPort, UpdateUserCouponPort {

    private final Map<Long, UserCoupon> userCoupons = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public List<LoadUserCouponPort.UserCouponInfo> loadUserCouponsByUserId(Long userId) {
        return userCoupons.values().stream()
                .filter(uc -> uc.getUserId().equals(userId))
                .map(this::toUserCouponInfo)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UserCoupon> loadUserCoupon(Long userCouponId) {
        return Optional.ofNullable(userCoupons.get(userCouponId));
    }

    @Override
    public UserCoupon saveUserCoupon(UserCoupon userCoupon) {
        if (userCoupon.getId() == null) {
            userCoupon.setId(idGenerator.getAndIncrement());
        }
        userCoupons.put(userCoupon.getId(), userCoupon);
        return userCoupon;
    }

    @Override
    public void updateUserCoupon(UserCoupon userCoupon) {
        userCoupons.put(userCoupon.getId(), userCoupon);
    }

    private LoadUserCouponPort.UserCouponInfo toUserCouponInfo(UserCoupon userCoupon) {
        return new LoadUserCouponPort.UserCouponInfo(
                userCoupon.getId(),
                userCoupon.getUserId(),
                userCoupon.getCouponId(),
                userCoupon.getStatus().name(),
                userCoupon.getIssuedAt() != null ? userCoupon.getIssuedAt().toString() : null,
                userCoupon.getUsedAt() != null ? userCoupon.getUsedAt().toString() : null,
                userCoupon.getOrderId()
        );
    }
} 