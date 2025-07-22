package kr.hhplus.be.server.coupon.adapter.in.dto;

public class CouponRequest {
    private String name;
    private Integer discountAmount;
    private Integer totalQuantity;

    public CouponRequest() {
    }

    public CouponRequest(String name, Integer discountAmount, Integer totalQuantity) {
        this.name = name;
        this.discountAmount = discountAmount;
        this.totalQuantity = totalQuantity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }
} 