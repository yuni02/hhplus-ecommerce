# 마이크로서비스 환경에서의 분산 트랜잭션 처리 설계 문서

## 1. 개요

서비스 확장에 따라 도메인별로 애플리케이션 서버와 데이터베이스를 분리할 때, 기존의 ACID 트랜잭션을 보장할 수 없는 상황이 발생합니다. 이 문서는 분산 환경에서의 트랜잭션 처리 한계점과 이를 해결하기 위한 코레오그래피 패턴 기반의 대응 방안을 제시합니다.

## 2. 현재 시스템 구조 vs 분산 시스템 구조

### 2.1 현재 모놀리식 구조
```mermaid
graph TB
    API[API Gateway]
    APP[Application Server]
    DB[(Single Database)]
    
    API --> APP
    APP --> DB
    
    subgraph "Single Transaction Boundary"
        APP
        DB
    end
```

### 2.2 분산 마이크로서비스 구조
```mermaid
graph TB
    API[API Gateway]
    
    subgraph "Order Service"
        ORDER_APP[Order App]
        ORDER_DB[(Order DB)]
        ORDER_APP --> ORDER_DB
    end
    
    subgraph "Product Service"
        PRODUCT_APP[Product App]
        PRODUCT_DB[(Product DB)]
        PRODUCT_APP --> PRODUCT_DB
    end
    
    subgraph "Coupon Service"
        COUPON_APP[Coupon App]
        COUPON_DB[(Coupon DB)]
        COUPON_APP --> COUPON_DB
    end
    
    subgraph "Balance Service"
        BALANCE_APP[Balance App]
        BALANCE_DB[(Balance DB)]
        BALANCE_APP --> BALANCE_DB
    end
    
    API --> ORDER_APP
    API --> PRODUCT_APP
    API --> COUPON_APP
    API --> BALANCE_APP
```

## 3. 분산 트랜잭션의 한계점

### 3.1 ACID 속성의 한계
- **원자성(Atomicity)**: 여러 서비스에 걸친 작업의 원자성 보장 불가
- **일관성(Consistency)**: 서로 다른 데이터베이스 간 일관성 유지 어려움
- **격리성(Isolation)**: 분산 환경에서의 동시성 제어 복잡성
- **지속성(Durability)**: 부분 실패 시 전체 작업의 지속성 보장 어려움

### 3.2 기술적 한계
- **2PC(Two-Phase Commit)의 문제점**: 블로킹, 단일 장애점
- **네트워크 지연 및 실패**: 타임아웃, 부분 실패
- **서비스 간 강결합**: 동기적 의존성으로 인한 가용성 저하

## 4. 코레오그래피 패턴 기반 해결 방안

### 4.1 Saga 패턴 적용
각 서비스가 로컬 트랜잭션을 수행하고, 실패 시 보상 트랜잭션을 통해 일관성을 유지합니다.

### 4.2 이벤트 기반 코레오그래피
```mermaid
sequenceDiagram
    participant Client
    participant OrderService
    participant ProductService
    participant CouponService
    participant BalanceService
    participant EventBus
    
    Client->>OrderService: 주문 요청
    OrderService->>OrderService: 주문 검증
    OrderService->>EventBus: OrderProcessingStarted 이벤트
    
    EventBus->>ProductService: 재고 차감 요청
    ProductService->>ProductService: 재고 확인 및 차감
    ProductService->>EventBus: StockProcessed 이벤트
    
    EventBus->>CouponService: 쿠폰 사용 요청
    CouponService->>CouponService: 쿠폰 검증 및 사용
    CouponService->>EventBus: CouponProcessed 이벤트
    
    EventBus->>BalanceService: 잔액 차감 요청
    BalanceService->>BalanceService: 잔액 확인 및 차감
    BalanceService->>EventBus: BalanceProcessed 이벤트
    
    EventBus->>OrderService: 주문 완료 처리
    OrderService->>OrderService: 주문 상태 완료
    OrderService->>EventBus: OrderCompleted 이벤트
    OrderService->>Client: 주문 완료 응답
```

### 4.3 실패 처리 및 보상 트랜잭션
```mermaid
sequenceDiagram
    participant OrderService
    participant ProductService
    participant CouponService
    participant BalanceService
    participant EventBus
    
    Note over EventBus: 정상 플로우 진행 중...
    
    EventBus->>BalanceService: 잔액 차감 요청
    BalanceService->>BalanceService: 잔액 부족 확인
    BalanceService->>EventBus: BalanceProcessingFailed 이벤트
    
    Note over EventBus: 보상 트랜잭션 시작
    
    EventBus->>CouponService: 쿠폰 복원 요청
    CouponService->>CouponService: 쿠폰 상태를 AVAILABLE로 복원
    CouponService->>EventBus: CouponRestored 이벤트
    
    EventBus->>ProductService: 재고 복원 요청
    ProductService->>ProductService: 차감된 재고 복원
    ProductService->>EventBus: StockRestored 이벤트
    
    EventBus->>OrderService: 주문 실패 처리
    OrderService->>OrderService: 주문 상태를 FAILED로 변경
```

