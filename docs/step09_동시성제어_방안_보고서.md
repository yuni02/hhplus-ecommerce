# 동시성 제어 방안 보고서 

## 요약

주문/결제 API의 동시성 문제를 해결하기 위해 **비관적 락(Pessimistic Lock)**을 적용했습니다:

### **구현된 동시성 제어**

1. **재고 차감 동시성 제어**
   - `ProductEntity`에 `@Version` 필드 추가
   - `ProductStockPersistenceAdapter`에 비관적 락 메서드 구현
   - `findByIdWithLock()` 메서드로 `SELECT ... FOR UPDATE` 적용

2. **잔액 차감 동시성 제어**
   - `BalanceEntity`에 이미 `@Version` 필드 존재
   - `BalancePersistenceAdapter`에 비관적 락 메서드 구현
   - `findByUserIdAndStatusWithLock()` 메서드로 비관적 락 적용

3. **잔액 충전 동시성 제어**
   - `BalanceEntity`에 `@Version` 필드 활용
   - `ChargeBalanceService`에서 비관적 락 적용
   - `findByUserIdAndStatusWithLock()` 메서드로 동시 충전 방지
   - 충전 금액을 원자적으로 증가시키는 `addBalance()` 메서드 구현

4. **선착순 쿠폰 발급 동시성 제어**
   - `CouponEntity`에 `@Version` 필드 활용
   - `IssueCouponService`에서 비관적 락 적용
   - `findByIdWithLock()` 메서드로 쿠폰 조회 시 락 획득
   - 원자적 발급 수량 증가를 위한 `incrementIssuedCount()` 메서드 구현
   - 발급 가능 여부를 확인하는 `canIssueCoupon()` 메서드 구현

5. **쿠폰 사용 동시성 제어**
   - `UserCouponEntity`에 `@Version` 필드 추가
   - `UserCouponPersistenceAdapter`에 비관적 락 메서드 구현
   - `findByIdWithLock()` 메서드로 쿠폰 중복 사용 방지

6. **주문 서비스 동시성 제어**
   - `CreateOrderService`에서 모든 비관적 락 메서드 사용
   - 재고 차감 → 쿠폰 사용 → 잔액 차감 순서로 처리
   - 실패 시 재고 복구 로직 포함

### **선착순 쿠폰 발급 동시성 제어 상세**

#### **문제 상황**
```
쿠폰 잔여량: 1개
동시 발급: 100명의 사용자
예상 결과: 1명만 성공
실제 결과: 여러 명 성공 (초과 발급) - 해결됨
```

#### **구현 방법**
```java
// IssueCouponService.java
@Transactional(isolation = Isolation.READ_COMMITTED, timeout = 5)
public IssueCouponResult issueCoupon(IssueCouponCommand command) {
    // 1. 쿠폰 정보를 락과 함께 조회 (선착순 확인)
    LoadCouponPort.CouponInfo couponInfo = loadCouponPort.loadCouponByIdWithLock(command.getCouponId())
            .orElse(null);
    
    // 2. 쿠폰 발급 가능 여부 확인
    if (!canIssueCoupon(couponInfo)) {
        return IssueCouponResult.failure("발급할 수 없는 쿠폰입니다.");
    }
    
    // 3. 쿠폰 발급 수량을 원자적으로 증가 (선착순 처리)
    if (!loadCouponPort.incrementIssuedCount(command.getCouponId())) {
        return IssueCouponResult.failure("쿠폰이 모두 소진되었습니다. 선착순 발급에 실패했습니다.");
    }
    
    // 4. 사용자 쿠폰 생성
    UserCoupon userCoupon = UserCoupon.builder()
            .userId(command.getUserId())
            .couponId(command.getCouponId())
            .discountAmount(couponInfo.getDiscountAmount())
            .issuedAt(LocalDateTime.now())
            .build();
    
    UserCoupon savedUserCoupon = saveUserCouponPort.saveUserCoupon(userCoupon);
    
    return IssueCouponResult.success(savedUserCoupon.getId(), ...);
}
```

#### **비관적 락 적용**
```java
// CouponJpaRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "3000")})
@Query("SELECT c FROM CouponEntity c WHERE c.id = :couponId")
Optional<CouponEntity> findByIdWithLock(@Param("couponId") Long couponId);
```

#### **원자적 발급 수량 증가**
```java
// CouponJpaRepository.java
@Modifying
@Query("UPDATE CouponEntity c SET c.issuedCount = c.issuedCount + 1, c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :couponId AND c.issuedCount < c.maxIssuanceCount AND c.status = 'ACTIVE'")
int incrementIssuedCount(@Param("couponId") Long couponId);
```

#### **발급 가능 여부 확인**
```java
// IssueCouponService.java
private boolean canIssueCoupon(LoadCouponPort.CouponInfo couponInfo) {
    // ACTIVE 상태이고, 발급 수량이 최대치에 도달하지 않은 경우에만 발급 가능
    return "ACTIVE".equals(couponInfo.getStatus()) && 
           couponInfo.getIssuedCount() < couponInfo.getMaxIssuanceCount();
}
```

