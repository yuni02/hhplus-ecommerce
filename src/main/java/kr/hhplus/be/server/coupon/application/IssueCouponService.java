package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.application.port.in.IssueCouponUseCase;
import kr.hhplus.be.server.coupon.application.port.out.LoadUserPort;
import kr.hhplus.be.server.coupon.application.port.out.LoadCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.SaveUserCouponPort;
import kr.hhplus.be.server.coupon.domain.UserCoupon;
import kr.hhplus.be.server.shared.kafka.CouponIssueMessage;
import kr.hhplus.be.server.shared.kafka.KafkaCouponEventProducer;
import kr.hhplus.be.server.shared.lock.DistributedLock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import java.util.Optional;

/**
 * 쿠폰 발급 Application 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IssueCouponService implements IssueCouponUseCase {

    private final LoadUserPort loadUserPort;
    private final LoadCouponPort loadCouponPort;
    private final SaveUserCouponPort saveUserCouponPort;
    private final RedisCouponService redisCouponService;
    private final RedisCouponQueueService queueService;
    private final KafkaCouponEventProducer kafkaEventProducer;  

    @Override
    public IssueCouponResult issueCoupon(IssueCouponCommand command) {
        // 빠른 검증 후 이벤트만 발행 (실제 처리는 비동기)
        return processCouponIssueAsync(command);
    }
    
    private IssueCouponResult processCouponIssueAsync(IssueCouponCommand command) {
        try {
            // 1. 기본 입력값 검증만 수행
            if (command.getUserId() == null || command.getUserId() <= 0) {
                return IssueCouponResult.failure("잘못된 사용자 ID입니다.");
            }
            
            if (command.getCouponId() == null || command.getCouponId() <= 0) {
                return IssueCouponResult.failure("잘못된 쿠폰 ID입니다.");
            }
            
            // 2. 빠른 사용자 존재 확인
            if (!loadUserPort.existsById(command.getUserId())) {
                return IssueCouponResult.failure("사용자를 찾을 수 없습니다.");
            }
            
            // 3. Redis에서 쿠폰 정보 조회 (캐시된 정보만)
            Optional<RedisCouponService.CouponInfo> cachedCouponInfo = 
                redisCouponService.getCouponInfoFromCache(command.getCouponId());
            
            LoadCouponPort.CouponInfo couponInfo;
            
            if (cachedCouponInfo.isPresent()) {
                RedisCouponService.CouponInfo cached = cachedCouponInfo.get();
                couponInfo = new LoadCouponPort.CouponInfo(
                    cached.getId(), cached.getName(), cached.getDescription(),
                    cached.getDiscountAmount(), cached.getMaxIssuanceCount(),
                    cached.getIssuedCount(), cached.getStatus(),
                    cached.getValidFrom(), cached.getValidTo()
                );
            } else {
                // 캐시 미스 시 DB 조회 (빠른 조회만)
                couponInfo = loadCouponPort.loadCouponById(command.getCouponId())
                        .orElse(null);
                
                if (couponInfo == null) {
                    return IssueCouponResult.failure("존재하지 않는 쿠폰입니다.");
                }
                
                // 캐싱
                redisCouponService.cacheCouponInfo(
                    couponInfo.getId(), couponInfo.getName(), couponInfo.getDescription(),
                    couponInfo.getDiscountAmount(), couponInfo.getMaxIssuanceCount(),
                    couponInfo.getIssuedCount(), couponInfo.getStatus(),
                    couponInfo.getValidFrom(), couponInfo.getValidTo()
                );
            }
            
            // 4. 기본적인 발급 가능 여부만 체크
            if (!canIssueCoupon(couponInfo)) {
                return IssueCouponResult.failure("발급할 수 없는 쿠폰입니다.");
            }
            
            // 5. 대기열에 추가
            boolean addedToQueue = queueService.addToQueue(command.getCouponId(), command.getUserId());
            if (!addedToQueue) {
                return IssueCouponResult.failure("이미 쿠폰 발급 요청이 처리 중입니다.");
            }
            
            // 6. Kafka 이벤트 발행 (실제 처리는 비동기)
            CouponIssueMessage message = CouponIssueMessage.of(
                command.getUserId(),
                command.getCouponId(),
                couponInfo.getName(),
                couponInfo.getDiscountAmount(),
                couponInfo.getMaxIssuanceCount()
            );
            
            kafkaEventProducer.publishCouponIssueEvent(message);
            
            log.info("쿠폰 발급 이벤트 발행 완료 - couponId: {}, userId: {}", 
                    command.getCouponId(), command.getUserId());
            
            // 7. 즉시 처리 중 응답 반환
            return IssueCouponResult.processing(
                command.getCouponId(),
                couponInfo.getName(),
                "쿠폰 발급 요청이 접수되었습니다. 잠시 후 결과를 확인해주세요."
            );
            
        } catch (Exception e) {
            log.error("쿠폰 발급 요청 처리 실패 - couponId: {}, userId: {}", 
                    command.getCouponId(), command.getUserId(), e);
            return IssueCouponResult.failure("쿠폰 발급 요청 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 쿠폰 발급 결과 조회 (대기열 처리 후)
     */
    public IssueCouponResult checkIssueResult(Long couponId, Long userId) {
        // 대기열 처리 결과 확인
        RedisCouponQueueService.CouponIssueResult result = queueService.getIssueResult(couponId, userId);
        
        if (result == null) {
            // 아직 처리되지 않음 - 대기열 순서 확인
            Long position = queueService.getUserQueuePosition(couponId, userId);
            if (position != null) {
                return IssueCouponResult.processing(
                    couponId,
                    null,
                    String.format("대기 중입니다. 현재 순서: %d", position)
                );
            }
            return IssueCouponResult.failure("발급 요청을 찾을 수 없습니다.");
        }
        
        if (result.isSuccess()) {
            return IssueCouponResult.success(
                null, // userCouponId는 별도로 조회 필요
                couponId,
                null, // couponName도 별도로 조회 필요
                null, // discountAmount도 별도로 조회 필요  
                "ISSUED",
                result.getProcessedAt()
            );
        } else {
            return IssueCouponResult.failure(result.getMessage());
        }
    }

    /**
     * 쿠폰 발급 가능 여부 확인
     */
    private boolean canIssueCoupon(LoadCouponPort.CouponInfo couponInfo) {
        LocalDateTime now = LocalDateTime.now();
        
        // 1. 상태 체크
        if (!"ACTIVE".equals(couponInfo.getStatus())) {
            return false;
        }
        
        // 2. 발급 수량 체크
        if (couponInfo.getIssuedCount() >= couponInfo.getMaxIssuanceCount()) {
            return false;
        }
        
        // 3. 발급 시작일 체크 (validFrom이 설정되어 있고, 아직 시작되지 않은 경우)
        if (couponInfo.getValidFrom() != null && now.isBefore(couponInfo.getValidFrom())) {
            return false;
        }
        
        // 4. 발급 종료일 체크 (validTo가 설정되어 있고, 이미 만료된 경우)
        if (couponInfo.getValidTo() != null && now.isAfter(couponInfo.getValidTo())) {
            return false;
        }
        
        return true;
    }
} 