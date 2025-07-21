package kr.hhplus.be.server.product.domain;

import java.util.List;
import java.util.Optional;

public interface ProductStatsRepository {
    
    List<ProductStats> findTopPopularProducts(int limit);
    
    Optional<ProductStats> findByProductId(Long productId);
    
    ProductStats save(ProductStats productStats);
    
    List<ProductStats> findAll();
} 