-- 외래키 관계로 인한 순서를 고려하여 테이블 삭제
DROP TABLE IF EXISTS user_coupons;
DROP TABLE IF EXISTS user_balance_tx;
DROP TABLE IF EXISTS product_stats;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS order_history_events;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS coupons;
DROP TABLE IF EXISTS balances;
DROP TABLE IF EXISTS users;

-- users 테이블 생성 (BaseEntity 상속)
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

-- balances 테이블 생성 (BaseEntity 상속)
CREATE TABLE balances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT DEFAULT 0 NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

-- coupons 테이블 생성 (BaseEntity 상속)
CREATE TABLE coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    discount_amount DECIMAL(38,2) NOT NULL,
    total_quantity INT NOT NULL,
    issued_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    valid_from DATETIME(6),
    valid_to DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

-- products 테이블 생성 (BaseEntity 상속)
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(15,2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

-- orders 테이블 생성 (BaseEntity 상속)
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    discounted_amount DECIMAL(15,2),
    discount_amount DECIMAL(15,2),
    user_coupon_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ordered_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

-- order_items 테이블 생성 (BaseEntity 상속)
CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL,
    total_price DECIMAL(15,2) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

-- order_history_events 테이블 생성 (BaseEntity 상속)
CREATE TABLE order_history_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    discount_amount DECIMAL(15,2),
    discounted_amount DECIMAL(15,2) NOT NULL,
    payment_method VARCHAR(50),
    refund_amount DECIMAL(15,2),
    cancel_reason VARCHAR(255),
    occurred_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

-- user_coupons 테이블 생성 (BaseEntity 상속)
CREATE TABLE user_coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    discount_amount INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    issued_at DATETIME(6) NOT NULL,
    used_at DATETIME(6),
    order_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

-- user_balance_tx 테이블 생성 (BaseEntity 상속)
CREATE TABLE user_balance_tx (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount DECIMAL(38,2) NOT NULL,
    tx_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    memo VARCHAR(255),
    related_order_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

-- 외래키 제약조건 추가 (논리적 관계만, 물리적 제약조건은 Entity에서 NO_CONSTRAINT로 비활성화)
-- 주석으로만 표시하여 논리적 관계를 문서화

-- balances.user_id → users.user_id (1:1)
-- ALTER TABLE balances ADD CONSTRAINT FK_balances_user FOREIGN KEY (user_id) REFERENCES users (user_id);

-- user_balance_tx.user_id → users.user_id (N:1)
-- ALTER TABLE user_balance_tx ADD CONSTRAINT FK_user_balance_tx_user FOREIGN KEY (user_id) REFERENCES users (user_id);

-- user_balance_tx.related_order_id → orders.id (N:1)
-- ALTER TABLE user_balance_tx ADD CONSTRAINT FK_user_balance_tx_order FOREIGN KEY (related_order_id) REFERENCES orders (id);

-- orders.user_id → users.user_id (N:1)
-- ALTER TABLE orders ADD CONSTRAINT FK_orders_user FOREIGN KEY (user_id) REFERENCES users (user_id);

-- orders.user_coupon_id → user_coupons.id (N:1)
-- ALTER TABLE orders ADD CONSTRAINT FK_orders_user_coupon FOREIGN KEY (user_coupon_id) REFERENCES user_coupons (id);

-- order_items.order_id → orders.id (N:1)
-- ALTER TABLE order_items ADD CONSTRAINT FK_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id);

-- order_items.product_id → products.id (N:1)
-- ALTER TABLE order_items ADD CONSTRAINT FK_order_items_product FOREIGN KEY (product_id) REFERENCES products (id);

-- order_history_events.order_id → orders.id (N:1)
-- ALTER TABLE order_history_events ADD CONSTRAINT FK_order_history_events_order FOREIGN KEY (order_id) REFERENCES orders (id);

-- user_coupons.user_id → users.user_id (N:1)
-- ALTER TABLE user_coupons ADD CONSTRAINT FK_user_coupons_user FOREIGN KEY (user_id) REFERENCES users (user_id);

-- user_coupons.coupon_id → coupons.id (N:1)
-- ALTER TABLE user_coupons ADD CONSTRAINT FK_user_coupons_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id);

-- user_coupons.order_id → orders.id (N:1)
-- ALTER TABLE user_coupons ADD CONSTRAINT FK_user_coupons_order FOREIGN KEY (order_id) REFERENCES orders (id);

