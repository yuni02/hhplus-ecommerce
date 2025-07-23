package kr.hhplus.be.server.product.adapter.out.persistence;

import kr.hhplus.be.server.product.application.port.out.LoadProductPort;
import kr.hhplus.be.server.product.application.port.out.LoadProductStatsPort;
import org.springframework.stereotype.Component;
        
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 상품 영속성 Adapter (Outgoing)
 */
@Component
public class ProductPersistenceAdapter implements LoadProductPort, LoadProductStatsPort {

    private final Map<Long, ProductData> products = new ConcurrentHashMap<>();
    private final Map<Long, ProductStatsData> productStats = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public ProductPersistenceAdapter() {
        // 더미 데이터 초기화
        initializeDummyData();
    }

    private void initializeDummyData() {
        // 상품 데이터 초기화
        for (long productId = 1; productId <= 5; productId++) {
            ProductData product = new ProductData(
                    productId,
                    "상품 " + productId,
                    "상품 " + productId + " 설명",
                    Integer.valueOf(10000 + (int)productId * 1000),
                    100,
                    "ACTIVE",
                    "전자제품"
            );
            products.put(productId, product);

            // 상품 통계 데이터 초기화
            ProductStatsData stats = new ProductStatsData(
                    productId,
                    "상품 " + productId,
                    Integer.valueOf(10 + (int)productId * 5), // 최근 판매량
                    Long.valueOf(100000L + productId * 10000L), // 최근 판매액
                    Integer.valueOf(50 + (int)productId * 10), // 전체 판매량
                    Long.valueOf(500000L + productId * 50000L), // 전체 판매액
                    (int) productId, // 순위
                    Double.valueOf(0.15 + productId * 0.02) // 전환율
            );
            productStats.put(productId, stats);
        }
    }

    @Override
    public Optional<LoadProductPort.ProductInfo> loadProductById(Long productId) {
        ProductData product = products.get(productId);
        if (product == null) {
            return Optional.empty();
        }
        
        return Optional.of(new LoadProductPort.ProductInfo(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCurrentPrice(),
                product.getStock(),
                product.getStatus(),
                product.getCategory()
        ));
    }

    @Override
    public List<LoadProductPort.ProductInfo> loadAllActiveProducts() {
        return products.values().stream()
                .filter(product -> "ACTIVE".equals(product.getStatus()))
                .map(product -> new LoadProductPort.ProductInfo(
                        product.getId(),
                        product.getName(),
                        product.getDescription(),
                        product.getCurrentPrice(),
                        product.getStock(),
                        product.getStatus(),
                        product.getCategory()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<LoadProductStatsPort.ProductStatsInfo> loadAllProductStats() {
        return productStats.values().stream()
                .map(stats -> new LoadProductStatsPort.ProductStatsInfo(
                        stats.getProductId(),
                        stats.getProductName(),
                        stats.getRecentSalesCount(),
                        stats.getRecentSalesAmount(),
                        stats.getTotalSalesCount(),
                        stats.getTotalSalesAmount(),
                        stats.getRank(),
                        stats.getConversionRate()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<LoadProductStatsPort.ProductStatsInfo> loadProductStatsByProductId(Long productId) {
        ProductStatsData stats = productStats.get(productId);
        if (stats == null) {
            return Optional.empty();
        }
        
        return Optional.of(new LoadProductStatsPort.ProductStatsInfo(
                stats.getProductId(),
                stats.getProductName(),
                stats.getRecentSalesCount(),
                stats.getRecentSalesAmount(),
                stats.getTotalSalesCount(),
                stats.getTotalSalesAmount(),
                stats.getRank(),
                stats.getConversionRate()
        ));
    }

    @Override
    public List<LoadProductStatsPort.ProductStatsInfo> loadTopProductsBySales(int limit) {
        return productStats.values().stream()
                .sorted(java.util.Comparator.comparing(ProductStatsData::getTotalSalesAmount).reversed())
                .limit(limit)
                .map(stats -> new LoadProductStatsPort.ProductStatsInfo(
                        stats.getProductId(),
                        stats.getProductName(),
                        stats.getRecentSalesCount(),    
                        stats.getRecentSalesAmount(),
                        stats.getTotalSalesCount(),
                        stats.getTotalSalesAmount(),
                        stats.getRank(),
                        stats.getConversionRate()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 상품 데이터 내부 클래스
     */
    private static class ProductData {
        private Long id;
        private String name;
        private String description;
        private Integer currentPrice;
        private Integer stock;
        private String status;
        private String category;

        public ProductData(Long id, String name, String description, Integer currentPrice,
                          Integer stock, String status, String category) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.currentPrice = currentPrice;
            this.stock = stock;
            this.status = status;
            this.category = category;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getCurrentPrice() { return currentPrice; }
        public void setCurrentPrice(Integer currentPrice) { this.currentPrice = currentPrice; }
        public Integer getStock() { return stock; }
        public void setStock(Integer stock) { this.stock = stock; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }

    /**
     * 상품 통계 데이터 내부 클래스
     */
    private static class ProductStatsData {
        private Long productId;
        private String productName;
        private Integer recentSalesCount;
        private Long recentSalesAmount;
        private Integer totalSalesCount;
        private Long totalSalesAmount;
        private Integer rank;
        private Double conversionRate;

        public ProductStatsData(Long productId, String productName, Integer recentSalesCount,
                              Long recentSalesAmount, Integer totalSalesCount, Long totalSalesAmount,
                              Integer rank, Double conversionRate) {
            this.productId = productId;
            this.productName = productName;
            this.recentSalesCount = recentSalesCount;
            this.recentSalesAmount = recentSalesAmount;
            this.totalSalesCount = totalSalesCount;
            this.totalSalesAmount = totalSalesAmount;
            this.rank = rank;
            this.conversionRate = conversionRate;
        }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public Integer getRecentSalesCount() { return recentSalesCount; }
        public void setRecentSalesCount(Integer recentSalesCount) { this.recentSalesCount = recentSalesCount; }
        public Integer getTotalSalesCount() { return totalSalesCount; }
        public void setTotalSalesCount(Integer totalSalesCount) { this.totalSalesCount = totalSalesCount; }
        public Long getRecentSalesAmount() { return recentSalesAmount; }
        public void setRecentSalesAmount(Long recentSalesAmount) { this.recentSalesAmount = recentSalesAmount; }
        public Long getTotalSalesAmount() { return totalSalesAmount; }
        public void setTotalSalesAmount(Long totalSalesAmount) { this.totalSalesAmount = totalSalesAmount; }
        public Integer getRank() { return rank; }
        public void setRank(Integer rank) { this.rank = rank; }
        public Double getConversionRate() { return conversionRate; }
        public void setConversionRate(Double conversionRate) { this.conversionRate = conversionRate; }
    }
} 