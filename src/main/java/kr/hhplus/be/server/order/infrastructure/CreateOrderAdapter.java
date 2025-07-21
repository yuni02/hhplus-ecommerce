package kr.hhplus.be.server.order.infrastructure;

import kr.hhplus.be.server.balance.application.GetBalanceUseCase;
import kr.hhplus.be.server.order.application.CreateOrderUseCase;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CreateOrderAdapter {
    private final GetBalanceUseCase getBalanceUseCase;

    public CreateOrderAdapter(GetBalanceUseCase getBalanceUseCase) {
        this.getBalanceUseCase = getBalanceUseCase;
    }

    public CreateOrderUseCase.Input adapt(Map<String, Object> request) {
        // 입력값 검증
        Long userId = Long.valueOf(request.get("userId").toString());
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> orderItemsMap = (List<Map<String, Object>>) request.get("orderItems");
        if (orderItemsMap == null || orderItemsMap.isEmpty()) {
            throw new IllegalArgumentException("주문 상품이 없습니다.");
        }

        // OrderItemInput 리스트로 변환
        List<CreateOrderUseCase.OrderItemInput> orderItems = orderItemsMap.stream()
                .map(item -> new CreateOrderUseCase.OrderItemInput(
                        Long.valueOf(item.get("productId").toString()),
                        Integer.valueOf(item.get("quantity").toString())
                ))
                .collect(Collectors.toList());

        Long userCouponId = request.get("userCouponId") != null ? 
                Long.valueOf(request.get("userCouponId").toString()) : null;

        return new CreateOrderUseCase.Input(userId, orderItems, userCouponId);
    }

    public Map<String, Object> adaptResponse(CreateOrderUseCase.Output output) {
        // 주문 후 잔액 조회
        var balanceOpt = getBalanceUseCase.execute(new GetBalanceUseCase.Input(output.getUserId()));
        int remainingBalance = balanceOpt.map(balance -> balance.getBalance().intValue()).orElse(0);

        return Map.of(
                "message", "주문이 성공적으로 생성되었습니다.",
                "order", Map.of(
                        "id", output.getId(),
                        "userId", output.getUserId(),
                        "userCouponId", output.getUserCouponId(),
                        "totalPrice", output.getTotalPrice(),
                        "discountedPrice", output.getDiscountedPrice(),
                        "status", output.getStatus(),
                        "orderItems", output.getOrderItems().stream()
                                .map(item -> Map.of(
                                        "id", item.getId(),
                                        "productId", item.getProductId(),
                                        "productName", item.getProductName(),
                                        "quantity", item.getQuantity(),
                                        "unitPrice", item.getUnitPrice(),
                                        "totalPrice", item.getTotalPrice()
                                ))
                                .collect(Collectors.toList()),
                        "createdAt", output.getCreatedAt()
                ),
                "payment", Map.of(
                        "userId", output.getUserId(),
                        "totalAmount", output.getTotalPrice(),
                        "discountedAmount", output.getDiscountedPrice(),
                        "remainingBalance", remainingBalance));
    }
} 