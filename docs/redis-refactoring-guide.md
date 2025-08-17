# Redis 중복 코드 제거 마이그레이션 가이드

## 개요

이 문서는 각 도메인별로 중복된 Redis 관련 코드를 공통 추상화 계층으로 통합하는 방법을 설명합니다.

## 문제점

### 기존 코드의 문제점
1. **중복된 Redis 작업 코드**: 각 도메인마다 비슷한 Redis 캐싱/락 로직이 반복됨
2. **일관성 부족**: 캐시 키 생성, 만료시간, 에러 처리 방식이 도메인마다 다름
3. **유지보수 어려움**: Redis 관련 변경사항이 여러 곳에 분산되어 있음
4. **테스트 복잡성**: 각 도메인별로 Redis 모킹이 필요함

## 해결 방안

### 1. 공통 추상화 계층 도입

#### RedisCacheManager
- 캐시 CRUD 작업을 위한 공통 인터페이스
- JSON 직렬화/역직렬화 처리
- 에러 처리 및 로깅 통합

#### DistributedLockManager (AOP 방식으로 대체)
- ~~분산락 획득/해제 로직 통합~~ → `@DistributedLock` 어노테이션으로 대체
- ~~락 타임아웃 및 재시도 전략 관리~~ → 어노테이션 속성으로 설정
- ~~Lua 스크립트를 활용한 안전한 락 해제~~ → AOP에서 자동 처리

#### CacheKeyGenerator
- 일관된 캐시 키 생성 전략
- 도메인별 키 생성 메서드 제공
- 버전 관리 지원

#### CacheConstants
- 캐시 만료시간, 락 타임아웃 등 상수 정의
- 도메인별 설정값 중앙화

### 2. 마이그레이션 단계

#### Step 1: 기존 코드 분석
```bash
# Redis 관련 코드 검색
find . -name "*.java" -exec grep -l "RedisTemplate\|redisTemplate" {} \;
```

#### Step 2: 공통 계층 도입
1. `RedisCacheManager` 구현
2. `@DistributedLock` AOP 구현 (기존 `DistributedLockManager` 대체)
3. `CacheKeyGenerator` 구현
4. `CacheConstants` 정의

#### Step 3: 도메인별 리팩토링
1. 기존 Redis 코드를 공통 계층 사용으로 변경
2. 캐시 키 생성 방식 통일
3. 에러 처리 로직 통합

#### Step 4: 테스트 업데이트
1. 공통 계층 모킹 설정
2. 도메인별 테스트 코드 수정
3. 통합 테스트 추가

### 3. 사용 예시

#### 기존 코드 (쿠폰 도메인)
```java
// 기존: 직접 RedisTemplate 사용
@Autowired
private RedisTemplate<String, Object> redisTemplate;

public void issueCoupon(Long couponId, Long userId) {
    String lockKey = "coupon:issue:" + couponId;
    String value = UUID.randomUUID().toString();
    
    Boolean acquired = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, value, Duration.ofSeconds(10));
    
    if (Boolean.TRUE.equals(acquired)) {
        try {
            // 쿠폰 발급 로직
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
}
```

#### 리팩토링 후 (AOP 방식)
```java
// 개선: AOP 어노테이션 사용
@DistributedLock(
    key = "coupon-issue:#{#couponId}",
    waitTime = 3,
    leaseTime = 10,
    timeUnit = TimeUnit.SECONDS
)
@Transactional
public void issueCoupon(Long couponId, Long userId) {
    // 쿠폰 발급 로직만 집중
    // 분산락은 AOP에서 자동 처리
}
```

### 4. 장점

#### 코드 중복 제거
- Redis 관련 코드가 70% 이상 감소
- 분산락 코드가 90% 이상 감소 (AOP 적용)
- 새로운 도메인 추가 시 어노테이션만 사용하면 됨

#### 일관성 향상
- 캐시 키 생성 방식 통일
- 에러 처리 및 로깅 표준화
- 만료시간 관리 중앙화

#### 유지보수성 개선
- Redis 관련 변경사항이 한 곳에서 관리됨
- 분산락 로직이 AOP에서 중앙 관리됨
- 버그 수정 및 기능 추가가 용이함
- 선언적 프로그래밍으로 코드 가독성 향상

#### 테스트 용이성
- 공통 계층만 모킹하면 모든 도메인 테스트 가능
- 테스트 코드 중복 제거

### 5. 주의사항

#### 점진적 마이그레이션
- 한 번에 모든 도메인을 변경하지 말고 단계적으로 진행
- 기존 코드와 새로운 코드를 병행 운영
- 충분한 테스트 후 기존 코드 제거

#### 성능 고려사항
- 공통 계층의 오버헤드 최소화
- 캐시 키 생성 시 문자열 연산 최적화
- 불필요한 직렬화/역직렬화 방지

#### 호환성 유지
- 기존 API 인터페이스 유지
- 점진적 마이그레이션을 위한 어댑터 패턴 활용

### 6. 모니터링 및 운영

#### 메트릭 수집
- 캐시 히트율 모니터링
- 락 경합 상황 추적
- 에러율 및 응답시간 측정

#### 로깅 전략
- 캐시 조회/저장 로그
- 락 획득/해제 로그
- 에러 상황 상세 로깅

#### 알림 설정
- 캐시 서비스 장애 알림
- 락 타임아웃 빈도 알림
- 메모리 사용량 임계값 알림

## 결론

Redis 중복 코드 제거를 통해 코드 품질과 유지보수성을 크게 향상시킬 수 있습니다. 공통 추상화 계층을 도입하고 점진적으로 마이그레이션하는 것이 안전하고 효과적인 방법입니다.
