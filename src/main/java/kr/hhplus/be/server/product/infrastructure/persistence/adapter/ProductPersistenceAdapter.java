package kr.hhplus.be.server.product.infrastructure.persistence.adapter;

import kr.hhplus.be.server.product.application.port.out.LoadProductPort;
import kr.hhplus.be.server.product.application.port.out.LoadProductStatsPort;
import kr.hhplus.be.server.product.application.port.out.SaveProductPort;
import kr.hhplus.be.server.product.infrastructure.persistence.entity.ProductEntity;
import kr.hhplus.be.server.product.infrastructure.persistence.repository.ProductJpaRepository;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Product 인프라스트럭처 영속성 Adapter
 * Product 도메인 전용 데이터 접근
 */
@Component("productProductPersistenceAdapter")
public class ProductPersistenceAdapter implements LoadProductPort, LoadProductStatsPort, SaveProductPort {

    private final ProductJpaRepository productJpaRepository;

    public ProductPersistenceAdapter(ProductJpaRepository productJpaRepository) {
        this.productJpaRepository = productJpaRepository;
    }

    @Override
    public Optional<LoadProductPort.ProductInfo> loadProductById(Long productId) {
        return productJpaRepository.findById(productId)
                .map(this::mapToProductInfo);
    }

    @Override
    public List<LoadProductPort.ProductInfo> loadAllActiveProducts() {
        return productJpaRepository.findByStatus("ACTIVE")
                .stream()
                .map(this::mapToProductInfo)
                .collect(Collectors.toList());
    }

    /**
     * ProductEntity를 ProductInfo로 변환
     */
    private LoadProductPort.ProductInfo mapToProductInfo(ProductEntity entity) {
        return new LoadProductPort.ProductInfo(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPrice().intValue(),   // 현재 가격 설정
                entity.getStockQuantity(),
                entity.getStatus(),
                "GENERAL", // category - 기본값 설정
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    @Override
    public List<LoadProductStatsPort.ProductStatsInfo> loadAllProductStats() {
        // TODO: ProductStatsEntity 구현 후 실제 데이터 반환
        return List.of();
    }

    @Override
    public Optional<LoadProductStatsPort.ProductStatsInfo> loadProductStatsByProductId(Long productId) {
        // TODO: ProductStatsEntity 구현 후 실제 데이터 반환
        return Optional.empty();
    }

    @Override
    public List<LoadProductStatsPort.ProductStatsInfo> loadTopProductsBySales(int limit) {
        // TODO: ProductStatsEntity 구현 후 실제 데이터 반환
        return List.of();
    }

    @Override
    public SaveProductPort.ProductInfo saveProduct(SaveProductPort.ProductInfo productInfo) {
        ProductEntity entity;
        
        if (productInfo.getId() != null) {
            // 기존 상품 업데이트
            entity = productJpaRepository.findById(productInfo.getId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + productInfo.getId()));
            
            // 비즈니스 메서드를 사용하여 업데이트
            entity.updateProduct(
                productInfo.getName(),
                productInfo.getDescription(),
                productInfo.getCurrentPrice(),
                productInfo.getStock(),
                productInfo.getStatus()
            );
        } else {
            // 새 상품 생성
            entity = ProductEntity.builder()
                    .name(productInfo.getName())
                    .description(productInfo.getDescription())
                    .price(productInfo.getCurrentPrice())
                    .stockQuantity(productInfo.getStock())
                    .status(productInfo.getStatus())
                    .build();
        }
        
        ProductEntity savedEntity = productJpaRepository.save(entity);
        
        return SaveProductPort.ProductInfo.builder()
                .id(savedEntity.getId())
                .name(savedEntity.getName())
                .description(savedEntity.getDescription())
                .currentPrice(savedEntity.getPrice())
                .stock(savedEntity.getStockQuantity())
                .status(savedEntity.getStatus())
                .category("GENERAL")
                .createdAt(savedEntity.getCreatedAt())
                .updatedAt(savedEntity.getUpdatedAt())
                .build();
    }
}