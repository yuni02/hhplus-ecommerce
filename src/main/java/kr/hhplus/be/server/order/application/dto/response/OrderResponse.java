package kr.hhplus.be.server.order.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 응답 DTO
 * 
 * 이 주문 데이터는 ORDER_HISTORY_EVENT 로그성 테이블에 이벤트로 기록됩니다.
 * - 주문 상태 변경 시마다 이벤트 로그 생성
 * - JSON payload로 주문 상세 정보 저장
 * - 외부 데이터 플랫폼 전송을 위한 이벤트 소싱
 */
public class OrderResponse {
    private Long id;
    private Long userId;
    private Long userCouponId;
    private Integer totalPrice;
    private Integer discountedPrice;
    private String status;
    private List<OrderItemResponse> orderItems;
    private LocalDateTime createdAt;

    /**
     * 주문 아이템 정보
     * ORDER_HISTORY_EVENT의 JSON payload에 포함되는 데이터
     */
    public static class OrderItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private Integer quantity;
        private Integer unitPriceSnapshot; // 주문 시점의 가격 스냅샷 (로그성 데이터)
        private Integer totalPrice;

        public OrderItemResponse() {
        }

        public OrderItemResponse(Long id, Long productId, String productName, Integer quantity,
                Integer unitPriceSnapshot, Integer totalPrice) {
            this.id = id;
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPriceSnapshot = unitPriceSnapshot;
            this.totalPrice = totalPrice;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Integer getUnitPriceSnapshot() {
            return unitPriceSnapshot;
        }

        public void setUnitPriceSnapshot(Integer unitPriceSnapshot) {
            this.unitPriceSnapshot = unitPriceSnapshot;
        }

        public Integer getTotalPrice() {
            return totalPrice;
        }

        public void setTotalPrice(Integer totalPrice) {
            this.totalPrice = totalPrice;
        }
    }

    public OrderResponse() {
    }

    public OrderResponse(Long id, Long userId, Long userCouponId, Integer totalPrice, Integer discountedPrice,
            String status, List<OrderItemResponse> orderItems, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.userCouponId = userCouponId;
        this.totalPrice = totalPrice;
        this.discountedPrice = discountedPrice;
        this.status = status;
        this.orderItems = orderItems;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getUserCouponId() {
        return userCouponId;
    }

    public void setUserCouponId(Long userCouponId) {
        this.userCouponId = userCouponId;
    }

    public Integer getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Integer totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Integer getDiscountedPrice() {
        return discountedPrice;
    }

    public void setDiscountedPrice(Integer discountedPrice) {
        this.discountedPrice = discountedPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<OrderItemResponse> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItemResponse> orderItems) {
        this.orderItems = orderItems;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
} 