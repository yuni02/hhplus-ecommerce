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