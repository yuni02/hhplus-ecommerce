package kr.hhplus.be.server.product.adapter.out.persistence;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.ProductStats;
import kr.hhplus.be.server.product.domain.QProduct;
import kr.hhplus.be.server.product.domain.QProductStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * ProductQueryRepository QueryDSL 구현체
 */
@Repository
public class ProductQueryRepositoryImpl implements ProductQueryRepository {

    private final JPAQueryFactory queryFactory;

    public ProductQueryRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<Product> findPopularProductsBySalesCount(LocalDate startDate, LocalDate endDate, int limit) {
        QProduct product = QProduct.product;
        QProductStats stats = QProductStats.productStats;

        return queryFactory
                .selectFrom(product)
                .leftJoin(stats).on(product.id.eq(stats.productId))
                .where(
                        product.status.eq(Product.ProductStatus.ACTIVE),
                        stats.date.between(startDate, endDate)
                )
                .orderBy(stats.recentSalesCount.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<Product> findPopularProductsByCategory(String category, LocalDate startDate, LocalDate endDate, int limit) {
        QProduct product = QProduct.product;
        QProductStats stats = QProductStats.productStats;

        return queryFactory
                .selectFrom(product)
                .leftJoin(stats).on(product.id.eq(stats.productId))
                .where(
                        product.status.eq(Product.ProductStatus.ACTIVE),
                        product.category.eq(category),
                        stats.date.between(startDate, endDate)
                )
                .orderBy(stats.recentSalesCount.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public Page<Product> findProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        QProduct product = QProduct.product;

        BooleanExpression priceCondition = product.currentPrice.between(minPrice, maxPrice);

        List<Product> products = queryFactory
                .selectFrom(product)
                .where(
                        product.status.eq(Product.ProductStatus.ACTIVE),
                        priceCondition
                )
                .orderBy(product.currentPrice.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .selectFrom(product)
                .where(
                        product.status.eq(Product.ProductStatus.ACTIVE),
                        priceCondition
                )
                .fetchCount();

        return new PageImpl<>(products, pageable, total);
    }

    @Override
    public Page<Product> searchProductsByKeyword(String keyword, Pageable pageable) {
        QProduct product = QProduct.product;

        BooleanExpression keywordCondition = product.name.containsIgnoreCase(keyword)
                .or(product.description.containsIgnoreCase(keyword));

        List<Product> products = queryFactory
                .selectFrom(product)
                .where(
                        product.status.eq(Product.ProductStatus.ACTIVE),
                        keywordCondition
                )
                .orderBy(product.name.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .selectFrom(product)
                .where(
                        product.status.eq(Product.ProductStatus.ACTIVE),
                        keywordCondition
                )
                .fetchCount();

        return new PageImpl<>(products, pageable, total);
    }

    @Override
    public List<Product> findLowStockProducts(int threshold) {
        QProduct product = QProduct.product;

        return queryFactory
                .selectFrom(product)
                .where(
                        product.status.eq(Product.ProductStatus.ACTIVE),
                        product.stock.loe(threshold)
                )
                .orderBy(product.stock.asc())
                .fetch();
    }

    @Override
    public List<ProductStats> findProductStatsWithProduct(LocalDate date) {
        QProductStats stats = QProductStats.productStats;
        QProduct product = QProduct.product;

        return queryFactory
                .selectFrom(stats)
                .leftJoin(product).on(stats.productId.eq(product.id))
                .where(stats.date.eq(date))
                .orderBy(stats.recentSalesCount.desc())
                .fetch();
    }

    @Override
    public Page<Product> findProductsByComplexCondition(String category, BigDecimal minPrice, BigDecimal maxPrice, 
                                                       String keyword, Pageable pageable) {
        QProduct product = QProduct.product;

        BooleanExpression conditions = product.status.eq(Product.ProductStatus.ACTIVE);

        if (category != null && !category.isEmpty()) {
            conditions = conditions.and(product.category.eq(category));
        }

        if (minPrice != null && maxPrice != null) {
            conditions = conditions.and(product.currentPrice.between(minPrice, maxPrice));
        }

        if (keyword != null && !keyword.isEmpty()) {
            conditions = conditions.and(
                    product.name.containsIgnoreCase(keyword)
                            .or(product.description.containsIgnoreCase(keyword))
            );
        }

        List<Product> products = queryFactory
                .selectFrom(product)
                .where(conditions)
                .orderBy(product.name.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .selectFrom(product)
                .where(conditions)
                .fetchCount();

        return new PageImpl<>(products, pageable, total);
    }
} 