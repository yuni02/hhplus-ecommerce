# Step 08: 쿼리 최적화 방안 보고서

## 📋 목차
1. [프로젝트 개요](#프로젝트-개요)
2. [데이터베이스 설계 이력](#데이터베이스-설계-이력)
3. [성능 최적화 전략](#성능-최적화-전략)
4. [동시성 제어 방안](#동시성-제어-방안)
5. [인덱스 설계](#인덱스-설계)
6. [트랜잭션 관리](#트랜잭션-관리)
7. [발견된 문제점 및 개선사항](#발견된-문제점-및-개선사항)
8. [결론 및 권장사항](#결론-및-권장사항)

## 🎯 프로젝트 개요

### 프로젝트 정보
- **프로젝트명**: HHPlus E-commerce Platform
- **기술 스택**: Spring Boot, JPA/Hibernate, MySQL, Testcontainers
- **아키텍처**: Hexagonal Architecture (Clean Architecture)
- **주요 도메인**: 사용자, 잔액, 쿠폰, 상품, 주문, 상품 통계

### 핵심 요구사항
- 실시간 잔액 충전 및 차감 (빈번한 업데이트 발생)
- 선착순 쿠폰 발급 시스템
- 재고 관리 및 주문 처리
- 상품 판매 통계 집계 (배치 처리로 병목 최소화)

## 🔄 데이터베이스 설계 이력

### 1. 잔액 테이블 설계 변천사

#### 초기 설계: 사용자 테이블 통합
```sql
-- 초기 설계 (사용자 테이블에 잔액 포함)
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    username VARCHAR(255),
    balance DECIMAL(15,2),  -- 잔액 필드 포함
    status VARCHAR(20)
);
```

**문제점 발견:**
- 잔액 업데이트 시 전체 사용자 레코드에 락 발생
- 잔액 조회 시에도 불필요한 락 대기 시간 발생
- 동시성 처리가 어려움

#### 개선 설계: 잔액 테이블 분리
```sql
-- 개선된 설계 (잔액 테이블 분리)
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    user_id BIGINT UNIQUE,
    username VARCHAR(255),
    status VARCHAR(20)
);

CREATE TABLE balances (
    id BIGINT PRIMARY KEY,
    user_id BIGINT UNIQUE,  -- 사용자별 1:1 관계
    amount DECIMAL(15,2),
    status VARCHAR(20),
    version BIGINT,         -- 낙관적 락
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

**개선 효과:**
- 잔액 업데이트 시 잔액 테이블만 락 발생
- 사용자 정보 조회와 잔액 조회 분리 가능
- 동시성 제어 용이성 향상
- **빈번한 잔액 업데이트에 대한 성능 최적화**: 잔액 충전/차감이 자주 발생하는 특성을 고려한 별도 테이블 분리

### 2. 사용자 ID 설계 개선

#### 초기 설계: 문자열 기반 사용자 ID
```sql
-- 초기 설계
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    user_id VARCHAR(50),    -- 문자열 사용자 ID
    username VARCHAR(255),
    status VARCHAR(20)
);
```

**문제점 발견:**
- 문자열 인덱스 성능 저하
- 정렬 및 비교 연산 비효율성
- 저장 공간 낭비

#### 개선 설계: 숫자 기반 사용자 ID
```sql
-- 개선된 설계
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,         -- 숫자 사용자 ID
    username VARCHAR(255),
    status VARCHAR(20)
);

-- 복합 인덱스 추가
CREATE INDEX idx_userid_status ON users (user_id, status);
```

**개선 효과:**
- 인덱스 성능 향상 (숫자 비교 > 문자열 비교)
- 저장 공간 절약
- 정렬 및 범위 검색 성능 향상

## ⚡ 성능 최적화 전략

### 1. 배치 처리 전략

#### 상품 통계 배치 처리
```java
// ProductStatsService.java - 별도 배치 API
@Service
public class UpdateProductStatsService {
    
    @Transactional
    public UpdateProductStatsResult updateRecentProductStats(LocalDate targetDate) {
        // 배치 처리로 통계 데이터 집계
        // 실시간 집계 대신 배치로 병목 최소화
        List<ProductStats> stats = calculateProductStats(targetDate);
        saveProductStats(stats);
        return UpdateProductStatsResult.success(stats.size());
    }
}
```

**배치 처리의 장점:**
- **실시간 집계 병목 방지**: 주문 처리 중 통계 계산으로 인한 성능 저하 방지
- **리소스 효율성**: 대량 데이터 처리 시 배치 단위로 최적화
- **일관성 보장**: 특정 시점 기준으로 일관된 통계 데이터 제공

#### 잔액 업데이트 최적화
```java
// 잔액 업데이트는 실시간 처리하되, 락 최소화
@Transactional
public ChargeBalanceResult chargeBalance(ChargeBalanceCommand command) {
    // 비관적 락으로 정확성 보장하면서도 빠른 처리
    Balance balance = loadBalanceWithLock(command.getUserId());
    balance.charge(command.getAmount());
    saveBalance(balance);
    return ChargeBalanceResult.success(...);
}
```

**잔액 업데이트 특성:**
- **빈번한 업데이트**: 충전, 결제, 환불 등으로 인한 잦은 잔액 변경
- **정확성 중요**: 금융 거래이므로 데이터 정확성 최우선
- **동시성 제어**: 비관적 락으로 동시 업데이트 방지

### 2. 인덱스 설계 전략

#### 핵심 인덱스
```sql
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
```

#### 인덱스 설계 원칙
1. **WHERE 절 조건**: 자주 사용되는 검색 조건 우선
2. **ORDER BY 절**: 정렬이 필요한 컬럼 포함
3. **JOIN 조건**: 외래키 관계 컬럼
4. **복합 인덱스**: 카디널리티가 높은 컬럼을 앞에 배치

### 2. 쿼리 최적화 방안

#### N+1 문제 해결
```java
// 문제가 있는 코드
@Query("SELECT u FROM User u")
List<User> findAllUsers();
// 각 사용자마다 잔액 조회 쿼리 추가 실행

// 개선된 코드
@Query("SELECT u, b FROM User u LEFT JOIN Balance b ON u.userId = b.userId")
List<Object[]> findUsersWithBalance();
```

#### 배치 처리 최적화
```yaml
# application.yml
spring:
  jpa:
    properties:
      hibernate:
        jdbc.batch_size: 20        # 배치 크기 설정
        order_inserts: true        # INSERT 순서 최적화
        order_updates: true        # UPDATE 순서 최적화
```

## 🔒 동시성 제어 방안

### 1. 락 전략

#### 비관적 락 (Pessimistic Lock)
```java
// 잔액 충전/차감 시 비관적 락 사용
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT b FROM BalanceEntity b WHERE b.userId = :userId AND b.status = :status")
Optional<BalanceEntity> findByUserIdAndStatusWithLock(
    @Param("userId") Long userId, 
    @Param("status") String status
);
```

**적용 대상:**
- 잔액 충전/차감 (금융 거래 정확성 중요, 빈번한 업데이트)
- 쿠폰 발급 (선착순 처리)
- 재고 차감 (재고 정확성 중요)

#### 낙관적 락 (Optimistic Lock)
```java
// BalanceEntity.java
@Version
@Column(name = "version")
private Long version;
```

**적용 대상:**
- 상품 정보 업데이트
- 사용자 정보 수정
- 통계 데이터 집계

### 2. 트랜잭션 관리

#### 트랜잭션 경계 설정
```java
// 애플리케이션 서비스 레벨에서 트랜잭션 관리
@Service
public class ChargeBalanceService {
    
    @Transactional
    public ChargeBalanceResult chargeBalance(ChargeBalanceCommand command) {
        // 1. 사용자 존재 확인
        // 2. 잔액 조회 (비관적 락)
        // 3. 잔액 충전
        // 4. 거래 내역 생성
        // 5. 결과 반환
    }
}
```

#### 트랜잭션 전파 설정
```java
// 중첩 트랜잭션 방지를 위한 전파 설정
@Transactional(propagation = Propagation.REQUIRES_NEW)
public BalanceTransaction saveBalanceTransaction(BalanceTransaction transaction) {
    // 새로운 트랜잭션에서 실행
}
```

## 📊 데이터 타입 최적화

### 1. 금액 데이터 타입 통일

#### 문제가 있던 설계
```java
// UserCouponEntity.java
@Column(name = "discount_amount")
private Integer discountAmount;  // ❌ Integer 사용

// CouponEntity.java  
@Column(name = "discount_amount")
private BigDecimal discountAmount;  // ✅ BigDecimal 사용
```

#### 개선된 설계
```java
// 모든 금액 필드를 BigDecimal로 통일
@Column(name = "amount", precision = 15, scale = 2)
private BigDecimal amount;

@Column(name = "discount_amount", precision = 15, scale = 2)
private BigDecimal discountAmount;
```

### 2. 날짜/시간 데이터 타입 최적화

```java
// UTC 시간대 통일
@Column(name = "created_at")
private LocalDateTime createdAt;

@Column(name = "updated_at")
private LocalDateTime updatedAt;

// JPA 설정
spring:
  jpa:
    properties:
      hibernate:
        timezone.default_storage: NORMALIZE_UTC
        jdbc.time_zone: UTC
```

## 🚨 발견된 문제점 및 개선사항

### 1. 스키마 설계 문제

#### 제약조건명 중복
```sql
-- 문제가 있는 스키마
CREATE TABLE balances (
    user_id BIGINT NOT NULL,
    CONSTRAINT user_id UNIQUE (user_id)  -- ❌ 컬럼명과 동일한 제약조건명
);

-- 개선된 스키마
CREATE TABLE balances (
    user_id BIGINT NOT NULL,
    CONSTRAINT uk_balances_user_id UNIQUE (user_id)  -- ✅ 의미있는 제약조건명
);
```

#### 데이터 타입 불일치
```sql
-- 문제가 있는 스키마
CREATE TABLE order_history_events (
    discount_amount INT NULL,           -- ❌ 금액이 INT
    discounted_amount INT NOT NULL,     -- ❌ 금액이 INT
    total_amount INT NOT NULL           -- ❌ 금액이 INT
);

-- 개선된 스키마
CREATE TABLE order_history_events (
    discount_amount DECIMAL(15,2) NULL,     -- ✅ 정확한 금액 표현
    discounted_amount DECIMAL(15,2) NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL
);
```

### 2. 트랜잭션 관리 문제

#### 중첩 트랜잭션으로 인한 예외
```java
// 문제가 있는 코드
@Transactional  // 외부 트랜잭션
public ChargeBalanceResult chargeBalance(...) {
    saveBalanceTransactionPort.saveBalanceTransaction(transaction);  // 내부 트랜잭션
}

@Transactional  // 내부 트랜잭션 (전파 설정 없음)
public BalanceTransaction saveBalanceTransaction(...) {
    // UnexpectedRollbackException 발생 가능
}

// 개선된 코드
@Transactional
public ChargeBalanceResult chargeBalance(...) {
    saveBalanceTransactionPort.saveBalanceTransaction(transaction);  // 같은 트랜잭션
}

// @Transactional 제거 (상위 트랜잭션 사용)
public BalanceTransaction saveBalanceTransaction(...) {
    // 상위 트랜잭션 내에서 실행
}
```

### 3. 테스트 환경 설정 문제

#### 테스트 데이터베이스 설정
```yaml
# 문제가 있는 설정
spring:
  jpa:
    hibernate:
      ddl-auto: none  # ❌ 테이블 생성 안함
  sql:
    init:
      mode: always    # ❌ schema.sql 실행 시도

# 개선된 설정
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop  # ✅ 테스트용 테이블 자동 생성
  sql:
    init:
      mode: never            # ✅ schema.sql 실행 안함
```

## 📈 성능 모니터링 및 로깅

### 1. 쿼리 성능 모니터링

```yaml
# application.yml
logging:
  level:
    org.hibernate.SQL: DEBUG                # SQL 쿼리 로깅
    org.hibernate.type.descriptor.sql: TRACE # 바인딩 파라미터 로깅
    org.springframework.orm.jpa: DEBUG       # JPA 관련 로깅
```

### 2. P6Spy 설정

```yaml
# P6Spy 설정
decorator:
  datasource:
    p6spy:
      enable-logging: true
      multiline: true
      logging: slf4j
```

## 🎯 결론 및 권장사항

### 1. 핵심 개선 성과

1. **동시성 제어 강화**: 잔액 테이블 분리로 동시성 문제 해결
2. **성능 최적화**: 숫자 기반 사용자 ID 및 적절한 인덱스 설계
3. **데이터 정확성**: BigDecimal 사용으로 금액 계산 정확성 확보
4. **트랜잭션 안정성**: 트랜잭션 경계 명확화 및 중첩 트랜잭션 문제 해결
5. **배치 처리 최적화**: 통계 집계를 별도 배치 API로 분리하여 실시간 처리 병목 최소화
6. **빈번한 업데이트 대응**: 잔액 업데이트의 빈번한 특성을 고려한 테이블 분리 및 락 전략 수립

### 2. 향후 개선 방안

#### 단기 개선사항 (1-2주)
- [ ] 스키마 제약조건명 표준화
- [ ] 모든 금액 필드를 BigDecimal로 통일
- [ ] 테스트 환경 설정 최적화
- [ ] 디버그 로그 제거 및 로깅 레벨 조정

#### 중기 개선사항 (1-2개월)
- [ ] 외래키 제약조건 활성화 검토
- [ ] 추가 인덱스 성능 테스트 및 적용
- [ ] 배치 처리 최적화 및 스케줄링 시스템 구축
- [ ] 쿼리 성능 모니터링 대시보드 구축
- [ ] 잔액 업데이트 성능 모니터링 및 최적화

#### 장기 개선사항 (3-6개월)
- [ ] 읽기 전용 복제본 도입 검토
- [ ] 캐싱 전략 수립 및 적용
- [ ] 데이터 파티셔닝 전략 수립
- [ ] 마이크로서비스 아키텍처 전환 검토

### 3. 권장사항

1. **정기적인 성능 모니터링**: 쿼리 실행 시간, 인덱스 사용률 모니터링
2. **코드 리뷰 강화**: 트랜잭션 경계, 락 사용 패턴 리뷰
3. **테스트 자동화**: 성능 테스트 및 동시성 테스트 자동화
4. **문서화**: 데이터베이스 설계 의사결정 및 변경 이력 문서화
