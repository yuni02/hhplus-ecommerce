package kr.hhplus.be.server.coupon.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * UserCoupon 인프라스트럭처 엔티티
 * UserCoupon 도메인 전용 JPA 매핑 엔티티
 * 외래키 제약조건 없이 느슨한 결합으로 설계
 */
@Entity
@Table(name = "user_coupons")
@Getter
@Setter(AccessLevel.PRIVATE) // setter는 private으로 제한
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCouponEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; // 외래키 제약조건 없음

    @Column(name = "coupon_id", nullable = false)
    private Long couponId; // 외래키 제약조건 없음

    @Column(name = "discount_amount", nullable = false)
    private Integer discountAmount; // 할인 금액

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "AVAILABLE"; // enum 대신 varchar

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "order_id")
    private Long orderId; // 사용된 주문 ID - 외래키 제약조건 없음

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (issuedAt == null) {
            issuedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 필요한 경우에만 public setter 제공
    public void use(Long orderId) {
        this.status = "USED";
        this.orderId = orderId;
        this.usedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateStatus(String status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setId(Long id) {
        this.id = id;
    }

    // 기존 복잡한 생성자 제거
    // 정적 팩토리 메서드 제공
    public static UserCouponEntity create(Long userId, Long couponId, Integer discountAmount, String status,
                                          LocalDateTime issuedAt, LocalDateTime usedAt, Long orderId,
                                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        UserCouponEntity entity = new UserCouponEntity();
        entity.userId = userId;
        entity.couponId = couponId;
        entity.discountAmount = discountAmount;
        entity.status = status != null ? status : "AVAILABLE";
        entity.issuedAt = issuedAt != null ? issuedAt : LocalDateTime.now();
        entity.usedAt = usedAt;
        entity.orderId = orderId;
        entity.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        entity.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
        return entity;
    }
}