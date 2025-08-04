# ERD

```mermaid
erDiagram
    %% BaseEntity 상속 구조 (공통 필드)
    %% - id (BIGINT AUTO_INCREMENT PK)
    %% - created_at (DATETIME NOT NULL)
    %% - updated_at (DATETIME NOT NULL)

    USER {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT user_id UK "UNIQUE NOT NULL"
        VARCHAR name "NOT NULL"
        VARCHAR email UK "UNIQUE NOT NULL"
        VARCHAR status "length=20, NOT NULL, default=ACTIVE"
        DATETIME created_at "NOT NULL (BaseEntity)"
        DATETIME updated_at "NOT NULL (BaseEntity)"
    }

    BALANCE {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT user_id UK "UNIQUE NOT NULL → users.user_id"
        DECIMAL amount "precision=15,scale=2, NOT NULL, default=0"
        VARCHAR status "length=20, NOT NULL, default=ACTIVE"
        BIGINT version "NOT NULL (낙관적 락)"
        DATETIME created_at "NOT NULL (BaseEntity)"
        DATETIME updated_at "NOT NULL (BaseEntity)"
    }

    PRODUCT {
        BIGINT id PK "AUTO_INCREMENT"
        VARCHAR name "NOT NULL"
        TEXT description "nullable"
        DECIMAL price "precision=15,scale=2, NOT NULL"
        INT stock_quantity "NOT NULL, default=0"
        VARCHAR status "length=20, NOT NULL, default=ACTIVE"
        DATETIME created_at "NOT NULL (BaseEntity)"
        DATETIME updated_at "NOT NULL (BaseEntity)"
    }

    COUPON {
        BIGINT id PK "AUTO_INCREMENT"
        VARCHAR name "NOT NULL"
        VARCHAR description "nullable"
        DECIMAL discount_amount "NOT NULL"
        INT total_quantity "NOT NULL (최대 발급 수량)"
        INT issued_count "NOT NULL, default=0"
        VARCHAR status "length=20, NOT NULL, default=ACTIVE"
        DATETIME valid_from "nullable"
        DATETIME valid_to "nullable"
        DATETIME created_at "NOT NULL (BaseEntity)"
        DATETIME updated_at "NOT NULL (BaseEntity)"
    }

    USER_COUPON {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT user_id "NOT NULL → users.user_id"
        BIGINT coupon_id "NOT NULL → coupons.id"
        INT discount_amount "NOT NULL"
        VARCHAR status "length=20, NOT NULL, default=AVAILABLE"
        DATETIME issued_at "NOT NULL (자동 설정)"
        DATETIME used_at "nullable"
        BIGINT order_id "nullable → orders.id"
        DATETIME created_at "NOT NULL (BaseEntity)"
        DATETIME updated_at "NOT NULL (BaseEntity)"
    }

    ORDER {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT user_id "NOT NULL → users.user_id"
        DECIMAL total_amount "precision=15,scale=2, NOT NULL"
        DECIMAL discounted_amount "precision=15,scale=2, nullable"
        DECIMAL discount_amount "precision=15,scale=2, nullable"
        BIGINT user_coupon_id "nullable → user_coupons.id"
        VARCHAR status "length=20, NOT NULL, default=PENDING"
        DATETIME ordered_at "nullable"
        DATETIME created_at "NOT NULL (BaseEntity)"
        DATETIME updated_at "NOT NULL (BaseEntity)"
    }

    ORDER_ITEM {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT order_id "NOT NULL → orders.id"
        BIGINT product_id "NOT NULL → products.id"
        VARCHAR product_name "NOT NULL"
        INT quantity "NOT NULL"
        DECIMAL unit_price "precision=15,scale=2, NOT NULL"
        DECIMAL total_price "precision=15,scale=2, NOT NULL"
        DATETIME created_at "NOT NULL (BaseEntity)"
        DATETIME updated_at "NOT NULL (BaseEntity)"
    }

    %% 로그성 테이블: 주문 이력 이벤트 (INSERT ONLY, 이벤트 소싱)
    ORDER_HISTORY_EVENT {
        BIGINT id PK "AUTO_INCREMENT (로그 고유 ID)"
        BIGINT order_id "NOT NULL → orders.id"
        VARCHAR event_type "length=50, NOT NULL"
        DECIMAL total_amount "precision=15,scale=2, NOT NULL"
        DECIMAL discount_amount "precision=15,scale=2, nullable"
        DECIMAL discounted_amount "precision=15,scale=2, NOT NULL"
        VARCHAR payment_method "length=50, nullable"
        DECIMAL refund_amount "precision=15,scale=2, nullable"
        VARCHAR cancel_reason "length=255, nullable"
        DATETIME occurred_at "NOT NULL"
        DATETIME created_at "NOT NULL (BaseEntity)"
        DATETIME updated_at "NOT NULL (BaseEntity)"
    }

    %% 로그성 테이블: 사용자 잔액 거래 내역 (INSERT ONLY, 감사 추적)
    USER_BALANCE_TX {
        BIGINT id PK "AUTO_INCREMENT (거래 로그 고유 ID)"
        BIGINT user_id "NOT NULL → users.user_id"
        DECIMAL amount "NOT NULL"
        VARCHAR tx_type "length=20, NOT NULL"
        VARCHAR status "length=20, NOT NULL, default=COMPLETED"
        VARCHAR memo "nullable"
        BIGINT related_order_id "nullable → orders.id"
        DATETIME created_at "NOT NULL (BaseEntity)"
        DATETIME updated_at "NOT NULL (BaseEntity)"
    }

    %% 복합키 테이블: 상품 통계 (product_id + date)
    PRODUCT_STATS {
        BIGINT product_id PK "→ products.id"
        DATE date PK "통계 날짜"
        DECIMAL total_sales "precision=15,scale=2, NOT NULL, default=0"
        INT total_quantity "NOT NULL, default=0"
        INT order_count "NOT NULL, default=0"
        DATETIME created_at "NOT NULL (감사 필드)"
        DATETIME updated_at "NOT NULL (감사 필드)"
    }

    %% 논리적 관계 정의 (물리적 FK 제약조건 없음)
    USER ||--o{ BALANCE : "user_id"
    USER ||--o{ USER_BALANCE_TX : "user_id"
    USER ||--o{ ORDER : "user_id"
    USER ||--o{ USER_COUPON : "user_id"
    
    COUPON ||--o{ USER_COUPON : "coupon_id"
    USER_COUPON ||--o{ ORDER : "user_coupon_id"
    
    ORDER ||--o{ ORDER_ITEM : "order_id"
    ORDER ||--o{ ORDER_HISTORY_EVENT : "order_id"
    ORDER ||--o{ USER_BALANCE_TX : "related_order_id"
    
    PRODUCT ||--o{ ORDER_ITEM : "product_id"
    PRODUCT ||--o{ PRODUCT_STATS : "product_id"

```

