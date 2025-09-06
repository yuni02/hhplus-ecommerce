package kr.hhplus.be.server.order.infrastructure.persistence.adapter;

import kr.hhplus.be.server.order.application.port.out.SaveOrderPort;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.domain.event.OrderHistoryEvent;
import kr.hhplus.be.server.order.infrastructure.persistence.entity.OrderEntity;
import kr.hhplus.be.server.order.infrastructure.persistence.entity.OrderItemEntity;
import kr.hhplus.be.server.order.infrastructure.persistence.entity.OrderHistoryEventEntity;
import kr.hhplus.be.server.order.infrastructure.persistence.repository.OrderJpaRepository;
import kr.hhplus.be.server.order.infrastructure.persistence.repository.OrderItemJpaRepository;
import kr.hhplus.be.server.order.infrastructure.persistence.repository.OrderHistoryEventJpaRepository;
import kr.hhplus.be.server.user.infrastructure.persistence.repository.UserJpaRepository;
import kr.hhplus.be.server.user.infrastructure.persistence.entity.UserEntity;
import kr.hhplus.be.server.product.infrastructure.persistence.repository.ProductJpaRepository;
import kr.hhplus.be.server.product.infrastructure.persistence.entity.ProductEntity;

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
    private final UserJpaRepository userJpaRepository;
    private final ProductJpaRepository productJpaRepository;

    public OrderPersistenceAdapter(OrderJpaRepository orderJpaRepository,
                                  OrderItemJpaRepository orderItemJpaRepository,
                                  OrderHistoryEventJpaRepository orderHistoryEventJpaRepository,
                                  UserJpaRepository userJpaRepository,
                                  ProductJpaRepository productJpaRepository) {
        this.orderJpaRepository = orderJpaRepository;
        this.orderItemJpaRepository = orderItemJpaRepository;
        this.orderHistoryEventJpaRepository = orderHistoryEventJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.productJpaRepository = productJpaRepository;
    }

    @Override
    @Transactional
    public Order saveOrder(Order order) {
        // 1. Order 도메인 객체를 OrderEntity로 변환
        OrderEntity orderEntity = mapToOrderEntity(order);
        
        // 2. Order 저장 및 flush로 즉시 DB에 반영하여 ID 생성
        OrderEntity savedOrderEntity = orderJpaRepository.saveAndFlush(orderEntity);
        
        // 3. ID가 제대로 생성되었는지 확인
        if (savedOrderEntity.getId() == null) {
            throw new RuntimeException("Order 저장 후 ID가 생성되지 않았습니다.");
        }
        
        // 4. OrderItem들 저장
        List<OrderItemEntity> orderItemEntities = order.getOrderItems().stream()
                .map(item -> mapToOrderItemEntity(item, savedOrderEntity.getId()))
                .toList();
        if (!orderItemEntities.isEmpty()) {
            orderItemJpaRepository.saveAll(orderItemEntities);
        }
        
        // 5. OrderHistoryEvent들 저장
        List<OrderHistoryEventEntity> historyEventEntities = order.getHistoryEvents().stream()
                .map(event -> mapToOrderHistoryEventEntity(event, savedOrderEntity.getId()))
                .toList();
        if (!historyEventEntities.isEmpty()) {
            orderHistoryEventJpaRepository.saveAll(historyEventEntities);
        }
        
        // 6. 저장된 OrderEntity를 다시 Order 도메인 객체로 변환하여 반환
        return mapToOrder(savedOrderEntity, orderItemEntities, historyEventEntities);
    }

    /**
     * Order 도메인 객체를 OrderEntity로 변환
     */
    private OrderEntity mapToOrderEntity(Order order) {
        // UserEntity 조회 (ACTIVE 상태인 사용자)
        UserEntity userEntity = userJpaRepository.findByUserIdAndStatus(order.getUserId(), "ACTIVE")
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + order.getUserId()));
        
        OrderEntity entity = OrderEntity.builder()
            .user(userEntity)  // user 관계 설정 (이를 통해 user_id 컬럼이 채워짐)
            .totalAmount(order.getTotalAmount())
            .discountedAmount(order.getDiscountedAmount())
            .discountAmount(order.getDiscountAmount())
            .userCouponId(order.getUserCouponId())
            .status(order.getStatus().name())
            .orderedAt(order.getOrderedAt())
            .build();
        return entity;
    }

    /**
     * OrderItem 도메인 객체를 OrderItemEntity로 변환
     */
    private OrderItemEntity mapToOrderItemEntity(OrderItem orderItem, Long orderId) {
        // OrderEntity 조회 (order_id를 통해)
        OrderEntity orderEntity = orderJpaRepository.findById(orderId).orElse(null);
        
        // ProductEntity 조회 (product_id를 통해)
        ProductEntity productEntity = null;
        if (orderItem.getProductId() != null) {
            productEntity = productJpaRepository.findById(orderItem.getProductId()).orElse(null);
        }
        
        OrderItemEntity entity = OrderItemEntity.builder()
                .order(orderEntity)  // order 관계 설정
                .product(productEntity)  // product 관계 설정
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
            .orderId(orderId)
            .eventType(event.getEventType().name())
            .occurredAt(event.getOccurredAt())
            .build();
        return entity;
    }

    /**
     * OrderEntity를 Order 도메인 객체로 변환
     */
    private Order mapToOrder(OrderEntity orderEntity, List<OrderItemEntity> orderItemEntities, 
                           List<OrderHistoryEventEntity> historyEventEntities) {
        // OrderItem들 변환
        List<OrderItem> orderItems = orderItemEntities.stream()
                .map(this::mapToOrderItem)
                .toList();
        
        // OrderHistoryEvent들 변환
        List<OrderHistoryEvent> historyEvents = historyEventEntities.stream()
                .map(this::mapToOrderHistoryEvent)
                .toList();
        
        return Order.builder()
                .id(orderEntity.getId())
                .userId(orderEntity.getUser() != null ? orderEntity.getUser().getUserId() : null)
                .totalAmount(orderEntity.getTotalAmount())
                .discountedAmount(orderEntity.getDiscountedAmount())
                .discountAmount(orderEntity.getDiscountAmount())
                .userCouponId(orderEntity.getUserCouponId())
                .status(Order.OrderStatus.valueOf(orderEntity.getStatus()))
                .orderedAt(orderEntity.getOrderedAt())
                .updatedAt(orderEntity.getUpdatedAt())
                .orderItems(orderItems)
                .historyEvents(historyEvents)
                .build();
    }

    /**
     * OrderItemEntity를 OrderItem 도메인 객체로 변환
     */
    private OrderItem mapToOrderItem(OrderItemEntity entity) {
        return OrderItem.builder()
                .id(entity.getId())
                .orderId(entity.getOrder().getId())
                .productId(entity.getProduct().getId())
                .productName(entity.getProductName())
                .quantity(entity.getQuantity())
                .unitPrice(entity.getUnitPrice())
                .totalPrice(entity.getTotalPrice())
                .build();
    }

    /**
     * OrderHistoryEventEntity를 OrderHistoryEvent 도메인 객체로 변환
     */
    private OrderHistoryEvent mapToOrderHistoryEvent(OrderHistoryEventEntity entity) {
        return OrderHistoryEvent.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .eventType(OrderHistoryEvent.OrderEventType.valueOf(entity.getEventType()))
                .occurredAt(entity.getOccurredAt())
                .build();
    }
}