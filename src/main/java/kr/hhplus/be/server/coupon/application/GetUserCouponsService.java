package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.application.port.in.GetUserCouponsUseCase;
import kr.hhplus.be.server.coupon.application.port.out.LoadUserPort;
import kr.hhplus.be.server.coupon.application.port.out.LoadUserCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.LoadCouponPort;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 사용자 쿠폰 조회 Application 서비스
 */
@Service
public class GetUserCouponsService implements GetUserCouponsUseCase {

    private final LoadUserPort loadUserPort;
    private final LoadUserCouponPort loadUserCouponPort;
    private final LoadCouponPort loadCouponPort;

    public GetUserCouponsService(LoadUserPort loadUserPort,
                                LoadUserCouponPort loadUserCouponPort,
                                LoadCouponPort loadCouponPort) {
        this.loadUserPort = loadUserPort;
        this.loadUserCouponPort = loadUserCouponPort;
        this.loadCouponPort = loadCouponPort;
    }

    @Override
    public GetUserCouponsResult getUserCoupons(GetUserCouponsCommand command) {
        try {
            // 1. 입력값 검증
            if (command.getUserId() == null || command.getUserId() <= 0) {
                throw new IllegalArgumentException("잘못된 사용자 ID입니다.");
            }
            
            // 2. 사용자 존재 확인
            if (!loadUserPort.existsById(command.getUserId())) {
                return new GetUserCouponsResult(List.of());
            }

            // 3. 사용자 쿠폰 목록 조회
            List<LoadUserCouponPort.UserCouponInfo> userCouponInfos = loadUserCouponPort.loadUserCouponsByUserId(command.getUserId());

            // 4. 쿠폰 상세 정보 조회 및 결과 생성
            List<UserCouponInfo> result = userCouponInfos.stream()
                    .map(this::enrichUserCouponInfo)
                    .collect(Collectors.toList());

            return new GetUserCouponsResult(result);

        } catch (Exception e) {
            return new GetUserCouponsResult(List.of());
        }
    }

    /**
     * 사용자 쿠폰 정보에 쿠폰 상세 정보를 추가
     */
    private UserCouponInfo enrichUserCouponInfo(LoadUserCouponPort.UserCouponInfo userCouponInfo) {
        try {
            // 쿠폰 상세 정보 조회
            LoadCouponPort.CouponInfo couponInfo = loadCouponPort.loadCouponById(userCouponInfo.getCouponId())
                    .orElse(null);

            if (couponInfo == null) {
                // 쿠폰 정보가 없는 경우 기본값으로 처리
                return new UserCouponInfo(
                        userCouponInfo.getId(),
                        userCouponInfo.getCouponId(),
                        "알 수 없는 쿠폰",
                        0,
                        userCouponInfo.getStatus(),
                        parseDateTime(userCouponInfo.getIssuedAt()),
                        parseDateTime(userCouponInfo.getUsedAt())
                );
            }

            return new UserCouponInfo(
                    userCouponInfo.getId(),
                    userCouponInfo.getCouponId(),
                    couponInfo.getName(),
                    couponInfo.getDiscountAmount(),
                    userCouponInfo.getStatus(),
                    parseDateTime(userCouponInfo.getIssuedAt()),
                    parseDateTime(userCouponInfo.getUsedAt())
            );

        } catch (Exception e) {
            // 오류 발생 시 기본값으로 처리
            return new UserCouponInfo(
                    userCouponInfo.getId(),
                    userCouponInfo.getCouponId(),
                    "오류",
                    0,
                    userCouponInfo.getStatus(),
                    null,
                    null
            );
        }
    }

    /**
     * 문자열을 LocalDateTime으로 변환 (null 허용)
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }
} 