## 📋 테이블 상세 정보

### 🏗️ **아키텍처 특징**
- **BaseEntity 상속**: 모든 엔티티가 공통 감사 필드를 상속받음
- **논리적 외래키**: 물리적 FK 제약조건 없이 논리적 관계만 유지
- **동시성 제어**: Balance 테이블에 낙관적 락 적용
- **이벤트 소싱**: INSERT ONLY 로그 테이블 활용

### 📊 **테이블별 상세 정보**

#### **1. users (사용자)**
- **테이블명**: `users`
- **주요 특징**: 
  - `user_id`: 비즈니스 식별자 (UNIQUE)
  - `email`: 이메일 주소 (UNIQUE)
- **상태값**: ACTIVE, INACTIVE, SUSPENDED

#### **2. balances (잔액)**
- **테이블명**: `balances`
- **주요 특징**:
  - `user_id`: users.user_id와 1:1 관계
  - `version`: 낙관적 락으로 동시성 제어
  - `amount`: precision=15, scale=2로 정확한 금액 관리
- **상태값**: ACTIVE, INACTIVE

#### **3. products (상품)**
- **테이블명**: `products`
- **주요 특징**:
  - `stock_quantity`: 재고 수량 관리
  - `price`: 현재 판매 가격
- **상태값**: ACTIVE, INACTIVE, SOLD_OUT

