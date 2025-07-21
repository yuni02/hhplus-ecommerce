package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.balance.application.ChargeBalanceUseCase;
import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderRepository;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.ProductRepository;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateOrderUseCase 단위 테스트")
class CreateOrderUseCaseTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChargeBalanceUseCase chargeBalanceUseCase;

    private CreateOrderUseCase createOrderUseCase;

    @BeforeEach
    void setUp() {
        createOrderUseCase = new CreateOrderUseCase(
                orderRepository, productRepository, userRepository, chargeBalanceUseCase);
    }

    @Test
    @DisplayName("정상적인 주문 생성")
    void createOrder_ValidRequest_Success() {
        // given
        Long userId = 1L;
        Long productId = 1L;
        Integer quantity = 2;
        Long userCouponId = null;

        User user = new User("사용자1", "user1@test.com");
        user.setId(userId);

        Product product = new Product("상품1", "상품1 설명", BigDecimal.valueOf(10000), 10, "전자제품");
        product.setId(productId);
        product.setStatus(Product.ProductStatus.ACTIVE);

        Balance balance = new Balance(userId);
        balance.setAmount(BigDecimal.valueOf(50000));

        Order order = new Order(userId, Arrays.asList(), BigDecimal.valueOf(20000), userCouponId);
        order.setId(1L);

        List<CreateOrderUseCase.OrderItemRequest> orderItems = Arrays.asList(
                new CreateOrderUseCase.OrderItemRequest(productId, quantity)
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(chargeBalanceUseCase.execute(eq(userId), any(BigDecimal.class))).thenReturn(balance);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // when
        Order result = createOrderUseCase.execute(userId, orderItems, userCouponId);

        // then
        assertThat(result).isNotNull();
        verify(userRepository).findById(userId);
        verify(productRepository).findById(productId);
        verify(productRepository).save(any(Product.class));
        verify(chargeBalanceUseCase).execute(eq(userId), any(BigDecimal.class));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 주문 시 예외 발생")
    void createOrder_NonExistentUser_ThrowsException() {
        // given
        Long userId = 999L;
        Long productId = 1L;
        Integer quantity = 2;

        List<CreateOrderUseCase.OrderItemRequest> orderItems = Arrays.asList(
                new CreateOrderUseCase.OrderItemRequest(productId, quantity)
        );

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> createOrderUseCase.execute(userId, orderItems, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");

        verify(userRepository).findById(userId);
        verifyNoInteractions(productRepository, chargeBalanceUseCase, orderRepository);
    }

    @Test
    @DisplayName("존재하지 않는 상품으로 주문 시 예외 발생")
    void createOrder_NonExistentProduct_ThrowsException() {
        // given
        Long userId = 1L;
        Long productId = 999L;
        Integer quantity = 2;

        User user = new User("사용자1", "user1@test.com");
        user.setId(userId);

        List<CreateOrderUseCase.OrderItemRequest> orderItems = Arrays.asList(
                new CreateOrderUseCase.OrderItemRequest(productId, quantity)
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> createOrderUseCase.execute(userId, orderItems, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 상품입니다: " + productId);

        verify(userRepository).findById(userId);
        verify(productRepository).findById(productId);
        verifyNoInteractions(chargeBalanceUseCase, orderRepository);
    }

    @Test
    @DisplayName("재고 부족으로 주문 시 예외 발생")
    void createOrder_InsufficientStock_ThrowsException() {
        // given
        Long userId = 1L;
        Long productId = 1L;
        Integer quantity = 15; // 재고보다 많은 수량

        User user = new User("사용자1", "user1@test.com");
        user.setId(userId);

        Product product = new Product("상품1", "상품1 설명", BigDecimal.valueOf(10000), 10, "전자제품");
        product.setId(productId);
        product.setStatus(Product.ProductStatus.ACTIVE);

        List<CreateOrderUseCase.OrderItemRequest> orderItems = Arrays.asList(
                new CreateOrderUseCase.OrderItemRequest(productId, quantity)
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> createOrderUseCase.execute(userId, orderItems, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("재고가 부족합니다: " + product.getName());

        verify(userRepository).findById(userId);
        verify(productRepository).findById(productId);
        verifyNoInteractions(chargeBalanceUseCase, orderRepository);
    }

    @Test
    @DisplayName("비활성 상품으로 주문 시 예외 발생")
    void createOrder_InactiveProduct_ThrowsException() {
        // given
        Long userId = 1L;
        Long productId = 1L;
        Integer quantity = 2;

        User user = new User("사용자1", "user1@test.com");
        user.setId(userId);

        Product product = new Product("상품1", "상품1 설명", BigDecimal.valueOf(10000), 10, "전자제품");
        product.setId(productId);
        product.setStatus(Product.ProductStatus.INACTIVE);

        List<CreateOrderUseCase.OrderItemRequest> orderItems = Arrays.asList(
                new CreateOrderUseCase.OrderItemRequest(productId, quantity)
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> createOrderUseCase.execute(userId, orderItems, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 상품입니다: " + product.getName());

        verify(userRepository).findById(userId);
        verify(productRepository).findById(productId);
        verifyNoInteractions(chargeBalanceUseCase, orderRepository);
    }

    @Test
    @DisplayName("여러 상품 주문")
    void createOrder_MultipleProducts_Success() {
        // given
        Long userId = 1L;
        Long productId1 = 1L;
        Long productId2 = 2L;
        Integer quantity1 = 2;
        Integer quantity2 = 1;

        User user = new User("사용자1", "user1@test.com");
        user.setId(userId);

        Product product1 = new Product("상품1", "상품1 설명", BigDecimal.valueOf(10000), 10, "전자제품");
        product1.setId(productId1);
        product1.setStatus(Product.ProductStatus.ACTIVE);

        Product product2 = new Product("상품2", "상품2 설명", BigDecimal.valueOf(20000), 5, "의류");
        product2.setId(productId2);
        product2.setStatus(Product.ProductStatus.ACTIVE);

        Balance balance = new Balance(userId);
        balance.setAmount(BigDecimal.valueOf(50000));

        Order order = new Order(userId, Arrays.asList(), BigDecimal.valueOf(40000), null);
        order.setId(1L);

        List<CreateOrderUseCase.OrderItemRequest> orderItems = Arrays.asList(
                new CreateOrderUseCase.OrderItemRequest(productId1, quantity1),
                new CreateOrderUseCase.OrderItemRequest(productId2, quantity2)
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId1)).thenReturn(Optional.of(product1));
        when(productRepository.findById(productId2)).thenReturn(Optional.of(product2));
        when(productRepository.save(any(Product.class))).thenReturn(product1, product2);
        when(chargeBalanceUseCase.execute(eq(userId), any(BigDecimal.class))).thenReturn(balance);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // when
        Order result = createOrderUseCase.execute(userId, orderItems, null);

        // then
        assertThat(result).isNotNull();
        verify(userRepository).findById(userId);
        verify(productRepository).findById(productId1);
        verify(productRepository).findById(productId2);
        verify(productRepository, times(2)).save(any(Product.class));
        verify(chargeBalanceUseCase).execute(eq(userId), any(BigDecimal.class));
        verify(orderRepository).save(any(Order.class));
    }
} 