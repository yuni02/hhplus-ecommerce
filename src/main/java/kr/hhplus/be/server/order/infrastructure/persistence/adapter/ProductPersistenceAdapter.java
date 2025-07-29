package kr.hhplus.be.server.order.infrastructure.persistence.adapter;

import kr.hhplus.be.server.order.application.port.out.LoadProductPort;
import kr.hhplus.be.server.product.infrastructure.persistence.entity.ProductEntity;
import kr.hhplus.be.server.product.infrastructure.persistence.repository.ProductJpaRepository;

import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Product 영속성 Adapter (Order 도메인용)
 * Order 도메인에서 상품 정보 조회를 위한 어댑터
 */
@Component("orderProductPersistenceAdapter")
public class ProductPersistenceAdapter implements LoadProductPort {

    private final ProductJpaRepository productJpaRepository;

    public ProductPersistenceAdapter(ProductJpaRepository productJpaRepository) {
        this.productJpaRepository = productJpaRepository;
    }

    @Override
    public Optional<LoadProductPort.ProductInfo> loadProductById(Long productId) {
        return productJpaRepository.findById(productId)
                .map(this::mapToProductInfo);
    }

    /**
     * ProductEntity를 ProductInfo로 변환
     */
    private LoadProductPort.ProductInfo mapToProductInfo(ProductEntity entity) {
        return new LoadProductPort.ProductInfo(
                entity.getId(),
                entity.getName(),
                entity.getCurrentPrice(),
                entity.getStock()
        );
    }
}