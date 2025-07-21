package kr.hhplus.be.server.coupon.domain;

import kr.hhplus.be.server.shared.domain.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Coupon extends BaseEntity {

    private String name;
    private String description;
    private BigDecimal discountAmount;
    private Integer maxIssuanceCount;
    private Integer issuedCount = 0;
    private CouponStatus status = CouponStatus.ACTIVE;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;

    public Coupon() {}

    public Coupon(String name, String description, BigDecimal discountAmount, 
                  Integer maxIssuanceCount, LocalDateTime validFrom, LocalDateTime validTo) {
        this.name = name;
        this.description = description;
        this.discountAmount = discountAmount;
        this.maxIssuanceCount = maxIssuanceCount;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Integer getMaxIssuanceCount() {
        return maxIssuanceCount;
    }

    public void setMaxIssuanceCount(Integer maxIssuanceCount) {
        this.maxIssuanceCount = maxIssuanceCount;
    }

    public Integer getIssuedCount() {
        return issuedCount;
    }

    public void setIssuedCount(Integer issuedCount) {
        this.issuedCount = issuedCount;
    }

    public CouponStatus getStatus() {
        return status;
    }

    public void setStatus(CouponStatus status) {
        this.status = status;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDateTime validTo) {
        this.validTo = validTo;
    }

    public boolean canIssue() {
        return status == CouponStatus.ACTIVE && 
               issuedCount < maxIssuanceCount &&
               LocalDateTime.now().isAfter(validFrom) &&
               LocalDateTime.now().isBefore(validTo);
    }

    public void incrementIssuedCount() {
        this.issuedCount++;
        if (this.issuedCount >= this.maxIssuanceCount) {
            this.status = CouponStatus.SOLD_OUT;
        }
    }

    public enum CouponStatus {
        ACTIVE, INACTIVE, SOLD_OUT, EXPIRED
    }
} 