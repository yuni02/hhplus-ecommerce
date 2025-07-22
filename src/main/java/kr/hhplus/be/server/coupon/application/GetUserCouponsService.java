package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.application.port.in.GetUserCouponsUseCase;
import kr.hhplus.be.server.coupon.application.port.out.LoadUserCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.LoadUserPort;
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

    public GetUserCouponsService(LoadUserPort loadUserPort, LoadUserCouponPort loadUserCouponPort) {
        this.loadUserPort = loadUserPort;
        this.loadUserCouponPort = loadUserCouponPort;
    }

    @Override
    public GetUserCouponsResult getUserCoupons(GetUserCouponsCommand command) {
        // 1. 사용자 존재 확인
        if (!loadUserPort.existsById(command.getUserId())) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
        
        // 2. 사용자 쿠폰 조회
        List<LoadUserCouponPort.UserCouponInfo> userCoupons = 
                loadUserCouponPort.loadUserCouponsByUserId(command.getUserId());
        
        // 3. 결과 변환
        List<UserCouponInfo> userCouponInfos = userCoupons.stream()
                .map(uc -> new UserCouponInfo(
                        uc.getId(),
                        uc.getCouponId(),
                        "쿠폰", // 실제로는 쿠폰 정보에서 가져와야 함
                        1000, // 실제로는 쿠폰 정보에서 가져와야 함
                        uc.getStatus(),
                        parseDateTime(uc.getIssuedAt()),
                        parseDateTime(uc.getUsedAt())
                ))
                .collect(Collectors.toList());
        
        return new GetUserCouponsResult(userCouponInfos);
    }

    /**
     * 날짜 문자열을 LocalDateTime으로 변환
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }
} 