# 시퀀스 다이어그램

## 1️⃣ 잔액 조회 API

```mermaid
sequenceDiagram
    participant Client as Client
    participant App as Application
    participant DB as Database

    Client->>+App: GET /api/balance/{userId}
    App->>+DB: USER 테이블 조회
    DB-->>-App: 잔액 정보 반환
    App-->>-Client: HTTP 200 + 잔액 정보

    Note over App: 간단한 조회는 DB 직접 접근
```

## 1️⃣ 잔액 충전 API

```mermaid
sequenceDiagram
    participant Client as Client
    participant App as Application
    participant Redis as Redis
    participant DB as Database

    Client->>+App: POST /api/balance/charge
    App->>+Redis: 분산 락 획득 (user:{userId}:lock)
    Redis-->>-App: 락 획득 성공

    App->>+DB: 트랜잭션 시작
    DB->>DB: USER.balance 업데이트
    DB->>DB: USER_BALANCE_TX 생성
    DB-->>-App: 트랜잭션 커밋

    App->>Redis: 분산 락 해제
    App-->>-Client: HTTP 200 + 충전 결과

    Note over App: 간단한 충전은 동기 처리로 충분
```

## 2️⃣ 상품 조회 API

```mermaid
sequenceDiagram
    participant Client as Client
    participant App as Application
    participant Cache as Redis Cache
    participant DB as Database

    Client->>+App: GET /api/products
    App->>+Cache: 상품 목록 캐시 조회

    alt 캐시 히트
        Cache-->>App: 캐시된 상품 목록
    else 캐시 미스
        Cache-->>-App: 캐시 없음
        App->>+DB: PRODUCT 테이블 조회
        DB-->>-App: 상품 목록 반환
        App->>Cache: 상품 목록 캐시 저장 (TTL: 1분)
    end

    App-->>-Client: HTTP 200 + 상품 목록
```

## 3️⃣ 보유 쿠폰 조회 API

```mermaid
sequenceDiagram
    participant Client as Client
    participant App as Application
    participant DB as Database

    Client->>+App: GET /api/users/coupons
    App->>+DB: USER_COUPON 테이블 조회
    DB->>DB: WHERE user_id = ? AND status = 'AVAILABLE'
    DB-->>-App: 보유 쿠폰 목록 반환
    App-->>-Client: HTTP 200 + 보유 쿠폰 목록
```

## 3️⃣ 선착순 쿠폰 발급 API

```mermaid
sequenceDiagram
    participant Client as Client
    participant App as Application
    participant Redis as Redis
    participant DB as Database

    Client->>App: POST /api/coupons/{couponId}/issue
    App->>Redis: 분산락 획득 (coupon:{couponId}:lock)
    Redis-->>App: 락 획득 성공

    App->>DB: 쿠폰 발급 가능 여부 확인
    DB-->>App: 발급 가능 여부 반환

    alt 발급 가능
        App->>DB: 트랜잭션 시작
        DB->>DB: COUPON.issued_count 증가
        DB->>DB: USER_COUPON 생성
        DB-->>App: 트랜잭션 커밋

        App->>Redis: 분산락 해제
        App-->>Client: HTTP 200 + 발급 성공
    else 발급 불가
        App->>Redis: 분산락 해제
        App-->>Client: HTTP 400 + 발급 실패
    end

    Note over Redis: 동시성 제어를 위한 분산락 사용
```

## 4️⃣ 주문/결제 API

