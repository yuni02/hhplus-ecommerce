package kr.hhplus.be.server.order.adapter.in.web;

import kr.hhplus.be.server.order.application.facade.OrderFacade;
import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.order.application.response.OrderResponse;
import kr.hhplus.be.server.order.adapter.in.dto.OrderRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order", description = "주문 관리 API")
public class OrderController {

    private final OrderFacade orderFacade;

    public OrderController(OrderFacade orderFacade) {
        this.orderFacade = orderFacade;
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
    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderRequest request) {
        
        // OrderRequest를 CreateOrderCommand로 변환
        List<CreateOrderUseCase.OrderItemCommand> orderItemCommands = request.getOrderItems().stream()
                .map(item -> new CreateOrderUseCase.OrderItemCommand(item.getProductId(), item.getQuantity()))
                .collect(Collectors.toList());

        CreateOrderUseCase.CreateOrderCommand command = new CreateOrderUseCase.CreateOrderCommand(
                request.getUserId(), orderItemCommands, request.getUserCouponId());

        CreateOrderUseCase.CreateOrderResult result = orderFacade.createOrder(command);

        if (!result.isSuccess()) {
            return ResponseEntity.badRequest().body(new kr.hhplus.be.server.shared.response.ErrorResponse(result.getErrorMessage()));
        }

        // CreateOrderResult를 OrderResponse로 변환
        List<OrderResponse.OrderItemResponse> orderItemResponses = result.getOrderItems().stream()
                .map(item -> new OrderResponse.OrderItemResponse(
                        item.getId(),
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice().intValue(),
                        item.getTotalPrice().intValue()))
                .collect(Collectors.toList());

        OrderResponse response = new OrderResponse(
                result.getOrderId(),
                result.getUserId(),
                result.getUserCouponId(),
                result.getTotalAmount().intValue(),
                result.getDiscountedAmount().intValue(),
                result.getStatus(),
                orderItemResponses,
                result.getCreatedAt());

        return ResponseEntity.ok(response);
    }
} 