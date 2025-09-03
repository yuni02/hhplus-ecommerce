package kr.hhplus.be.server.unit.product.domain;

import kr.hhplus.be.server.product.domain.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    @DisplayName("Product 생성 성공")
    void createProduct_Success() {
        // given
        String name = "테스트 상품";
        String description = "테스트용 상품 설명";
        BigDecimal price = new BigDecimal("25000");
        Integer stock = 100;
        String category = "전자제품";

        // when
        Product product = Product.builder()
                .name(name)
                .description(description)
                .currentPrice(price)
                .stock(stock)
                .category(category)
                .build();

        // then
        assertThat(product.getName()).isEqualTo(name);
        assertThat(product.getDescription()).isEqualTo(description);
        assertThat(product.getCurrentPrice()).isEqualTo(price);
        assertThat(product.getStock()).isEqualTo(stock);
        assertThat(product.getCategory()).isEqualTo(category);
        assertThat(product.getStatus()).isEqualTo(Product.ProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("상품 판매 가능 여부 확인 - 판매 가능")
    void isAvailable_True() {
        // given
        Product product = Product.builder()
                .name("테스트 상품")
                .currentPrice(new BigDecimal("15000"))
                .stock(10)
                .status(Product.ProductStatus.ACTIVE)
                .build();

        // when
        boolean isAvailable = product.isAvailable();

        // then
        assertThat(isAvailable).isTrue();
    }

    @Test
    @DisplayName("상품 판매 가능 여부 확인 - 재고 없음으로 판매 불가")
    void isAvailable_False_NoStock() {
        // given
        Product product = Product.builder()
                .name("테스트 상품")
                .currentPrice(new BigDecimal("15000"))
                .stock(0)
                .status(Product.ProductStatus.ACTIVE)
                .build();

        // when
        boolean isAvailable = product.isAvailable();

        // then
        assertThat(isAvailable).isFalse();
    }

    @Test
    @DisplayName("상품 판매 가능 여부 확인 - 비활성 상태로 판매 불가")
    void isAvailable_False_Inactive() {
        // given
        Product product = Product.builder()
                .name("테스트 상품")
                .currentPrice(new BigDecimal("15000"))
                .stock(10)
                .status(Product.ProductStatus.INACTIVE)
                .build();

        // when
        boolean isAvailable = product.isAvailable();

        // then
        assertThat(isAvailable).isFalse();
    }

    @Test
    @DisplayName("상품 판매 가능 여부 확인 - 품절 상태로 판매 불가")
    void isAvailable_False_SoldOut() {
        // given
        Product product = Product.builder()
                .name("테스트 상품")
                .currentPrice(new BigDecimal("15000"))
                .stock(5)
                .status(Product.ProductStatus.SOLD_OUT)
                .build();

        // when
        boolean isAvailable = product.isAvailable();

        // then
        assertThat(isAvailable).isFalse();
    }

    @Test
    @DisplayName("상품 재고 감소 성공")
    void decreaseStock_Success() {
        // given
        Product product = Product.builder()
                .name("테스트 상품")
                .currentPrice(new BigDecimal("15000"))
                .stock(10)
                .build();
        int decreaseQuantity = 3;

        // when
        product.decreaseStock(decreaseQuantity);

        // then
        assertThat(product.getStock()).isEqualTo(7);
        assertThat(product.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("상품 재고 감소 실패 - 재고 부족")
    void decreaseStock_Failed_InsufficientStock() {
        // given
        Product product = Product.builder()
                .name("테스트 상품")
                .currentPrice(new BigDecimal("15000"))
                .stock(5)
                .build();
        int decreaseQuantity = 10;

        // when & then
        assertThatThrownBy(() -> product.decreaseStock(decreaseQuantity))
                .isInstanceOf(Product.InsufficientStockException.class)
                .hasMessage("재고가 부족합니다. 현재 재고: 5, 요청 수량: 10");
        assertThat(product.getStock()).isEqualTo(5); // 재고 변경 없음
    }

    @Test
    @DisplayName("상품 재고 감소 실패 - 0 이하 수량")
    void decreaseStock_Failed_InvalidQuantity() {
        // given
        Product product = Product.builder()
                .name("테스트 상품")
                .currentPrice(new BigDecimal("15000"))
                .stock(10)
                .build();

        // when & then
        assertThatThrownBy(() -> product.decreaseStock(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("수량은 0보다 커야 합니다.");

        assertThatThrownBy(() -> product.decreaseStock(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("수량은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("상품 재고 증가 성공")
    void increaseStock_Success() {
        // given
        Product product = Product.builder()
                .name("테스트 상품")
                .currentPrice(new BigDecimal("15000"))
                .stock(5)
                .build();
        int increaseQuantity = 10;

        // when
        product.increaseStock(increaseQuantity);

        // then
        assertThat(product.getStock()).isEqualTo(15);
        assertThat(product.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("상품 재고 증가 실패 - 0 이하 수량")
    void increaseStock_Failed_InvalidQuantity() {
        // given
        Product product = Product.builder()
                .name("테스트 상품")
                .currentPrice(new BigDecimal("15000"))
                .stock(10)
                .build();

        // when & then
        assertThatThrownBy(() -> product.increaseStock(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("수량은 0보다 커야 합니다.");

        assertThatThrownBy(() -> product.increaseStock(-5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("수량은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("상품 재고 전부 소진")
    void decreaseStock_ToZero() {
        // given
        Product product = Product.builder()
                .name("테스트 상품")
                .currentPrice(new BigDecimal("15000"))
                .stock(5)
                .build();

        // when
        product.decreaseStock(5);

        // then
        assertThat(product.getStock()).isEqualTo(0);
        assertThat(product.isAvailable()).isFalse(); // 재고가 0이므로 판매 불가
    }

    @Test
    @DisplayName("Product ID 설정 테스트")
    void productWithId() {
        // given
        Product product = Product.builder()
                .id(100L)
                .name("ID 설정 테스트 상품")
                .currentPrice(new BigDecimal("20000"))
                .stock(15)
                .build();

        // then
        assertThat(product.getId()).isEqualTo(100L);
        assertThat(product.getName()).isEqualTo("ID 설정 테스트 상품");
        assertThat(product.getCurrentPrice()).isEqualTo(new BigDecimal("20000"));
        assertThat(product.getStock()).isEqualTo(15);
    }

    @Test
    @DisplayName("Product 생성일시 및 수정일시 설정")
    void productWithTimestamps() {
        // given
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();

        // when
        Product product = Product.builder()
                .name("시간 설정 테스트 상품")
                .currentPrice(new BigDecimal("12000"))
                .stock(8)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        // then
        assertThat(product.getCreatedAt()).isEqualTo(createdAt);
        assertThat(product.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("ProductStatus enum 검증")
    void productStatusEnum() {
        // then
        assertThat(Product.ProductStatus.values()).containsExactly(
                Product.ProductStatus.ACTIVE,
                Product.ProductStatus.INACTIVE,
                Product.ProductStatus.SOLD_OUT
        );
    }

    @Test
    @DisplayName("상품 재고 연속 증감 테스트")
    void stockOperations_Multiple() {
        // given
        Product product = Product.builder()
                .name("재고 연산 테스트 상품")
                .currentPrice(new BigDecimal("18000"))
                .stock(20)
                .build();

        // when & then
        product.decreaseStock(5);
        assertThat(product.getStock()).isEqualTo(15);

        product.increaseStock(10);
        assertThat(product.getStock()).isEqualTo(25);

        product.decreaseStock(3);
        assertThat(product.getStock()).isEqualTo(22);

        assertThat(product.isAvailable()).isTrue();
    }
}