package kr.hhplus.be.server.order.infrastructure.persistence.adapter;

import kr.hhplus.be.server.order.application.port.out.SaveOrderPort;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.domain.OrderHistoryEvent;
import kr.hhplus.be.server.order.infrastructure.persistence.entity.OrderEntity;
import kr.hhplus.be.server.order.infrastructure.persistence.entity.OrderItemEntity;
import kr.hhplus.be.server.order.infrastructure.persistence.entity.OrderHistoryEventEntity;
import kr.hhplus.be.server.order.infrastructure.persistence.repository.OrderJpaRepository;
import kr.hhplus.be.server.order.infrastructure.persistence.repository.OrderItemJpaRepository;
import kr.hhplus.be.server.order.infrastructure.persistence.repository.OrderHistoryEventJpaRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Order 인프라스트럭처 영속성 Adapter
 * 실제 DB와 연결하여 Order 도메인 객체를 저장/조회
 */
@Component
public class OrderPersistenceAdapter implements SaveOrderPort {

    private final OrderJpaRepository orderJpaRepository;
    private final OrderItemJpaRepository orderItemJpaRepository;
    private final OrderHistoryEventJpaRepository orderHistoryEventJpaRepository;

    public OrderPersistenceAdapter(OrderJpaRepository orderJpaRepository,
                                  OrderItemJpaRepository orderItemJpaRepository,
                                  OrderHistoryEventJpaRepository orderHistoryEventJpaRepository) {
        this.orderJpaRepository = orderJpaRepository;
        this.orderItemJpaRepository = orderItemJpaRepository;
        this.orderHistoryEventJpaRepository = orderHistoryEventJpaRepository;
    }

    @Override
    @Transactional
    public Order saveOrder(Order order) {
        // 1. Order 도메인 객체를 OrderEntity로 변환
        OrderEntity orderEntity = mapToOrderEntity(order);
        
        // 2. Order 저장
        OrderEntity savedOrderEntity = orderJpaRepository.save(orderEntity);
        
        // 3. OrderItem들 저장
        List<OrderItemEntity> orderItemEntities = order.getOrderItems().stream()
                .map(item -> mapToOrderItemEntity(item, savedOrderEntity.getId()))
                .collect(Collectors.toList());
        orderItemJpaRepository.saveAll(orderItemEntities);
        
        // 4. OrderHistoryEvent들 저장
        List<OrderHistoryEventEntity> historyEventEntities = order.getHistoryEvents().stream()
                .map(event -> mapToOrderHistoryEventEntity(event, savedOrderEntity.getId()))
                .collect(Collectors.toList());
        orderHistoryEventJpaRepository.saveAll(historyEventEntities);
        
        // 5. 저장된 OrderEntity를 다시 Order 도메인 객체로 변환하여 반환
        return mapToOrder(savedOrderEntity, orderItemEntities, historyEventEntities);
    }

    /**
     * Order 도메인 객체를 OrderEntity로 변환
     */
    private OrderEntity mapToOrderEntity(Order order) {
        OrderEntity entity = OrderEntity.builder()
            .userId(order.getUserId())
            .totalAmount(order.getTotalAmount())
            .discountedAmount(order.getDiscountedAmount())
            .discountAmount(order.getDiscountAmount())
            .userCouponId(order.getUserCouponId())
            .status(order.getStatus().name())
            .orderedAt(order.getOrderedAt())
            .createdAt(order.getOrderedAt())
            .updatedAt(order.getUpdatedAt())
            .build();
        return entity;
    }

    /**
     * OrderItem 도메인 객체를 OrderItemEntity로 변환
     */
    private OrderItemEntity mapToOrderItemEntity(OrderItem orderItem, Long orderId) {
        OrderItemEntity entity = OrderItemEntity.builder()
            .orderId(orderId)
            .productId(orderItem.getProductId())
            .productName(orderItem.getProductName())
            .quantity(orderItem.getQuantity())
            .unitPrice(orderItem.getUnitPrice())
            .totalPrice(orderItem.getTotalPrice())
            .build();
        return entity;
    }