-- 데이터 삽입 프로시저들 (Spring Boot SQL 스크립트 실행 시 DELIMITER 문제로 주석 처리)

-- users 테이블 대량 데이터 삽입 프로시저
-- DELIMITER //
-- CREATE PROCEDURE InsertUsersBulk()
-- BEGIN
--     DECLARE i INT DEFAULT 1;
--     WHILE i <= 10000 DO
--         INSERT INTO users (user_id, name, email, status, created_at, updated_at) 
--         VALUES (
--             i,
--             CONCAT('User_', i),
--             CONCAT('user', i, '@example.com'),
--             'ACTIVE',
--             NOW(),
--             NOW()
--         );
--         SET i = i + 1;
--     END WHILE;
-- END //
-- DELIMITER ;

-- coupons 테이블 대량 데이터 삽입 프로시저
-- DELIMITER //
-- CREATE PROCEDURE InsertCouponsBulk()
-- BEGIN
--     DECLARE i INT DEFAULT 1;
--     WHILE i <= 10000 DO
--         INSERT INTO coupons (name, description, discount_amount, total_quantity, issued_count, status, valid_from, valid_to, created_at, updated_at) 
--         VALUES (
--             CONCAT('쿠폰_', i),
--             CONCAT('할인 쿠폰 ', i, '번'),
--             CASE 
--                 WHEN i % 5 = 0 THEN 10000.00
--                 WHEN i % 4 = 0 THEN 5000.00
--                 WHEN i % 3 = 0 THEN 3000.00
--                 WHEN i % 2 = 0 THEN 2000.00
--                 ELSE 1000.00
--             END,
--             CASE 
--                 WHEN i % 10 = 0 THEN 1000
--                 WHEN i % 5 = 0 THEN 500
--                 ELSE 100
--             END,
--             0,
--             'ACTIVE',
--             DATE_ADD(NOW(), INTERVAL -30 DAY),
--             DATE_ADD(NOW(), INTERVAL 30 DAY),
--             NOW(),
--             NOW()
--         );
--         SET i = i + 1;
--     END WHILE;
-- END //
-- DELIMITER ;

-- products 테이블 대량 데이터 삽입 프로시저
-- DELIMITER //
-- CREATE PROCEDURE InsertProductsBulk()
-- BEGIN
--     DECLARE i INT DEFAULT 1;
--     WHILE i <= 10000 DO
--         INSERT INTO products (name, description, price, stock_quantity, status, created_at, updated_at) 
--         VALUES (
--             CONCAT('상품_', i),
--             CONCAT('상품 ', i, '번 설명입니다.'),
--             CASE 
--                 WHEN i % 10 = 0 THEN 100000.00
--                 WHEN i % 5 = 0 THEN 50000.00
--                 WHEN i % 3 = 0 THEN 30000.00
--                 WHEN i % 2 = 0 THEN 10000.00
--                 ELSE 5000.00
--             END,
--             CASE 
--                 WHEN i % 20 = 0 THEN 1000
--                 WHEN i % 10 = 0 THEN 500
--                 WHEN i % 5 = 0 THEN 100
--                 ELSE 50
--             END,
--             'ACTIVE',
--             NOW(),
--             NOW()
--         );
--         SET i = i + 1;
--     END WHILE;
-- END //
-- DELIMITER ;

-- 샘플 데이터 삽입 (외래키 관계 순서 고려)

