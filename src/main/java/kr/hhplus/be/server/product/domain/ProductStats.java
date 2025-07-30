package kr.hhplus.be.server.product.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 상품 통계 도메인 엔티티
 * 순수한 비즈니스 로직만 포함 (JPA 어노테이션 없음)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductStats {

    private Long productId;
    private LocalDate date;
    private Integer recentSalesCount; // 최근 3일간 판매량
    private BigDecimal recentSalesAmount; // 최근 3일간 판매액
    private Integer totalSalesCount; // 전체 판매량
    private BigDecimal totalSalesAmount; // 전체 판매액
    private Integer rank; // 인기 순위
    private BigDecimal conversionRate; // 전환율
    private LocalDateTime lastOrderDate; // 마지막 주문일
    private LocalDateTime aggregationDate; // 집계일
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Product product;

    // 비즈니스 로직 메서드들
    public void addSale(Integer quantity, BigDecimal amount) {
        this.recentSalesCount = (this.recentSalesCount != null ? this.recentSalesCount : 0) + quantity;
        this.recentSalesAmount = (this.recentSalesAmount != null ? this.recentSalesAmount : BigDecimal.ZERO).add(amount);
        this.totalSalesCount = (this.totalSalesCount != null ? this.totalSalesCount : 0) + quantity;
        this.totalSalesAmount = (this.totalSalesAmount != null ? this.totalSalesAmount : BigDecimal.ZERO).add(amount);
        this.lastOrderDate = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void calculateConversionRate(Integer totalViews) {
        if (totalViews != null && totalViews > 0 && this.totalSalesCount != null) {
            this.conversionRate = BigDecimal.valueOf(this.totalSalesCount)
                    .divide(BigDecimal.valueOf(totalViews), 4, java.math.RoundingMode.HALF_UP);
            this.updatedAt = LocalDateTime.now();
        }
    }
}