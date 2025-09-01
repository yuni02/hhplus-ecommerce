package kr.hhplus.be.server.unit.order.adapter.in.web;

import kr.hhplus.be.server.order.application.CreateOrderService;
import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.order.adapter.in.web.OrderController;
import kr.hhplus.be.server.order.adapter.in.dto.OrderRequest;
import kr.hhplus.be.server.shared.exception.GlobalExceptionHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @MockBean
    private CreateOrderService createOrderService;

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("주문 생성 성공 - 쿠폰 없이")
    void createOrder_Success_WithoutCoupon() throws Exception {
        // given
        Long userId = 1L;
        Long productId = 1L;
        Integer quantity = 2;
        OrderRequest.OrderItemRequest orderItem = new OrderRequest.OrderItemRequest(productId, quantity);
        OrderRequest request = new OrderRequest(userId, List.of(orderItem), null);

        CreateOrderUseCase.OrderItemResult orderItemResult = new CreateOrderUseCase.OrderItemResult(
                1L, productId, "상품명", quantity, BigDecimal.valueOf(10000), BigDecimal.valueOf(20000));
        CreateOrderUseCase.CreateOrderResult result = CreateOrderUseCase.CreateOrderResult.success(     
                1L, userId, null, BigDecimal.valueOf(20000), BigDecimal.valueOf(20000), 
                BigDecimal.ZERO, BigDecimal.valueOf(20000), "COMPLETED", List.of(orderItemResult), LocalDateTime.now());

        when(createOrderService.createOrder(any(CreateOrderUseCase.CreateOrderCommand.class)))
                .thenReturn(result);

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.totalAmount").value(20000))
                .andExpect(jsonPath("$.discountAmount").value(0))
                .andExpect(jsonPath("$.discountedAmount").value(20000));

        verify(createOrderService).createOrder(any(CreateOrderUseCase.CreateOrderCommand.class));
    }

    @Test
    @DisplayName("주문 생성 성공 - 쿠폰 사용")
    void createOrder_Success_WithCoupon() throws Exception {
        // given
        Long userId = 1L;
        Long productId = 1L;
        Integer quantity = 2;
        Long userCouponId = 1L;
        OrderRequest.OrderItemRequest orderItem = new OrderRequest.OrderItemRequest(productId, quantity);
        OrderRequest request = new OrderRequest(userId, List.of(orderItem), userCouponId);

        CreateOrderUseCase.OrderItemResult orderItemResult = new CreateOrderUseCase.OrderItemResult(
                1L, productId, "상품명", quantity, BigDecimal.valueOf(10000), BigDecimal.valueOf(20000));
        CreateOrderUseCase.CreateOrderResult result = CreateOrderUseCase.CreateOrderResult.success(
                1L, userId, userCouponId, BigDecimal.valueOf(20000), BigDecimal.valueOf(19000), 
                BigDecimal.valueOf(1000), BigDecimal.valueOf(19000), "COMPLETED", List.of(orderItemResult), LocalDateTime.now());    

        when(createOrderService.createOrder(any(CreateOrderUseCase.CreateOrderCommand.class)))
                .thenReturn(result);

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.userCouponId").value(userCouponId))
                .andExpect(jsonPath("$.totalAmount").value(20000))
                .andExpect(jsonPath("$.discountAmount").value(1000))
                .andExpect(jsonPath("$.discountedAmount").value(19000));

        verify(createOrderService).createOrder(any(CreateOrderUseCase.CreateOrderCommand.class));
    }

    @Test
    @DisplayName("주문 생성 실패 - 사용자가 존재하지 않는 경우")
    void createOrder_Failure_UserNotFound() throws Exception {
        // given
        Long userId = 999L;
        Long productId = 1L;
        Integer quantity = 2;
        OrderRequest.OrderItemRequest orderItem = new OrderRequest.OrderItemRequest(productId, quantity);
        OrderRequest request = new OrderRequest(userId, List.of(orderItem), null);

        CreateOrderUseCase.CreateOrderResult result = CreateOrderUseCase.CreateOrderResult
                .failure("사용자를 찾을 수 없습니다.");

        when(createOrderService.createOrder(any(CreateOrderUseCase.CreateOrderCommand.class)))
                .thenReturn(result);

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));

        verify(createOrderService).createOrder(any(CreateOrderUseCase.CreateOrderCommand.class));
    }

    @Test
    @DisplayName("주문 생성 실패 - 상품이 존재하지 않는 경우")
    void createOrder_Failure_ProductNotFound() throws Exception {
        // given
        Long userId = 1L;
        Long productId = 999L;
        Integer quantity = 2;
        OrderRequest.OrderItemRequest orderItem = new OrderRequest.OrderItemRequest(productId, quantity);
        OrderRequest request = new OrderRequest(userId, List.of(orderItem), null);

        CreateOrderUseCase.CreateOrderResult result = CreateOrderUseCase.CreateOrderResult
                .failure("상품을 찾을 수 없습니다: " + productId);

        when(createOrderService.createOrder(any(CreateOrderUseCase.CreateOrderCommand.class)))
                .thenReturn(result);

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("상품을 찾을 수 없습니다: " + productId));

        verify(createOrderService).createOrder(any(CreateOrderUseCase.CreateOrderCommand.class));
    }

    @Test
    @DisplayName("주문 생성 실패 - 재고 부족")
    void createOrder_Failure_InsufficientStock() throws Exception {
        // given
        Long userId = 1L;
        Long productId = 1L;
        Integer quantity = 100;
        OrderRequest.OrderItemRequest orderItem = new OrderRequest.OrderItemRequest(productId, quantity);
        OrderRequest request = new OrderRequest(userId, List.of(orderItem), null);

        CreateOrderUseCase.CreateOrderResult result = CreateOrderUseCase.CreateOrderResult
                .failure("재고가 부족합니다: 10");

        when(createOrderService.createOrder(any(CreateOrderUseCase.CreateOrderCommand.class)))
                .thenReturn(result);

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("재고가 부족합니다: 10"));

        verify(createOrderService).createOrder(any(CreateOrderUseCase.CreateOrderCommand.class));
    }

    @Test
    @DisplayName("주문 생성 실패 - 잘못된 요청")
    void createOrder_Failure_InvalidRequest() throws Exception {
        // given
        String invalidJson = "{\"userId\": \"invalid\", \"orderItems\": []}";

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("주문 생성 실패 - 서버 오류")
    void createOrder_Failure_ServerError() throws Exception {
        // given
        Long userId = 1L;
        Long productId = 1L;
        Integer quantity = 2;
        OrderRequest.OrderItemRequest orderItem = new OrderRequest.OrderItemRequest(productId, quantity);
        OrderRequest request = new OrderRequest(userId, List.of(orderItem), null);

        CreateOrderUseCase.CreateOrderResult result = CreateOrderUseCase.CreateOrderResult
                .failure("주문 생성 중 오류가 발생했습니다.");

        when(createOrderService.createOrder(any(CreateOrderUseCase.CreateOrderCommand.class)))
                .thenReturn(result);

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("주문 생성 중 오류가 발생했습니다."));

        verify(createOrderService).createOrder(any(CreateOrderUseCase.CreateOrderCommand.class));
    }
} 