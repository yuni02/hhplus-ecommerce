package kr.hhplus.be.server.balance.infrastructure;

import kr.hhplus.be.server.balance.application.ChargeBalanceUseCase;
import kr.hhplus.be.server.balance.application.GetBalanceUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Balance", description = "사용자 잔액 관리 API")
public class BalanceController {

    private final GetBalanceUseCase getBalanceUseCase;
    private final ChargeBalanceUseCase chargeBalanceUseCase;
    private final BalanceAdapter balanceAdapter;

    public BalanceController(GetBalanceUseCase getBalanceUseCase, 
                           ChargeBalanceUseCase chargeBalanceUseCase,
                           BalanceAdapter balanceAdapter) {
        this.getBalanceUseCase = getBalanceUseCase;
        this.chargeBalanceUseCase = chargeBalanceUseCase;
        this.balanceAdapter = balanceAdapter;
    }

    /**
     * 잔액 조회 API
     */
    @GetMapping("/balance")
    @Operation(summary = "잔액 조회", description = "사용자의 현재 잔액을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> getBalance(
            @Parameter(description = "사용자 ID", required = true, example = "1")
            @RequestParam("userId") Long userId) {
        try {
            GetBalanceUseCase.Input input = balanceAdapter.adaptGetBalanceRequest(userId);
            var balanceOpt = getBalanceUseCase.execute(input);
            
            if (balanceOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "사용자를 찾을 수 없습니다."));
            }

            return ResponseEntity.ok(balanceAdapter.adaptGetBalanceResponse(balanceOpt.get()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "잔액 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 잔액 충전 API
     */
    @PostMapping("/balance/charge")
    @Operation(summary = "잔액 충전", description = "사용자의 잔액을 충전합니다. (최대 1,000,000원)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "충전 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> chargeBalance(@RequestBody Map<String, Object> request) {
        try {
            ChargeBalanceUseCase.Input input = balanceAdapter.adaptChargeRequest(request);
            ChargeBalanceUseCase.Output output = chargeBalanceUseCase.execute(input);
            return ResponseEntity.ok(balanceAdapter.adaptChargeResponse(output));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "잔액 충전 중 오류가 발생했습니다."));
        }
    }
} 