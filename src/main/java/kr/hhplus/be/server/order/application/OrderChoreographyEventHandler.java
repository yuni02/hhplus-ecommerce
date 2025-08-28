package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.order.domain.*;
import kr.hhplus.be.server.product.domain.service.ProductDomainService;
import kr.hhplus.be.server.coupon.domain.service.CouponDomainService;
import kr.hhplus.be.server.balance.domain.service.BalanceDomainService;
import kr.hhplus.be.server.order.domain.service.OrderDomainService;
import kr.hhplus.be.server.shared.event.AsyncEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 코레오그래피 방식의 주문 처리 이벤트 핸들러
 * 각 이벤트에 반응하여 다음 단계를 진행하는 방식
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderChoreographyEventHandler {

    private final ProductDomainService productDomainService;
    private final CouponDomainService couponDomainService;
    private final BalanceDomainService balanceDomainService;
    private final OrderDomainService orderDomainService;
    private final AsyncEventPublisher eventPublisher;

    /**
     * 1단계: 주문 처리 시작 → 재고 차감 요청
     */
    @Async
    @EventListener
    public void handleOrderProcessingStarted(OrderProcessingStartedEvent event) {
        log.debug("주문 처리 시작 이벤트 수신 - userId: {}", event.getCommand().getUserId());
        
        try {
            // 재고 차감 처리
            ProductDomainService.StockProcessResult stockResult = 
                productDomainService.processStockDeduction(event.getCommand());
            
            if (stockResult.isSuccess()) {
                // 재고 차감 성공 이벤트 발행
                eventPublisher.publishAsync(new StockProcessedEvent(
                    this, event.getCommand(), stockResult.getOrderItems(), stockResult.getTotalAmount()
                ));
            } else {
                // 재고 차감 실패 이벤트 발행
                eventPublisher.publishAsync(new OrderProcessingFailedEvent(
                    this, event.getCommand(), "재고 부족: " + stockResult.getErrorMessage()
                ));
            }
        } catch (Exception e) {
            log.error("재고 차감 처리 중 오류 발생", e);
            eventPublisher.publishAsync(new OrderProcessingFailedEvent(
                this, event.getCommand(), "재고 처리 오류: " + e.getMessage()
            ));
        }
    }

    /**
     * 2단계: 재고 차감 완료 → 쿠폰 사용 처리
     */
    @Async
    @EventListener
    public void handleStockProcessed(StockProcessedEvent event) {
        log.debug("재고 처리 완료 이벤트 수신 - userId: {}", event.getCommand().getUserId());
        
        try {
            // 쿠폰 할인 처리
            CouponDomainService.CouponProcessResult couponResult = 
                couponDomainService.processCouponDiscount(event.getCommand(), event.getTotalAmount());
            
            if (couponResult.isSuccess()) {
                // 쿠폰 사용 성공 이벤트 발행
                eventPublisher.publishAsync(new CouponProcessedEvent(
                    this, event.getCommand(), event.getOrderItems(), 
                    couponResult.getDiscountedAmount(), couponResult.getDiscountAmount()
                ));
            } else {
                // 쿠폰 사용 실패 → 재고 복원 요청
                for (OrderItem item : event.getOrderItems()) {
                    eventPublisher.publishAsync(new StockRestorationRequestedEvent(
                        this, UUID.randomUUID().toString(), item.getProductId(), item.getQuantity(), "쿠폰 사용 실패"
                    ));
                }
                eventPublisher.publishAsync(new OrderProcessingFailedEvent(
                    this, event.getCommand(), "쿠폰 사용 실패: " + couponResult.getErrorMessage()
                ));
            }
        } catch (Exception e) {
            log.error("쿠폰 처리 중 오류 발생", e);
            for (OrderItem item : event.getOrderItems()) {
                eventPublisher.publishAsync(new StockRestorationRequestedEvent(
                    this, UUID.randomUUID().toString(), item.getProductId(), item.getQuantity(), "쿠폰 처리 오류"
                ));
            }
            eventPublisher.publishAsync(new OrderProcessingFailedEvent(
                this, event.getCommand(), "쿠폰 처리 오류: " + e.getMessage()
            ));
        }
    }

    /**
     * 3단계: 쿠폰 사용 완료 → 잔액 차감 처리
     */
    @Async
    @EventListener
    public void handleCouponProcessed(CouponProcessedEvent event) {
        log.debug("쿠폰 처리 완료 이벤트 수신 - userId: {}", event.getCommand().getUserId());
        
        try {
            // 잔액 차감 처리
            BalanceDomainService.BalanceProcessResult balanceResult = 
                balanceDomainService.processBalanceDeduction(
                    event.getCommand().getUserId(), event.getDiscountedAmount()
                );
            
            if (balanceResult.isSuccess()) {
                // 잔액 차감 성공 이벤트 발행
                eventPublisher.publishAsync(new BalanceProcessedEvent(
                    this, event.getCommand(), event.getOrderItems(), 
                    event.getDiscountedAmount(), BigDecimal.valueOf(event.getDiscountAmount())
                ));
            } else {
                // 잔액 차감 실패 → 쿠폰 및 재고 복원
                if (event.getCommand().getUserCouponId() != null) {
                    eventPublisher.publishAsync(new CouponRestorationRequestedEvent(
                        this, UUID.randomUUID().toString(), event.getCommand().getUserId(), 
                        event.getCommand().getUserCouponId(), "잔액 차감 실패"
                    ));
                }
                for (OrderItem item : event.getOrderItems()) {
                    eventPublisher.publishAsync(new StockRestorationRequestedEvent(
                        this, UUID.randomUUID().toString(), item.getProductId(), item.getQuantity(), "잔액 차감 실패"
                    ));
                }
                eventPublisher.publishAsync(new OrderProcessingFailedEvent(
                    this, event.getCommand(), "잔액 부족: " + balanceResult.getErrorMessage()
                ));
            }
        } catch (Exception e) {
            log.error("잔액 처리 중 오류 발생", e);
            if (event.getCommand().getUserCouponId() != null) {
                eventPublisher.publishAsync(new CouponRestorationRequestedEvent(
                    this, UUID.randomUUID().toString(), event.getCommand().getUserId(), 
                    event.getCommand().getUserCouponId(), "잔액 처리 오류"
                ));
            }
            for (OrderItem item : event.getOrderItems()) {
                eventPublisher.publishAsync(new StockRestorationRequestedEvent(
                    this, UUID.randomUUID().toString(), item.getProductId(), item.getQuantity(), "잔액 처리 오류"
                ));
            }
            eventPublisher.publishAsync(new OrderProcessingFailedEvent(
                this, event.getCommand(), "잔액 처리 오류: " + e.getMessage()
            ));
        }
    }

    /**
     * 4단계: 잔액 차감 완료 → 주문 생성 및 저장
     */
    @Async
    @EventListener
    public void handleBalanceProcessed(BalanceProcessedEvent event) {
        log.debug("잔액 처리 완료 이벤트 수신 - userId: {}", event.getCommand().getUserId());
        
        try {
            // 주문 생성 및 저장
            OrderDomainService.OrderCreationResult orderResult = 
                orderDomainService.createAndSaveOrder(
                    event.getCommand(), event.getOrderItems(), 
                    event.getOrderItems().stream()
                        .map(item -> item.getTotalPrice())
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                    event.getDiscountedAmount(), event.getDiscountAmount()
                );
            
            if (orderResult.isSuccess()) {
                // 주문 완료 이벤트 발행
                Order order = orderResult.getOrder();
                eventPublisher.publishAsync(new OrderCompletedEvent(
                    this, order.getId(), order.getUserId(), event.getOrderItems(),
                    event.getOrderItems().stream().map(OrderItem::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add),
                    event.getDiscountedAmount(), event.getDiscountAmount(),
                    order.getUserCouponId(), order.getOrderedAt()
                ));
            } else {
                // 주문 생성 실패 → 모든 복원 작업
                if (event.getCommand().getUserCouponId() != null) {
                    eventPublisher.publishAsync(new CouponRestorationRequestedEvent(
                        this, UUID.randomUUID().toString(), event.getCommand().getUserId(), 
                        event.getCommand().getUserCouponId(), "주문 저장 실패"
                    ));
                }
                for (OrderItem item : event.getOrderItems()) {
                    eventPublisher.publishAsync(new StockRestorationRequestedEvent(
                        this, UUID.randomUUID().toString(), item.getProductId(), item.getQuantity(), "주문 저장 실패"
                    ));
                }
                // 잔액 복원은 별도 처리 필요
                eventPublisher.publishAsync(new OrderProcessingFailedEvent(
                    this, event.getCommand(), "주문 저장 실패: " + orderResult.getErrorMessage()
                ));
            }
        } catch (Exception e) {
            log.error("주문 저장 중 오류 발생", e);
            if (event.getCommand().getUserCouponId() != null) {
                eventPublisher.publishAsync(new CouponRestorationRequestedEvent(
                    this, UUID.randomUUID().toString(), event.getCommand().getUserId(), 
                    event.getCommand().getUserCouponId(), "주문 저장 오류"
                ));
            }
            for (OrderItem item : event.getOrderItems()) {
                eventPublisher.publishAsync(new StockRestorationRequestedEvent(
                    this, UUID.randomUUID().toString(), item.getProductId(), item.getQuantity(), "주문 저장 오류"
                ));
            }
            eventPublisher.publishAsync(new OrderProcessingFailedEvent(
                this, event.getCommand(), "주문 저장 오류: " + e.getMessage()
            ));
        }
    }

    /**
     * 실패 처리: 주문 처리 실패 이벤트 처리
     */
    @Async
    @EventListener
    public void handleOrderProcessingFailed(OrderProcessingFailedEvent event) {
        log.error("주문 처리 실패 - userId: {}, 사유: {}", 
            event.getCommand().getUserId(), event.getErrorMessage());
        
        // 여기서 실패 알림, 로그 기록 등 추가 처리 가능
    }
}