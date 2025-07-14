# 기술적 계층 기준 시퀀스 다이어그램 (Redis + Kafka)

## 1️⃣ 잔액 조회 API

```mermaid
sequenceDiagram
    participant Client as Client
    participant API as API_Gateway
    participant App as Application
    participant DB as Database

    Client->>+API: GET /api/balance/{userId}
    API->>+App: 잔액 조회 요청
    App->>+DB: USER 테이블 조회
    DB-->>-App: 잔액 정보 반환
    App-->>-API: 잔액 정보 응답
    API-->>-Client: HTTP 200 + 잔액 정보

    Note over App: 간단한 조회는 DB 직접 접근
```

## 1️⃣ 잔액 충전 API

```mermaid
sequenceDiagram
    participant Client as 클라이언트
    participant API as API Gateway
    participant App as Application
    participant Redis as Redis Cache
    participant DB as Database
    participant Kafka as Kafka

    Client->>+API: POST /api/balance/charge
    API->>+App: 잔액 충전 요청
    
    App->>+Redis: 분산 락 획득 (user:{userId}:lock)
    Redis-->>-App: 락 획득 성공
    
    App->>+DB: 트랜잭션 시작
    DB->>DB: USER.balance 업데이트
    DB->>DB: USER_BALANCE_TX 생성
    DB-->>-App: 트랜잭션 커밋
    
    App->>Redis: 잔액 캐시 업데이트
    App->>Redis: 분산 락 해제
    
    App->>+Kafka: 잔액 변경 이벤트 발행
    Kafka->>Kafka: balance-changed 토픽
    Kafka-->>-App: 이벤트 발행 완료
    
    App-->>-API: 충전 완료 응답
    API-->>-Client: HTTP 200 + 충전 결과
```

## 2️⃣ 상품 조회 API

```mermaid
sequenceDiagram
    participant Client as 클라이언트
    participant API as API Gateway
    participant App as Application
    participant Redis as Redis Cache
    participant DB as Database

    Client->>+API: GET /api/products
    API->>+App: 상품 목록 조회 요청
    
    App->>+Redis: 상품 목록 캐시 조회
    
    alt 캐시 히트
        Redis-->>App: 캐시된 상품 목록
    else 캐시 미스
        Redis-->>-App: 캐시 없음
        App->>+DB: PRODUCT 테이블 조회
        DB-->>-App: 상품 목록 반환
        App->>Redis: 상품 목록 캐시 저장 (TTL: 1분)
    end
    
    App-->>-API: 상품 목록 응답
    API-->>-Client: HTTP 200 + 상품 목록
```

## 3️⃣ 선착순 쿠폰 발급 API

```mermaid
sequenceDiagram
    participant Client as Client
    participant API as API_Gateway
    participant App as Application
    participant Redis as Redis_Lock
    participant DB as Database

    Client->>API: POST /api/coupons/couponId/issue
    API->>App: 쿠폰 발급 요청

    App->>Redis: 분산락 획득 (필수)
    Redis-->>App: 락 획득 성공

    App->>DB: 쿠폰 발급 가능 여부 확인
    DB-->>App: 발급 가능 여부 반환

    alt 발급 가능
        App->>DB: 트랜잭션 시작
        DB->>DB: COUPON.issued_count 증가
        DB->>DB: USER_COUPON 생성
        DB-->>App: 트랜잭션 커밋

        App->>Redis: 분산락 해제
        App-->>API: 발급 성공 응답
    else 발급 불가
        App->>Redis: 분산락 해제
        App-->>API: 발급 실패 응답
    end

    API-->>Client: HTTP 응답

    Note over Redis: 동시성 제어만 Redis 사용
```

## 4️⃣ 주문/결제 API

```mermaid
sequenceDiagram
    participant Client as 클라이언트
    participant API as API Gateway
    participant App as Application
    participant Redis as Redis Cache
    participant DB as Database
    participant External as 외부 데이터플랫폼

    Client->>+API: POST /api/orders
    API->>+App: 주문 요청

    Note over App, DB: 1. 재고 확인 및 예약
    App->>+Redis: 상품별 분산 락 획득
    Redis-->>-App: 락 획득 성공

    App->>+DB: 재고 확인
    DB-->>-App: 재고 정보 반환

    alt 재고 부족
        App->>Redis: 분산 락 해제
        App-->>API: 주문 실패 (재고 부족)
    else 재고 충분
        Note over App, DB: 2. 주문 생성 및 결제 처리
        App->>+DB: 트랜잭션 시작
        DB->>DB: ORDER 생성 (status=PROCESSING)
        DB->>DB: ORDER_ITEM 생성
        DB->>DB: PRODUCT.stock 차감

        opt 쿠폰 사용
            DB->>DB: USER_COUPON.status = USED
        end

        DB->>DB: USER.balance 차감
        DB->>DB: USER_BALANCE_TX 생성
        DB->>DB: ORDER.status = COMPLETED
        DB-->>-App: 트랜잭션 커밋

        App->>Redis: 분산 락 해제

        Note over App, External: 3. 동기적 데이터 전송
        App->>External: 주문 통계 데이터 전송 (REST API)

        App-->>-API: 주문 성공 응답
    end

    API-->>-Client: HTTP 응답
```

## 5️⃣ 인기 상품 조회 API

```mermaid
sequenceDiagram
    participant Client as 클라이언트
    participant API as API Gateway
    participant App as Application
    participant DB as Database
    
    Client->>+API: GET /api/products/popular
    API->>+App: 인기 상품 조회 요청
    
    App->>+DB: 최근 3일 판매량 기준 상위 5개 상품 조회
    DB->>DB: SELECT * FROM PRODUCT_STAT
    DB->>DB: WHERE date >= CURDATE() - INTERVAL 3 DAY
    DB->>DB: ORDER BY quantity_sold DESC LIMIT 5
    DB-->>-App: 인기 상품 목록 반환
    
    App-->>-API: 인기 상품 목록 응답
    API-->>-Client: HTTP 200 + 인기 상품 목록
```

## 📊 실시간 통계 처리 (Kafka Consumer)

```mermaid
sequenceDiagram
    participant Order as 주문 서비스
    participant DB as Database
    
    Note over Order, DB: 주문 완료 시 즉시 통계 업데이트
    Order->>Order: 주문 처리 완료
    Order->>+DB: 통계 테이블 업데이트
    DB->>DB: UPDATE PRODUCT_STAT SET quantity_sold = quantity_sold + ?
    DB->>DB: WHERE product_id = ? AND date = CURDATE()
    DB-->>-Order: 업데이트 완료
```


## 🔹 기술적 계층 구성

### **1. 프레젠테이션 계층**
- **API Gateway**: 라우팅, 인증, 로드밸런싱
- **Client**: 웹/모바일 클라이언트

### **2. 애플리케이션 계층**
- **Application Service**: 비즈니스 로직 처리
- **분산 락 관리**: Redis 기반 동시성 제어

### **3. 캐싱 계층**
- **Redis Cache**:
    - 잔액/상품 정보 캐시
    - 실시간 통계 저장
    - 분산 락 구현

### **4. 메시징 계층**
- **Kafka**:
    - 이벤트 스트리밍
    - 비동기 처리
    - 시스템 간 디커플링

### **5. 데이터 계층**
- **Database**: 영구 데이터 저장
- **외부 데이터플랫폼**: 분석 및 통계
