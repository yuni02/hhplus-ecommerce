-- 상품 데이터만 삽입
USE ecommerce;

DELETE FROM products;

INSERT INTO products (product_id, name, description, price, stock_quantity, status, created_at, updated_at) VALUES
(1, 'iPhone 15 Pro', 'Apple iPhone 15 Pro 128GB', 1200000.00, 50, 'ACTIVE', NOW(), NOW()),
(2, 'MacBook Air M2', 'Apple MacBook Air 13인치 M2 칩', 1490000.00, 30, 'ACTIVE', NOW(), NOW()),
(3, 'Galaxy S24', 'Samsung Galaxy S24 256GB', 1100000.00, 80, 'ACTIVE', NOW(), NOW()),
(4, 'iPad Pro', 'Apple iPad Pro 11인치', 899000.00, 40, 'ACTIVE', NOW(), NOW()),
(5, 'AirPods Pro', 'Apple AirPods Pro 2세대', 349000.00, 100, 'ACTIVE', NOW(), NOW()),
(6, 'Galaxy Watch', 'Samsung Galaxy Watch 6', 350000.00, 60, 'ACTIVE', NOW(), NOW()),
(7, 'Nintendo Switch', 'Nintendo Switch OLED 모델', 390000.00, 70, 'ACTIVE', NOW(), NOW()),
(8, 'PlayStation 5', 'Sony PlayStation 5', 799000.00, 25, 'ACTIVE', NOW(), NOW()),
(9, 'Dell Monitor', 'Dell 27인치 4K 모니터', 450000.00, 35, 'ACTIVE', NOW(), NOW()),
(10, 'Logitech Mouse', 'Logitech MX Master 3', 129000.00, 150, 'ACTIVE', NOW(), NOW()),
(11, 'Mechanical Keyboard', '기계식 키보드 Cherry MX', 189000.00, 90, 'ACTIVE', NOW(), NOW()),
(12, 'Wireless Earbuds', '무선 이어폰 프리미엄', 159000.00, 120, 'ACTIVE', NOW(), NOW()),
(13, 'Smart TV', 'LG 55인치 OLED TV', 1800000.00, 20, 'ACTIVE', NOW(), NOW()),
(14, 'Coffee Machine', '전자동 커피머신', 890000.00, 15, 'ACTIVE', NOW(), NOW()),
(15, 'Vacuum Cleaner', '무선청소기 프리미엄', 499000.00, 45, 'ACTIVE', NOW(), NOW()),
(16, 'Air Purifier', '공기청정기 대용량', 350000.00, 55, 'ACTIVE', NOW(), NOW()),
(17, 'Gaming Chair', '게이밍 의자 프로', 280000.00, 25, 'ACTIVE', NOW(), NOW()),
(18, 'Desk Lamp', 'LED 데스크 램프', 89000.00, 200, 'ACTIVE', NOW(), NOW()),
(19, 'Backpack', '노트북 백팩 프리미엄', 120000.00, 80, 'ACTIVE', NOW(), NOW()),
(20, 'Power Bank', '대용량 보조배터리 20000mAh', 59000.00, 300, 'ACTIVE', NOW(), NOW());