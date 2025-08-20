package kr.hhplus.be.server.coupon.infrastructure.persistence.adapter;

import kr.hhplus.be.server.coupon.application.port.out.LoadCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.SaveCouponPort;
import kr.hhplus.be.server.coupon.application.RedisCouponService;
import kr.hhplus.be.server.coupon.infrastructure.persistence.entity.CouponEntity;
import kr.hhplus.be.server.coupon.infrastructure.persistence.repository.CouponJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 쿠폰 영속성 Adapter (Outgoing)
 * JPA Repository를 사용하여 실제 데이터베이스와 연결
 */
@Component
public class CouponPersistenceAdapter implements LoadCouponPort, SaveCouponPort {

    private final CouponJpaRepository couponJpaRepository;
    private final RedisCouponService redisCouponService; // Redis 캐시 업데이트용

    public CouponPersistenceAdapter(CouponJpaRepository couponJpaRepository, 
                                   RedisCouponService redisCouponService) {
        this.couponJpaRepository = couponJpaRepository;
        this.redisCouponService = redisCouponService;
    }

    // private void initializeDummyDataIfNeeded() {
    //     if (couponJpaRepository.count() == 0) {
    //         // 더미 데이터 생성
    //         for (long couponId = 1; couponId <= 3; couponId++) {
    //             CouponEntity coupon = new CouponEntity(
    //                     "쿠폰 " + couponId,
    //                     "쿠폰 " + couponId + " 설명",
    //                     BigDecimal.valueOf(1000 * couponId),
    //                     100,
    //                     LocalDateTime.now(),
    //                     LocalDateTime.now().plusDays(30)
    //             );
    //             couponJpaRepository.save(coupon);
    //         }
    //     }
    // }

    @Override
    public Optional<LoadCouponPort.CouponInfo> loadCouponById(Long couponId) {
        return couponJpaRepository.findById(couponId)
                .map(this::mapToCouponInfo);
    }

    @Override
    public Optional<LoadCouponPort.CouponInfo> loadCouponByIdWithLock(Long couponId) {
        return couponJpaRepository.findByIdWithLock(couponId)
                .map(this::mapToCouponInfo);
    }

    @Override
    @Transactional
    public SaveCouponPort.CouponInfo saveCoupon(SaveCouponPort.CouponInfo couponInfo) {
        CouponEntity entity = mapToCouponEntity(couponInfo);
        CouponEntity savedEntity = couponJpaRepository.save(entity);
        return mapToSaveCouponInfo(savedEntity);
    }

    @Override
    @Transactional
    public boolean incrementIssuedCount(Long couponId) {
        int updatedRows = couponJpaRepository.incrementIssuedCount(couponId);
        
        if (updatedRows > 0) {
            // DB 업데이트 성공 시 Redis 캐시도 업데이트
            try {
                // 현재 발급 수량 조회
                Optional<CouponEntity> couponOpt = couponJpaRepository.findById(couponId);
                if (couponOpt.isPresent()) {
                    CouponEntity coupon = couponOpt.get();
                    redisCouponService.updateCouponIssuedCount(couponId, coupon.getIssuedCount());
                }
            } catch (Exception e) {
                // Redis 업데이트 실패는 로그만 남기고 DB 업데이트는 성공으로 처리
                // Redis는 캐시이므로 DB가 원본 데이터
            }
        }
        
        return updatedRows > 0;
    }

    @Override
    public List<LoadCouponPort.CouponInfo> loadAllCoupons() {
        return couponJpaRepository.findAll().stream()
                .map(this::mapToCouponInfo)
                .collect(Collectors.toList());
    }

    private LoadCouponPort.CouponInfo mapToCouponInfo(CouponEntity entity) {
        return new LoadCouponPort.CouponInfo(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getDiscountAmount().intValue(),
                entity.getMaxIssuanceCount(),
                entity.getIssuedCount(),
                entity.getStatus(),
                entity.getValidFrom(),  // validFrom을 startDate로 매핑
                entity.getValidTo()     // validTo를 endDate로 매핑
        );
    }

    private SaveCouponPort.CouponInfo mapToSaveCouponInfo(CouponEntity entity) {
        return new SaveCouponPort.CouponInfo(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getDiscountAmount().intValue(),
                entity.getMaxIssuanceCount(),
                entity.getIssuedCount(),
                entity.getStatus()
        );
    }

    private CouponEntity mapToCouponEntity(SaveCouponPort.CouponInfo couponInfo) {
        return CouponEntity.create(
                couponInfo.getName(),
                couponInfo.getDescription(),
                BigDecimal.valueOf(couponInfo.getDiscountAmount()),
                couponInfo.getMaxIssuanceCount(),   
                couponInfo.getIssuedCount(),
                couponInfo.getStatus(),
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        );
    }
} 