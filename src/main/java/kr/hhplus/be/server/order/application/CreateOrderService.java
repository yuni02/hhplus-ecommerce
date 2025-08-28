package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.order.domain.*;
import kr.hhplus.be.server.order.domain.service.OrderDomainService;
import kr.hhplus.be.server.shared.event.AsyncEventPublisher;
import kr.hhplus.be.server.shared.lock.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateOrderService implements CreateOrderUseCase {

    private final OrderDomainService orderDomainService;
    private final AsyncEventPublisher eventPublisher;
    
    // 주문 처리 상태를 추적하는 임시 저장소 (실제로는 Redis나 DB를 사용)
    private final Map<String, OrderProcessingState> orderProcessingStates = new ConcurrentHashMap<>();

    /**
     * 코레오그래피 방식의 주문 생성
     * 이벤트를 발행하고 각 도메인이 독립적으로 처리
     */
    @DistributedLock(
        key = "'order_' + #command.userId",
        waitTime = 10,
        leaseTime = 30,
        timeUnit = TimeUnit.SECONDS
    )
    public CreateOrderUseCase.CreateOrderResult createOrder(CreateOrderUseCase.CreateOrderCommand command) {
        log.debug("코레오그래피 주문 처리 시작 - userId: {}", command.getUserId());
        
        try {
            // 1. 주문 검증만 동기적으로 수행
            OrderDomainService.OrderValidationResult validationResult = orderDomainService.validateOrder(command);
            if (!validationResult.isSuccess()) {
                return CreateOrderUseCase.CreateOrderResult.failure(validationResult.getErrorMessage());
            }

            // 2. 주문 처리 상태 초기화
            Long orderId = System.currentTimeMillis(); // 간단한 orderId 생성
            String orderIdStr = orderId.toString();
            OrderProcessingState state = new OrderProcessingState(orderIdStr, command);
            orderProcessingStates.put(orderIdStr, state);
            
            // 3. 주문 처리 시작 이벤트 발행 - 이후 모든 처리는 이벤트 기반으로 진행
            eventPublisher.publishAsync(new OrderProcessingStartedEvent(this, command));
            
            // 4. 비동기 처리 결과를 기다리거나 별도 조회 API로 상태 확인하도록 안내
            return CreateOrderUseCase.CreateOrderResult.success(
                orderId, // 생성된 orderId
                command.getUserId(),
                command.getUserCouponId(),
                null, // totalAmount는 이후 계산
                null, // discountedAmount는 이후 계산
                null, // discountAmount는 이후 계산
                null, // finalAmount는 이후 계산
                "PROCESSING", // 처리 중 상태
                null, // orderItems는 이후 설정
                null  // orderedAt은 이후 설정
            );

        } catch (Exception e) {
            log.error("코레오그래피 주문 처리 중 예외 발생 - userId: {}", command.getUserId(), e);
            return CreateOrderUseCase.CreateOrderResult.failure("주문 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }


    /**
     * 주문 결과 생성
     */
    private CreateOrderUseCase.CreateOrderResult createOrderResult(Order order, List<OrderItem> orderItems) {
        List<CreateOrderUseCase.OrderItemResult> orderItemResults = new ArrayList<>();
        for (OrderItem item : orderItems) {
            orderItemResults.add(new CreateOrderUseCase.OrderItemResult(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice()
            ));
        }

        return CreateOrderUseCase.CreateOrderResult.success(
            order.getId(),
            order.getUserId(),
            order.getUserCouponId(),
            order.getTotalAmount(),
            order.getDiscountedAmount(),
            order.getDiscountAmount(),
            order.getDiscountedAmount(), // finalAmount는 discountedAmount와 동일
            order.getStatus().name(),
            orderItemResults,
            order.getOrderedAt()
        );
    }


}