## 5. 구현 아키텍처

### 5.1 이벤트 기반 아키텍처
```mermaid
graph TB
    subgraph "Event-Driven Choreography Architecture"
        CLIENT[Client Application]
        
        subgraph "API Gateway Layer"
            GATEWAY[API Gateway]
        end
        
        subgraph "Service Mesh"
            subgraph "Order Domain"
                ORDER_SVC[Order Service]
                ORDER_HANDLER[Order Event Handler]
                ORDER_DB[(Order Database)]
            end
            
            subgraph "Product Domain"
                PRODUCT_SVC[Product Service]
                PRODUCT_HANDLER[Product Event Handler]
                PRODUCT_DB[(Product Database)]
            end
            
            subgraph "Coupon Domain"
                COUPON_SVC[Coupon Service]
                COUPON_HANDLER[Coupon Event Handler]
                COUPON_DB[(Coupon Database)]
            end
            
            subgraph "Balance Domain"
                BALANCE_SVC[Balance Service]
                BALANCE_HANDLER[Balance Event Handler]
                BALANCE_DB[(Balance Database)]
            end
        end
        
        subgraph "Event Infrastructure"
            EVENT_BUS[Event Bus / Message Broker]
            EVENT_STORE[(Event Store)]
        end
        
        subgraph "Monitoring & Observability"
            SAGA_MONITOR[Saga Monitor]
            TRACING[Distributed Tracing]
            METRICS[Metrics Collection]
        end
    end
    
    CLIENT --> GATEWAY
    GATEWAY --> ORDER_SVC
    
    ORDER_SVC --> ORDER_DB
    ORDER_HANDLER --> EVENT_BUS
    
    PRODUCT_SVC --> PRODUCT_DB
    PRODUCT_HANDLER --> EVENT_BUS
    
    COUPON_SVC --> COUPON_DB
    COUPON_HANDLER --> EVENT_BUS
    
    BALANCE_SVC --> BALANCE_DB
    BALANCE_HANDLER --> EVENT_BUS
    
    EVENT_BUS --> EVENT_STORE
    EVENT_BUS --> SAGA_MONITOR
    
    SAGA_MONITOR --> TRACING
    SAGA_MONITOR --> METRICS
```

### 5.2 코레오그래피 플로우
```mermaid
flowchart TD
    START([주문 요청]) --> VALIDATE{주문 검증}
    VALIDATE -->|성공| EMIT_START[OrderProcessingStarted 발행]
    VALIDATE -->|실패| END_FAIL([주문 실패])
    
    EMIT_START --> STOCK_PROCESS[재고 처리 서비스]
    STOCK_PROCESS --> STOCK_CHECK{재고 확인}
    STOCK_CHECK -->|성공| EMIT_STOCK[StockProcessed 발행]
    STOCK_CHECK -->|실패| EMIT_STOCK_FAIL[StockFailed 발행]
    
    EMIT_STOCK --> COUPON_PROCESS[쿠폰 처리 서비스]
    COUPON_PROCESS --> COUPON_CHECK{쿠폰 검증}
    COUPON_CHECK -->|성공| EMIT_COUPON[CouponProcessed 발행]
    COUPON_CHECK -->|실패| RESTORE_STOCK[재고 복원]
    
    EMIT_COUPON --> BALANCE_PROCESS[잔액 처리 서비스]
    BALANCE_PROCESS --> BALANCE_CHECK{잔액 확인}
    BALANCE_CHECK -->|성공| EMIT_BALANCE[BalanceProcessed 발행]
    BALANCE_CHECK -->|실패| RESTORE_COUPON[쿠폰 복원]
    
    EMIT_BALANCE --> ORDER_COMPLETE[주문 완료 처리]
    ORDER_COMPLETE --> EMIT_COMPLETE[OrderCompleted 발행]
    EMIT_COMPLETE --> END_SUCCESS([주문 완료])
    
    RESTORE_COUPON --> RESTORE_STOCK
    RESTORE_STOCK --> EMIT_STOCK_FAIL
    EMIT_STOCK_FAIL --> END_FAIL
    
    style START fill:#e1f5fe
    style END_SUCCESS fill:#e8f5e8
    style END_FAIL fill:#ffebee
    style VALIDATE fill:#fff3e0
    style STOCK_CHECK fill:#fff3e0
    style COUPON_CHECK fill:#fff3e0
    style BALANCE_CHECK fill:#fff3e0
```

