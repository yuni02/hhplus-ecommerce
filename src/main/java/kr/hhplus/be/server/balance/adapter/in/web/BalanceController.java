package kr.hhplus.be.server.balance.adapter.in.web;

import kr.hhplus.be.server.balance.adapter.in.dto.ChargeBalanceRequest;
import kr.hhplus.be.server.balance.application.facade.BalanceFacade;
import kr.hhplus.be.server.balance.application.port.in.ChargeBalanceUseCase;
import kr.hhplus.be.server.balance.application.port.in.GetBalanceUseCase;
import kr.hhplus.be.server.balance.application.response.BalanceResponse;
import kr.hhplus.be.server.balance.application.response.ChargeBalanceResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Balance", description = "사용자 잔액 관리 API")
public class BalanceController {

    private final BalanceFacade balanceFacade;

    public BalanceController(BalanceFacade balanceFacade) {
        this.balanceFacade = balanceFacade;
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
        
        GetBalanceUseCase.GetBalanceCommand command = new GetBalanceUseCase.GetBalanceCommand(userId);
        var balanceOpt = balanceFacade.getBalance(command);
        
        if (balanceOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new kr.hhplus.be.server.shared.response.ErrorResponse("사용자를 찾을 수 없습니다."));
        }

        GetBalanceUseCase.GetBalanceResult result = balanceOpt.get();
        BalanceResponse response = new BalanceResponse(result.getUserId(), result.getBalance().intValue());
        return ResponseEntity.ok(response);
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
    public ResponseEntity<?> chargeBalance(@Valid @RequestBody ChargeBalanceRequest request) {
        
        ChargeBalanceUseCase.ChargeBalanceCommand command = 
            new ChargeBalanceUseCase.ChargeBalanceCommand(request.getUserId(), BigDecimal.valueOf(request.getAmount()));
        
        ChargeBalanceUseCase.ChargeBalanceResult result = balanceFacade.chargeBalance(command);
        
        if (!result.isSuccess()) {
            return ResponseEntity.badRequest().body(new kr.hhplus.be.server.shared.response.ErrorResponse(result.getErrorMessage()));
        }
        
        ChargeBalanceResponse response = new ChargeBalanceResponse(
                result.getUserId(),
                request.getAmount(),        
                result.getNewBalance().intValue());
        
        return ResponseEntity.ok(response);
    }
} 