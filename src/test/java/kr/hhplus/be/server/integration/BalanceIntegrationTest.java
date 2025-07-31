package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.config.BaseIntegrationTest;
import kr.hhplus.be.server.balance.application.port.in.GetBalanceUseCase;
import kr.hhplus.be.server.balance.application.port.in.ChargeBalanceUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 잔액 관련 통합테스트
 * BaseIntegrationTest를 상속받아 공통 설정 자동 적용
 */
class BalanceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private GetBalanceUseCase getBalanceUseCase;

    @Autowired
    private ChargeBalanceUseCase chargeBalanceUseCase;

    @Test
    void 잔액_조회_테스트() {
        // Given
        Long userId = 1001L;

        // When
        var result = getBalanceUseCase.getBalance(
            new GetBalanceUseCase.GetBalanceCommand(userId)
        );

        // Then
        assertTrue(result.isPresent());
        assertNotNull(result.get().getBalance());
        // SQL 로그가 자동으로 출력됨 (TestLogConfiguration 적용)
    }

    @Test
    void 잔액_충전_테스트() {
        // Given
        Long userId = 1001L;
        BigDecimal chargeAmount = new BigDecimal("10000");

        // When
        var result = chargeBalanceUseCase.chargeBalance(
            new ChargeBalanceUseCase.ChargeBalanceCommand(userId, chargeAmount)
        );

        // Then
        assertTrue(result.isSuccess());
        assertEquals(chargeAmount, result.getChargeAmount());
        // SQL 로그가 자동으로 출력됨 (TestLogConfiguration 적용)
    }

    @Override
    protected void setUpTestData() {
        // 테스트 데이터 초기화 (필요시)
        super.setUpTestData();
    }

    @Override
    protected void tearDownTestData() {
        // 테스트 데이터 정리 (필요시)
        super.tearDownTestData();
    }
} 