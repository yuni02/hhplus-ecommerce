package kr.hhplus.be.server.controller;

import kr.hhplus.be.server.dto.request.OrderRequest;
import kr.hhplus.be.server.dto.response.OrderResponse;
import kr.hhplus.be.server.service.DummyDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final DummyDataService dummyDataService;

    public OrderController(DummyDataService dummyDataService) {
        this.dummyDataService = dummyDataService;
    }

    /**
     * 주문 생성 API
     */
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest request) {
        try {
            // 입력값 검증
            if (request.getUserId() == null || request.getUserId() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "유효하지 않은 사용자 ID입니다."));
            }
            if (request.getOrderItems() == null || request.getOrderItems().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "주문 상품이 없습니다."));
            }

            // 사용자 존재 확인
            var user = dummyDataService.getUser(request.getUserId());
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "사용자를 찾을 수 없습니다."));
            }

            // 주문 상품 검증 및 총 금액 계산
            int totalPrice = 0;
            for (OrderRequest.OrderItemRequest item : request.getOrderItems()) {
                if (item.getProductId() == null || item.getProductId() <= 0) {
                    return ResponseEntity.badRequest().body(Map.of("message", "유효하지 않은 상품 ID입니다."));
                }
                if (item.getQuantity() == null || item.getQuantity() <= 0) {
                    return ResponseEntity.badRequest().body(Map.of("message", "상품 수량은 1개 이상이어야 합니다."));
                }

                // 상품 존재 및 재고 확인
                var product = dummyDataService.getProduct(item.getProductId());
                if (product == null) {
                    return ResponseEntity.badRequest().body(Map.of("message", "존재하지 않는 상품입니다: " + item.getProductId()));
                }
                if (product.getStock() < item.getQuantity()) {
                    return ResponseEntity.badRequest().body(Map.of("message", "재고가 부족합니다: " + product.getName()));
                }

                // 총 금액 계산
                totalPrice += product.getCurrentPrice() * item.getQuantity();
            }

            // 쿠폰 할인 적용
            int discountedPrice = totalPrice;
            if (request.getUserCouponId() != null) {
                var userCoupons = dummyDataService.getUserCouponsUpdated(request.getUserId());
                var validCoupon = userCoupons.stream()
                        .filter(uc -> uc.getUserCouponId().equals(request.getUserCouponId())
                                && "AVAILABLE".equals(uc.getStatus()))
                        .findFirst();

                if (validCoupon.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("message", "사용할 수 없는 쿠폰입니다."));
                }

                // 할인 금액 적용
                discountedPrice = Math.max(0, totalPrice - validCoupon.get().getDiscountAmount());
            }

            // 잔액 확인
            var userBalance = dummyDataService.getUserBalance(request.getUserId());
            if (userBalance.getBalance() < discountedPrice) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "잔액이 부족합니다.",
                        "userId", request.getUserId(),
                        "currentBalance", userBalance.getBalance(),
                        "requiredAmount", discountedPrice,
                        "shortfall", discountedPrice - userBalance.getBalance()));
            }

            // 주문 생성 (잔액 차감 포함)
            OrderResponse order = dummyDataService.createOrder(
                    request.getUserId(),
                    request.getOrderItems(),
                    request.getUserCouponId());

            // 주문 후 잔액 정보 포함하여 응답
            var updatedBalance = dummyDataService.getUserBalance(request.getUserId());

            return ResponseEntity.ok(Map.of(
                    "message", "주문이 성공적으로 생성되었습니다.",
                    "order", order,
                    "payment", Map.of(
                            "userId", request.getUserId(),
                            "totalAmount", totalPrice,
                            "discountedAmount", discountedPrice,
                            "remainingBalance", updatedBalance.getBalance())));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "주문 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 주문 상세 조회 API
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Long id) {
        try {
            // 입력값 검증
            if (id == null || id <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "유효하지 않은 주문 ID입니다."));
            }

            OrderResponse order = dummyDataService.getOrder(id);
            if (order == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(order);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "주문 조회 중 오류가 발생했습니다."));
        }
    }
}

/**
 * 사용자별 주문 조회를 위한 별도 컨트롤러
 */
@RestController
@RequestMapping("/api/users")
class UserOrderController {

    private final DummyDataService dummyDataService;

    public UserOrderController(DummyDataService dummyDataService) {
        this.dummyDataService = dummyDataService;
    }

    /**
     * 사용자 주문 내역 조회 API
     */
    @GetMapping("/orders")
    public ResponseEntity<?> getUserOrders(@RequestParam Long userId) {
        try {
            // 입력값 검증
            if (userId == null || userId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "유효하지 않은 사용자 ID입니다."));
            }

            // 사용자 존재 확인
            if (dummyDataService.getUser(userId) == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "사용자를 찾을 수 없습니다."));
            }

            List<OrderResponse> orders = dummyDataService.getUserOrders(userId);

            if (orders.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "message", "주문 내역이 없습니다.",
                        "orders", orders));
            }

            return ResponseEntity.ok(Map.of(
                    "message", "주문 내역 조회 성공",
                    "orders", orders));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "주문 내역 조회 중 오류가 발생했습니다."));
        }
    }
}