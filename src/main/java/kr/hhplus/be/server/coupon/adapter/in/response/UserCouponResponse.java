package kr.hhplus.be.server.coupon.adapter.in.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "사용자 쿠폰 응답")
public class UserCouponResponse {

    @Schema(description = "사용자 쿠폰 ID", example = "1")
    private final Long userCouponId;

    @Schema(description = "쿠폰 ID", example = "1")
    private final Long couponId;

    @Schema(description = "쿠폰명", example = "신규 가입 쿠폰")
    private final String couponName;

    @Schema(description = "할인 금액", example = "1000")
    private final Integer discountAmount;

    @Schema(description = "상태", example = "AVAILABLE")
    private String status;

    @Schema(description = "발급일시")
    private final LocalDateTime issuedAt;

    @Schema(description = "사용일시")
    private LocalDateTime usedAt;

    public UserCouponResponse(Long userCouponId, Long couponId, String couponName,
                             Integer discountAmount, String status,
                             LocalDateTime issuedAt, LocalDateTime usedAt) {
        this.userCouponId = userCouponId;
        this.couponId = couponId;
        this.couponName = couponName;
        this.discountAmount = discountAmount;
        this.status = status;
        this.issuedAt = issuedAt;
        this.usedAt = usedAt;
    }

    public Long getUserCouponId() {
        return userCouponId;
    }

    public Long getCouponId() {
        return couponId;
    }

    public String getCouponName() {
        return couponName;
    }

    public Integer getDiscountAmount() {
        return discountAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }
} 