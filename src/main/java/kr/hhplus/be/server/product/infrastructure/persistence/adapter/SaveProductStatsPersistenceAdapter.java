package kr.hhplus.be.server.product.infrastructure.persistence.adapter;

import kr.hhplus.be.server.product.application.port.out.SaveProductStatsPort;
import kr.hhplus.be.server.product.domain.ProductStats;
import kr.hhplus.be.server.product.infrastructure.persistence.entity.ProductStatsEntity;
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
            .productId(productStats.getProductId())
            .date(productStats.getDate())
            .recentSalesCount(productStats.getRecentSalesCount())
            .recentSalesAmount(productStats.getRecentSalesAmount())
            .totalSalesCount(productStats.getTotalSalesCount())
            .totalSalesAmount(productStats.getTotalSalesAmount())
            .rank(productStats.getRank())
            .conversionRate(productStats.getConversionRate())
            .lastOrderDate(productStats.getLastOrderDate())
            .aggregationDate(productStats.getAggregationDate())
            .createdAt(productStats.getCreatedAt())
            .updatedAt(productStats.getUpdatedAt())
            .build();
    }

    private ProductStats mapToProductStats(ProductStatsEntity entity) {
        return ProductStats.builder()
            .productId(entity.getProductId())
            .date(entity.getDate())
            .recentSalesCount(entity.getRecentSalesCount())
            .recentSalesAmount(entity.getRecentSalesAmount())
            .totalSalesCount(entity.getTotalSalesCount())
            .totalSalesAmount(entity.getTotalSalesAmount())
            .rank(entity.getRank())
            .conversionRate(entity.getConversionRate())
            .lastOrderDate(entity.getLastOrderDate())
            .aggregationDate(entity.getAggregationDate())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
} 