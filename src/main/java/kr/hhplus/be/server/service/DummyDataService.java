package kr.hhplus.be.server.service;

import org.springframework.stereotype.Service;

import kr.hhplus.be.server.dto.request.CouponRequest;
import kr.hhplus.be.server.dto.request.OrderRequest;
import kr.hhplus.be.server.dto.request.ProductRequest;
import kr.hhplus.be.server.dto.response.BalanceHistoryResponse;
import kr.hhplus.be.server.dto.response.BalanceResponse;
import kr.hhplus.be.server.dto.response.CouponResponse;
import kr.hhplus.be.server.dto.response.OrderResponse;
import kr.hhplus.be.server.dto.response.PopularProductStatsResponse;
import kr.hhplus.be.server.dto.response.ProductResponse;
import kr.hhplus.be.server.dto.response.UserCouponResponse;
import kr.hhplus.be.server.dto.response.UserResponse;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class DummyDataService {

    private final AtomicLong userIdGenerator = new AtomicLong(1);
    private final AtomicLong productIdGenerator = new AtomicLong(1);
    private final AtomicLong couponIdGenerator = new AtomicLong(1);
    private final AtomicLong userCouponIdGenerator = new AtomicLong(1);
    private final AtomicLong orderIdGenerator = new AtomicLong(1);
    private final AtomicLong transactionIdGenerator = new AtomicLong(1);

    private final Map<Long, UserResponse> users = new HashMap<>();
    private final Map<Long, ProductResponse> products = new HashMap<>();
    private final Map<Long, CouponResponse> coupons = new HashMap<>();
    private final Map<Long, UserCouponResponse> userCoupons = new HashMap<>();
    private final Map<Long, OrderResponse> orders = new HashMap<>();
    private final Map<Long, List<BalanceHistoryResponse.TransactionHistory>> userTransactions = new HashMap<>();

    public DummyDataService() {
        initializeData();
    }

    private void initializeData() {
        // 초기 사용자 데이터
        createUser("user1", "김철수", 100000);
        createUser("user2", "이영희", 50000);
        createUser("user3", "박민수", 200000);

        // 초기 상품 데이터
        createProduct("iPhone 15", 1200000, 50);
        createProduct("Galaxy S24", 1100000, 30);
        createProduct("MacBook Pro", 2500000, 20);
        createProduct("iPad Air", 800000, 40);
        createProduct("AirPods Pro", 300000, 100);
        createProduct("Galaxy Buds", 250000, 80);

        // 초기 쿠폰 데이터
        createCoupon("신규회원 할인", 10000, 100);
        createCoupon("여름 특가 할인", 50000, 50);
        createCoupon("VIP 할인", 100000, 20);
        createCoupon("생일 축하 할인", 20000, 200);

        // 초기 사용자 쿠폰 발급
        issueUserCoupon(1L, 1L);
        issueUserCoupon(1L, 2L);
        issueUserCoupon(2L, 1L);

        // 초기 거래 내역
        addTransactionHistory(1L, "DEPOSIT", 100000, "충전");
        addTransactionHistory(2L, "DEPOSIT", 50000, "충전");
        addTransactionHistory(3L, "DEPOSIT", 200000, "충전");
    }

    // 사용자 관련 메서드
    public UserResponse createUser(String username, String name, Integer initialBalance) {
        Long id = userIdGenerator.getAndIncrement();
        UserResponse user = new UserResponse(id, username, name, initialBalance, LocalDateTime.now());
        users.put(id, user);
        userTransactions.put(id, new ArrayList<>());
        return user;
    }

    public UserResponse getUser(Long userId) {
        return users.get(userId);
    }

    public boolean userExists(String username) {
        return users.values().stream()
                .anyMatch(user -> user.getUsername().equals(username));
    }

    // 잔액 관련 메서드
    public BalanceResponse getUserBalance(Long userId) {
        UserResponse user = users.get(userId);
        if (user == null)
            return null;
        return new BalanceResponse(userId, user.getBalance());
    }

    public BalanceResponse chargeBalance(Long userId, Integer amount) {
        UserResponse user = users.get(userId);
        if (user == null)
            return null;

        user.setBalance(user.getBalance() + amount);
        Long transactionId = addTransactionHistory(userId, "DEPOSIT", amount, "잔액 충전");
        return new BalanceResponse(userId, user.getBalance(), transactionId);
    }

    public BalanceHistoryResponse getBalanceHistory(Long userId) {
        List<BalanceHistoryResponse.TransactionHistory> transactions = userTransactions.get(userId);
        if (transactions == null)
            return new BalanceHistoryResponse(new ArrayList<>());
        return new BalanceHistoryResponse(new ArrayList<>(transactions));
    }

    // 🔥 USER_BALANCE_TX 로그성 테이블 처리 강화
    /**
     * 잔액 거래 내역 추가 (로그성 테이블)
     * 
     * 특징:
     * - INSERT ONLY: 모든 거래를 기록하고 절대 삭제하지 않음
     * - 감사 추적: 모든 잔액 변동의 완전한 히스토리 보존
     * - 무결성: 잔액 계산의 정확성을 위한 불변 기록
     */
    private Long addTransactionHistory(Long userId, String txType, Integer amount, String memo) {
        Long transactionId = transactionIdGenerator.getAndIncrement();
        BalanceHistoryResponse.TransactionHistory transaction = new BalanceHistoryResponse.TransactionHistory(
                transactionId, txType, amount, "COMPLETED", memo, LocalDateTime.now());
        userTransactions.computeIfAbsent(userId, k -> new ArrayList<>()).add(transaction);

        // 로그성 테이블 특성 강조
        System.out.println("💰 [USER_BALANCE_TX 로그 생성] ID: " + transactionId +
                ", User: " + userId + ", Type: " + txType + ", Amount: " + amount);

        return transactionId;
    }

    // 상품 관련 메서드
    public List<ProductResponse> getAllProducts() {
        return new ArrayList<>(products.values());
    }

    public ProductResponse getProduct(Long productId) {
        return products.get(productId);
    }

    public ProductResponse createProduct(String name, Integer price, Integer stock) {
        Long id = productIdGenerator.getAndIncrement();
        ProductResponse product = new ProductResponse(id, name, price, stock, "ACTIVE", LocalDateTime.now(),
                LocalDateTime.now());
        products.put(id, product);
        return product;
    }

    public ProductResponse updateProduct(Long productId, ProductRequest request) {
        ProductResponse product = products.get(productId);
        if (product == null)
            return null;

        product.setName(request.getName());
        product.setCurrentPrice(request.getCurrentPrice());
        product.setStock(request.getStock());
        product.setUpdatedAt(LocalDateTime.now());
        return product;
    }

    public boolean deleteProduct(Long productId) {
        return products.remove(productId) != null;
    }

    public List<ProductResponse> getPopularProducts() {
        // 인기 상품을 위한 모의 데이터 (판매량 기준 상위 5개)
        return products.values().stream()
                .sorted((p1, p2) -> Long.compare(p2.getId(), p1.getId())) // ID 역순으로 정렬 (최신 상품)
                .limit(5)
                .collect(Collectors.toList());
    }

    public List<PopularProductStatsResponse> getPopularProductStats() {
        // 인기상품 판매 통계를 위한 모의 데이터
        List<PopularProductStatsResponse> stats = new ArrayList<>();

        // 상위 5개 상품에 대해 가짜 통계 데이터 생성
        List<ProductResponse> popularProducts = getPopularProducts();
        for (int i = 0; i < popularProducts.size(); i++) {
            ProductResponse product = popularProducts.get(i);

            // 가짜 통계 데이터 생성
            int totalSalesCount = (int) (Math.random() * 1000) + 100; // 100~1099
            long totalSalesAmount = (long) totalSalesCount * product.getCurrentPrice();
            int recentSalesCount = (int) (totalSalesCount * 0.3); // 최근 3일은 전체의 30%
            long recentSalesAmount = (long) recentSalesCount * product.getCurrentPrice();
            double conversionRate = Math.random() * 10 + 5; // 5%~15%
            LocalDateTime lastOrderDate = LocalDateTime.now().minusHours((int) (Math.random() * 72)); // 최근 72시간 내

            PopularProductStatsResponse stat = new PopularProductStatsResponse(
                    product.getId(),
                    product.getName(),
                    product.getCurrentPrice(),
                    product.getStock(),
                    totalSalesCount,
                    totalSalesAmount,
                    recentSalesCount,
                    recentSalesAmount,
                    conversionRate,
                    lastOrderDate,
                    i + 1 // 순위
            );

            stats.add(stat);
        }

        return stats;
    }

    

    // 쿠폰 관련 메서드
    public List<CouponResponse> getAvailableCoupons() {
        return coupons.values().stream()
                .filter(coupon -> "ACTIVE".equals(coupon.getStatus()) &&
                        coupon.getIssuedCount() < coupon.getTotalQuantity())
                .collect(Collectors.toList());
    }

    public CouponResponse getCoupon(Long couponId) {
        return coupons.get(couponId);
    }

    public CouponResponse createCoupon(String name, Integer discountAmount, Integer totalQuantity) {
        Long id = couponIdGenerator.getAndIncrement();
        CouponResponse coupon = new CouponResponse(id, name, discountAmount, totalQuantity, 0, "ACTIVE",
                LocalDateTime.now());
        coupons.put(id, coupon);
        return coupon;
    }

    public CouponResponse updateCoupon(Long couponId, CouponRequest request) {
        CouponResponse coupon = coupons.get(couponId);
        if (coupon == null)
            return null;

        coupon.setName(request.getName());
        coupon.setDiscountAmount(request.getDiscountAmount());
        coupon.setTotalQuantity(request.getTotalQuantity());
        return coupon;
    }

    public UserCouponResponse issueCoupon(Long userId, Long couponId) {
        CouponResponse coupon = coupons.get(couponId);
        if (coupon == null || !"ACTIVE".equals(coupon.getStatus()) ||
                coupon.getIssuedCount() >= coupon.getTotalQuantity()) {
            return null;
        }

        // 중복 발급 체크
        boolean alreadyIssued = userCoupons.entrySet().stream()
                .anyMatch(entry -> entry.getValue().getCouponId().equals(couponId) &&
                        userId.equals(userCouponToUser.get(entry.getKey())));
        if (alreadyIssued) {
            return null;
        }

        Long userCouponId = userCouponIdGenerator.getAndIncrement();
        UserCouponResponse userCoupon = new UserCouponResponse(
                userCouponId, couponId, coupon.getName(), coupon.getDiscountAmount(),
                "AVAILABLE", LocalDateTime.now(), null);
        userCoupons.put(userCouponId, userCoupon);
        userCouponToUser.put(userCouponId, userId); // 사용자-쿠폰 매핑 추가

        // 쿠폰 발급 수량 증가
        coupon.setIssuedCount(coupon.getIssuedCount() + 1);
        if (coupon.getIssuedCount() >= coupon.getTotalQuantity()) {
            coupon.setStatus("SOLD_OUT");
        }

        return userCoupon;
    }

    public List<UserCouponResponse> getUserCoupons(Long userId) {
        return userCoupons.entrySet().stream()
                .filter(entry -> userId.equals(userCouponToUser.get(entry.getKey())))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    // UserCouponResponse에 userId 필드가 없으므로 별도로 관리
    private final Map<Long, Long> userCouponToUser = new HashMap<>();

    private UserCouponResponse issueUserCoupon(Long userId, Long couponId) {
        CouponResponse coupon = coupons.get(couponId);
        if (coupon == null)
            return null;

        Long userCouponId = userCouponIdGenerator.getAndIncrement();
        UserCouponResponse userCoupon = new UserCouponResponse(
                userCouponId, couponId, coupon.getName(), coupon.getDiscountAmount(),
                "AVAILABLE", LocalDateTime.now(), null);
        userCoupons.put(userCouponId, userCoupon);
        userCouponToUser.put(userCouponId, userId);
        return userCoupon;
    }

    // 주문 관련 메서드
    public OrderResponse createOrder(Long userId, List<OrderRequest.OrderItemRequest> orderItems, Long userCouponId) {
        Long orderId = orderIdGenerator.getAndIncrement();

        List<OrderResponse.OrderItemResponse> orderItemResponses = new ArrayList<>();
        int totalPrice = 0;

        for (OrderRequest.OrderItemRequest item : orderItems) {
            ProductResponse product = products.get(item.getProductId());
            if (product == null)
                continue;

            int itemTotal = product.getCurrentPrice() * item.getQuantity();
            totalPrice += itemTotal;

            orderItemResponses.add(new OrderResponse.OrderItemResponse(
                    orderIdGenerator.getAndIncrement(),
                    product.getId(),
                    product.getName(),
                    item.getQuantity(),
                    product.getCurrentPrice(),
                    itemTotal));

            // 재고 차감
            product.setStock(product.getStock() - item.getQuantity());
        }

        int discountedPrice = totalPrice;
        if (userCouponId != null) {
            UserCouponResponse userCoupon = userCoupons.get(userCouponId);
            if (userCoupon != null && "AVAILABLE".equals(userCoupon.getStatus())) {
                discountedPrice = Math.max(0, totalPrice - userCoupon.getDiscountAmount());
                userCoupon.setStatus("USED");
                userCoupon.setUsedAt(LocalDateTime.now());
            }
        }

        // 사용자 잔액 차감
        UserResponse user = users.get(userId);
        if (user != null && user.getBalance() >= discountedPrice) {
            user.setBalance(user.getBalance() - discountedPrice);
            addTransactionHistory(userId, "PAYMENT", -discountedPrice, "주문 결제");
        }

        OrderResponse order = new OrderResponse(orderId, userId, userCouponId, totalPrice, discountedPrice,
                "COMPLETED", orderItemResponses, LocalDateTime.now());
        orders.put(orderId, order);

        // 🔥 로그성 테이블: ORDER_HISTORY_EVENT 생성
        createOrderHistoryEvent(order);

        return order;
    }

    // 🔥 ORDER_HISTORY_EVENT 로그성 테이블 처리
    /**
     * 주문 이력 이벤트 로그 생성 (로그성 테이블)
     * 
     * 특징:
     * - INSERT ONLY: 한번 생성되면 절대 수정/삭제되지 않음
     * - 이벤트 소싱: 주문의 모든 상태 변화를 JSON으로 기록
     * - 외부 연동: 데이터 플랫폼으로 전송될 이벤트 데이터
     */
    private void createOrderHistoryEvent(OrderResponse order) {
        // JSON payload 생성 (실제로는 ObjectMapper 사용 권장)
        String eventPayload = String.format("""
                {
                    "eventType": "ORDER_COMPLETED",
                    "timestamp": "%s",
                    "orderId": %d,
                    "userId": %d,
                    "orderDetails": {
                        "totalAmount": %d,
                        "discountAmount": %d,
                        "paymentMethod": "BALANCE",
                        "items": %s
                    },
                    "couponInfo": %s
                }
                """,
                LocalDateTime.now().toString(),
                order.getId(),
                order.getUserId(),
                order.getTotalPrice(),
                order.getTotalPrice() - order.getDiscountedPrice(),
                generateOrderItemsJson(order.getOrderItems()),
                order.getUserCouponId() != null ? generateCouponInfoJson(order.getUserCouponId()) : "null");

        // 로그성 테이블에 이벤트 기록 (INSERT ONLY)
        Long eventId = transactionIdGenerator.getAndIncrement();
        System.out.println("📝 [ORDER_HISTORY_EVENT 로그 생성] ID: " + eventId + ", Order: " + order.getId());
        System.out.println("📄 JSON Payload: " + eventPayload);

        // 실제 구현에서는 이 데이터를 데이터베이스의 ORDER_HISTORY_EVENT 테이블에 INSERT
        // 그리고 외부 데이터 플랫폼으로 비동기 전송
    }

    private String generateOrderItemsJson(List<OrderResponse.OrderItemResponse> items) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            OrderResponse.OrderItemResponse item = items.get(i);
            json.append(String.format("""
                    {
                        "productId": %d,
                        "productName": "%s",
                        "quantity": %d,
                        "unitPrice": %d,
                        "totalPrice": %d
                    }
                    """, item.getProductId(), item.getProductName(), item.getQuantity(),
                    item.getUnitPriceSnapshot(), item.getTotalPrice()));

            if (i < items.size() - 1)
                json.append(",");
        }
        json.append("]");
        return json.toString();
    }

    private String generateCouponInfoJson(Long userCouponId) {
        UserCouponResponse userCoupon = userCoupons.get(userCouponId);
        if (userCoupon == null)
            return "null";

        return String.format("""
                {
                    "couponId": %d,
                    "couponName": "%s",
                    "discountAmount": %d
                }
                """, userCoupon.getCouponId(), userCoupon.getCouponName(), userCoupon.getDiscountAmount());
    }

    public OrderResponse getOrder(Long orderId) {
        return orders.get(orderId);
    }

    public List<OrderResponse> getUserOrders(Long userId) {
        return orders.values().stream()
                .filter(order -> order.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    // UserCouponResponse에서 userId를 가져오는 헬퍼 메서드
    private Long getUserIdForUserCoupon(Long userCouponId) {
        return userCouponToUser.get(userCouponId);
    }

    // 수정된 getUserCoupons 메서드
    public List<UserCouponResponse> getUserCouponsUpdated(Long userId) {
        return userCoupons.entrySet().stream()
                .filter(entry -> userId.equals(userCouponToUser.get(entry.getKey())))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    // UserCouponResponse에서 userId를 가져오는 헬퍼 메서드
    private Long getUserIdFromUserCoupon(Long userCouponId) {
        return userCouponToUser.get(userCouponId);
    }
}