-- 1. users 테이블 샘플 데이터 (1001~1100)
INSERT INTO users (user_id, name, email, status, created_at, updated_at) VALUES
(1001, 'User_1001', 'user1001@example.com', 'ACTIVE', NOW(), NOW()),
(1002, 'User_1002', 'user1002@example.com', 'ACTIVE', NOW(), NOW()),
(1003, 'User_1003', 'user1003@example.com', 'ACTIVE', NOW(), NOW()),
(1004, 'User_1004', 'user1004@example.com', 'ACTIVE', NOW(), NOW()),
(1005, 'User_1005', 'user1005@example.com', 'ACTIVE', NOW(), NOW()),
(1006, 'User_1006', 'user1006@example.com', 'ACTIVE', NOW(), NOW()),
(1007, 'User_1007', 'user1007@example.com', 'ACTIVE', NOW(), NOW()),
(1008, 'User_1008', 'user1008@example.com', 'ACTIVE', NOW(), NOW()),
(1009, 'User_1009', 'user1009@example.com', 'ACTIVE', NOW(), NOW()),
(1010, 'User_1010', 'user1010@example.com', 'ACTIVE', NOW(), NOW()),
(1011, 'User_1011', 'user1011@example.com', 'ACTIVE', NOW(), NOW()),
(1012, 'User_1012', 'user1012@example.com', 'ACTIVE', NOW(), NOW()),
(1013, 'User_1013', 'user1013@example.com', 'ACTIVE', NOW(), NOW()),
(1014, 'User_1014', 'user1014@example.com', 'ACTIVE', NOW(), NOW()),
(1015, 'User_1015', 'user1015@example.com', 'ACTIVE', NOW(), NOW()),
(1016, 'User_1016', 'user1016@example.com', 'ACTIVE', NOW(), NOW()),
(1017, 'User_1017', 'user1017@example.com', 'ACTIVE', NOW(), NOW()),
(1018, 'User_1018', 'user1018@example.com', 'ACTIVE', NOW(), NOW()),
(1019, 'User_1019', 'user1019@example.com', 'ACTIVE', NOW(), NOW()),
(1020, 'User_1020', 'user1020@example.com', 'ACTIVE', NOW(), NOW());

-- 2. balances 테이블 샘플 데이터 (users와 1:1 관계)
INSERT INTO balances (user_id, amount, status, version, created_at, updated_at) VALUES
(1001, 100000.00, 'ACTIVE', 0, NOW(), NOW()),
(1002, 150000.00, 'ACTIVE', 0, NOW(), NOW()),
(1003, 200000.00, 'ACTIVE', 0, NOW(), NOW()),
(1004, 80000.00, 'ACTIVE', 0, NOW(), NOW()),
(1005, 120000.00, 'ACTIVE', 0, NOW(), NOW()),
(1006, 90000.00, 'ACTIVE', 0, NOW(), NOW()),
(1007, 110000.00, 'ACTIVE', 0, NOW(), NOW()),
(1008, 170000.00, 'ACTIVE', 0, NOW(), NOW()),
(1009, 250000.00, 'ACTIVE', 0, NOW(), NOW()),
(1010, 50000.00, 'ACTIVE', 0, NOW(), NOW()),
(1011, 300000.00, 'ACTIVE', 0, NOW(), NOW()),
(1012, 75000.00, 'ACTIVE', 0, NOW(), NOW()),
(1013, 130000.00, 'ACTIVE', 0, NOW(), NOW()),
(1014, 95000.00, 'ACTIVE', 0, NOW(), NOW()),
(1015, 180000.00, 'ACTIVE', 0, NOW(), NOW()),
(1016, 220000.00, 'ACTIVE', 0, NOW(), NOW()),
(1017, 85000.00, 'ACTIVE', 0, NOW(), NOW()),
(1018, 140000.00, 'ACTIVE', 0, NOW(), NOW()),
(1019, 60000.00, 'ACTIVE', 0, NOW(), NOW()),
(1020, 190000.00, 'ACTIVE', 0, NOW(), NOW());

