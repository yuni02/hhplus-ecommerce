package kr.hhplus.be.server.order.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.order.adapter.in.dto.OrderRequest;
import kr.hhplus.be.server.order.application.facade.OrderFacade;
import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderFacade orderFacade;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderFacade))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("주문 생성 성공 - 쿠폰 없이")
    void createOrder_Success_WithoutCoupon() throws Exception {
        // given
        Long userId = 1L;
        Long productId = 1L;
        Integer quantity = 2;
        Long userCouponId = null;
        
        OrderRequest request = new OrderRequest();
        request.setUserId(userId);
        request.setUserCouponId(userCouponId);
        request.setOrderItems(List.of(new OrderRequest.OrderItemRequest(productId, quantity)));
        
        Long orderId = 1L;
        BigDecimal totalAmount = BigDecimal.valueOf(20000);
        BigDecimal discountedAmount = BigDecimal.valueOf(20000);
        String status = "COMPLETED";
        LocalDateTime createdAt = LocalDateTime.now();
        
        CreateOrderUseCase.OrderItemResult orderItemResult = 
            new CreateOrderUseCase.OrderItemResult(1L, productId, "테스트 상품", quantity, BigDecimal.valueOf(10000), BigDecimal.valueOf(20000));
        
        CreateOrderUseCase.CreateOrderResult result = CreateOrderUseCase.CreateOrderResult.success(
                orderId, userId, userCouponId, totalAmount, discountedAmount, status, List.of(orderItemResult), createdAt);
        
        when(orderFacade.createOrder(any(CreateOrderUseCase.CreateOrderCommand.class)))
            .thenReturn(result);

        // when & then
        mockMvc.perform(post("/api/orders")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.userCouponId").isEmpty())
                .andExpect(jsonPath("$.totalPrice").value(20000))
                .andExpect(jsonPath("$.discountedPrice").value(20000))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.orderItems").isArray())
                .andExpect(jsonPath("$.orderItems[0].id").value(1L))
                .andExpect(jsonPath("$.orderItems[0].productId").value(productId))
                .andExpect(jsonPath("$.orderItems[0].productName").value("테스트 상품"))
                .andExpect(jsonPath("$.orderItems[0].quantity").value(quantity))
                .andExpect(jsonPath("$.orderItems[0].unitPrice").value(10000))
                .andExpect(jsonPath("$.orderItems[0].totalPrice").value(20000));
    }

    @Test
    @DisplayName("주문 생성 성공 - 쿠폰 사용")
    void createOrder_Success_WithCoupon() throws Exception {
        // given
        Long userId = 1L;
        Long productId = 1L;
        Integer quantity = 2;
        Long userCouponId = 1L;
        
        OrderRequest request = new OrderRequest();
        request.setUserId(userId);
        request.setUserCouponId(userCouponId);
        request.setOrderItems(List.of(new OrderRequest.OrderItemRequest(productId, quantity)));
        
        Long orderId = 1L;
        BigDecimal totalAmount = BigDecimal.valueOf(20000);
        BigDecimal discountedAmount = BigDecimal.valueOf(18000);
        String status = "COMPLETED";
        LocalDateTime createdAt = LocalDateTime.now();
        
        CreateOrderUseCase.OrderItemResult orderItemResult = 
            new CreateOrderUseCase.OrderItemResult(1L, productId, "테스트 상품", quantity, BigDecimal.valueOf(10000), BigDecimal.valueOf(20000));
        
        CreateOrderUseCase.CreateOrderResult result = CreateOrderUseCase.CreateOrderResult.success(
                orderId, userId, userCouponId, totalAmount, discountedAmount, status, List.of(orderItemResult), createdAt);
        
        when(orderFacade.createOrder(any(CreateOrderUseCase.CreateOrderCommand.class)))
            .thenReturn(result);

        // when & then
        mockMvc.perform(post("/api/orders")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.userCouponId").value(userCouponId))
                .andExpect(jsonPath("$.totalPrice").value(20000))
                .andExpect(jsonPath("$.discountedPrice").value(18000))
                .andExpect(jsonPath("$.status").value(status));
    }

    @Test
    @DisplayName("주문 생성 실패 - 사용자가 존재하지 않는 경우")
    void createOrder_Failure_UserNotFound() throws Exception {
        // given
        Long userId = 999L;
        Long productId = 1L;
        Integer quantity = 2;
        
        OrderRequest request = new OrderRequest();
        request.setUserId(userId);
        request.setUserCouponId(null);
        request.setOrderItems(List.of(new OrderRequest.OrderItemRequest(productId, quantity)));
        
        CreateOrderUseCase.CreateOrderResult result = 
            CreateOrderUseCase.CreateOrderResult.failure("사용자를 찾을 수 없습니다.");
        
        when(orderFacade.createOrder(any(CreateOrderUseCase.CreateOrderCommand.class)))
            .thenReturn(result);

        // when & then
        mockMvc.perform(post("/api/orders")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("주문 생성 실패 - 상품이 존재하지 않는 경우")
    void createOrder_Failure_ProductNotFound() throws Exception {
        // given
        Long userId = 1L;
        Long productId = 999L;
        Integer quantity = 2;
        
        OrderRequest request = new OrderRequest();
        request.setUserId(userId);
        request.setUserCouponId(null);
        request.setOrderItems(List.of(new OrderRequest.OrderItemRequest(productId, quantity)));
        
        CreateOrderUseCase.CreateOrderResult result = 
            CreateOrderUseCase.CreateOrderResult.failure("상품을 찾을 수 없습니다.");
        
        when(orderFacade.createOrder(any(CreateOrderUseCase.CreateOrderCommand.class)))
            .thenReturn(result);

        // when & then
        mockMvc.perform(post("/api/orders")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("상품을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("주문 생성 실패 - 재고 부족")
    void createOrder_Failure_InsufficientStock() throws Exception {
        // given
        Long userId = 1L;
        Long productId = 1L;
        Integer quantity = 1000;
        
        OrderRequest request = new OrderRequest();
        request.setUserId(userId);
        request.setUserCouponId(null);
        request.setOrderItems(List.of(new OrderRequest.OrderItemRequest(productId, quantity)));
        
        CreateOrderUseCase.CreateOrderResult result = 
            CreateOrderUseCase.CreateOrderResult.failure("재고가 부족합니다.");
        
        when(orderFacade.createOrder(any(CreateOrderUseCase.CreateOrderCommand.class)))
            .thenReturn(result);

        // when & then
        mockMvc.perform(post("/api/orders")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("재고가 부족합니다."));
    }

    @Test
    @DisplayName("주문 생성 실패 - 잘못된 요청")
    void createOrder_Failure_InvalidRequest() throws Exception {
        // when & then
        mockMvc.perform(post("/api/orders")
                        .content("invalid json")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("잘못된 JSON 형식입니다."));
    }

    @Test
    @DisplayName("주문 생성 실패 - 서버 오류")
    void createOrder_Failure_ServerError() throws Exception {
        // given
        Long userId = 1L;
        Long productId = 1L;
        Integer quantity = 2;
        
        OrderRequest request = new OrderRequest();
        request.setUserId(userId);
        request.setUserCouponId(null);
        request.setOrderItems(List.of(new OrderRequest.OrderItemRequest(productId, quantity)));
        
        when(orderFacade.createOrder(any(CreateOrderUseCase.CreateOrderCommand.class)))
            .thenThrow(new RuntimeException("데이터베이스 오류"));

        // when & then
        mockMvc.perform(post("/api/orders")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다: 데이터베이스 오류"));
    }
} 