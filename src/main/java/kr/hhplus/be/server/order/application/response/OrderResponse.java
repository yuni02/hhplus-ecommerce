package kr.hhplus.be.server.order.application.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "주문 응답")
public class OrderResponse {
    
    @Schema(description = "주문 ID", example = "1")
    private Long id;
    
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
    
    @Schema(description = "사용자 쿠폰 ID", example = "1")
    private Long userCouponId;
    
    @Schema(description = "총 주문 금액", example = "50000")
    private Integer totalPrice;
    
    @Schema(description = "할인 후 금액", example = "45000")
    private Integer discountedPrice;
    
    @Schema(description = "주문 상태", example = "COMPLETED")
    private String status;
    
    @Schema(description = "주문 아이템 목록")
    private List<OrderItemResponse> orderItems;
    
    @Schema(description = "주문 생성일시")
    private LocalDateTime createdAt;

    public static class OrderItemResponse {
        @Schema(description = "주문 아이템 ID", example = "1")
        private Long id;
        
        @Schema(description = "상품 ID", example = "1")
        private Long productId;
        
        @Schema(description = "상품명", example = "상품명")
        private String productName;
        
        @Schema(description = "수량", example = "2")
        private Integer quantity;
        
        @Schema(description = "단가", example = "10000")
        private Integer unitPrice;
        
        @Schema(description = "총 가격", example = "20000")
        private Integer totalPrice;

        public OrderItemResponse() {}

        public OrderItemResponse(Long id, Long productId, String productName, Integer quantity,
                               Integer unitPrice, Integer totalPrice) {
            this.id = id;
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.totalPrice = totalPrice;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public Integer getUnitPrice() { return unitPrice; }
        public void setUnitPrice(Integer unitPrice) { this.unitPrice = unitPrice; }
        public Integer getTotalPrice() { return totalPrice; }
        public void setTotalPrice(Integer totalPrice) { this.totalPrice = totalPrice; }
    }

    public OrderResponse() {}

    public OrderResponse(Long id, Long userId, Long userCouponId, Integer totalPrice, 
                        Integer discountedPrice, String status, List<OrderItemResponse> orderItems, 
                        LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.userCouponId = userCouponId;
        this.totalPrice = totalPrice;
        this.discountedPrice = discountedPrice;
        this.status = status;
        this.orderItems = orderItems;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getUserCouponId() { return userCouponId; }
    public void setUserCouponId(Long userCouponId) { this.userCouponId = userCouponId; }
    public Integer getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Integer totalPrice) { this.totalPrice = totalPrice; }
    public Integer getDiscountedPrice() { return discountedPrice; }
    public void setDiscountedPrice(Integer discountedPrice) { this.discountedPrice = discountedPrice; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<OrderItemResponse> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItemResponse> orderItems) { this.orderItems = orderItems; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
} 