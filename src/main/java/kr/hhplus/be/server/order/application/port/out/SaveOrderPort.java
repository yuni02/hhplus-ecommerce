package kr.hhplus.be.server.order.application.port.out;

import kr.hhplus.be.server.order.domain.Order;

/**
 * 주문 저장 Outgoing Port
 */
public interface SaveOrderPort {
    
    /**
     * 주문 저장
     */
    Order saveOrder(Order order);
} 