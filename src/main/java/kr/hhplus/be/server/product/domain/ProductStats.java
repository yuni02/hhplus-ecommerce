package kr.hhplus.be.server.product.domain;

import lombok.AccessLevel;
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
    
    @Builder.Default
    private Integer recentSalesCount = 0; // 최근 3일간 판매량
    
    @Builder.Default
    private BigDecimal recentSalesAmount = BigDecimal.ZERO; // 최근 3일간 판매액
    
    @Builder.Default
    private Integer totalSalesCount = 0; // 전체 판매량
    
    @Builder.Default
    private BigDecimal totalSalesAmount = BigDecimal.ZERO; // 전체 판매액
    
    @Builder.Default
    private Integer rank = 0; // 인기 순위
    
    @Builder.Default
    private BigDecimal conversionRate = BigDecimal.ZERO; // 전환율
    
    private LocalDateTime lastOrderDate; // 마지막 주문일
    
    @Builder.Default
    private LocalDateTime aggregationDate = LocalDateTime.now(); // 집계일
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 비즈니스 로직 메서드들
    public void updateRecentSales(Integer quantity, BigDecimal amount) {
        this.recentSalesCount += quantity;
        this.recentSalesAmount = this.recentSalesAmount.add(amount);
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updateTotalSales(Integer quantity, BigDecimal amount) {
        this.totalSalesCount += quantity;
        this.totalSalesAmount = this.totalSalesAmount.add(amount);
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setRank(Integer rank) {
        this.rank = rank;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updateLastOrderDate(LocalDateTime orderDate) {
        if (this.lastOrderDate == null || orderDate.isAfter(this.lastOrderDate)) {
            this.lastOrderDate = orderDate;
            this.updatedAt = LocalDateTime.now();
        }
    }
}