    /**
     * OrderHistoryEvent 도메인 객체를 OrderHistoryEventEntity로 변환
     */
    private OrderHistoryEventEntity mapToOrderHistoryEventEntity(OrderHistoryEvent event, Long orderId) {
        OrderHistoryEventEntity entity = OrderHistoryEventEntity.builder()
            .id(event.getId())
            .orderId(orderId)
            .eventType(event.getEventType().name())
            .occurredAt(event.getOccurredAt())
            .cancelReason(event.getCancelReason())
            .refundAmount(event.getRefundAmount())
            .paymentMethod(event.getPaymentMethod())
            .totalAmount(event.getTotalAmount())
            .discountAmount(event.getDiscountAmount())
            .discountedAmount(event.getDiscountedAmount())  
            .createdAt(event.getCreatedAt())
            .build();
        return entity;
    }

    /**
     * OrderEntity를 Order 도메인 객체로 변환
     */
    private Order mapToOrder(OrderEntity orderEntity, List<OrderItemEntity> orderItemEntities, 
                           List<OrderHistoryEventEntity> historyEventEntities) {
        Order order = new Order();
        order.setId(orderEntity.getId());
        order.setUserId(orderEntity.getUserId());
        order.setTotalAmount(orderEntity.getTotalAmount());
        order.setDiscountedAmount(orderEntity.getDiscountedAmount());
        order.setDiscountAmount(orderEntity.getDiscountAmount());
        order.setUserCouponId(orderEntity.getUserCouponId());
        order.setStatus(Order.OrderStatus.valueOf(orderEntity.getStatus())); // string을 enum으로 변환
        order.setOrderedAt(orderEntity.getOrderedAt());
        order.setUpdatedAt(orderEntity.getUpdatedAt());
        
        // OrderItem들 변환
        List<OrderItem> orderItems = orderItemEntities.stream()
                .map(this::mapToOrderItem)
                .collect(Collectors.toList());
        order.setOrderItems(orderItems);
        
        // OrderHistoryEvent들 변환
        List<OrderHistoryEvent> historyEvents = historyEventEntities.stream()
                .map(this::mapToOrderHistoryEvent)
                .collect(Collectors.toList());
        order.setHistoryEvents(historyEvents);
        
        return order;
    }

    /**
     * OrderItemEntity를 OrderItem 도메인 객체로 변환
     */
    private OrderItem mapToOrderItem(OrderItemEntity entity) {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(entity.getId());
        orderItem.setOrderId(entity.getOrderId());
        orderItem.setProductId(entity.getProductId());
        orderItem.setProductName(entity.getProductName());
        orderItem.setQuantity(entity.getQuantity());
        orderItem.setUnitPrice(entity.getUnitPrice());
        orderItem.setTotalPrice(entity.getTotalPrice());
        return orderItem;
    }

    /**
     * OrderHistoryEventEntity를 OrderHistoryEvent 도메인 객체로 변환
     */
    private OrderHistoryEvent mapToOrderHistoryEvent(OrderHistoryEventEntity entity) {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setId(entity.getId());
        event.setOrderId(entity.getOrderId());
        event.setEventType(OrderHistoryEvent.OrderEventType.valueOf(entity.getEventType())); // string을 enum으로 변환
        event.setOccurredAt(entity.getOccurredAt());
        event.setCancelReason(entity.getCancelReason());
        event.setRefundAmount(entity.getRefundAmount());
        event.setPaymentMethod(entity.getPaymentMethod());
        event.setTotalAmount(entity.getTotalAmount());
        event.setDiscountAmount(entity.getDiscountAmount());
        event.setDiscountedAmount(entity.getDiscountedAmount());  
        event.setCreatedAt(entity.getCreatedAt());
        return event;
    }
}