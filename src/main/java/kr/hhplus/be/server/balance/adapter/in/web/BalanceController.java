package kr.hhplus.be.server.balance.adapter.in.web;

import kr.hhplus.be.server.balance.adapter.in.dto.ChargeBalanceRequest;
import kr.hhplus.be.server.balance.application.port.in.ChargeBalanceUseCase;
import kr.hhplus.be.server.balance.application.port.in.GetBalanceUseCase;
import kr.hhplus.be.server.balance.adapter.in.dto.BalanceResponse;
import kr.hhplus.be.server.balance.adapter.in.dto.ChargeBalanceResponse;   

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.math.BigDecimal;    

@RestController
@RequestMapping("/api/users")
public class BalanceController implements BalanceApiDocumentation {

    private final GetBalanceUseCase getBalanceUseCase;
    private final ChargeBalanceUseCase chargeBalanceUseCase;

    public BalanceController(GetBalanceUseCase getBalanceUseCase, 
                           ChargeBalanceUseCase chargeBalanceUseCase) {
        this.getBalanceUseCase = getBalanceUseCase;
        this.chargeBalanceUseCase = chargeBalanceUseCase;
    }

    @Override
    public ResponseEntity<?> getBalance(
            @RequestParam("userId") Long userId) {
        
        GetBalanceUseCase.GetBalanceCommand command = new GetBalanceUseCase.GetBalanceCommand(userId);
        var balanceOpt = getBalanceUseCase.getBalance(command);
        
        if (balanceOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new kr.hhplus.be.server.shared.response.ErrorResponse("사용자를 찾을 수 없습니다."));
        }

        GetBalanceUseCase.GetBalanceResult result = balanceOpt.get();
        BalanceResponse response = new BalanceResponse(result.getUserId(), result.getBalance().intValue());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<?> chargeBalance(@Valid @RequestBody ChargeBalanceRequest request) {
        
        ChargeBalanceUseCase.ChargeBalanceCommand command = 
            new ChargeBalanceUseCase.ChargeBalanceCommand(request.getUserId(), BigDecimal.valueOf(request.getAmount()));
        
        ChargeBalanceUseCase.ChargeBalanceResult result = chargeBalanceUseCase.chargeBalance(command);
        
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