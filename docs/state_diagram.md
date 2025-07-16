# 도메인 기반 상태 다이어그램

## 1️⃣ 주문(Order) 상태 다이어그램

```mermaid
stateDiagram-v2
    [*] --> PENDING : 주문 생성 요청

    PENDING --> VALIDATING : 재고/쿠폰 검증 시작
    PENDING --> CANCELLED : 입력값 검증 실패

    VALIDATING --> PROCESSING : 검증 통과, 결제 처리 시작
    VALIDATING --> CANCELLED : 재고 부족
    VALIDATING --> CANCELLED : 상품 존재하지 않음
    VALIDATING --> CANCELLED : 쿠폰 사용 불가

    PROCESSING --> COMPLETED : 결제 성공
    PROCESSING --> CANCELLED : 잔액 부족
    PROCESSING --> CANCELLED : 사용자 없음
    PROCESSING --> CANCELLED : 결제 실패

    COMPLETED --> [*]
    CANCELLED --> [*]

    note right of VALIDATING : 재고 검증 (FOR UPDATE), 쿠폰 유효성 검증, 할인 금액 계산
    note right of PROCESSING : 잔액 차감, 쿠폰 사용 처리, 통계 업데이트
```

## 2️⃣ 상품(Product) 재고 상태 다이어그램

```mermaid
stateDiagram-v2
    [*] --> ACTIVE : 상품 등록

    ACTIVE --> ACTIVE : 재고 보충
    ACTIVE --> OUT_OF_STOCK : 재고 소진 (stock = 0)
    ACTIVE --> INACTIVE : 관리자가 비활성화

    OUT_OF_STOCK --> ACTIVE : 재고 보충
    OUT_OF_STOCK --> INACTIVE : 관리자가 비활성화

    INACTIVE --> ACTIVE : 관리자가 활성화 (재고 있는 경우)
    INACTIVE --> OUT_OF_STOCK : 관리자가 활성화 (재고 없는 경우)

    note right of ACTIVE : 주문 가능, 재고 차감 가능
    note right of OUT_OF_STOCK : 주문 불가, 재고 보충 필요
    note right of INACTIVE : 주문 불가, 관리자 판단으로 중단
```

## 3️⃣ 쿠폰(Coupon) 상태 다이어그램

```mermaid
stateDiagram-v2
    [*] --> ACTIVE : 쿠폰 생성

    ACTIVE --> SOLD_OUT : 발급 수량 소진 (issued_count >= total_quantity)
    ACTIVE --> EXPIRED : 발급 기간 만료

    SOLD_OUT --> [*]
    EXPIRED --> [*]

    note right of ACTIVE : 발급 가능, issued_count < total_quantity
    note right of SOLD_OUT : 발급 불가, 수량 소진
    note right of EXPIRED : 발급 불가, 기간 만료
```

## 4️⃣ 사용자 쿠폰(UserCoupon) 상태 다이어그램

```mermaid
stateDiagram-v2
    [*] --> AVAILABLE : 쿠폰 발급 성공

    AVAILABLE --> USED : 주문에서 사용
    AVAILABLE --> EXPIRED : 사용 기간 만료

    USED --> AVAILABLE : 주문 취소/실패 시 복구

    USED --> [*] : 주문 완료 (최종)
    EXPIRED --> [*]

    note right of AVAILABLE : 사용 가능한 상태, 주문 시 적용 가능
    note right of USED : 주문에서 사용됨 (일시적, 복구 가능)
    note right of EXPIRED : 사용 기간 만료 (최종 상태)
```

## 5️⃣ 잔액 거래(UserBalanceTransaction) 상태 다이어그램

```mermaid
stateDiagram-v2
    [*] --> PENDING : 거래 요청 생성

    PENDING --> PROCESSING : 거래 처리 시작

    PROCESSING --> COMPLETED : 거래 성공
    PROCESSING --> FAILED : 거래 실패

    COMPLETED --> [*]
    FAILED --> [*]

    note right of PENDING : 거래 대기 상태, 초기 생성 시점
    note right of PROCESSING : 거래 처리 중, 잔액 업데이트 진행
    note right of COMPLETED : 거래 완료 - DEPOSIT: 잔액 증가, PAYMENT: 잔액 차감
    note right of FAILED : 거래 실패, 잔액 부족 등의 이유
```

## 6️⃣ 주문 프로세스 전체 상태 플로우

