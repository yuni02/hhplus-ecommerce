package kr.hhplus.be.server.coupon.adapter.in.docs; 

public interface CouponSchemaDescription {

    String userId = "사용자 ID";
    String couponId = "쿠폰 ID";
    String userCouponId = "사용자 쿠폰 ID";
    String couponName = "쿠폰명";
    String discountType = "할인 유형";
    String discountValue = "할인 값";
    String validFrom = "유효 시작일";
    String validTo = "유효 종료일";
    String isUsed = "사용 여부";
    String issuedAt = "발급 일시";

} 