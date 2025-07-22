package kr.hhplus.be.server.order.adapter.out.persistence;

import org.springframework.stereotype.Component;

import kr.hhplus.be.server.order.application.port.out.LoadProductPort;
import kr.hhplus.be.server.order.application.port.out.UpdateProductStockPort;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 상품 영속성 Adapter (Outgoing)
 */
@Component
public class ProductPersistenceAdapter implements LoadProductPort, UpdateProductStockPort {

    private final Map<Long, ProductData> products = new ConcurrentHashMap<>();
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
                    BigDecimal.valueOf(10000 + productId * 1000),
                    100
            );
            products.put(productId, product);
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
                product.getCurrentPrice(),
                product.getStock()
        ));
    }

    @Override
    public boolean deductStock(Long productId, Integer quantity) {
        ProductData product = products.get(productId);
        if (product == null || product.getStock() < quantity) {
            return false;
        }
        
        product.setStock(product.getStock() - quantity);
        return true;
    }

    /**
     * 상품 데이터 내부 클래스
     */
    private static class ProductData {
        private Long id;
        private String name;
        private BigDecimal currentPrice;
        private Integer stock;

        public ProductData(Long id, String name, BigDecimal currentPrice, Integer stock) {
            this.id = id;
            this.name = name;
            this.currentPrice = currentPrice;
            this.stock = stock;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public BigDecimal getCurrentPrice() {
            return currentPrice;
        }

        public void setCurrentPrice(BigDecimal currentPrice) {
            this.currentPrice = currentPrice;
        }

        public Integer getStock() {
            return stock;
        }

        public void setStock(Integer stock) {
            this.stock = stock;
        }
    }
} 