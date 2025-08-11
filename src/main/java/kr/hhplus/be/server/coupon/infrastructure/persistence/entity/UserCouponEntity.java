package kr.hhplus.be.server.coupon.infrastructure.persistence.entity;

import jakarta.persistence.*;
import kr.hhplus.be.server.shared.domain.BaseEntity;
import kr.hhplus.be.server.user.infrastructure.persistence.entity.UserEntity;
import kr.hhplus.be.server.order.infrastructure.persistence.entity.OrderEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 사용자 쿠폰 전용 엔티티
 * 사용자 쿠폰 도메인 전용 JPA 매핑 엔티티
 */
@Entity
@Table(name = "user_coupons")
@Getter
@Setter(AccessLevel.PRIVATE) // setter는 private으로 제한
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCouponEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id",
                foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private UserEntity user;

    @Column(name = "user_id", nullable = false, insertable = false, updatable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", referencedColumnName = "id",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private CouponEntity coupon;

    @Column(name = "coupon_id", nullable = false, insertable = false, updatable = false)
    private Long couponId;

    @Column(name = "discount_amount", nullable = false)
    private Integer discountAmount; // 할인 금액

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "AVAILABLE"; // enum 대신 varchar

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @PrePersist
    protected void onCreate() {
        if (issuedAt == null) {
            issuedAt = LocalDateTime.now();
        }
    }

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", referencedColumnName = "id",
                foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private OrderEntity order;

    @Column(name = "order_id", insertable = false, updatable = false)
    private Long orderId;

    // 비즈니스 메서드들
    public void use(Long orderId) {
        this.status = "USED";
        this.orderId = orderId;
        this.usedAt = LocalDateTime.now();
    }

    public void updateStatus(String status) {
        this.status = status;
    }

    public void setId(Long id) {
        // BaseEntity의 id 필드에 접근하기 위해 super를 사용
        super.setId(id);
    }

    // 정적 팩토리 메서드
    public static UserCouponEntity create(Long userId, Long couponId, Integer discountAmount, String status,
                                          LocalDateTime issuedAt, LocalDateTime usedAt, Long orderId,
                                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        return UserCouponEntity.builder()
                .userId(userId)
                .couponId(couponId)
                .discountAmount(discountAmount)
                .status(status)
                .issuedAt(issuedAt)
                .usedAt(usedAt)
                .orderId(orderId)
                .build();
    }
}