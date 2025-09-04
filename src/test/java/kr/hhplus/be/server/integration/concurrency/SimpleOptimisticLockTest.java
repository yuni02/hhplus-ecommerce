package kr.hhplus.be.server.integration.concurrency;

import kr.hhplus.be.server.order.infrastructure.persistence.adapter.ProductStockPersistenceAdapter;
import kr.hhplus.be.server.product.infrastructure.persistence.entity.ProductEntity;
import kr.hhplus.be.server.product.infrastructure.persistence.repository.ProductJpaRepository;
import kr.hhplus.be.server.TestcontainersConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 간단한 낙관적 락 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("간단한 낙관적 락 테스트")
public class SimpleOptimisticLockTest {

    @Autowired private ProductStockPersistenceAdapter productStockAdapter;
    @Autowired private ProductJpaRepository productRepository;

    private ProductEntity testProduct;

    @BeforeEach
    @Transactional
    void setUp() {
        productRepository.deleteAll();
        
        testProduct = ProductEntity.builder()
                .name("테스트상품")
                .description("테스트용 상품")
                .price(BigDecimal.valueOf(10000))
                .stockQuantity(100)
                .status("ACTIVE")
                .build();
        productRepository.save(testProduct);
    }

    @Test
    @DisplayName("단일 재고 차감 테스트")
    void singleStockDeduction() {
        // Given
        int quantity = 5;

        // When
        boolean result = productStockAdapter.deductStock(testProduct.getId(), quantity);

        // Then
        assertThat(result).isTrue();
        
        ProductEntity updatedProduct = productRepository.findById(testProduct.getId()).get();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(95);
    }

    @Test
    @DisplayName("재고 부족 시 차감 실패 테스트")
    void stockDeductionFailWhenInsufficient() {
        // Given
        testProduct.updateStock(5);
        productRepository.save(testProduct);

        // When
        boolean result = productStockAdapter.deductStock(testProduct.getId(), 10);

        // Then
        assertThat(result).isFalse();
        
        ProductEntity updatedProduct = productRepository.findById(testProduct.getId()).get();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(5); // 변화 없음
    }

    @Test
    @DisplayName("동시 재고 차감 - 5개 스레드")
    void concurrentStockDeduction() throws InterruptedException {
        // Given
        int threadCount = 5;
        int quantityPerThread = 10;
        int expectedFinalStock = 100 - (threadCount * quantityPerThread);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // When
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    boolean success = productStockAdapter.deductStock(testProduct.getId(), quantityPerThread);
                    if (success) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Then
        ProductEntity updatedProduct = productRepository.findById(testProduct.getId()).get();
        
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(expectedFinalStock);
    }
}