```mermaid
stateDiagram-v2
    [*] --> OrderRequested : 사용자 주문 요청

    OrderRequested --> ValidatingInputs : 입력값 검증
    ValidatingInputs --> ValidatingStock : 재고 검증
    ValidatingStock --> ValidatingCoupon : 쿠폰 검증
    ValidatingCoupon --> ProcessingPayment : 결제 처리
    ProcessingPayment --> UpdatingInventory : 재고 차감
    UpdatingInventory --> ApplyingCoupon : 쿠폰 사용
    ApplyingCoupon --> RecordingTransaction : 거래 기록
    RecordingTransaction --> UpdatingStatistics : 통계 업데이트
    UpdatingStatistics --> SendingToDataPlatform : 데이터 플랫폼 전송
    SendingToDataPlatform --> OrderCompleted : 주문 완료

    ValidatingInputs --> OrderFailed : 입력값 오류
    ValidatingStock --> RollingBack : 재고 부족
    ValidatingCoupon --> RollingBack : 쿠폰 무효
    ProcessingPayment --> RollingBack : 잔액 부족
    UpdatingInventory --> RollingBack : 재고 업데이트 실패
    ApplyingCoupon --> RollingBack : 쿠폰 사용 실패

    RollingBack --> RestoringStock : 재고 복원
    RestoringStock --> RestoringCoupon : 쿠폰 복원
    RestoringCoupon --> OrderFailed : 롤백 완료

    OrderCompleted --> [*]
    OrderFailed --> [*]

    note right of ValidatingStock : FOR UPDATE로 동시성 제어
    note right of ProcessingPayment : 잔액 차감 처리, 거래 기록 생성
    note right of RollingBack : 트랜잭션 롤백, 모든 변경사항 복구
```

## 7️⃣ 쿠폰 발급 프로세스 상태 다이어그램

```mermaid
stateDiagram-v2
    [*] --> CouponIssueRequested : 쿠폰 발급 요청

    CouponIssueRequested --> AcquiringLock : 분산락 획득 시도

    AcquiringLock --> LockAcquired : 락 획득 성공
    AcquiringLock --> IssueFailed : 락 획득 실패 (타임아웃)

    LockAcquired --> ValidatingCoupon : 쿠폰 유효성 검증

    ValidatingCoupon --> CheckingDuplicate : 중복 발급 확인
    ValidatingCoupon --> ReleasingLock : 쿠폰 무효/소진

    CheckingDuplicate --> CreatingUserCoupon : 발급 가능
    CheckingDuplicate --> ReleasingLock : 이미 발급받음

    CreatingUserCoupon --> UpdatingCouponCount : 사용자 쿠폰 생성
    UpdatingCouponCount --> ReleasingLock : 발급 수량 업데이트

    ReleasingLock --> IssueCompleted : 발급 성공
    ReleasingLock --> IssueFailed : 발급 실패

    IssueCompleted --> [*]
    IssueFailed --> [*]

    note right of AcquiringLock : Redis 분산락, couponId 기준
    note right of ValidatingCoupon : FOR UPDATE로 쿠폰 정보 조회
    note right of CreatingUserCoupon : 원자적 처리, 트랜잭션 보장
```

## 8️⃣ 통계 업데이트 상태 다이어그램

```mermaid
stateDiagram-v2
    [*] --> StatUpdateRequested : 통계 업데이트 요청

    StatUpdateRequested --> UpdatingDailyStat : 일별 통계 업데이트

    UpdatingDailyStat --> StatUpdateCompleted : 업데이트 성공
    UpdatingDailyStat --> StatUpdateFailed : 업데이트 실패

    StatUpdateFailed --> QueueForRetry : 재시도 큐에 추가
    QueueForRetry --> UpdatingDailyStat : 재시도

    StatUpdateCompleted --> UpdatingCache : 캐시 무효화
    UpdatingCache --> [*]

    note right of UpdatingDailyStat : UPSERT 방식, product_id + date 기준
    note right of QueueForRetry : 비동기 재처리, 별도 배치 작업
```

## 9️⃣ 캐시 관리 상태 다이어그램

```mermaid
stateDiagram-v2
    [*] --> CacheEmpty : 캐시 없음

    CacheEmpty --> CacheLoading : 데이터 로딩 시작
    CacheLoading --> CacheHit : 로딩 완료
    CacheLoading --> CacheLoadFailed : 로딩 실패

    CacheHit --> CacheExpired : TTL 만료
    CacheHit --> CacheInvalidated : 수동 무효화

    CacheExpired --> CacheEmpty : 캐시 제거
    CacheInvalidated --> CacheEmpty : 캐시 제거
    CacheLoadFailed --> CacheEmpty : 재시도 대기

    note right of CacheHit : 상품 목록 TTL 5분
    note right of CacheInvalidated : 데이터 변경 시 즉시 무효화
```

## 상태 다이어그램 설명

### 주요 특징

1. **트랜잭션 기반 상태 관리**: 각 도메인의 상태 변경은 데이터베이스 트랜잭션과 연동
2. **동시성 제어**: 분산락과 FOR UPDATE를 통한 안전한 상태 전환
3. **실패 처리**: 모든 상태에서 실패 시나리오와 롤백 메커니즘 포함
4. **비동기 처리**: 통계 업데이트와 외부 시스템 연동은 별도 상태로 관리

### 핵심 상태 전환 패턴

- **주문**: PENDING → VALIDATING → PROCESSING → COMPLETED/CANCELLED
- **재고**: ACTIVE ↔ OUT_OF_STOCK ↔ INACTIVE
- **쿠폰**: ACTIVE → SOLD_OUT/EXPIRED
- **사용자쿠폰**: AVAILABLE → USED/EXPIRED (복구 가능)
- **거래**: PENDING → PROCESSING → COMPLETED/FAILED

### 동시성 제어 요소

- **분산락**: 쿠폰 발급 시 Redis 락 사용
- **DB 락**: 재고 차감과 잔액 처리 시 FOR UPDATE 사용
- **원자적 연산**: 모든 상태 변경은 트랜잭션 내에서 처리
