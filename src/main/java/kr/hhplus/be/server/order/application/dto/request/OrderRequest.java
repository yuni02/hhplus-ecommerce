package kr.hhplus.be.server.order.application.dto.request;

import java.util.List;

public class OrderRequest {
    private Long userId;
    private List<OrderItemRequest> orderItems;
    private Long userCouponId;

    public static class OrderItemRequest {
        private Long productId;
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