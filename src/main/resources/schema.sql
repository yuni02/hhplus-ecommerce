-- 외래키 제약조건 삭제
SET FOREIGN_KEY_CHECKS = 0;

-- orders 테이블의 외래키 제약조건 삭제
ALTER TABLE orders DROP FOREIGN KEY IF EXISTS FK32ql8ubntj5uh44ph9659tiih;
ALTER TABLE orders DROP FOREIGN KEY IF EXISTS FK_orders_user_id;
ALTER TABLE orders DROP FOREIGN KEY IF EXISTS FK_orders_user_coupon_id;

-- order_items 테이블의 외래키 제약조건 삭제
ALTER TABLE order_items DROP FOREIGN KEY IF EXISTS FK_order_items_order_id;
ALTER TABLE order_items DROP FOREIGN KEY IF EXISTS FK_order_items_product_id;

-- order_history_events 테이블의 외래키 제약조건 삭제
ALTER TABLE order_history_events DROP FOREIGN KEY IF EXISTS FK_order_history_events_order_id;

-- user_coupons 테이블의 외래키 제약조건 삭제
ALTER TABLE user_coupons DROP FOREIGN KEY IF EXISTS FK_user_coupons_user_id;
ALTER TABLE user_coupons DROP FOREIGN KEY IF EXISTS FK_user_coupons_coupon_id;
ALTER TABLE user_coupons DROP FOREIGN KEY IF EXISTS FK_user_coupons_order_id;

-- user_balance_tx 테이블의 외래키 제약조건 삭제
ALTER TABLE user_balance_tx DROP FOREIGN KEY IF EXISTS FK_user_balance_tx_user_id;
ALTER TABLE user_balance_tx DROP FOREIGN KEY IF EXISTS FK_user_balance_tx_reference_id;

-- product_stats 테이블의 외래키 제약조건 삭제
ALTER TABLE product_stats DROP FOREIGN KEY IF EXISTS FK_product_stats_product_id;

-- BalanceEntity는 users 테이블을 공유하므로 별도 제약조건 없음

SET FOREIGN_KEY_CHECKS = 1;

-- 새로운 balance 테이블 생성
CREATE TABLE IF NOT EXISTS balances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

-- 기존 users 테이블에서 amount 데이터를 balances 테이블로 마이그레이션
INSERT INTO balances (user_id, amount, status, created_at, updated_at, version)
SELECT user_id, amount, status, created_at, updated_at, 0
FROM users 
WHERE amount > 0 OR amount IS NOT NULL;

-- users 테이블에서 amount 컬럼 제거 (선택사항)
-- ALTER TABLE users DROP COLUMN amount;
-- 사용자 조회 최적화
CREATE INDEX idx_userid_status ON users (user_id, status);

-- 주문 조회 최적화
CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_ordered_at ON orders (ordered_at);

-- 상품 통계 조회 최적화
CREATE INDEX idx_product_stats_date ON product_stats (date);
CREATE INDEX idx_product_stats_product_id ON product_stats (product_id);

-- 잔액 거래 내역 조회 최적화
CREATE INDEX idx_balance_tx_user_id ON user_balance_tx (user_id);
CREATE INDEX idx_balance_tx_created_at ON user_balance_tx (created_at);