```mermaid
sequenceDiagram
    participant Client as Client
    participant App as Application
    participant Redis as Redis
    participant DB as Database
    participant External as 외부 데이터플랫폼

    Client->>App: POST /api/orders
    
    Note over App, DB: 1. 재고 확인 및 예약
    App->>Redis: 상품별 분산 락 획득
    Redis-->>App: 락 획득 성공

    App->>DB: 재고 확인
    DB-->>App: 재고 정보 반환

    alt 재고 부족
        App->>Redis: 분산 락 해제
        App-->>Client: HTTP 400 + 주문 실패 (재고 부족)
    else 재고 충분
        Note over App, DB: 2. 주문 생성 및 결제 처리
        App->>DB: 트랜잭션 시작
        DB->>DB: ORDER 생성 (status=PROCESSING)
        DB->>DB: ORDER_ITEM 생성
        DB->>DB: PRODUCT.stock 차감

        opt 쿠폰 사용
            DB->>DB: USER_COUPON.status = USED
        end

        DB->>DB: USER.balance 차감
        DB->>DB: USER_BALANCE_TX 생성
        DB->>DB: ORDER.status = COMPLETED
        DB-->>App: 트랜잭션 커밋

        App->>Redis: 분산 락 해제

        Note over App, External: 3. 비동기 데이터 전송
        App->>External: 주문 통계 데이터 전송 (REST API)

        App-->>Client: HTTP 200 + 주문 성공
    end
```

## 5️⃣ 인기 상품 조회 API

```mermaid
sequenceDiagram
    participant Client as Client
    participant App as Application
    participant Cache as Redis Cache
    participant DB as Database

    Client->>+App: GET /api/products/popular
    
    App->>+Cache: 인기 상품 캐시 조회
    
    alt 캐시 히트
        Cache-->>App: 캐시된 인기 상품 목록
    else 캐시 미스
        Cache-->>-App: 캐시 없음
        App->>+DB: 최근 3일 판매량 기준 상위 5개 상품 조회
        DB->>DB: SELECT * FROM PRODUCT_STAT
        DB->>DB: WHERE date >= CURDATE() - INTERVAL 3 DAY
        DB->>DB: ORDER BY quantity_sold DESC LIMIT 5
        DB-->>-App: 인기 상품 목록 반환
        
        App->>Cache: 인기 상품 캐시 저장 (TTL: 30분)
    end

    App-->>-Client: HTTP 200 + 인기 상품 목록
```

## 📊 실시간 통계 처리

```mermaid
sequenceDiagram
    participant Order as 주문 서비스
    participant Async as 비동기 처리
    participant DB as Database

    Note over Order, DB: 주문 완료 후 비동기 통계 업데이트
    Order->>Order: 주문 처리 완료
    Order->>+Async: 통계 업데이트 요청 (비동기)
    Async->>+DB: 통계 테이블 업데이트
    DB->>DB: UPSERT PRODUCT_STAT
    DB-->>-Async: 업데이트 완료
    Async-->>-Order: 처리 완료
```

## 📋 추가 API 목록

### 상품 관리 API
```mermaid
sequenceDiagram
    participant Client as Client
    participant App as Application
    participant DB as Database

    Note over Client, DB: 상품 상세 조회
    Client->>+App: GET /api/products/{productId}
    App->>+DB: PRODUCT 테이블 조회
    DB-->>-App: 상품 상세 정보 반환
    App-->>-Client: HTTP 200 + 상품 정보
```

### 주문 내역 조회 API
```mermaid
sequenceDiagram
    participant Client as Client
    participant App as Application
    participant DB as Database

    Note over Client, DB: 사용자 주문 내역 조회
    Client->>+App: GET /api/users/orders
    App->>+DB: ORDER 테이블 조회
    DB->>DB: WHERE user_id = ? ORDER BY created_at DESC
    DB-->>-App: 주문 내역 목록 반환
    App-->>-Client: HTTP 200 + 주문 내역
```

### 잔액 거래 내역 조회 API
```mermaid
sequenceDiagram
    participant Client as Client
    participant App as Application
    participant DB as Database

    Note over Client, DB: 잔액 거래 내역 조회
    Client->>+App: GET /api/users/balance/history
    App->>+DB: USER_BALANCE_TX 테이블 조회
    DB->>DB: WHERE user_id = ? ORDER BY created_at DESC
    DB-->>-App: 거래 내역 목록 반환
    App-->>-Client: HTTP 200 + 거래 내역
```
