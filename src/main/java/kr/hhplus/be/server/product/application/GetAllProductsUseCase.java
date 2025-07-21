package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.ProductRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 상품 목록 조회 UseCase
 * 외부 의존성 없이 도메인 서비스만 호출
 */
@Component
public class GetAllProductsUseCase {

    private final ProductRepository productRepository;

    public GetAllProductsUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> execute() {
        return productRepository.findAllActive();
    }
}