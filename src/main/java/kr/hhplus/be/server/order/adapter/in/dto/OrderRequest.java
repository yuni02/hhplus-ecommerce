package kr.hhplus.be.server.order.adapter.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.hhplus.be.server.order.adapter.in.docs.OrderSchemaDescription;
import java.util.List;  

@Schema(description = "주문 요청")
public class OrderRequest {
    
    @Schema(description = OrderSchemaDescription.userId, example = "1001", required = true)
    private Long userId;
    
    @Schema(description = OrderSchemaDescription.orderProduct, required = true)
    private List<OrderItemRequest> orderItems;
    
    @Schema(description = OrderSchemaDescription.couponId + " (선택사항)", example = "1")
    private Long userCouponId;

    @Schema(description = "주문 상품 요청")
    public static class OrderItemRequest {
        
        @Schema(description = OrderSchemaDescription.productId, example = "1", required = true)
        private Long productId;
        
        @Schema(description = OrderSchemaDescription.quantity, example = "2", required = true)
        private Integer quantity;

        public OrderItemRequest() {
        }

        public OrderItemRequest(Long productId, Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    public OrderRequest() {
    }

    public OrderRequest(Long userId, List<OrderItemRequest> orderItems, Long userCouponId) {
        this.userId = userId;
        this.orderItems = orderItems;
        this.userCouponId = userCouponId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<OrderItemRequest> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItemRequest> orderItems) {
        this.orderItems = orderItems;
    }

    public Long getUserCouponId() {
        return userCouponId;
    }

    public void setUserCouponId(Long userCouponId) {
        this.userCouponId = userCouponId;
    }
} 