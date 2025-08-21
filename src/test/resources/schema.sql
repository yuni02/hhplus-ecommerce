-- 외래키 관계로 인한 순서를 고려하여 테이블 삭제
DROP TABLE IF EXISTS user_coupons;
DROP TABLE IF EXISTS user_balance_tx;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS order_history_events;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS coupons;
DROP TABLE IF EXISTS balances;

-- 테이블 생성
CREATE TABLE balances
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT                          NOT NULL,
    amount     DECIMAL(15, 2) DEFAULT 0.00     NOT NULL,
    status     VARCHAR(20)    DEFAULT 'ACTIVE' NOT NULL,
    version    BIGINT         DEFAULT 0        NOT NULL,
    created_at TIMESTAMP                       NOT NULL,
    updated_at TIMESTAMP                       NOT NULL,
    CONSTRAINT user_id UNIQUE (user_id)
);

CREATE TABLE coupons
(
    discount_amount DECIMAL(38, 2) NOT NULL,
    issued_count    INT            NOT NULL,
    total_quantity  INT            NOT NULL,
    created_at      DATETIME(6)    NOT NULL,
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    updated_at      DATETIME(6)    NOT NULL,
    valid_from      DATETIME(6)    NULL,
    valid_to        DATETIME(6)    NULL,
    status          VARCHAR(20)    NOT NULL,
    description     VARCHAR(255)   NULL,
    name            VARCHAR(255)   NOT NULL
);

CREATE TABLE products
(
    price          DECIMAL(38, 2) NOT NULL,
    stock_quantity INT            NOT NULL,
    created_at     DATETIME(6)    NOT NULL,
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    updated_at     DATETIME(6)    NOT NULL,
    status         VARCHAR(20)    NOT NULL,
    description    VARCHAR(255)   NULL,
    name           VARCHAR(255)   NOT NULL
);

CREATE TABLE orders
(
    discounted_amount DECIMAL(38, 2) NULL,
    discount_amount   DECIMAL(38, 2) NOT NULL,
    total_amount      DECIMAL(38, 2) NOT NULL,
    created_at        DATETIME(6)    NOT NULL,
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    ordered_at        DATETIME(6)    NOT NULL,
    updated_at        DATETIME(6)    NOT NULL,
    user_coupon_id    BIGINT         NULL,
    user_id           BIGINT         NOT NULL,
    status            VARCHAR(20)    NOT NULL,
    payment_method    VARCHAR(50)    NULL
);

CREATE TABLE order_history_events
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id          BIGINT        NOT NULL,
    event_type        VARCHAR(50)   NOT NULL,
    total_amount      DECIMAL(15,2) NOT NULL,
    discount_amount   DECIMAL(15,2),
    discounted_amount DECIMAL(15,2) NOT NULL,
    payment_method    VARCHAR(50),
    refund_amount     DECIMAL(15,2),
    cancel_reason     VARCHAR(255),
    occurred_at       DATETIME(6)   NOT NULL,
    created_at        DATETIME(6)   NOT NULL,
    updated_at        DATETIME(6)   NOT NULL
);

CREATE TABLE order_items
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id     BIGINT         NOT NULL,
    product_id   BIGINT         NOT NULL,
    product_name VARCHAR(255)   NOT NULL,
    quantity     INT            NOT NULL,
    unit_price   DECIMAL(15,2)  NOT NULL,
    total_price  DECIMAL(15,2)  NOT NULL,
    created_at   DATETIME(6)    NOT NULL,
    updated_at   DATETIME(6)    NOT NULL
);


CREATE TABLE user_balance_tx
(
    amount           DECIMAL(38, 2) NOT NULL,
    created_at       DATETIME(6)    NOT NULL,
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    related_order_id BIGINT         NULL,
    updated_at       DATETIME(6)    NOT NULL,
    user_id          BIGINT         NOT NULL,
    status           VARCHAR(20)    NOT NULL,
    tx_type          VARCHAR(20)    NOT NULL,
    memo             VARCHAR(255)   NULL
);

CREATE TABLE user_coupons
(
    discount_amount INT         NOT NULL,
    coupon_id       BIGINT      NOT NULL,
    created_at      DATETIME(6) NOT NULL,
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    issued_at       DATETIME(6) NOT NULL,
    order_id        BIGINT      NULL,
    updated_at      DATETIME(6) NOT NULL,
    used_at         DATETIME(6) NULL,
    user_id         BIGINT      NOT NULL,
    status          VARCHAR(20) NOT NULL
);