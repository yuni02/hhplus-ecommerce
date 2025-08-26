package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.order.domain.StockDeductionRequestedEvent;
import kr.hhplus.be.server.order.domain.StockDeductionCompletedEvent;
import kr.hhplus.be.server.order.domain.StockRestorationRequestedEvent;
import kr.hhplus.be.server.order.domain.StockRestorationCompletedEvent;
import kr.hhplus.be.server.order.application.port.out.LoadProductPort;
import kr.hhplus.be.server.order.application.port.out.UpdateProductStockPort;
import kr.hhplus.be.server.shared.event.SynchronousEventProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 이벤트 핸들러
 * 재고 차감 이벤트를 별도 트랜잭션에서 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventHandler {
    
    private final LoadProductPort loadProductPort;
    private final UpdateProductStockPort updateProductStockPort;
    private final SynchronousEventProcessor synchronousEventProcessor;
    
    /**
     * 재고 차감 요청 이벤트 처리
     * 별도 트랜잭션에서 실행
     */
    @EventListener
    @Transactional
    public void handleStockDeductionRequested(StockDeductionRequestedEvent event) {
        log.debug("재고 차감 요청 이벤트 처리 - requestId: {}, productId: {}, quantity: {}", 
                 event.getRequestId(), event.getProductId(), event.getQuantity());
        
        StockDeductionCompletedEvent responseEvent;
        
        try {
            // 상품 정보 조회
            LoadProductPort.ProductInfo productInfo = loadProductPort.loadProductById(event.getProductId())
                .orElse(null);
            
            if (productInfo == null) {
                responseEvent = StockDeductionCompletedEvent.failure(
                    this, event.getRequestId(), event.getProductId(), event.getQuantity(),
                    "상품을 찾을 수 없습니다: " + event.getProductId()
                );
            } else if (productInfo.getStock() < event.getQuantity()) {
                responseEvent = StockDeductionCompletedEvent.failure(
                    this, event.getRequestId(), event.getProductId(), event.getQuantity(),
                    "재고가 부족합니다: " + productInfo.getName()
                );
            } else {
                // 비관적 락으로 재고 차감
                boolean success = updateProductStockPort.deductStockWithPessimisticLock(
                    event.getProductId(), event.getQuantity()
                );
                
                if (success) {
                    responseEvent = StockDeductionCompletedEvent.success(
                        this, event.getRequestId(), event.getProductId(), event.getQuantity(),
                        productInfo.getName(), productInfo.getCurrentPrice()
                    );
                    
                    log.debug("재고 차감 성공 - requestId: {}, productId: {}", 
                             event.getRequestId(), event.getProductId());
                } else {
                    responseEvent = StockDeductionCompletedEvent.failure(
                        this, event.getRequestId(), event.getProductId(), event.getQuantity(),
                        "재고 차감에 실패했습니다: " + productInfo.getName()
                    );
                }
            }
            
        } catch (Exception e) {
            log.error("재고 차감 처리 중 예외 발생 - requestId: {}", event.getRequestId(), e);
            
            responseEvent = StockDeductionCompletedEvent.failure(
                this, event.getRequestId(), event.getProductId(), event.getQuantity(),
                "재고 처리 중 예외가 발생했습니다: " + e.getMessage()
            );
        }
        
        // 응답 이벤트를 동기 처리기에 전달
        synchronousEventProcessor.handleResponse(event.getRequestId(), responseEvent);
    }
    
    /**
     * 재고 복원 요청 이벤트 처리
     * 보상 트랜잭션 처리
     */
    @EventListener
    @Transactional
    public void handleStockRestoration(StockRestorationRequestedEvent event) {
        log.debug("재고 복원 요청 이벤트 처리 - requestId: {}, productId: {}, quantity: {}, reason: {}", 
                 event.getRequestId(), event.getProductId(), event.getQuantity(), event.getReason());
        
        StockRestorationCompletedEvent responseEvent;
        
        try {
            // 재고 복원 (반대 연산: 재고 증가)
            boolean success = updateProductStockPort.restoreStock(event.getProductId(), event.getQuantity());
            
            if (success) {
                responseEvent = StockRestorationCompletedEvent.success(
                    this, event.getRequestId(), event.getProductId(), event.getQuantity()
                );
                
                log.info("재고 복원 성공 - requestId: {}, productId: {}, quantity: {}", 
                         event.getRequestId(), event.getProductId(), event.getQuantity());
            } else {
                responseEvent = StockRestorationCompletedEvent.failure(
                    this, event.getRequestId(), event.getProductId(), event.getQuantity(),
                    "재고 복원에 실패했습니다"
                );
                
                log.warn("재고 복원 실패 - requestId: {}, productId: {}", 
                         event.getRequestId(), event.getProductId());
            }
            
        } catch (Exception e) {
            log.error("재고 복원 처리 중 예외 발생 - requestId: {}", event.getRequestId(), e);
            
            responseEvent = StockRestorationCompletedEvent.failure(
                this, event.getRequestId(), event.getProductId(), event.getQuantity(),
                "재고 복원 처리 중 예외가 발생했습니다: " + e.getMessage()
            );
        }
        
        // 응답 이벤트를 동기 처리기에 전달
        synchronousEventProcessor.handleResponse(event.getRequestId(), responseEvent);
    }
}