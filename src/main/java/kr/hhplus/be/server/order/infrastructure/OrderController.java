package kr.hhplus.be.server.order.infrastructure;

import kr.hhplus.be.server.order.application.CreateOrderUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order", description = "주문 관리 API")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final CreateOrderAdapter createOrderAdapter;

    public OrderController(CreateOrderUseCase createOrderUseCase,
                          CreateOrderAdapter createOrderAdapter) {
        this.createOrderUseCase = createOrderUseCase;
        this.createOrderAdapter = createOrderAdapter;
    }

    /**
     * 주문 생성 API
     */
    @PostMapping
    @Operation(summary = "주문 생성", description = "상품을 주문하고 결제를 처리합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "주문 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 주문 실패"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> request) {
        try {
            CreateOrderUseCase.Input input = createOrderAdapter.adapt(request);
            CreateOrderUseCase.Output output = createOrderUseCase.execute(input);
            return ResponseEntity.ok(createOrderAdapter.adaptResponse(output));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "주문 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
} 