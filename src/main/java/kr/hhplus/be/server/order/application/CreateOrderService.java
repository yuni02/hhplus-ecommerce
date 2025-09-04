package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.order.application.port.out.LoadProductPort;
import kr.hhplus.be.server.order.application.port.out.UpdateProductStockPort;
import kr.hhplus.be.server.order.application.port.out.DeductBalancePort;
import kr.hhplus.be.server.balance.application.port.out.LoadBalancePort;
import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.order.domain.service.OrderDomainService;
import kr.hhplus.be.server.shared.lock.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.domain.event.OrderCompletedEvent;
import org.springframework.context.ApplicationEventPublisher;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateOrderService implements CreateOrderUseCase {

    private final OrderDomainService orderDomainService;
    private final LoadProductPort loadProductPort;
    private final UpdateProductStockPort updateProductStockPort;
    private final LoadBalancePort loadBalancePort;
    private final DeductBalancePort deductBalancePort;
    private final ApplicationEventPublisher eventPublisher;


    /**
     * 동기식 Saga 패턴의 주문 생성
     * MSA 경계 내에서 동기적으로 각 단계를 처리하여 동시성 제어
     */
    @DistributedLock(
        key = "'order_user_' + #command.userId",
        waitTime = 5,
        leaseTime = 10,
        timeUnit = TimeUnit.SECONDS
    )
    @Transactional
    public CreateOrderUseCase.CreateOrderResult createOrder(CreateOrderUseCase.CreateOrderCommand command) {
        log.debug("동기식 Saga 주문 처리 시작 - userId: {}", command.getUserId());
        
        try {
            // 1. 주문 기본 검증
            OrderDomainService.OrderValidationResult validationResult = orderDomainService.validateOrder(command);
            if (!validationResult.isSuccess()) {
                return CreateOrderUseCase.CreateOrderResult.failure(validationResult.getErrorMessage());
            }

            // 2. 상품 정보 및 재고 확인 + 차감 (비관적 락)
            BigDecimal totalAmount = BigDecimal.ZERO;
            List<CreateOrderUseCase.OrderItemResult> orderItemResults = new ArrayList<>();
            
            for (CreateOrderUseCase.OrderItemCommand itemCommand : command.getOrderItems()) {
                // 상품 정보 조회 (락 포함)
                LoadProductPort.ProductInfo productInfo = loadProductPort.loadProductByIdWithLock(itemCommand.getProductId())
                    .orElse(null);
                
                if (productInfo == null) {
                    return CreateOrderUseCase.CreateOrderResult.failure("존재하지 않는 상품입니다: " + itemCommand.getProductId());
                }
                
                if (!"ACTIVE".equals(productInfo.getStatus())) {
                    return CreateOrderUseCase.CreateOrderResult.failure("판매 중지된 상품입니다: " + productInfo.getName());
                }
                
                if (productInfo.getStock() < itemCommand.getQuantity()) {
                    return CreateOrderUseCase.CreateOrderResult.failure("재고가 부족합니다: " + productInfo.getName() + " (남은 재고: " + productInfo.getStock() + ")");
                }
                
                // 재고 차감 (비관적 락)
                boolean stockDeducted = updateProductStockPort.deductStockWithPessimisticLock(
                    itemCommand.getProductId(), itemCommand.getQuantity()
                );
                
                if (!stockDeducted) {
                    return CreateOrderUseCase.CreateOrderResult.failure("재고 차감에 실패했습니다: " + productInfo.getName());
                }
                
                BigDecimal itemAmount = productInfo.getCurrentPrice().multiply(new BigDecimal(itemCommand.getQuantity()));
                totalAmount = totalAmount.add(itemAmount);
                
                orderItemResults.add(new CreateOrderUseCase.OrderItemResult(
                    null, // id는 나중에 설정
                    itemCommand.getProductId(),
                    productInfo.getName(),
                    itemCommand.getQuantity(),
                    productInfo.getCurrentPrice(),
                    itemAmount
                ));
            }

            // 3. 잔액 확인 + 차감 (비관적 락)
            Balance balance = loadBalancePort.loadActiveBalanceByUserIdWithLock(command.getUserId())
                .orElse(null);
            
            if (balance == null) {
                // 재고 롤백 필요 (간단히 처리)
                rollbackStock(command);
                return CreateOrderUseCase.CreateOrderResult.failure("잔액 정보를 찾을 수 없습니다.");
            }
            
            if (balance.getAmount().compareTo(totalAmount) < 0) {
                // 재고 롤백 필요
                rollbackStock(command);
                return CreateOrderUseCase.CreateOrderResult.failure("잔액이 부족합니다. 현재 잔액: " + balance.getAmount() + ", 주문 금액: " + totalAmount);
            }
            
            // 잔액 차감
            boolean balanceDeducted = deductBalancePort.deductBalanceWithPessimisticLock(
                command.getUserId(), totalAmount
            );
            
            if (!balanceDeducted) {
                // 재고 롤백 필요
                rollbackStock(command);
                return CreateOrderUseCase.CreateOrderResult.failure("잔액 차감에 실패했습니다.");
            }

            // 4. 주문 생성 및 저장
            List<OrderItem> orderItems = orderItemResults.stream()
                .map(item -> OrderItem.builder()
                    .productId(item.getProductId())
                    .productName(item.getProductName())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .build())
                .toList();
            
            OrderDomainService.OrderCreationResult orderCreationResult = orderDomainService.createAndSaveOrder(
                command, orderItems, totalAmount, totalAmount, BigDecimal.ZERO
            );
            
            if (!orderCreationResult.isSuccess()) {
                // 재고 롤백 필요
                rollbackStock(command);
                return CreateOrderUseCase.CreateOrderResult.failure(orderCreationResult.getErrorMessage());
            }
            
            Order savedOrder = orderCreationResult.getOrder();
            
            // 5. 주문 완료 이벤트 발행 (트랜잭션 완료 후 처리)
            publishOrderCompletedEvent(savedOrder, orderItems);
            
            return CreateOrderUseCase.CreateOrderResult.success(
                savedOrder.getId(),
                command.getUserId(),
                command.getUserCouponId(),
                totalAmount,
                totalAmount, // 쿠폰 적용 로직 생략
                BigDecimal.ZERO, // 할인 금액
                totalAmount, // 최종 금액
                "COMPLETED", // 동기 처리 완료
                orderItemResults,
                savedOrder.getOrderedAt()
            );

        } catch (Exception e) {
            log.error("동기식 Saga 주문 처리 중 예외 발생 - userId: {}", command.getUserId(), e);
            return CreateOrderUseCase.CreateOrderResult.failure("주문 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 재고 롤백 (보상 트랜잭션)
     */
    private void rollbackStock(CreateOrderUseCase.CreateOrderCommand command) {
        for (CreateOrderUseCase.OrderItemCommand itemCommand : command.getOrderItems()) {
            try {
                updateProductStockPort.restoreStock(itemCommand.getProductId(), itemCommand.getQuantity());
            } catch (Exception e) {
                log.error("재고 롤백 실패 - productId: {}", itemCommand.getProductId(), e);
            }
        }
    }
    
    /**
     * 주문 완료 이벤트 발행
     */
    private void publishOrderCompletedEvent(Order order, List<OrderItem> orderItems) {
        try {
            OrderCompletedEvent event = new OrderCompletedEvent(
                this,
                order.getId(),
                order.getUserId(),
                orderItems,
                order.getTotalAmount(),
                order.getDiscountedAmount(),
                order.getDiscountAmount(),
                order.getUserCouponId(),
                order.getOrderedAt()
            );
            
            eventPublisher.publishEvent(event);
            log.debug("OrderCompletedEvent 발행 완료 - orderId: {}", order.getId());
            
        } catch (Exception e) {
            log.error("OrderCompletedEvent 발행 실패 - orderId: {}", order.getId(), e);
            // 이벤트 발행 실패는 메인 트랜잭션에 영향 주지 않음
        }
    }





}
