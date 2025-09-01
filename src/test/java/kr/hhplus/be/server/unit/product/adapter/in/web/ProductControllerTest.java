package kr.hhplus.be.server.unit.product.adapter.in.web;

import kr.hhplus.be.server.product.adapter.in.web.ProductController;
import kr.hhplus.be.server.product.application.port.in.GetProductDetailUseCase;
import kr.hhplus.be.server.product.application.port.in.GetPopularProductsUseCase;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @MockBean
    private GetProductDetailUseCase getProductDetailUseCase;
    
    @MockBean
    private GetPopularProductsUseCase getPopularProductsUseCase;

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("상품 상세 조회 성공")
    void getProductDetail_Success() throws Exception {
        // given
        Long productId = 1L;
        GetProductDetailUseCase.GetProductDetailResult result = 
            new GetProductDetailUseCase.GetProductDetailResult(
                productId, "상품명", 10000, 100, "ACTIVE", LocalDateTime.now(), LocalDateTime.now());

        when(getProductDetailUseCase.getProductDetail(any(GetProductDetailUseCase.GetProductDetailCommand.class)))
                .thenReturn(Optional.of(result));

        // when & then
        mockMvc.perform(get("/api/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value("상품명"))
                .andExpect(jsonPath("$.currentPrice").value(10000))
                .andExpect(jsonPath("$.stock").value(100));

        verify(getProductDetailUseCase).getProductDetail(any(GetProductDetailUseCase.GetProductDetailCommand.class));
    }

    @Test
    @DisplayName("상품 상세 조회 실패 - 상품이 존재하지 않는 경우")
    void getProductDetail_Failure_ProductNotFound() throws Exception {
        // given
        Long productId = 999L;

        when(getProductDetailUseCase.getProductDetail(any(GetProductDetailUseCase.GetProductDetailCommand.class)))
                .thenReturn(Optional.empty());

        // when & then
        mockMvc.perform(get("/api/products/{productId}", productId))
                .andExpect(status().isNotFound());

        verify(getProductDetailUseCase).getProductDetail(any(GetProductDetailUseCase.GetProductDetailCommand.class));
    }

    @Test
    @DisplayName("상품 상세 조회 실패 - 잘못된 요청")
    void getProductDetail_Failure_InvalidRequest() throws Exception {
        // when & then
        mockMvc.perform(get("/api/products/{productId}", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("상품 상세 조회 실패 - 서버 오류")
    void getProductDetail_Failure_ServerError() throws Exception {
        // given
        Long productId = 1L;
        when(getProductDetailUseCase.getProductDetail(any(GetProductDetailUseCase.GetProductDetailCommand.class)))
                .thenThrow(new RuntimeException("서버 오류"));

        // when & then
        mockMvc.perform(get("/api/products/{productId}", productId))
                .andExpect(status().isInternalServerError());

        verify(getProductDetailUseCase).getProductDetail(any(GetProductDetailUseCase.GetProductDetailCommand.class));
    }

    @Test
    @DisplayName("인기 상품 조회 성공")
    void getPopularProducts_Success() throws Exception {
        // given
        GetPopularProductsUseCase.PopularProductInfo popularProduct = 
            new GetPopularProductsUseCase.PopularProductInfo(
                1L, "인기상품", 20000, 50, 100, 2000000L, 30, 600000L, 0.8, LocalDateTime.now(), 1);
        GetPopularProductsUseCase.GetPopularProductsResult result = 
            new GetPopularProductsUseCase.GetPopularProductsResult(List.of(popularProduct));

        when(getPopularProductsUseCase.getPopularProducts(any(GetPopularProductsUseCase.GetPopularProductsCommand.class)))
                .thenReturn(result);

        // when & then
        mockMvc.perform(get("/api/products/popular"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].productId").value(1L))
                .andExpect(jsonPath("$[0].productName").value("인기상품"))
                .andExpect(jsonPath("$[0].rank").value(1));

        verify(getPopularProductsUseCase).getPopularProducts(any(GetPopularProductsUseCase.GetPopularProductsCommand.class));
    }

    @Test
    @DisplayName("인기 상품 조회 성공 - 빈 결과")
    void getPopularProducts_Success_EmptyResult() throws Exception {
        // given
        GetPopularProductsUseCase.GetPopularProductsResult result = 
            new GetPopularProductsUseCase.GetPopularProductsResult(List.of());

        when(getPopularProductsUseCase.getPopularProducts(any(GetPopularProductsUseCase.GetPopularProductsCommand.class)))
                .thenReturn(result);

        // when & then
        mockMvc.perform(get("/api/products/popular"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(getPopularProductsUseCase).getPopularProducts(any(GetPopularProductsUseCase.GetPopularProductsCommand.class));
    }

    @Test
    @DisplayName("인기 상품 조회 실패 - 서버 오류")
    void getPopularProducts_Failure_ServerError() throws Exception {
        // given
        when(getPopularProductsUseCase.getPopularProducts(any(GetPopularProductsUseCase.GetPopularProductsCommand.class)))
                .thenThrow(new RuntimeException("서버 오류"));

        // when & then
        mockMvc.perform(get("/api/products/popular"))
                .andExpect(status().isInternalServerError());

        verify(getPopularProductsUseCase).getPopularProducts(any(GetPopularProductsUseCase.GetPopularProductsCommand.class));
    }
} 