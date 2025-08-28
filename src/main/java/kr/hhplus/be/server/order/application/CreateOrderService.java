package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.domain.service.OrderDomainService;
import kr.hhplus.be.server.product.domain.service.ProductDomainService;
import kr.hhplus.be.server.coupon.domain.service.CouponDomainService;
import kr.hhplus.be.server.balance.domain.service.BalanceDomainService;
import kr.hhplus.be.server.shared.lock.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateOrderService implements CreateOrderUseCase {

    private final OrderDomainService orderDomainService;
    private final ProductDomainService productDomainService;
    private final CouponDomainService couponDomainService;
    private final BalanceDomainService balanceDomainService;

    /**
     * 세밀한 분산락으로 주문 생성
     * 사용자 + 상품 + 잔액 조합으로 락을 사용하여 최대한 병렬 처리 허용
     */
    @DistributedLock(
        key = "'order_' + #command.userId",
        waitTime = 10,
        leaseTime = 30,
        timeUnit = TimeUnit.SECONDS
    )
    public CreateOrderUseCase.CreateOrderResult createOrder(CreateOrderUseCase.CreateOrderCommand command) {
        log.debug("분산 트랜잭션 주문 생성 시작 - userId: {}", command.getUserId());
        
        try {
            // 1. 주문 검증
            OrderDomainService.OrderValidationResult validationResult = orderDomainService.validateOrder(command);
            if (!validationResult.isSuccess()) {
                return CreateOrderUseCase.CreateOrderResult.failure(validationResult.getErrorMessage());
            }

            // 2. 재고 확인 및 차감
            ProductDomainService.StockProcessResult stockResult = productDomainService.processStockDeduction(command);
            if (!stockResult.isSuccess()) {
                return CreateOrderUseCase.CreateOrderResult.failure(stockResult.getErrorMessage());
            }

            // 3. 쿠폰 할인 적용
            CouponDomainService.CouponProcessResult couponResult = couponDomainService.processCouponDiscount(command, stockResult.getTotalAmount());
            if (!couponResult.isSuccess()) {
                // 재고 복원 (보상 트랜잭션)
                productDomainService.rollbackStockDeduction(stockResult.getOrderItems(), "쿠폰 사용 실패");
                return CreateOrderUseCase.CreateOrderResult.failure(couponResult.getErrorMessage());
            }

            // 4. 잔액 차감 - 결제로 정의
            BalanceDomainService.BalanceProcessResult balanceResult = balanceDomainService.processBalanceDeduction(
                command.getUserId(), couponResult.getDiscountedAmount());
            if (!balanceResult.isSuccess()) {
                // 쿠폰 및 재고 복원 (보상 트랜잭션)
                couponDomainService.rollbackCouponUsage(command, "잔액 차감 실패");
                productDomainService.rollbackStockDeduction(stockResult.getOrderItems(), "잔액 차감 실패");
                return CreateOrderUseCase.CreateOrderResult.failure(balanceResult.getErrorMessage());
            }

            // 5. 주문 생성 및 저장
            OrderDomainService.OrderCreationResult orderResult = orderDomainService.createAndSaveOrder(
                command, stockResult.getOrderItems(), stockResult.getTotalAmount(), 
                couponResult.getDiscountedAmount(), BigDecimal.valueOf(couponResult.getDiscountAmount()));
            
            if (!orderResult.isSuccess()) {
                // 모든 보상 트랜잭션 실행
                couponDomainService.rollbackCouponUsage(command, "주문 저장 실패");
                productDomainService.rollbackStockDeduction(stockResult.getOrderItems(), "주문 저장 실패");
                return CreateOrderUseCase.CreateOrderResult.failure(orderResult.getErrorMessage());
            }
            
            Order order = orderResult.getOrder();

            // 6. 성공 결과 반환
            CreateOrderUseCase.CreateOrderResult result = createOrderResult(order, stockResult.getOrderItems());
            log.debug("분산 트랜잭션 주문 생성 완료 - userId: {}, orderId: {}", command.getUserId(), order.getId());
            
            return result;

        } catch (Exception e) {
            log.error("분산 트랜잭션 주문 생성 중 예외 발생 - userId: {}", command.getUserId(), e);
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