-- 3. coupons 테이블 샘플 데이터
INSERT INTO coupons (name, description, discount_amount, total_quantity, issued_count, status, valid_from, valid_to, created_at, updated_at) VALUES
('신규회원 할인쿠폰', '신규회원 전용 1000원 할인', 1000.00, 1000, 0, 'ACTIVE', DATE_ADD(NOW(), INTERVAL -7 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), NOW(), NOW()),
('VIP 회원 할인쿠폰', 'VIP 회원 전용 5000원 할인', 5000.00, 500, 0, 'ACTIVE', DATE_ADD(NOW(), INTERVAL -3 DAY), DATE_ADD(NOW(), INTERVAL 60 DAY), NOW(), NOW()),
('선착순 특가쿠폰', '선착순 100명 한정 3000원 할인', 3000.00, 100, 0, 'ACTIVE', DATE_ADD(NOW(), INTERVAL -1 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY), NOW(), NOW()),
('주말 특별쿠폰', '주말 한정 2000원 할인', 2000.00, 200, 0, 'ACTIVE', DATE_ADD(NOW(), INTERVAL -1 HOUR), DATE_ADD(NOW(), INTERVAL 48 HOUR), NOW(), NOW()),
('대용량 할인쿠폰', '대용량 구매 시 10000원 할인', 10000.00, 50, 0, 'ACTIVE', DATE_ADD(NOW(), INTERVAL -12 HOUR), DATE_ADD(NOW(), INTERVAL 72 HOUR), NOW(), NOW()),
('친구초대 쿠폰', '친구 초대 시 4000원 할인', 4000.00, 300, 0, 'ACTIVE', DATE_ADD(NOW(), INTERVAL -2 DAY), DATE_ADD(NOW(), INTERVAL 14 DAY), NOW(), NOW()),
('생일축하 쿠폰', '생일 축하 7000원 할인', 7000.00, 1000, 0, 'ACTIVE', DATE_ADD(NOW(), INTERVAL -10 DAY), DATE_ADD(NOW(), INTERVAL 90 DAY), NOW(), NOW()),
('첫구매 감사쿠폰', '첫 구매 감사 1500원 할인', 1500.00, 800, 0, 'ACTIVE', DATE_ADD(NOW(), INTERVAL -5 DAY), DATE_ADD(NOW(), INTERVAL 21 DAY), NOW(), NOW()),
('리뷰작성 쿠폰', '리뷰 작성 시 2500원 할인', 2500.00, 400, 0, 'ACTIVE', DATE_ADD(NOW(), INTERVAL -8 DAY), DATE_ADD(NOW(), INTERVAL 45 DAY), NOW(), NOW()),
('월말결산 쿠폰', '월말 결산 특가 6000원 할인', 6000.00, 150, 0, 'ACTIVE', DATE_ADD(NOW(), INTERVAL -6 HOUR), DATE_ADD(NOW(), INTERVAL 24 HOUR), NOW(), NOW());

-- 4. products 테이블 샘플 데이터
INSERT INTO products (name, description, price, stock_quantity, status, created_at, updated_at) VALUES
('iPhone 15 Pro', 'Apple iPhone 15 Pro 128GB', 1200000.00, 50, 'ACTIVE', NOW(), NOW()),
('MacBook Air M2', 'Apple MacBook Air 13인치 M2 칩', 1490000.00, 30, 'ACTIVE', NOW(), NOW()),
('Galaxy S24', 'Samsung Galaxy S24 256GB', 1100000.00, 80, 'ACTIVE', NOW(), NOW()),
('iPad Pro', 'Apple iPad Pro 11인치', 899000.00, 40, 'ACTIVE', NOW(), NOW()),
('AirPods Pro', 'Apple AirPods Pro 2세대', 349000.00, 100, 'ACTIVE', NOW(), NOW()),
('Galaxy Watch', 'Samsung Galaxy Watch 6', 350000.00, 60, 'ACTIVE', NOW(), NOW()),
('Nintendo Switch', 'Nintendo Switch OLED 모델', 390000.00, 70, 'ACTIVE', NOW(), NOW()),
('PlayStation 5', 'Sony PlayStation 5', 799000.00, 25, 'ACTIVE', NOW(), NOW()),
('Dell Monitor', 'Dell 27인치 4K 모니터', 450000.00, 35, 'ACTIVE', NOW(), NOW()),
('Logitech Mouse', 'Logitech MX Master 3', 129000.00, 150, 'ACTIVE', NOW(), NOW()),
('Mechanical Keyboard', '기계식 키보드 Cherry MX', 189000.00, 90, 'ACTIVE', NOW(), NOW()),
('Wireless Earbuds', '무선 이어폰 프리미엄', 159000.00, 120, 'ACTIVE', NOW(), NOW()),
('Smart TV', 'LG 55인치 OLED TV', 1800000.00, 20, 'ACTIVE', NOW(), NOW()),
('Coffee Machine', '전자동 커피머신', 890000.00, 15, 'ACTIVE', NOW(), NOW()),
('Vacuum Cleaner', '무선청소기 프리미엄', 499000.00, 45, 'ACTIVE', NOW(), NOW()),
('Air Purifier', '공기청정기 대용량', 350000.00, 55, 'ACTIVE', NOW(), NOW()),
('Gaming Chair', '게이밍 의자 프로', 280000.00, 25, 'ACTIVE', NOW(), NOW()),
('Desk Lamp', 'LED 데스크 램프', 89000.00, 200, 'ACTIVE', NOW(), NOW()),
('Backpack', '노트북 백팩 프리미엄', 120000.00, 80, 'ACTIVE', NOW(), NOW()),
('Power Bank', '대용량 보조배터리 20000mAh', 59000.00, 300, 'ACTIVE', NOW(), NOW());