## 6. 이벤트 설계

### 6.1 도메인 이벤트 정의
```mermaid
classDiagram
    class OrderProcessingStartedEvent {
        +String eventId
        +CreateOrderCommand command
        +LocalDateTime occurredAt
    }
    
    class StockProcessedEvent {
        +String eventId
        +CreateOrderCommand command
        +List~OrderItem~ orderItems
        +BigDecimal totalAmount
        +LocalDateTime occurredAt
    }
    
    class CouponProcessedEvent {
        +String eventId
        +CreateOrderCommand command
        +List~OrderItem~ orderItems
        +BigDecimal discountedAmount
        +Integer discountAmount
        +LocalDateTime occurredAt
    }
    
    class BalanceProcessedEvent {
        +String eventId
        +CreateOrderCommand command
        +List~OrderItem~ orderItems
        +BigDecimal discountedAmount
        +BigDecimal discountAmount
        +LocalDateTime occurredAt
    }
    
    class OrderCompletedEvent {
        +String eventId
        +Long orderId
        +Long userId
        +List~OrderItem~ orderItems
        +BigDecimal totalAmount
        +LocalDateTime occurredAt
    }
    
    class OrderProcessingFailedEvent {
        +String eventId
        +CreateOrderCommand command
        +String errorMessage
        +LocalDateTime occurredAt
    }
```

### 6.2 보상 트랜잭션 이벤트
```mermaid
classDiagram
    class StockRestorationRequestedEvent {
        +String requestId
        +Long productId
        +Integer quantity
        +String reason
    }
    
    class CouponRestorationRequestedEvent {
        +String requestId
        +Long userId
        +Long userCouponId
        +String reason
    }
    
    class BalanceRestorationRequestedEvent {
        +String requestId
        +Long userId
        +BigDecimal amount
        +String reason
    }
```

## 7. 데이터 일관성 보장 방안

### 7.1 Eventually Consistent
- **최종 일관성**: 모든 이벤트 처리 완료 후 데이터 일관성 보장
- **중간 상태 허용**: 처리 중인 임시 불일치 상태 허용
- **멱등성**: 동일한 이벤트의 중복 처리 방지

### 7.2 Saga 상태 관리
```mermaid
stateDiagram-v2
    [*] --> OrderCreated: 주문 생성
    OrderCreated --> StockReserved: 재고 예약
    StockReserved --> CouponApplied: 쿠폰 적용
    CouponApplied --> PaymentProcessed: 결제 처리
    PaymentProcessed --> OrderCompleted: 주문 완료
    OrderCompleted --> [*]
    
    StockReserved --> StockRestored: 재고 복원
    CouponApplied --> CouponRestored: 쿠폰 복원
    CouponRestored --> StockRestored
    PaymentProcessed --> PaymentRefunded: 결제 환불
    PaymentRefunded --> CouponRestored
    
    StockRestored --> OrderFailed: 주문 실패
    OrderFailed --> [*]
```
## 8. 구현 시 고려사항

### 8.1 기술적 고려사항
- **메시지 브로커 선택**: Kafka, RabbitMQ, AWS SQS/SNS
- **이벤트 저장소**: Event Sourcing 적용
- **중복 처리 방지**: 멱등성 키 사용
- **순서 보장**: 파티셔닝 전략

### 8.2 운영적 고려사항
- **장애 복구**: Dead Letter Queue 활용
- **성능 최적화**: 비동기 배치 처리
- **확장성**: 서비스별 독립적 스케일링
- **보안**: 이벤트 암호화 및 인증

## 9. 결론

분산 마이크로서비스 환경에서는 전통적인 ACID 트랜잭션의 한계를 인정하고, 이벤트 기반 코레오그래피 패턴을 통해 최종 일관성을 보장하려고 하였습니다.

**핵심 이점:**
- **느슨한 결합**: 서비스 간 독립성 보장
- **확장성**: 각 서비스별 독립적 확장
- **장애 격리**: 부분 실패가 전체 시스템에 미치는 영향 최소화
- **가시성**: 이벤트 기반 모니터링 및 추적

**주의사항:**
- **복잡성 증가**: 분산 시스템의 본질적 복잡성
- **디버깅 어려움**: 분산된 로직의 디버깅 복잡성
- **최종 일관성**: 즉시 일관성이 필요한 경우 부적합

이러한 trade-off를 충분히 고려하여 비즈니스 요구사항에 맞는 적절한 설계를 선택해야 합니다.