package kr.hhplus.be.server.coupon.infrastructure.persistence.adapter;

import kr.hhplus.be.server.coupon.application.port.out.LoadUserCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.SaveUserCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.UpdateUserCouponPort;
import kr.hhplus.be.server.coupon.domain.UserCoupon;
import kr.hhplus.be.server.coupon.infrastructure.persistence.entity.CouponEntity;
import kr.hhplus.be.server.coupon.infrastructure.persistence.entity.UserCouponEntity;
import kr.hhplus.be.server.coupon.infrastructure.persistence.repository.CouponJpaRepository;
import kr.hhplus.be.server.coupon.infrastructure.persistence.repository.UserCouponJpaRepository;
import kr.hhplus.be.server.user.infrastructure.persistence.entity.UserEntity;
import kr.hhplus.be.server.user.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 사용자 쿠폰 영속성 Adapter (Outgoing)
 * JPA Repository를 사용하여 실제 데이터베이스와 연결
 */
@Component("couponUserCouponPersistenceAdapter")
public class UserCouponPersistenceAdapter implements LoadUserCouponPort, SaveUserCouponPort, UpdateUserCouponPort {

    private final UserCouponJpaRepository userCouponJpaRepository;
    private final CouponJpaRepository couponJpaRepository;
    private final UserJpaRepository userJpaRepository;

    public UserCouponPersistenceAdapter(UserCouponJpaRepository userCouponJpaRepository, CouponJpaRepository couponJpaRepository, UserJpaRepository userJpaRepository) {
        this.userCouponJpaRepository = userCouponJpaRepository;
        this.couponJpaRepository = couponJpaRepository;
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public List<LoadUserCouponPort.UserCouponInfo> loadUserCouponsByUserId(Long userId) {
        return userCouponJpaRepository.findByUserId(userId)
                .stream()
                .map(this::toUserCouponInfo)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UserCoupon> loadUserCoupon(Long userCouponId) {
        return userCouponJpaRepository.findById(userCouponId)
                .map(this::mapToUserCoupon);
    }

    @Override
    public Optional<UserCoupon> loadUserCouponWithLock(Long userCouponId) {
        return userCouponJpaRepository.findByIdWithLock(userCouponId)
                .map(this::mapToUserCoupon);
    }

    @Override
    @Transactional
    public UserCoupon saveUserCoupon(UserCoupon userCoupon) {
        UserCouponEntity entity = mapToUserCouponEntity(userCoupon);
        UserCouponEntity savedEntity = userCouponJpaRepository.save(entity);
        return mapToUserCoupon(savedEntity);
    }

    @Override
    @Transactional
    public void updateUserCoupon(UserCoupon userCoupon) {
        UserCouponEntity entity = mapToUserCouponEntity(userCoupon);
        userCouponJpaRepository.save(entity);
    }

    private LoadUserCouponPort.UserCouponInfo toUserCouponInfo(UserCouponEntity entity) {
        return new LoadUserCouponPort.UserCouponInfo(
                entity.getId(),
                entity.getUserId(),
                entity.getCouponId(),
                entity.getStatus(),
                entity.getIssuedAt() != null ? entity.getIssuedAt().toString() : null,
                entity.getUsedAt() != null ? entity.getUsedAt().toString() : null,
                entity.getOrderId()
        );
    }

    private UserCoupon mapToUserCoupon(UserCouponEntity entity) {
        return UserCoupon.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .couponId(entity.getCouponId())
                .discountAmount(entity.getDiscountAmount())
                .status(UserCoupon.UserCouponStatus.valueOf(entity.getStatus()))
                .issuedAt(entity.getIssuedAt())
                .usedAt(entity.getUsedAt())
                .orderId(entity.getOrderId())
                .build();
    }

    private UserCouponEntity mapToUserCouponEntity(UserCoupon userCoupon) {
        // CouponEntity 조회 (coupon_id를 통해)
        CouponEntity couponEntity = null;
        if (userCoupon.getCouponId() != null) {
            couponEntity = couponJpaRepository.findById(userCoupon.getCouponId()).orElse(null);
        }
        
        // UserEntity 조회 (user_id를 통해)
        UserEntity userEntity = null;
        if (userCoupon.getUserId() != null) {
            userEntity = userJpaRepository.findByUserIdAndStatus(userCoupon.getUserId(), "ACTIVE").orElse(null);
        }
        
        UserCouponEntity entity = UserCouponEntity.builder()
                .user(userEntity)  // user 관계 설정
                .coupon(couponEntity)  // coupon 관계 설정
                .discountAmount(userCoupon.getDiscountAmount())
                .status(userCoupon.getStatus().name())
                .issuedAt(userCoupon.getIssuedAt())
                .usedAt(userCoupon.getUsedAt())
                .orderId(userCoupon.getOrderId())
                .build();
        
        // id가 있으면 설정 (UPDATE를 위해)
        if (userCoupon.getId() != null) {
            entity.setId(userCoupon.getId());
        }
        
        return entity;
    }
} 