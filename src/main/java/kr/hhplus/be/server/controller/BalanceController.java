package kr.hhplus.be.server.controller;

import kr.hhplus.be.server.dto.request.BalanceChargeRequest;
import kr.hhplus.be.server.dto.response.BalanceHistoryResponse;
import kr.hhplus.be.server.dto.response.BalanceResponse;
import kr.hhplus.be.server.service.DummyDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class BalanceController {

    private final DummyDataService dummyDataService;

    public BalanceController(DummyDataService dummyDataService) {
        this.dummyDataService = dummyDataService;
    }

    /**
     * 잔액 조회 API
     */
    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(@RequestParam Long userId) {
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

    /**
     * 잔액 거래 내역 조회 API
     */
    @GetMapping("/balance/history")
    public ResponseEntity<?> getBalanceHistory(@RequestParam Long userId) {
        try {
            // 입력값 검증
            if (userId == null || userId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "유효하지 않은 사용자 ID입니다."));
            }

            // 사용자 존재 확인
            if (dummyDataService.getUser(userId) == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "사용자를 찾을 수 없습니다."));
            }

            BalanceHistoryResponse response = dummyDataService.getBalanceHistory(userId);

            if (response.getTransactions().isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "message", "거래 내역이 없습니다.",
                        "history", response));
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "거래 내역 조회 중 오류가 발생했습니다."));
        }
    }
}