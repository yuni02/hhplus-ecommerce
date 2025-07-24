package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.product.application.port.in.GetPopularProductsUseCase;
import kr.hhplus.be.server.product.application.facade.ProductFacade;

import org.springframework.stereotype.Service;

/**
 * 인기 상품 조회 Application 서비스 (Facade 패턴 적용)
 */
@Service
public class GetPopularProductsService implements GetPopularProductsUseCase {

    private final ProductFacade productFacade;

    public GetPopularProductsService(ProductFacade productFacade) {
        this.productFacade = productFacade;
    }

    @Override
    public GetPopularProductsResult getPopularProducts(GetPopularProductsCommand command) {
        return productFacade.getPopularProducts(command);
    }
} 