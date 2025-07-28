package kr.hhplus.be.server.coupon.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 쿠폰 도메인 엔티티
 * ERD의 COUPON 테이블과 매핑
 */
@Entity
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "total_quantity", nullable = false)
    private Integer maxIssuanceCount;

    @Column(name = "issued_count", nullable = false)
    private Integer issuedCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CouponStatus status = CouponStatus.ACTIVE;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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
        return status == CouponStatus.ACTIVE 
            && issuedCount < maxIssuanceCount
            && (validFrom == null || LocalDateTime.now().isAfter(validFrom))
            && (validTo == null || LocalDateTime.now().isBefore(validTo));
    }

    public void incrementIssuedCount() {
        if (!canIssue()) {
            throw new IllegalStateException("쿠폰을 발급할 수 없습니다.");
        }
        this.issuedCount++;
        this.updatedAt = LocalDateTime.now();
        
        // 발급 수량이 최대치에 도달하면 상태를 SOLD_OUT으로 변경
        if (this.issuedCount >= this.maxIssuanceCount) {
            this.status = CouponStatus.SOLD_OUT;
        }
    }

    public enum CouponStatus {
        ACTIVE, INACTIVE, SOLD_OUT, EXPIRED
    }
} 