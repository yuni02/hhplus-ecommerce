package kr.hhplus.be.server.product.application.port.in;

import java.time.LocalDate;

/**
 * 상품 통계 업데이트 UseCase
 */
public interface UpdateProductStatsUseCase {
    
    /**
     * 최근 3일간 판매 통계를 계산하여 product_stats 테이블에 저장
     */
    UpdateProductStatsResult updateRecentProductStats(LocalDate targetDate);
    
    /**
     * 상품 통계 업데이트 명령
     */
    class UpdateProductStatsCommand {
        private final LocalDate targetDate;
        
        public UpdateProductStatsCommand(LocalDate targetDate) {
            this.targetDate = targetDate;
        }
        
        public LocalDate getTargetDate() {
            return targetDate;
        }
    }
    
    /**
     * 상품 통계 업데이트 결과
     */
    class UpdateProductStatsResult {
        private final boolean success;
        private final String message;
        private final Integer updatedCount;
        private final String errorMessage;
        
        private UpdateProductStatsResult(boolean success, String message, Integer updatedCount, String errorMessage) {
            this.success = success;
            this.message = message;
            this.updatedCount = updatedCount;
            this.errorMessage = errorMessage;
        }
        
        public static UpdateProductStatsResult success(String message, Integer updatedCount) {
            return new UpdateProductStatsResult(true, message, updatedCount, null);
        }
        
        public static UpdateProductStatsResult failure(String errorMessage) {
            return new UpdateProductStatsResult(false, null, null, errorMessage);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public Integer getUpdatedCount() {
            return updatedCount;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
} 