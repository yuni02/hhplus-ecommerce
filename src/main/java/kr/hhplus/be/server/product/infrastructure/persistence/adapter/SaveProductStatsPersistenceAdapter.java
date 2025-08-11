package kr.hhplus.be.server.product.infrastructure.persistence.adapter;

import kr.hhplus.be.server.product.application.port.out.SaveProductStatsPort;
import kr.hhplus.be.server.product.domain.ProductStats;
import kr.hhplus.be.server.product.infrastructure.persistence.entity.ProductStatsEntity;
import kr.hhplus.be.server.product.infrastructure.persistence.entity.ProductStatsId;
import kr.hhplus.be.server.product.infrastructure.persistence.repository.ProductStatsJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 상품 통계 저장 영속성 Adapter
 */
@Component
public class SaveProductStatsPersistenceAdapter implements SaveProductStatsPort {

    private final ProductStatsJpaRepository productStatsJpaRepository;

    public SaveProductStatsPersistenceAdapter(ProductStatsJpaRepository productStatsJpaRepository) {
        this.productStatsJpaRepository = productStatsJpaRepository;
    }

    @Override
    @Transactional
    public ProductStats saveProductStats(ProductStats productStats) {
        ProductStatsEntity entity = mapToProductStatsEntity(productStats);
        ProductStatsEntity savedEntity = productStatsJpaRepository.save(entity);
        return mapToProductStats(savedEntity);
    }

    @Override
    @Transactional
    public List<ProductStats> saveAllProductStats(List<ProductStats> productStatsList) {
        List<ProductStatsEntity> entities = productStatsList.stream()
            .map(this::mapToProductStatsEntity)
            .collect(Collectors.toList());
        
        List<ProductStatsEntity> savedEntities = productStatsJpaRepository.saveAll(entities);
        
        return savedEntities.stream()
            .map(this::mapToProductStats)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteProductStatsByDate(LocalDate date) {
        productStatsJpaRepository.deleteByDate(date);
    }

    private ProductStatsEntity mapToProductStatsEntity(ProductStats productStats) {
        return ProductStatsEntity.builder()
            .id(new ProductStatsId(productStats.getProductId(), productStats.getDate()))
            .totalSales(productStats.getTotalSalesAmount())
            .totalQuantity(productStats.getTotalSalesCount())
            .orderCount(productStats.getRecentSalesCount())
            .build();
    }

    private ProductStats mapToProductStats(ProductStatsEntity entity) {
        return ProductStats.builder()
            .productId(entity.getId().getProductId())
            .date(entity.getId().getDate())
            .totalSalesCount(entity.getTotalQuantity())
            .totalSalesAmount(entity.getTotalSales())
            .recentSalesCount(entity.getOrderCount())
            .recentSalesAmount(entity.getTotalSales()) // 임시로 totalSales 사용
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
} 