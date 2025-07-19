package kr.hhplus.be.server.controller;

import kr.hhplus.be.server.dto.request.BalanceChargeRequest;
// import kr.hhplus.be.server.dto.response.BalanceHistoryResponse; 
import kr.hhplus.be.server.dto.response.BalanceResponse;
import kr.hhplus.be.server.service.DummyDataService;
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

    private final DummyDataService dummyDataService;

    public BalanceController(DummyDataService dummyDataService) {
        this.dummyDataService = dummyDataService;
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
            @RequestParam Long userId) {
        try {
            // 입력값 검증
            if (userId == null || userId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "유효하지 않은 사용자 ID입니다."));
            }

            BalanceResponse response = dummyDataService.getUserBalance(userId);
            if (response == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "사용자를 찾을 수 없습니다."));
            }

            return ResponseEntity.ok(response);

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
    public ResponseEntity<?> chargeBalance(@RequestBody BalanceChargeRequest request) {
        try {
            // 입력값 검증
            if (request.getUserId() == null || request.getUserId() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "유효하지 않은 사용자 ID입니다."));
            }
            if (request.getAmount() == null || request.getAmount() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "충전 금액은 양수여야 합니다."));
            }
            if (request.getAmount() > 1000000) {
                return ResponseEntity.badRequest().body(Map.of("message", "1회 최대 충전 금액은 1,000,000원입니다."));
            }

            BalanceResponse response = dummyDataService.chargeBalance(request.getUserId(), request.getAmount());
            if (response == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "사용자를 찾을 수 없습니다."));
            }

            return ResponseEntity.ok(Map.of(
                    "message", "잔액 충전이 완료되었습니다.",
                    "balance", response));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "잔액 충전 중 오류가 발생했습니다."));
        }
    }

}