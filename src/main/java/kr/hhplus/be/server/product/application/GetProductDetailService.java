package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.product.application.port.in.GetProductDetailUseCase;
import kr.hhplus.be.server.product.application.port.out.LoadProductPort;
import kr.hhplus.be.server.product.application.port.out.LoadProductStatsPort;

import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 상품 상세 조회 Application 서비스
 */
@Service
public class GetProductDetailService implements GetProductDetailUseCase {

    private final LoadProductPort loadProductPort;
    private final LoadProductStatsPort loadProductStatsPort;

    public GetProductDetailService(LoadProductPort loadProductPort, 
                                  LoadProductStatsPort loadProductStatsPort) {
        this.loadProductPort = loadProductPort;
        this.loadProductStatsPort = loadProductStatsPort;
    }

    @Override
    public Optional<GetProductDetailResult> getProductDetail(GetProductDetailCommand command) {
        try {
            // 1. 입력값 검증
            if (command.getProductId() == null || command.getProductId() <= 0) {
                throw new IllegalArgumentException("잘못된 상품 ID입니다.");
            }
            
            // 2. 상품 조회
            Optional<LoadProductPort.ProductInfo> productInfoOpt = loadProductPort.loadProductById(command.getProductId());
            
            if (productInfoOpt.isEmpty()) {
                return Optional.empty();
            }

            LoadProductPort.ProductInfo productInfo = productInfoOpt.get();

            // 3. 상품 통계 조회 (선택적)
            Optional<LoadProductStatsPort.ProductStatsInfo> statsInfoOpt = 
                loadProductStatsPort.loadProductStatsByProductId(command.getProductId());

            // 4. 결과 생성
            GetProductDetailResult result = new GetProductDetailResult(
                    productInfo.getId(),
                    productInfo.getName(),
                    productInfo.getCurrentPrice(),
                    productInfo.getStock(),
                    productInfo.getStatus(),
                    null, // createdAt - ProductInfo에 없는 경우
                    null  // updatedAt - ProductInfo에 없는 경우
            );

            return Optional.of(result);

        } catch (Exception e) {
            return Optional.empty();
        }
    }
} 