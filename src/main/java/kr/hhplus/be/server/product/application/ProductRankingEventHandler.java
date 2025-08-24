package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.product.application.port.in.ProductRankingUseCase;
import kr.hhplus.be.server.product.domain.ProductRankingUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductRankingEventHandler {

    private final ProductRankingUseCase productRankingUseCase;

    @Async("productRankingExecutor")
    @EventListener
    public void handleProductRankingUpdate(ProductRankingUpdateEvent event) {
        try {
            log.debug("상품 랭킹 업데이트 이벤트 처리 시작 - productId: {}, quantity: {}", 
                     event.getProductId(), event.getQuantity());
            
            productRankingUseCase.updateProductRanking(event.getProductId(), event.getQuantity());
            
            log.debug("상품 랭킹 업데이트 이벤트 처리 완료 - productId: {}", event.getProductId());
            
        } catch (Exception e) {
            log.warn("상품 랭킹 업데이트 실패 - productId: {}, quantity: {}", 
                    event.getProductId(), event.getQuantity(), e);
        }
    }
}