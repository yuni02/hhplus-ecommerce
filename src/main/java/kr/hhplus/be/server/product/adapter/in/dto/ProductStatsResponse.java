package kr.hhplus.be.server.product.adapter.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품 통계 업데이트 응답")
public class ProductStatsResponse {
    
    @Schema(description = "응답 메시지", example = "최근 3일간 상품 통계가 성공적으로 업데이트되었습니다.")
    private String message;
    
    @Schema(description = "업데이트된 통계 개수", example = "5")
    private Integer updatedCount;
    
    @Schema(description = "통계 대상 날짜", example = "2025-01-01")
    private String targetDate;

    public ProductStatsResponse() {}

    public ProductStatsResponse(String message, Integer updatedCount, String targetDate) {
        this.message = message;
        this.updatedCount = updatedCount;
        this.targetDate = targetDate;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getUpdatedCount() {
        return updatedCount;
    }

    public void setUpdatedCount(Integer updatedCount) {
        this.updatedCount = updatedCount;
    }

    public String getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(String targetDate) {
        this.targetDate = targetDate;
    }
} 