#### **4. coupons (쿠폰)**
- **테이블명**: `coupons`
- **주요 특징**:
  - `total_quantity`: 최대 발급 가능 수량
  - `issued_count`: 현재 발급된 수량
  - `valid_from/to`: 쿠폰 유효 기간
- **상태값**: ACTIVE, INACTIVE, SOLD_OUT, EXPIRED

#### **5. user_coupons (사용자 쿠폰)**
- **테이블명**: `user_coupons`
- **주요 특징**:
  - 사용자별 발급된 쿠폰 관리
  - `issued_at`: 발급 시점 자동 기록
  - `used_at`: 사용 시점 기록
- **상태값**: AVAILABLE, USED, EXPIRED

#### **6. orders (주문)**
- **테이블명**: `orders`
- **주요 특징**:
  - `total_amount`: 원래 주문 금액
  - `discount_amount`: 할인 금액
  - `discounted_amount`: 최종 결제 금액
- **상태값**: PENDING, VALIDATING, PROCESSING, COMPLETED, CANCELLED, FAILED

#### **7. order_items (주문 상품)**
- **테이블명**: `order_items`
- **주요 특징**:
  - `product_name`: 주문 시점의 상품명 스냅샷
  - `unit_price`: 주문 시점의 단가 스냅샷
  - `total_price`: 계산된 총 금액

### 🔄 **로그성 테이블 (INSERT ONLY)**

#### **8. user_balance_tx (잔액 거래 내역)**
- **테이블명**: `user_balance_tx`
- **주요 특징**:
  - 모든 잔액 변동 내역 기록
  - `related_order_id`: 주문과 연관된 거래 추적
- **거래 타입**: DEPOSIT, PAYMENT, REFUND
- **상태값**: PENDING, PROCESSING, COMPLETED, FAILED

#### **9. order_history_events (주문 이력)**
- **테이블명**: `order_history_events`
- **주요 특징**:
  - 주문 관련 모든 이벤트 추적
  - 이벤트 소싱 패턴 적용
- **이벤트 타입**: ORDER_COMPLETED, CANCELLED, REFUNDED

### 📈 **통계 테이블**

#### **10. product_stats (상품 통계)**
- **테이블명**: `product_stats`
- **주요 특징**:
  - 복합키: (product_id, date)
  - 일별 상품 판매 통계 집계
  - BaseEntity 상속하지 않음 (독립적인 감사 필드)

### 🔒 **제약조건 및 인덱스**

#### **UNIQUE 제약조건**
- `users.user_id`: 비즈니스 식별자 유일성
- `users.email`: 이메일 주소 유일성  
- `balances.user_id`: 사용자당 하나의 잔액 계정

#### **복합키**
- `product_stats`: (product_id, date)

#### **동시성 제어**
- `balances.version`: 낙관적 락으로 동시 수정 방지

### 📝 **설계 원칙**

1. **논리적 외래키**: 성능과 유연성을 위해 물리적 FK 제약조건 미적용
2. **이벤트 소싱**: 중요한 비즈니스 이벤트를 로그 테이블로 추적
3. **감사 추적**: 모든 금액 변동과 주문 변경 이력 보존
4. **스냅샷 패턴**: 주문 시점의 상품 정보를 별도 저장
5. **상태 관리**: 각 엔티티별 명확한 상태 전환 규칙 적용
