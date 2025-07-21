package kr.hhplus.be.server.order.infrastructure;

import kr.hhplus.be.server.balance.application.GetBalanceUseCase;
import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.dto.request.OrderRequest;
import kr.hhplus.be.server.dto.response.OrderResponse;
import kr.hhplus.be.server.order.application.CreateOrderUseCase;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order", description = "주문 관리 API")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final GetBalanceUseCase getBalanceUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase,
                          GetBalanceUseCase getBalanceUseCase) {
        this.createOrderUseCase = createOrderUseCase;
        this.getBalanceUseCase = getBalanceUseCase;
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
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest request) {
        try {
            // 입력값 검증
            if (request.getUserId() == null || request.getUserId() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "유효하지 않은 사용자 ID입니다."));
            }
            if (request.getOrderItems() == null || request.getOrderItems().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "주문 상품이 없습니다."));
            }

            // OrderItemRequest 리스트로 변환
            List<CreateOrderUseCase.OrderItemRequest> orderItems = request.getOrderItems().stream()
                    .map(item -> new CreateOrderUseCase.OrderItemRequest(item.getProductId(), item.getQuantity()))
                    .collect(Collectors.toList());

            // 주문 생성
            Order order = createOrderUseCase.execute(request.getUserId(), orderItems, request.getUserCouponId());

            // 주문 후 잔액 조회
            var balanceOpt = getBalanceUseCase.execute(request.getUserId());
            int remainingBalance = balanceOpt.map(balance -> balance.getAmount().intValue()).orElse(0);

            // OrderResponse로 변환
            OrderResponse orderResponse = new OrderResponse(
                    order.getId(),
                    order.getUserId(),
                    order.getUserCouponId(),
                    order.getTotalAmount().intValue(),
                    order.getDiscountedAmount().intValue(),
                    order.getStatus().name(),
                    order.getOrderItems().stream()
                            .map(this::convertToOrderItemResponse)
                            .collect(Collectors.toList()),
                    order.getOrderedAt()
            );

            return ResponseEntity.ok(Map.of(
                    "message", "주문이 성공적으로 생성되었습니다.",
                    "order", orderResponse,
                    "payment", Map.of(
                            "userId", request.getUserId(),
                            "totalAmount", order.getTotalAmount().intValue(),
                            "discountedAmount", order.getDiscountedAmount().intValue(),
                            "remainingBalance", remainingBalance)));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "주문 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    private OrderResponse.OrderItemResponse convertToOrderItemResponse(OrderItem orderItem) {
        return new OrderResponse.OrderItemResponse(
                orderItem.getId(),
                orderItem.getProductId(),
                orderItem.getProductName(),
                orderItem.getQuantity(),
                orderItem.getUnitPrice().intValue(),
                orderItem.getTotalPrice().intValue()
        );
    }
} 