package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.product.application.port.in.UpdateProductStatsUseCase;
import kr.hhplus.be.server.product.application.port.out.LoadOrderStatsPort;
import kr.hhplus.be.server.product.application.port.out.SaveProductStatsPort;
import kr.hhplus.be.server.product.domain.ProductStats;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 상품 통계 업데이트 Application 서비스
 */
@Service
public class UpdateProductStatsService implements UpdateProductStatsUseCase {

    private final LoadOrderStatsPort loadOrderStatsPort;
    private final SaveProductStatsPort saveProductStatsPort;

    public UpdateProductStatsService(LoadOrderStatsPort loadOrderStatsPort,
                                   SaveProductStatsPort saveProductStatsPort) {
        this.loadOrderStatsPort = loadOrderStatsPort;
        this.saveProductStatsPort = saveProductStatsPort;
    }

    @Override
    @Transactional
    public UpdateProductStatsResult updateRecentProductStats(LocalDate targetDate) {
        try {
            // 1. 최근 3일간 기간 계산
            LocalDateTime endDate = targetDate.atTime(23, 59, 59);
            LocalDateTime startDate = targetDate.minusDays(2).atTime(0, 0, 0);

            // 2. 최근 3일간 상품별 판매 통계 조회
            List<LoadOrderStatsPort.ProductSalesStats> salesStats = 
                loadOrderStatsPort.loadRecentProductSalesStats(startDate, endDate);

            if (salesStats.isEmpty()) {
                return UpdateProductStatsResult.success("최근 3일간 판매 데이터가 없습니다.", 0);
            }

            // 3. 기존 통계 삭제 (해당 날짜)
            saveProductStatsPort.deleteProductStatsByDate(targetDate);

            // 4. 판매량 기준으로 정렬하여 순위 부여
            List<LoadOrderStatsPort.ProductSalesStats> sortedStats = salesStats.stream()
                .sorted(Comparator.comparing(LoadOrderStatsPort.ProductSalesStats::getTotalQuantity).reversed())
                .limit(5) // 상위 5개만
                .collect(Collectors.toList());

            // 5. ProductStats 도메인 객체 생성
            List<ProductStats> productStatsList = new ArrayList<>();
            for (int i = 0; i < sortedStats.size(); i++) {
                LoadOrderStatsPort.ProductSalesStats salesStat = sortedStats.get(i);
                
                ProductStats productStats = ProductStats.builder()
                    .productId(salesStat.getProductId())
                    .date(targetDate)
                    .recentSalesCount(salesStat.getTotalQuantity())
                    .recentSalesAmount(salesStat.getTotalAmount())
                    .rank(i + 1) // 1부터 시작하는 순위
                    .lastOrderDate(salesStat.getLastOrderDate())
                    .aggregationDate(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
                
                productStatsList.add(productStats);
            }

            // 6. 통계 저장
            List<ProductStats> savedStats = saveProductStatsPort.saveAllProductStats(productStatsList);

            return UpdateProductStatsResult.success(
                String.format("최근 3일간 상품 통계가 성공적으로 업데이트되었습니다. (대상: %s, 업데이트: %d개)", 
                    targetDate, savedStats.size()),
                savedStats.size()
            );

        } catch (Exception e) {
            return UpdateProductStatsResult.failure("상품 통계 업데이트 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
} 