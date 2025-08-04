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

-- product_stats 테이블 생성 (BaseEntity 상속은 아니지만 복합키)
CREATE TABLE product_stats (
    product_id BIGINT NOT NULL,
    date DATE NOT NULL,
    quantity_sold INT NOT NULL,
    revenue DECIMAL(38,2) NOT NULL,
    conversion_rate DECIMAL(38,2),
    product_rank INT,
    total_sales_amount DECIMAL(38,2),
    total_sales_count INT,
    aggregation_date DATETIME(6),
    last_order_date DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (date, product_id),
    CONSTRAINT FKqawohfr96evam9fw5pt69rata FOREIGN KEY (product_id) REFERENCES products (id)
);

-- 데이터 삽입 프로시저들

-- users 테이블 대량 데이터 삽입 프로시저
DELIMITER //
CREATE PROCEDURE InsertUsersBulk()
BEGIN
    DECLARE i INT DEFAULT 1;
    WHILE i <= 10000 DO
        INSERT INTO users (user_id, name, email, status, created_at, updated_at) 
        VALUES (
            i,
            CONCAT('User_', i),
            CONCAT('user', i, '@example.com'),
            'ACTIVE',
            NOW(),
            NOW()
        );
        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;

-- coupons 테이블 대량 데이터 삽입 프로시저
DELIMITER //
CREATE PROCEDURE InsertCouponsBulk()
BEGIN
    DECLARE i INT DEFAULT 1;
    WHILE i <= 10000 DO
        INSERT INTO coupons (name, description, discount_amount, total_quantity, issued_count, status, valid_from, valid_to, created_at, updated_at) 
        VALUES (
            CONCAT('쿠폰_', i),
            CONCAT('할인 쿠폰 ', i, '번'),
            CASE 
                WHEN i % 5 = 0 THEN 10000.00
                WHEN i % 4 = 0 THEN 5000.00
                WHEN i % 3 = 0 THEN 3000.00
                WHEN i % 2 = 0 THEN 2000.00
                ELSE 1000.00
            END,
            CASE 
                WHEN i % 10 = 0 THEN 1000
                WHEN i % 5 = 0 THEN 500
                ELSE 100
            END,
            0,
            'ACTIVE',
            DATE_ADD(NOW(), INTERVAL -30 DAY),
            DATE_ADD(NOW(), INTERVAL 30 DAY),
            NOW(),
            NOW()
        );
        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;

-- products 테이블 대량 데이터 삽입 프로시저
DELIMITER //
CREATE PROCEDURE InsertProductsBulk()
BEGIN
    DECLARE i INT DEFAULT 1;
    WHILE i <= 10000 DO
        INSERT INTO products (name, description, price, stock_quantity, status, created_at, updated_at) 
        VALUES (
            CONCAT('상품_', i),
            CONCAT('상품 ', i, '번 설명입니다.'),
            CASE 
                WHEN i % 10 = 0 THEN 100000.00
                WHEN i % 5 = 0 THEN 50000.00
                WHEN i % 3 = 0 THEN 30000.00
                WHEN i % 2 = 0 THEN 10000.00
                ELSE 5000.00
            END,
            CASE 
                WHEN i % 20 = 0 THEN 1000
                WHEN i % 10 = 0 THEN 500
                WHEN i % 5 = 0 THEN 100
                ELSE 50
            END,
            'ACTIVE',
            NOW(),
            NOW()
        );
        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;

-- 프로시저 실행
CALL InsertUsersBulk();
CALL InsertCouponsBulk();
CALL InsertProductsBulk();

-- 프로시저 삭제
DROP PROCEDURE IF EXISTS InsertUsersBulk;
DROP PROCEDURE IF EXISTS InsertCouponsBulk;
DROP PROCEDURE IF EXISTS InsertProductsBulk;