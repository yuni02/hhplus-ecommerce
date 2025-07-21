package kr.hhplus.be.server.product.infrastructure;

import kr.hhplus.be.server.product.domain.ProductStats;
import kr.hhplus.be.server.product.domain.ProductStatsRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryProductStatsRepository implements ProductStatsRepository {

    private final Map<Long, ProductStats> productStats = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public InMemoryProductStatsRepository() {
        // 더미 데이터 초기화
        initializeDummyData();
    }

    private void initializeDummyData() {
        // 상품 1 통계
        ProductStats stats1 = new ProductStats(1L, "MacBook Pro 14");
        stats1.setId(idGenerator.getAndIncrement());
        stats1.setRecentSalesCount(15);
        stats1.setRecentSalesAmount(BigDecimal.valueOf(37500000));
        stats1.setTotalSalesCount(45);
        stats1.setTotalSalesAmount(BigDecimal.valueOf(112500000));
        stats1.setConversionRate(BigDecimal.valueOf(3.2));
        stats1.setLastOrderDate(LocalDateTime.now().minusHours(2));
        productStats.put(stats1.getId(), stats1);

        // 상품 2 통계
        ProductStats stats2 = new ProductStats(2L, "iPhone 15 Pro");
        stats2.setId(idGenerator.getAndIncrement());
        stats2.setRecentSalesCount(12);
        stats2.setRecentSalesAmount(BigDecimal.valueOf(18000000));
        stats2.setTotalSalesCount(38);
        stats2.setTotalSalesAmount(BigDecimal.valueOf(57000000));
        stats2.setConversionRate(BigDecimal.valueOf(2.8));
        stats2.setLastOrderDate(LocalDateTime.now().minusHours(1));
        productStats.put(stats2.getId(), stats2);

        // 상품 3 통계
        ProductStats stats3 = new ProductStats(3L, "AirPods Pro");
        stats3.setId(idGenerator.getAndIncrement());
        stats3.setRecentSalesCount(8);
        stats3.setRecentSalesAmount(BigDecimal.valueOf(2800000));
        stats3.setTotalSalesCount(25);
        stats3.setTotalSalesAmount(BigDecimal.valueOf(8750000));
        stats3.setConversionRate(BigDecimal.valueOf(1.5));
        stats3.setLastOrderDate(LocalDateTime.now().minusHours(3));
        productStats.put(stats3.getId(), stats3);

        // 상품 4 통계
        ProductStats stats4 = new ProductStats(4L, "클린 코드");
        stats4.setId(idGenerator.getAndIncrement());
        stats4.setRecentSalesCount(6);
        stats4.setRecentSalesAmount(BigDecimal.valueOf(150000));
        stats4.setTotalSalesCount(18);
        stats4.setTotalSalesAmount(BigDecimal.valueOf(450000));
        stats4.setConversionRate(BigDecimal.valueOf(1.2));
        stats4.setLastOrderDate(LocalDateTime.now().minusHours(4));
        productStats.put(stats4.getId(), stats4);

        // 상품 5 통계
        ProductStats stats5 = new ProductStats(5L, "캐시미어 니트");
        stats5.setId(idGenerator.getAndIncrement());
        stats5.setRecentSalesCount(4);
        stats5.setRecentSalesAmount(BigDecimal.valueOf(356000));
        stats5.setTotalSalesCount(12);
        stats5.setTotalSalesAmount(BigDecimal.valueOf(1068000));
        stats5.setConversionRate(BigDecimal.valueOf(0.8));
        stats5.setLastOrderDate(LocalDateTime.now().minusHours(5));
        productStats.put(stats5.getId(), stats5);
    }

    @Override
    public List<ProductStats> findTopPopularProducts(int limit) {
        return productStats.values().stream()
                .sorted(Comparator.comparing(ProductStats::getRecentSalesCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ProductStats> findByProductId(Long productId) {
        return productStats.values().stream()
                .filter(stats -> stats.getProductId().equals(productId))
                .findFirst();
    }

    @Override
    public ProductStats save(ProductStats productStats) {
        if (productStats.getId() == null) {
            productStats.setId(idGenerator.getAndIncrement());
        }
        this.productStats.put(productStats.getId(), productStats);
        return productStats;
    }

    @Override
    public List<ProductStats> findAll() {
        return new ArrayList<>(productStats.values());
    }
} 
