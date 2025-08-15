# Redisson AOP 분산락 구현 가이드

## 개요

Redisson을 사용하여 AOP 방식으로 분산락을 구현했습니다. `@DistributedLock` 어노테이션을 메서드에 적용하면 자동으로 분산락이 획득되고 해제됩니다.

## 🚀 주요 특징

### 1. AOP 방식 구현
- 어노테이션 기반으로 간편한 사용
- 메서드 실행 전후 자동 락 관리
- 코드 침투성 최소화

### 2. Redisson 기반
- 안정적인 분산락 구현
- 자동 락 해제 (watchdog 메커니즘)
- 클러스터 환경 지원

### 3. SpEL 표현식 지원
- 동적 락 키 생성
- 메서드 파라미터 기반 키 생성
- 유연한 락 키 설계

## 📦 의존성

```kotlin
// build.gradle.kts
implementation("org.redisson:redisson-spring-boot-starter:3.27.1")
```

## ⚙️ 설정

### 1. Redisson 설정
```java
@Configuration
public class RedisConfig {

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
            .setAddress("redis://localhost:6379")
            .setConnectionPoolSize(10)
            .setConnectionMinimumIdleSize(5);
        return Redisson.create(config);
    }
}
```

### 2. AOP 설정
```java
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {
    private final RedissonClient redissonClient;
    
    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) {
        // 락 획득 및 해제 로직
    }
}
```

## 🎯 사용법

### 1. 기본 사용법
```java
@Service
public class CouponService {

    @DistributedLock
    @Transactional
    public IssueCouponResult issueCoupon(IssueCouponCommand command) {
        // 쿠폰 발급 로직
        // 자동으로 분산락 적용됨
    }
}
```

### 2. 커스텀 락 키 설정
```java
@Service
public class CouponService {

    @DistributedLock(
        key = "coupon-issue:#{#command.couponId}",
        waitTime = 5,
        leaseTime = 10,
        timeUnit = TimeUnit.SECONDS
    )
    @Transactional
    public IssueCouponResult issueCoupon(IssueCouponCommand command) {
        // 쿠폰 발급 로직
    }
}
```

### 3. 다양한 사용 예시

#### 쿠폰 발급
```java
@DistributedLock(
    key = "coupon-issue:#{#command.couponId}",
    waitTime = 5,
    leaseTime = 10
)
public IssueCouponResult issueCoupon(IssueCouponCommand command) {
    // 선착순 쿠폰 발급 로직
}
```

#### 잔액 충전
```java
@DistributedLock(
    key = "balance-charge:#{#userId}",
    waitTime = 3,
    leaseTime = 5
)
public void chargeBalance(Long userId, Integer amount) {
    // 잔액 충전 로직
}
```

#### 상품 재고 차감
```java
@DistributedLock(
    key = "product-stock:#{#productId}",
    waitTime = 2,
    leaseTime = 3
)
public boolean deductProductStock(Long productId, Integer quantity) {
    // 재고 차감 로직
}
```

## 🔧 어노테이션 속성

### @DistributedLock 속성

| 속성 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `key` | String | "" | 락 키 (SpEL 표현식 지원) |
| `waitTime` | long | 3 | 락 대기 시간 |
| `leaseTime` | long | 10 | 락 보유 시간 |
| `timeUnit` | TimeUnit | SECONDS | 시간 단위 |
| `throwException` | boolean | true | 락 획득 실패 시 예외 발생 여부 |

### SpEL 표현식 예시

```java
// 파라미터 값 사용
key = "user:#{#userId}"

// 메서드 호출
key = "product:#{#product.getId()}"

// 복합 표현식
key = "order:#{#userId}-#{#orderId}"

// 조건부 표현식
key = "coupon:#{#command.couponId != null ? #command.couponId : 'default'}"
```

## 📊 락 키 생성 규칙

### 1. 어노테이션에 key가 지정된 경우
```java
@DistributedLock(key = "custom-key:#{#param}")
```
- SpEL 표현식으로 평가
- 결과를 락 키로 사용

### 2. key가 지정되지 않은 경우
```java
@DistributedLock
```
- 기본 형식: `lock:클래스명.메서드명:파라미터값들`
- 예시: `lock:CouponService.issueCoupon:1001-123`

## 🛡️ 안전성 보장

### 1. 자동 락 해제
- Redisson의 watchdog 메커니즘
- 메서드 실행 완료 후 자동 해제
- 예외 발생 시에도 안전한 해제

### 2. 락 소유권 확인
```java
if (lock.isHeldByCurrentThread()) {
    lock.unlock();
}
```

### 3. 타임아웃 설정
- `waitTime`: 락 획득 대기 시간
- `leaseTime`: 락 보유 시간
- 자동 해제로 데드락 방지

## 🚨 예외 처리

### 1. 락 획득 실패
```java
if (!acquired) {
    if (distributedLock.throwException()) {
        throw new RuntimeException("Failed to acquire distributed lock: " + lockKey);
    }
    return null;
}
```

### 2. 인터럽트 처리
```java
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    throw new RuntimeException("Lock acquisition interrupted", e);
}
```

## 📈 성능 최적화

### 1. 락 키 설계
```java
// 좋은 예: 구체적이고 짧은 키
key = "coupon:#{#couponId}"

// 나쁜 예: 너무 긴 키
key = "very-long-lock-key-with-many-parameters-and-descriptions"
```

### 2. 타임아웃 설정
```java
// 빠른 처리: 짧은 타임아웃
@DistributedLock(waitTime = 1, leaseTime = 2)

// 느린 처리: 긴 타임아웃
@DistributedLock(waitTime = 10, leaseTime = 30)
```

### 3. 락 범위 최소화
```java
// 락이 필요한 부분만 분리
@DistributedLock(key = "stock:#{#productId}")
public void updateStock(Long productId, Integer quantity) {
    // 재고 업데이트만 락 적용
}

public void processOrder(OrderRequest request) {
    // 주문 처리 로직
    updateStock(request.getProductId(), request.getQuantity());
    // 다른 로직들...
}
```

## 🔍 모니터링

### 1. 로그 레벨 설정
```properties
# application.yml
logging:
  level:
    kr.hhplus.be.server.shared.lock: DEBUG
```

### 2. 락 상태 확인
```java
@Autowired
private RedissonClient redissonClient;

public boolean isLocked(String lockKey) {
    RLock lock = redissonClient.getLock(lockKey);
    return lock.isLocked();
}
```

## 결론

Redisson AOP 분산락을 사용하면:

1. **간편한 사용**: 어노테이션만으로 분산락 적용
2. **안정성**: Redisson의 검증된 분산락 구현
3. **유연성**: SpEL 표현식으로 동적 락 키 생성
4. **성능**: 최적화된 락 관리 및 자동 해제
5. **모니터링**: 상세한 로그 및 상태 확인

이를 통해 동시성 문제를 효과적으로 해결하면서도 코드의 복잡성을 최소화할 수 있습니다! 
