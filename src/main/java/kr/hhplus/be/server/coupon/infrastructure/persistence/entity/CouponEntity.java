package kr.hhplus.be.server.coupon.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Coupon 인프라스트럭처 엔티티
 * Coupon 도메인 전용 JPA 매핑 엔티티
 * 외래키 제약조건 없이 느슨한 결합으로 설계
 */
@Entity
@Table(name = "coupons")
@Getter
@Setter(AccessLevel.PRIVATE) // setter는 private으로 제한
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponEntity {

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
    @Builder.Default
    private Integer issuedCount = 0;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // enum 대신 varchar

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 필요한 경우에만 public setter 제공
    public void incrementIssuedCount() {
        this.issuedCount++;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateStatus(String status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    // 기존 복잡한 생성자 제거
    // 정적 팩토리 메서드 제공
    public static CouponEntity create(String name, String description, BigDecimal discountAmount,
                                      Integer maxIssuanceCount, Integer issuedCount, String status,
                                      LocalDateTime validFrom, LocalDateTime validTo) {
        CouponEntity entity = new CouponEntity();
        entity.name = name;
        entity.description = description;
        entity.discountAmount = discountAmount;
        entity.maxIssuanceCount = maxIssuanceCount;
        entity.issuedCount = issuedCount != null ? issuedCount : 0;
        entity.status = status != null ? status : "ACTIVE";
        entity.validFrom = validFrom;
        entity.validTo = validTo;
        entity.createdAt = LocalDateTime.now();
        entity.updatedAt = LocalDateTime.now();
        return entity;
    }
}