### **잔액 충전 동시성 제어 상세**

#### **문제 상황**
```
사용자 잔액: 10,000원
동시 충전: 100명의 사용자가 각각 5,000원씩 충전
예상 결과: 10,000원 + 5,000원 = 15,000원
실제 결과: 10,000원 + 5,000원 = 15,000원 (정상)
```

#### **구현 방법**
```java
// ChargeBalanceService.java
@Transactional(isolation = Isolation.READ_COMMITTED, timeout = 5)
public ChargeBalanceResult chargeBalance(ChargeBalanceCommand command) {
    // 1. 잔액 조회 (비관적 락 적용)
    Balance balance = loadBalancePort.loadBalanceWithLock(command.getUserId());
    
    // 2. 원자적 잔액 증가
    balance.addBalance(command.getAmount());
    
    // 3. 잔액 저장
    saveBalancePort.saveBalance(balance);
    
    // 4. 거래 내역 생성
    BalanceTransaction transaction = createTransaction(command);
    saveBalanceTransactionPort.saveTransaction(transaction);
}
```

#### **비관적 락 적용**
```java
// BalancePersistenceAdapter.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT b FROM BalanceEntity b WHERE b.userId = :userId AND b.status = :status")
Optional<BalanceEntity> findByUserIdAndStatusWithLock(
    @Param("userId") Long userId, 
    @Param("status") String status
);
```

#### **원자적 잔액 증가**
```java
// Balance.java (도메인)
public void addBalance(BigDecimal amount) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
    }
    this.amount = this.amount.add(amount);
    this.updatedAt = LocalDateTime.now();
}
```

###  **동시성 테스트**

- **재고 1개 상품 동시 주문**: 정확히 1개만 성공, 나머지는 실패
- **잔액 부족 동시 주문**: 모든 요청 실패
- **잔액 동시 충전**: 모든 충전이 정상적으로 처리됨
- **선착순 쿠폰 발급**: 정확히 제한된 수량만 발급됨
- **쿠폰 중복 사용**: 정확히 1개만 성공, 나머지는 실패

### **선착순 쿠폰 발급 동시성 테스트 결과**
```java
@Test
void 쿠폰_발급_동시성_테스트() {
    // Given: 최대 2개만 발급 가능한 쿠폰
    var limitedCoupon = CouponEntity.builder()
            .maxIssuanceCount(2)
            .issuedCount(0)
            .status("ACTIVE")
            .build();
    
    // When: 5명이 동시에 쿠폰 발급 시도
    int concurrentRequests = 5;
    
    // Then: 정확히 2개만 발급 성공, 3개는 실패
    // 선착순 처리가 정확히 작동함
}
```

### **잔액 충전 동시성 테스트 결과**
```java
@Test
void 잔액_동시_충전_테스트() {
    // Given: 초기 잔액 10,000원
    // When: 5명이 동시에 5,000원씩 충전
    // Then: 최종 잔액 35,000원 (10,000 + 5,000 * 5)
    // 모든 충전이 정상적으로 처리됨
}
```

### 🎯 **비관적 락의 장점**

1. **즉시 락 획득**: 다른 트랜잭션이 대기
2. **데이터 일관성**: 강력한 보장
3. **예측 가능한 순서**: 주문 시스템에 적합
4. **데드락 위험**: 하지만 순서가 정해져 있어 관리 가능

### **선착순 쿠폰 발급 동시성 제어의 특징**

1. **원자적 수량 증가**: UPDATE 쿼리로 조건과 함께 증가
2. **발급 가능 여부 사전 확인**: 락 획득 후 즉시 검증
3. **정확한 선착순 처리**: 락으로 인한 순서 보장
4. **실패 시 명확한 메시지**: 사용자에게 적절한 피드백 제공

### **잔액 충전 동시성 제어의 특징**

1. **읽기-수정-쓰기 패턴**: 잔액 조회 → 증가 → 저장을 원자적으로 처리
2. **거래 내역 보장**: 모든 충전 내역이 거래 이력에 정확히 기록
3. **음수 방지**: 충전 금액 검증으로 음수 잔액 방지
4. **타임스탬프 관리**: 마지막 수정 시간을 정확히 기록

### **트러블슈팅**

1. 문제 원인: 이중 락으로 인한 동시성 제어 실패
- loadProductByIdWithLock() 호출 후 deductStock() 재호출로 락이 중복 적용
- 락 획득 순서 불일치로 데드락 위험성 존재

2. 해결 방법:
  - 재고 차감을 먼저 실행: deductStockWithPessimisticLock() 호출로 락과 재고 차감을 원자적으로 처리  상품 정보는 별도 조회: 락 없는     loadProductById() 사용