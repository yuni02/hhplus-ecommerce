package kr.hhplus.be.server.order.adapter.out.persistence;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.QOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * OrderQueryRepository QueryDSL 구현체
 */
@Repository
public class OrderQueryRepositoryImpl implements OrderQueryRepository {

    private final JPAQueryFactory queryFactory;

    public OrderQueryRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<Order> findUserOrderStats(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        QOrder order = QOrder.order;

        return queryFactory
                .selectFrom(order)
                .where(
                        order.userId.eq(userId),
                        order.orderedAt.between(startDate, endDate)
                )
                .orderBy(order.orderedAt.desc())
                .fetch();
    }

    @Override
    public Page<Order> findOrdersByAmountRange(BigDecimal minAmount, BigDecimal maxAmount, Pageable pageable) {
        QOrder order = QOrder.order;

        BooleanExpression amountCondition = order.totalAmount.between(minAmount, maxAmount);

        List<Order> orders = queryFactory
                .selectFrom(order)
                .where(amountCondition)
                .orderBy(order.orderedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .selectFrom(order)
                .where(amountCondition)
                .fetchCount();

        return new PageImpl<>(orders, pageable, total);
    }

    @Override
    public List<Order> findOrdersWithCouponUsage(LocalDateTime startDate, LocalDateTime endDate) {
        QOrder order = QOrder.order;

        return queryFactory
                .selectFrom(order)
                .where(
                        order.userCouponId.isNotNull(),
                        order.orderedAt.between(startDate, endDate)
                )
                .orderBy(order.orderedAt.desc())
                .fetch();
    }

    @Override
    public Page<Order> findOrdersByComplexCondition(Long userId, String status, BigDecimal minAmount, 
                                                  BigDecimal maxAmount, LocalDateTime startDate, 
                                                  LocalDateTime endDate, Pageable pageable) {
        QOrder order = QOrder.order;

        BooleanExpression conditions = order.id.isNotNull(); // 기본 조건

        if (userId != null) {
            conditions = conditions.and(order.userId.eq(userId));
        }

        if (status != null && !status.isEmpty()) {
            conditions = conditions.and(order.status.eq(Order.OrderStatus.valueOf(status)));
        }

        if (minAmount != null && maxAmount != null) {
            conditions = conditions.and(order.totalAmount.between(minAmount, maxAmount));
        }

        if (startDate != null && endDate != null) {
            conditions = conditions.and(order.orderedAt.between(startDate, endDate));
        }

        List<Order> orders = queryFactory
                .selectFrom(order)
                .where(conditions)
                .orderBy(order.orderedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .selectFrom(order)
                .where(conditions)
                .fetchCount();

        return new PageImpl<>(orders, pageable, total);
    }
} 