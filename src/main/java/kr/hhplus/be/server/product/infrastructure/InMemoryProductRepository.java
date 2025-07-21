package kr.hhplus.be.server.product.infrastructure;

import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.ProductRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryProductRepository implements ProductRepository {

    private final Map<Long, Product> products = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public InMemoryProductRepository() {
        // 더미 데이터 초기화
        initializeDummyData();
    }

    private void initializeDummyData() {
        // 상품 1: 노트북
        Product product1 = new Product(
                "MacBook Pro 14",
                "Apple M2 Pro 칩 탑재 노트북",
                BigDecimal.valueOf(2500000),
                10,
                "전자제품"
        );
        product1.setId(idGenerator.getAndIncrement());
        products.put(product1.getId(), product1);

        // 상품 2: 스마트폰
        Product product2 = new Product(
                "iPhone 15 Pro",
                "A17 Pro 칩 탑재 스마트폰",
                BigDecimal.valueOf(1500000),
                20,
                "전자제품"
        );
        product2.setId(idGenerator.getAndIncrement());
        products.put(product2.getId(), product2);

        // 상품 3: 헤드폰
        Product product3 = new Product(
                "AirPods Pro",
                "노이즈 캔슬링 무선 이어폰",
                BigDecimal.valueOf(350000),
                50,
                "전자제품"
        );
        product3.setId(idGenerator.getAndIncrement());
        products.put(product3.getId(), product3);

        // 상품 4: 책
        Product product4 = new Product(
                "클린 코드",
                "로버트 C. 마틴의 클린 코드",
                BigDecimal.valueOf(25000),
                100,
                "도서"
        );
        product4.setId(idGenerator.getAndIncrement());
        products.put(product4.getId(), product4);

        // 상품 5: 의류
        Product product5 = new Product(
                "캐시미어 니트",
                "100% 캐시미어 소재 니트",
                BigDecimal.valueOf(89000),
                30,
                "의류"
        );
        product5.setId(idGenerator.getAndIncrement());
        products.put(product5.getId(), product5);
    }

    @Override
    public List<Product> findAllActive() {
        return products.values().stream()
                .filter(Product::isActive)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(products.get(id));
    }

    @Override
    public Product save(Product product) {
        if (product.getId() == null) {
            product.setId(idGenerator.getAndIncrement());
        }
        products.put(product.getId(), product);
        return product;
    }

    @Override
    public boolean existsById(Long id) {
        return products.containsKey(id);
    }
} 
