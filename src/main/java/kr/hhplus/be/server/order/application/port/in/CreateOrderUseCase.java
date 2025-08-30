package kr.hhplus.be.server.order.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 생성 Incoming Port (Use Case)
 */
public interface CreateOrderUseCase {
    
    /**
     * 주문 생성 실행
     */
    CreateOrderResult createOrder(CreateOrderCommand command);
    
    /**
     * 주문 생성 명령
     */
    class CreateOrderCommand {
        private final Long userId;
        private final List<OrderItemCommand> orderItems;
        private final Long userCouponId;
        
        public CreateOrderCommand(Long userId, List<OrderItemCommand> orderItems, Long userCouponId) {
            this.userId = userId;
            this.orderItems = orderItems;
            this.userCouponId = userCouponId;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public List<OrderItemCommand> getOrderItems() {
            return orderItems;
        }
        
        public Long getUserCouponId() {
            return userCouponId;
        }
    }
    
    /**
     * 주문 아이템 명령
     */
    class OrderItemCommand {
        private final Long productId;
        private final Integer quantity;
        
        public OrderItemCommand(Long productId, Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }
        
        public Long getProductId() {
            return productId;
        }
        
        public Integer getQuantity() {
            return quantity;
        }
    }
    
    /**
     * 주문 생성 결과
     */
    class CreateOrderResult {
        private final boolean success;
        private final Long orderId;
        private final Long userId;
        private final Long userCouponId;
        private final BigDecimal totalAmount;
        private final BigDecimal discountedAmount;
        private final BigDecimal discountAmount;  // 할인 금액 추가
        private final BigDecimal finalAmount;
        private final String status;
        private final List<OrderItemResult> orderItems;
        private final LocalDateTime createdAt;
        private final String errorMessage;
        
        private CreateOrderResult(boolean success, Long orderId, Long userId, Long userCouponId,
                                BigDecimal totalAmount, BigDecimal discountedAmount, BigDecimal discountAmount, 
                                BigDecimal finalAmount, String status, List<OrderItemResult> orderItems, 
                                LocalDateTime createdAt, String errorMessage) {
            this.success = success;
            this.orderId = orderId;
            this.userId = userId;
            this.userCouponId = userCouponId;
            this.totalAmount = totalAmount;
            this.discountedAmount = discountedAmount;
            this.discountAmount = discountAmount;
            this.finalAmount = finalAmount;
            this.status = status;
            this.orderItems = orderItems;
            this.createdAt = createdAt;
            this.errorMessage = errorMessage;
        }
        
        public static CreateOrderResult success(Long orderId, Long userId, Long userCouponId,
                                              BigDecimal totalAmount, BigDecimal discountedAmount, BigDecimal discountAmount,
                                              BigDecimal finalAmount, String status, List<OrderItemResult> orderItems, 
                                              LocalDateTime createdAt) {
            return new CreateOrderResult(true, orderId, userId, userCouponId, totalAmount, discountedAmount, 
                                       discountAmount, finalAmount, status, orderItems, createdAt, null);
        }
        
        public static CreateOrderResult failure(String errorMessage) {
            return new CreateOrderResult(false, null, null, null, null, null, null, null, null, null, null, errorMessage);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public Long getOrderId() {
            return orderId;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public Long getUserCouponId() {
            return userCouponId;
        }
        
        public BigDecimal getTotalAmount() {
            return totalAmount;
        }
        
        public BigDecimal getDiscountedAmount() {
            return discountedAmount;
        }
        
        public BigDecimal getDiscountAmount() {
            return discountAmount;
        }
        
        public String getStatus() {
            return status;
        }
        
        public List<OrderItemResult> getOrderItems() {
            return orderItems;
        }
        
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
        
        public LocalDateTime getOrderedAt() {
            return createdAt;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
    
    /**
     * 주문 아이템 결과
     */
    class OrderItemResult {
        private final Long id;
        private final Long productId;
        private final String productName;
        private final Integer quantity;
        private final BigDecimal unitPrice;
        private final BigDecimal totalPrice;
        
        public OrderItemResult(Long id, Long productId, String productName, Integer quantity,
                              BigDecimal unitPrice, BigDecimal totalPrice) {
            this.id = id;
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.totalPrice = totalPrice;
        }
        
        public Long getId() {
            return id;
        }
        
        public Long getProductId() {
            return productId;
        }
        
        public String getProductName() {
            return productName;
        }
        
        public Integer getQuantity() {
            return quantity;
        }
        
        public BigDecimal getUnitPrice() {
            return unitPrice;
        }
        
        public BigDecimal getTotalPrice() {
            return totalPrice;
        }
    }
} 