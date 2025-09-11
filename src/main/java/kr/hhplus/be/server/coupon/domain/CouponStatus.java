package kr.hhplus.be.server.coupon.domain;

public enum CouponStatus {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE"),
    EXPIRED("EXPIRED");
    
    private final String value;
    
    CouponStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
}