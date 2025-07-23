package kr.hhplus.be.server.product.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.product.application.facade.ProductFacade;
import kr.hhplus.be.server.product.application.port.in.GetPopularProductsUseCase;
import kr.hhplus.be.server.product.application.port.in.GetProductDetailUseCase;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductFacade productFacade;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(productFacade))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("상품 상세 조회 성공")
    void getProductDetail_Success() throws Exception {
        // given
        Long productId = 1L;
        String productName = "테스트 상품";
        Integer currentPrice = 10000;
        Integer stock = 50;
        String status = "ACTIVE";
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        
        GetProductDetailUseCase.GetProductDetailResult result = 
            new GetProductDetailUseCase.GetProductDetailResult(productId, productName, currentPrice, stock, status, createdAt, updatedAt);
        
        when(productFacade.getProductDetail(any(GetProductDetailUseCase.GetProductDetailCommand.class)))
            .thenReturn(Optional.of(result));

        // when & then
        mockMvc.perform(get("/api/products/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value(productName))
                .andExpect(jsonPath("$.currentPrice").value(currentPrice))
                .andExpect(jsonPath("$.stock").value(stock))
                .andExpect(jsonPath("$.status").value(status));
    }

    @Test
    @DisplayName("상품 상세 조회 실패 - 상품이 존재하지 않는 경우")
    void getProductDetail_Failure_ProductNotFound() throws Exception {
        // given
        Long productId = 999L;
        
        when(productFacade.getProductDetail(any(GetProductDetailUseCase.GetProductDetailCommand.class)))
            .thenReturn(Optional.empty());

        // when & then
        mockMvc.perform(get("/api/products/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("상품 상세 조회 실패 - 잘못된 요청")
    void getProductDetail_Failure_InvalidRequest() throws Exception {
        // given
        Long productId = 1L;
        
        when(productFacade.getProductDetail(any(GetProductDetailUseCase.GetProductDetailCommand.class)))
            .thenThrow(new IllegalArgumentException("잘못된 상품 ID입니다."));

        // when & then
        mockMvc.perform(get("/api/products/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("잘못된 상품 ID입니다."));
    }

    @Test
    @DisplayName("상품 상세 조회 실패 - 서버 오류")
    void getProductDetail_Failure_ServerError() throws Exception {
        // given
        Long productId = 1L;
        
        when(productFacade.getProductDetail(any(GetProductDetailUseCase.GetProductDetailCommand.class)))
            .thenThrow(new RuntimeException("데이터베이스 오류"));

        // when & then
        mockMvc.perform(get("/api/products/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다: 데이터베이스 오류"));
    }

    @Test
    @DisplayName("인기 상품 조회 성공")
    void getPopularProducts_Success() throws Exception {
        // given
        GetPopularProductsUseCase.GetPopularProductsResult result = 
            new GetPopularProductsUseCase.GetPopularProductsResult(List.of());
        
        when(productFacade.getPopularProducts(any(GetPopularProductsUseCase.GetPopularProductsCommand.class)))
            .thenReturn(result);

        // when & then
        mockMvc.perform(get("/api/products/popular")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("인기 상품 조회 성공 - 빈 결과")
    void getPopularProducts_Success_EmptyResult() throws Exception {
        // given
        GetPopularProductsUseCase.GetPopularProductsResult result = 
            new GetPopularProductsUseCase.GetPopularProductsResult(List.of());
        
        when(productFacade.getPopularProducts(any(GetPopularProductsUseCase.GetPopularProductsCommand.class)))
            .thenReturn(result);

        // when & then
        mockMvc.perform(get("/api/products/popular")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("인기 상품 조회 실패 - 서버 오류")
    void getPopularProducts_Failure_ServerError() throws Exception {
        // given
        when(productFacade.getPopularProducts(any(GetPopularProductsUseCase.GetPopularProductsCommand.class)))
            .thenThrow(new RuntimeException("데이터베이스 오류"));

        // when & then
        mockMvc.perform(get("/api/products/popular")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다: 데이터베이스 오류"));
    }
} 