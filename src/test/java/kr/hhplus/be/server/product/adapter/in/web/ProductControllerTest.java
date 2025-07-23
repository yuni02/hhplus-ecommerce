package kr.hhplus.be.server.product.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.product.application.facade.ProductFacade;
import kr.hhplus.be.server.product.application.port.in.GetPopularProductsUseCase;
import kr.hhplus.be.server.product.application.port.in.GetProductDetailUseCase;
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
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(productFacade)).build();
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
        
        GetProductDetailUseCase.GetProductDetailCommand command = 
            new GetProductDetailUseCase.GetProductDetailCommand(productId);
        GetProductDetailUseCase.GetProductDetailResult result = 
            new GetProductDetailUseCase.GetProductDetailResult(productId, productName, currentPrice, stock, status, createdAt, updatedAt);
        
        when(productFacade.getProductDetail(command)).thenReturn(Optional.of(result));

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
        
        GetProductDetailUseCase.GetProductDetailCommand command = 
            new GetProductDetailUseCase.GetProductDetailCommand(productId);
        
        when(productFacade.getProductDetail(command)).thenReturn(Optional.empty());

        // when & then
        mockMvc.perform(get("/api/products/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("상품 상세 조회 실패 - 잘못된 요청")
    void getProductDetail_Failure_InvalidRequest() throws Exception {
        // given
        Long productId = -1L;
        
        when(productFacade.getProductDetail(any())).thenThrow(new IllegalArgumentException("잘못된 상품 ID입니다."));

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
        
        when(productFacade.getProductDetail(any())).thenThrow(new RuntimeException("데이터베이스 연결 오류"));

        // when & then
        mockMvc.perform(get("/api/products/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("상품 조회 중 오류가 발생했습니다: 데이터베이스 연결 오류"));
    }

    @Test
    @DisplayName("인기 상품 조회 성공")
    void getPopularProducts_Success() throws Exception {
        // given
        int limit = 5;
        
        GetPopularProductsUseCase.GetPopularProductsCommand command = 
            new GetPopularProductsUseCase.GetPopularProductsCommand(limit);
        
        GetPopularProductsUseCase.PopularProductInfo productInfo1 = 
            new GetPopularProductsUseCase.PopularProductInfo(1L, "인기 상품 1", 10000, 100, 200, 2000000L, 20, 200000L, 0.9, LocalDateTime.now(), 1);
        GetPopularProductsUseCase.PopularProductInfo productInfo2 = 
            new GetPopularProductsUseCase.PopularProductInfo(2L, "인기 상품 2", 8000, 80, 150, 1500000L, 15, 150000L, 0.8, LocalDateTime.now(), 2);
        
        GetPopularProductsUseCase.GetPopularProductsResult result = 
            new GetPopularProductsUseCase.GetPopularProductsResult(List.of(productInfo1, productInfo2));
        
        when(productFacade.getPopularProducts(command)).thenReturn(result);

        // when & then
        mockMvc.perform(get("/api/products/popular")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].productId").value(1L))
                .andExpect(jsonPath("$[0].productName").value("인기 상품 1"))
                .andExpect(jsonPath("$[0].currentPrice").value(10000))
                .andExpect(jsonPath("$[0].stock").value(100))
                .andExpect(jsonPath("$[0].totalSalesCount").value(200))
                .andExpect(jsonPath("$[0].totalSalesAmount").value(2000000L))
                .andExpect(jsonPath("$[0].recentSalesCount").value(20))
                .andExpect(jsonPath("$[0].recentSalesAmount").value(200000L))
                .andExpect(jsonPath("$[0].conversionRate").value(0.9))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[1].productId").value(2L))
                .andExpect(jsonPath("$[1].productName").value("인기 상품 2"))
                .andExpect(jsonPath("$[1].currentPrice").value(8000))
                .andExpect(jsonPath("$[1].stock").value(80))
                .andExpect(jsonPath("$[1].totalSalesCount").value(150))
                .andExpect(jsonPath("$[1].totalSalesAmount").value(1500000L))
                .andExpect(jsonPath("$[1].recentSalesCount").value(15))
                .andExpect(jsonPath("$[1].recentSalesAmount").value(150000L))
                .andExpect(jsonPath("$[1].conversionRate").value(0.8))
                .andExpect(jsonPath("$[1].rank").value(2));
    }

    @Test
    @DisplayName("인기 상품 조회 성공 - 빈 결과")
    void getPopularProducts_Success_EmptyResult() throws Exception {
        // given
        int limit = 5;
        
        GetPopularProductsUseCase.GetPopularProductsCommand command = 
            new GetPopularProductsUseCase.GetPopularProductsCommand(limit);
        GetPopularProductsUseCase.GetPopularProductsResult result = 
            new GetPopularProductsUseCase.GetPopularProductsResult(List.of());
        
        when(productFacade.getPopularProducts(command)).thenReturn(result);

        // when & then
        mockMvc.perform(get("/api/products/popular")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("인기 상품 조회 실패 - 서버 오류")
    void getPopularProducts_Failure_ServerError() throws Exception {
        // given
        when(productFacade.getPopularProducts(any())).thenThrow(new RuntimeException("데이터베이스 연결 오류"));

        // when & then
        mockMvc.perform(get("/api/products/popular")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("인기 상품 조회 중 오류가 발생했습니다: 데이터베이스 연결 오류"));
    }
} 