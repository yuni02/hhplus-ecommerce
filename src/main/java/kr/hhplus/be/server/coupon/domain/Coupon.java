package kr.hhplus.be.server.coupon.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 쿠폰 도메인 엔티티
 * 순수한 비즈니스 로직만 포함
 */
public class Coupon {

    private Long id;
    private String name;
    private String description;
    private BigDecimal discountAmount;
    private Integer maxIssuanceCount;
    private Integer issuedCount = 0;
    private CouponStatus status = CouponStatus.ACTIVE;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Coupon() {}

    public Coupon(String name, String description, BigDecimal discountAmount, 
                  Integer maxIssuanceCount, LocalDateTime validFrom, LocalDateTime validTo) {
        this.name = name;
        this.description = description;
        this.discountAmount = discountAmount;
        this.maxIssuanceCount = maxIssuanceCount;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean canIssue() {
        return status == CouponStatus.ACTIVE && 
               issuedCount < maxIssuanceCount && 
               LocalDateTime.now().isAfter(validFrom) && 
               LocalDateTime.now().isBefore(validTo);
    }

    public void incrementIssuedCount() {
        this.issuedCount++;
        this.updatedAt = LocalDateTime.now();
        if (this.issuedCount >= this.maxIssuanceCount) {
            this.status = CouponStatus.SOLD_OUT;
        }
    }

    public enum CouponStatus {
        ACTIVE, INACTIVE, SOLD_OUT, EXPIRED
    }
} 