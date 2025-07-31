package kr.hhplus.be.server.product.infrastructure.persistence.adapter;

import kr.hhplus.be.server.product.application.port.out.LoadOrderStatsPort;
import kr.hhplus.be.server.order.infrastructure.persistence.repository.OrderItemJpaRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 주문 통계 데이터 조회 영속성 Adapter
 */
@Component
public class LoadOrderStatsPersistenceAdapter implements LoadOrderStatsPort {

    private final OrderItemJpaRepository orderItemJpaRepository;

    public LoadOrderStatsPersistenceAdapter(OrderItemJpaRepository orderItemJpaRepository) {
        this.orderItemJpaRepository = orderItemJpaRepository;
    }

    @Override
    public List<ProductSalesStats> loadRecentProductSalesStats(LocalDateTime startDate, LocalDateTime endDate) {
        return orderItemJpaRepository.findProductSalesStatsByDateRange(startDate, endDate)
            .stream()
            .map(this::mapToProductSalesStats)
            .collect(Collectors.toList());
    }

    private ProductSalesStats mapToProductSalesStats(Object[] result) {
        Long productId = (Long) result[0];
        String productName = (String) result[1];
        Integer totalQuantity = ((Number) result[2]).intValue();
        BigDecimal totalAmount = (BigDecimal) result[3];
        LocalDateTime lastOrderDate = (LocalDateTime) result[4];

        return new ProductSalesStats(productId, productName, totalQuantity, totalAmount, lastOrderDate);
    }
} 