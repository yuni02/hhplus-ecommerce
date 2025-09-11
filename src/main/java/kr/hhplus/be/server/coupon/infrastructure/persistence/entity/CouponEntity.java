package kr.hhplus.be.server.coupon.infrastructure.persistence.entity;

import jakarta.persistence.*;
import kr.hhplus.be.server.shared.domain.BaseEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 쿠폰 전용 엔티티
 * 쿠폰 도메인 전용 JPA 매핑 엔티티
 */
@Entity
@Table(name = "coupons")
@Getter
@Setter(AccessLevel.PRIVATE) // setter는 private으로 제한
@NoArgsConstructor
@AllArgsConstructor
@Builder
@AttributeOverrides({
    @AttributeOverride(name = "id", column = @Column(name = "coupon_id"))
})
public class CouponEntity extends BaseEntity {

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

    @Version
    @Column(name = "version")
    private Long version;

    // 비즈니스 메서드들
    public void incrementIssuedCount() {
        this.issuedCount++;
    }

    public void updateStatus(String status) {
        this.status = status;
    }

    // 정적 팩토리 메서드
    public static CouponEntity create(String name, String description, BigDecimal discountAmount,
                                      Integer maxIssuanceCount, Integer issuedCount, String status,
                                      LocalDateTime validFrom, LocalDateTime validTo) {
        return CouponEntity.builder()
                .name(name)
                .description(description)
                .discountAmount(discountAmount)
                .maxIssuanceCount(maxIssuanceCount)
                .issuedCount(issuedCount)
                .status(status)
                .validFrom(validFrom)
                .validTo(validTo)
                .build();
    }
}