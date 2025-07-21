package kr.hhplus.be.server.coupon.infrastructure;

import kr.hhplus.be.server.coupon.domain.UserCoupon;
import kr.hhplus.be.server.coupon.domain.UserCouponRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryUserCouponRepository implements UserCouponRepository {

    private final Map<Long, UserCoupon> userCoupons = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public List<UserCoupon> findByUserId(Long userId) {
        return userCoupons.values().stream()
                .filter(uc -> uc.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserCoupon> findByUserIdAndStatus(Long userId, UserCoupon.UserCouponStatus status) {
        return userCoupons.values().stream()
                .filter(uc -> uc.getUserId().equals(userId) && uc.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByUserIdAndCouponId(Long userId, Long couponId) {
        return userCoupons.values().stream()
                .anyMatch(uc -> uc.getUserId().equals(userId) && uc.getCouponId().equals(couponId));
    }

    @Override
    public UserCoupon save(UserCoupon userCoupon) {
        if (userCoupon.getId() == null) {
            userCoupon.setId(idGenerator.getAndIncrement());
        }
        userCoupons.put(userCoupon.getId(), userCoupon);
        return userCoupon;
    }

    @Override
    public Optional<UserCoupon> findById(Long id) {
        return Optional.ofNullable(userCoupons.get(id));
    }
} 