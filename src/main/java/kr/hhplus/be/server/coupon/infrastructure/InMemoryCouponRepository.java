package kr.hhplus.be.server.coupon.infrastructure;

import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.CouponRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryCouponRepository implements CouponRepository {

    private final Map<Long, Coupon> coupons = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public InMemoryCouponRepository() {
        // 더미 데이터 초기화
        initializeDummyData();
    }

    private void initializeDummyData() {
        // 쿠폰 1: 10% 할인 쿠폰
        Coupon coupon1 = new Coupon(
                "10% 할인 쿠폰",
                "모든 상품 10% 할인",
                BigDecimal.valueOf(1000),
                100,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        coupon1.setId(idGenerator.getAndIncrement());
        coupons.put(coupon1.getId(), coupon1);

        // 쿠폰 2: 5,000원 할인 쿠폰
        Coupon coupon2 = new Coupon(
                "5,000원 할인 쿠폰",
                "50,000원 이상 구매 시 5,000원 할인",
                BigDecimal.valueOf(5000),
                50,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        coupon2.setId(idGenerator.getAndIncrement());
        coupons.put(coupon2.getId(), coupon2);

        // 쿠폰 3: 20% 할인 쿠폰 (품절)
        Coupon coupon3 = new Coupon(
                "20% 할인 쿠폰",
                "모든 상품 20% 할인",
                BigDecimal.valueOf(2000),
                10,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        coupon3.setId(idGenerator.getAndIncrement());
        coupon3.setIssuedCount(10); // 품절 상태
        coupon3.setStatus(Coupon.CouponStatus.SOLD_OUT);
        coupons.put(coupon3.getId(), coupon3);
    }

    @Override
    public Optional<Coupon> findById(Long id) {
        return Optional.ofNullable(coupons.get(id));
    }

    @Override
    public Coupon save(Coupon coupon) {
        if (coupon.getId() == null) {
            coupon.setId(idGenerator.getAndIncrement());
        }
        coupons.put(coupon.getId(), coupon);
        return coupon;
    }

    @Override
    public boolean existsById(Long id) {
        return coupons.containsKey(id);
    }
} 