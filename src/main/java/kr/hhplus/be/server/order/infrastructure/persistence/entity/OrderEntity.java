package kr.hhplus.be.server.order.infrastructure.persistence.entity;

import jakarta.persistence.*;
import kr.hhplus.be.server.shared.domain.BaseEntity;
import kr.hhplus.be.server.user.infrastructure.persistence.entity.UserEntity;
import kr.hhplus.be.server.coupon.infrastructure.persistence.entity.UserCouponEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주문 전용 엔티티
 * 주문 도메인 전용 JPA 매핑 엔티티
 */
@Entity
@Table(name = "orders")
@Getter
@Setter(AccessLevel.PRIVATE) // setter는 private으로 제한
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id",
                foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private UserEntity user;

    @Column(name = "user_id", nullable = false, insertable = false, updatable = false)
    private Long userId;

    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "discounted_amount", precision = 15, scale = 2)
    private BigDecimal discountedAmount;

    @Column(name = "discount_amount", precision = 15, scale = 2)
    private BigDecimal discountAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_coupon_id", referencedColumnName = "id",
                foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private UserCouponEntity userCoupon;

    @Column(name = "user_coupon_id", insertable = false, updatable = false)
    private Long userCouponId;

    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "ordered_at")
    private LocalDateTime orderedAt;

    // 비즈니스 메서드들
    public void updateStatus(String status) {
        this.status = status;
    }

    public void setOrderedAt(LocalDateTime orderedAt) {
        this.orderedAt = orderedAt;
    }

    public void updateAmounts(BigDecimal totalAmount, BigDecimal discountedAmount, BigDecimal discountAmount) {
        this.totalAmount = totalAmount;
        this.discountedAmount = discountedAmount;
        this.discountAmount = discountAmount;
    }
}