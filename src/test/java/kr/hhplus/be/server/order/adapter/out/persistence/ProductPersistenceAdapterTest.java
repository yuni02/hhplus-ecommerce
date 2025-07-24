package kr.hhplus.be.server.order.adapter.out.persistence;

import kr.hhplus.be.server.order.application.port.out.LoadProductPort;
import kr.hhplus.be.server.order.application.port.out.UpdateProductStockPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ProductPersistenceAdapterTest {

    private ProductPersistenceAdapter productPersistenceAdapter;

    @BeforeEach
    void setUp() {
        productPersistenceAdapter = new ProductPersistenceAdapter();
    }

    @Test
    @DisplayName("상품 조회 - 정상 조회")
    void loadProductById_Success() {
        // given
        Long productId = 1L;

        // when
        Optional<LoadProductPort.ProductInfo> result = productPersistenceAdapter.loadProductById(productId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(productId);
        assertThat(result.get().getName()).isEqualTo("상품 1");
        assertThat(result.get().getCurrentPrice()).isEqualTo(BigDecimal.valueOf(11000));
        assertThat(result.get().getStock()).isEqualTo(100);
    }

    @Test
    @DisplayName("상품 조회 - 존재하지 않는 상품")
    void loadProductById_NotFound() {
        // given
        Long productId = 999L;

        // when
        Optional<LoadProductPort.ProductInfo> result = productPersistenceAdapter.loadProductById(productId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("재고 차감 - 정상 차감")
    void deductStock_Success() {
        // given
        Long productId = 1L;
        Integer quantity = 10;

        // when
        boolean result = productPersistenceAdapter.deductStock(productId, quantity);

        // then
        assertThat(result).isTrue();
        
        // 재고가 실제로 차감되었는지 확인
        Optional<LoadProductPort.ProductInfo> productInfo = productPersistenceAdapter.loadProductById(productId);
        assertThat(productInfo).isPresent();
        assertThat(productInfo.get().getStock()).isEqualTo(90); // 100 - 10
    }

    @Test
    @DisplayName("재고 차감 - 재고 부족")
    void deductStock_InsufficientStock() {
        // given
        Long productId = 1L;
        Integer quantity = 150; // 재고(100)보다 많은 수량

        // when
        boolean result = productPersistenceAdapter.deductStock(productId, quantity);

        // then
        assertThat(result).isFalse();
        
        // 재고가 변경되지 않았는지 확인
        Optional<LoadProductPort.ProductInfo> productInfo = productPersistenceAdapter.loadProductById(productId);
        assertThat(productInfo).isPresent();
        assertThat(productInfo.get().getStock()).isEqualTo(100); // 원래 재고 유지
    }

    @Test
    @DisplayName("재고 차감 - 존재하지 않는 상품")
    void deductStock_ProductNotFound() {
        // given
        Long productId = 999L;
        Integer quantity = 10;

        // when
        boolean result = productPersistenceAdapter.deductStock(productId, quantity);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("재고 복구 - 정상 복구")
    void restoreStock_Success() {
        // given
        Long productId = 1L;
        Integer quantity = 10;
        
        // 먼저 재고 차감
        productPersistenceAdapter.deductStock(productId, quantity);
        
        // 재고 차감 확인
        Optional<LoadProductPort.ProductInfo> beforeRestore = productPersistenceAdapter.loadProductById(productId);
        assertThat(beforeRestore).isPresent();
        assertThat(beforeRestore.get().getStock()).isEqualTo(90);

        // when
        boolean result = productPersistenceAdapter.restoreStock(productId, quantity);

        // then
        assertThat(result).isTrue();
        
        // 재고가 실제로 복구되었는지 확인
        Optional<LoadProductPort.ProductInfo> afterRestore = productPersistenceAdapter.loadProductById(productId);
        assertThat(afterRestore).isPresent();
        assertThat(afterRestore.get().getStock()).isEqualTo(100); // 90 + 10
    }

    @Test
    @DisplayName("재고 복구 - 존재하지 않는 상품")
    void restoreStock_ProductNotFound() {
        // given
        Long productId = 999L;
        Integer quantity = 10;

        // when
        boolean result = productPersistenceAdapter.restoreStock(productId, quantity);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("재고 복구 - 복구 후 재고 증가 확인")
    void restoreStock_StockIncreased() {
        // given
        Long productId = 1L;
        Integer originalStock = 100;
        Integer deductQuantity = 30;
        Integer restoreQuantity = 30;
        
        // 재고 차감
        productPersistenceAdapter.deductStock(productId, deductQuantity);
        
        // 재고 차감 확인
        Optional<LoadProductPort.ProductInfo> afterDeduct = productPersistenceAdapter.loadProductById(productId);
        assertThat(afterDeduct).isPresent();
        assertThat(afterDeduct.get().getStock()).isEqualTo(originalStock - deductQuantity);

        // when
        boolean result = productPersistenceAdapter.restoreStock(productId, restoreQuantity);

        // then
        assertThat(result).isTrue();
        
        // 재고가 원래대로 복구되었는지 확인
        Optional<LoadProductPort.ProductInfo> afterRestore = productPersistenceAdapter.loadProductById(productId);
        assertThat(afterRestore).isPresent();
        assertThat(afterRestore.get().getStock()).isEqualTo(originalStock);
    }

    @Test
    @DisplayName("재고 복구 - 부분 복구")
    void restoreStock_PartialRestore() {
        // given
        Long productId = 1L;
        Integer originalStock = 100;
        Integer deductQuantity = 50;
        Integer restoreQuantity = 20; // 일부만 복구
        
        // 재고 차감
        productPersistenceAdapter.deductStock(productId, deductQuantity);
        
        // 재고 차감 확인
        Optional<LoadProductPort.ProductInfo> afterDeduct = productPersistenceAdapter.loadProductById(productId);
        assertThat(afterDeduct).isPresent();
        assertThat(afterDeduct.get().getStock()).isEqualTo(originalStock - deductQuantity);

        // when
        boolean result = productPersistenceAdapter.restoreStock(productId, restoreQuantity);

        // then
        assertThat(result).isTrue();
        
        // 부분 복구 확인
        Optional<LoadProductPort.ProductInfo> afterRestore = productPersistenceAdapter.loadProductById(productId);
        assertThat(afterRestore).isPresent();
        assertThat(afterRestore.get().getStock()).isEqualTo(originalStock - deductQuantity + restoreQuantity);
    }

    @Test
    @DisplayName("재고 복구 - 복구 후 재고 초과 가능")
    void restoreStock_CanExceedOriginalStock() {
        // given
        Long productId = 1L;
        Integer originalStock = 100;
        Integer deductQuantity = 30;
        Integer restoreQuantity = 50; // 원래 재고보다 많이 복구
        
        // 재고 차감
        productPersistenceAdapter.deductStock(productId, deductQuantity);
        
        // 재고 차감 확인
        Optional<LoadProductPort.ProductInfo> afterDeduct = productPersistenceAdapter.loadProductById(productId);
        assertThat(afterDeduct).isPresent();
        assertThat(afterDeduct.get().getStock()).isEqualTo(originalStock - deductQuantity);

        // when
        boolean result = productPersistenceAdapter.restoreStock(productId, restoreQuantity);

        // then
        assertThat(result).isTrue();
        
        // 재고가 초과되어도 복구 가능한지 확인
        Optional<LoadProductPort.ProductInfo> afterRestore = productPersistenceAdapter.loadProductById(productId);
        assertThat(afterRestore).isPresent();
        assertThat(afterRestore.get().getStock()).isEqualTo(originalStock - deductQuantity + restoreQuantity);
        assertThat(afterRestore.get().getStock()).isGreaterThan(originalStock);
    }

    @Test
    @DisplayName("재고 차감 후 복구 - 전체 사이클 테스트")
    void deductAndRestoreStock_FullCycle() {
        // given
        Long productId = 1L;
        Integer originalStock = 100;
        Integer quantity = 25;
        
        // 초기 재고 확인
        Optional<LoadProductPort.ProductInfo> initial = productPersistenceAdapter.loadProductById(productId);
        assertThat(initial).isPresent();
        assertThat(initial.get().getStock()).isEqualTo(originalStock);

        // 1단계: 재고 차감
        boolean deductResult = productPersistenceAdapter.deductStock(productId, quantity);
        assertThat(deductResult).isTrue();
        
        Optional<LoadProductPort.ProductInfo> afterDeduct = productPersistenceAdapter.loadProductById(productId);
        assertThat(afterDeduct).isPresent();
        assertThat(afterDeduct.get().getStock()).isEqualTo(originalStock - quantity);

        // 2단계: 재고 복구
        boolean restoreResult = productPersistenceAdapter.restoreStock(productId, quantity);
        assertThat(restoreResult).isTrue();
        
        Optional<LoadProductPort.ProductInfo> afterRestore = productPersistenceAdapter.loadProductById(productId);
        assertThat(afterRestore).isPresent();
        assertThat(afterRestore.get().getStock()).isEqualTo(originalStock);

        // 3단계: 최종 확인
        assertThat(afterRestore.get().getStock()).isEqualTo(initial.get().getStock());
    }
} 