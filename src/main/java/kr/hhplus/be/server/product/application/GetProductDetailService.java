package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.product.application.port.in.GetProductDetailUseCase;
import kr.hhplus.be.server.product.application.port.out.LoadProductPort;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 상품 상세 조회 Application 서비스
 */
@Service
public class GetProductDetailService implements GetProductDetailUseCase {

    private final LoadProductPort loadProductPort;

    public GetProductDetailService(LoadProductPort loadProductPort) {
        this.loadProductPort = loadProductPort;
    }

    @Override
    public Optional<GetProductDetailResult> getProductDetail(GetProductDetailCommand command) {
        return loadProductPort.loadProductById(command.getProductId())
                .map(productInfo -> new GetProductDetailResult(
                        productInfo.getId(),
                        productInfo.getName(),
                        productInfo.getCurrentPrice(),
                        productInfo.getStock(),
                        productInfo.getStatus(),
                        null, // createdAt은 별도 조회 필요
                        null  // updatedAt은 별도 조회 필요
                ));
    }
} 