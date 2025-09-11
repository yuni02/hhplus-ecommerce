package kr.hhplus.be.server.coupon.domain;

public enum CouponIssueStatus {
    PROCESSING("PROCESSING"),
    ISSUED("ISSUED"),
    FAILED("FAILED");
    
    private final String value;
    
    CouponIssueStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
}