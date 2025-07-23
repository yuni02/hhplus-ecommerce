package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.product.application.port.in.GetProductDetailUseCase;
import kr.hhplus.be.server.product.application.facade.ProductFacade;

import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 상품 상세 조회 Application 서비스 (Facade 패턴 적용)
 */
@Service
public class GetProductDetailService implements GetProductDetailUseCase {

    private final ProductFacade productFacade;

    public GetProductDetailService(ProductFacade productFacade) {
        this.productFacade = productFacade;
    }

    @Override
    public Optional<GetProductDetailResult> getProductDetail(GetProductDetailCommand command) {
        return productFacade.getProductDetail(command);
    }
} 