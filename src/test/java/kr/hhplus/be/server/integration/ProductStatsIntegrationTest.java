package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.product.application.UpdateProductStatsService;
import kr.hhplus.be.server.product.application.port.in.UpdateProductStatsUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("ProductStats 도메인 통합테스트")
class ProductStatsIntegrationTest {

    @Autowired
    private UpdateProductStatsService updateProductStatsService;

    @Test
    @DisplayName("상품 통계 업데이트 성공")
    void 상품_통계_업데이트_성공() {
        // when
        UpdateProductStatsUseCase.UpdateProductStatsResult result = updateProductStatsService.updateRecentProductStats(LocalDate.now());

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUpdatedCount()).isGreaterThanOrEqualTo(0);
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("상품 통계 업데이트 성공 - 특정 날짜")
    void 상품_통계_업데이트_성공_특정_날짜() {
        // given
        LocalDate targetDate = LocalDate.of(2025, 1, 1);

        // when
        UpdateProductStatsUseCase.UpdateProductStatsResult result = updateProductStatsService.updateRecentProductStats(targetDate);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("상품 통계 업데이트 성공 - 판매 데이터 없음")
    void 상품_통계_업데이트_성공_판매_데이터_없음() {
        // when
        UpdateProductStatsUseCase.UpdateProductStatsResult result = updateProductStatsService.updateRecentProductStats(LocalDate.now().minusDays(1));

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUpdatedCount()).isGreaterThanOrEqualTo(0);
    }
} 