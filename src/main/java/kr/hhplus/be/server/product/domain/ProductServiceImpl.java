package kr.hhplus.be.server.product.domain;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public ProductValidationResult validateProduct(Long productId, Integer quantity) {
        try {
            // 상품 존재 확인
            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isEmpty()) {
                return ProductValidationResult.failure("존재하지 않는 상품입니다: " + productId);
            }

            Product product = productOpt.get();

            // 상품 유효성 검증
            if (!ProductDomainService.isValidProduct(product)) {
                return ProductValidationResult.failure("유효하지 않은 상품입니다: " + product.getName());
            }

            // 재고 검증
            if (!ProductDomainService.hasSufficientStock(product, quantity)) {
                return ProductValidationResult.failure("재고가 부족합니다: " + product.getName());
            }

            return ProductValidationResult.success(product);
        } catch (Exception e) {
            return ProductValidationResult.failure("상품 검증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Override
    public StockDeductResult deductStock(Long productId, Integer quantity) {
        try {
            // 상품 조회
            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isEmpty()) {
                return StockDeductResult.failure("존재하지 않는 상품입니다: " + productId);
            }

            Product product = productOpt.get();

            // 재고 차감
            product = ProductDomainService.decreaseStock(product, quantity);

            // 상품 저장
            product = productRepository.save(product);

            return StockDeductResult.success(product);
        } catch (Exception e) {
            return StockDeductResult.failure("재고 차감 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Override
    public Optional<Product> findById(Long productId) {
        return productRepository.findById(productId);
    }

    @Override
    public List<Product> findAllActive() {
        return productRepository.findAllActive();
    }
} 