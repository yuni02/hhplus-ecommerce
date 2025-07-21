package kr.hhplus.be.server.order.domain;

import kr.hhplus.be.server.user.domain.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final UserRepository userRepository;

    public OrderServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OrderValidationResult validateOrderCreation(Long userId, List<OrderItem> orderItems) {
        try {
            // 사용자 존재 확인
            if (!userRepository.existsById(userId)) {
                return OrderValidationResult.failure("사용자를 찾을 수 없습니다.");
            }

            // 주문 아이템 검증
            if (orderItems == null || orderItems.isEmpty()) {
                return OrderValidationResult.failure("주문 상품이 없습니다.");
            }

            // 각 주문 아이템 검증
            for (OrderItem item : orderItems) {
                if (!OrderDomainService.isValidOrderItem(item)) {
                    return OrderValidationResult.failure("유효하지 않은 주문 아이템입니다: " + item.getProductName());
                }
            }

            return OrderValidationResult.success();
        } catch (Exception e) {
            return OrderValidationResult.failure("주문 검증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Override
    public BigDecimal calculateTotalAmount(List<OrderItem> orderItems) {
        return OrderDomainService.calculateTotalAmount(orderItems);
    }

    @Override
    public BigDecimal applyCouponDiscount(BigDecimal totalAmount, BigDecimal discountAmount) {
        return OrderDomainService.applyCouponDiscount(totalAmount, discountAmount);
    }
} 