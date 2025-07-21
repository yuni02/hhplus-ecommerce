package kr.hhplus.be.server.order.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * 주문 도메인 서비스 인터페이스
 * 주문 생성과 관련된 비즈니스 로직을 캡슐화
 */
public interface OrderService {
    
    /**
     * 주문 생성 검증
     */
    OrderValidationResult validateOrderCreation(Long userId, List<OrderItem> orderItems);
    
    /**
     * 주문 총액 계산
     */
    BigDecimal calculateTotalAmount(List<OrderItem> orderItems);
    
    /**
     * 쿠폰 할인 적용
     */
    BigDecimal applyCouponDiscount(BigDecimal totalAmount, BigDecimal discountAmount);
}

 