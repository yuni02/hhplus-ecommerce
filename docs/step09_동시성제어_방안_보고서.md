

## 요약


문제 원인: 이중 락으로 인한 동시성 제어 실패
- loadProductByIdWithLock() 호출 후 deductStock() 재호출로 락이 중복 적용
- 락 획득 순서 불일치로 데드락 위험성 존재

해결 방법:
1. 재고 차감을 먼저 실행: deductStockWithPessimisticLock() 호출로 락과 재고 차감을 원자적으로 처리
2. 상품 정보는 별도 조회: 락 없는 loadProductById() 사용


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

3. **쿠폰 사용 동시성 제어**
   - `UserCouponEntity`에 `@Version` 필드 추가
   - `UserCouponPersistenceAdapter`에 비관적 락 메서드 구현
   - `findByIdWithLock()` 메서드로 쿠폰 중복 사용 방지

4. **주문 서비스 동시성 제어**
   - `CreateOrderService`에서 모든 비관적 락 메서드 사용
   - 재고 차감 → 쿠폰 사용 → 잔액 차감 순서로 처리
   - 실패 시 재고 복구 로직 포함

###  **동시성 테스트**

- **재고 1개 상품 동시 주문**: 정확히 1개만 성공, 나머지는 실패
- **잔액 부족 동시 주문**: 모든 요청 실패
- **쿠폰 중복 사용**: 정확히 1개만 성공, 나머지는 실패

### 🎯 **비관적 락의 장점**

1. **즉시 락 획득**: 다른 트랜잭션이 대기
2. **데이터 일관성**: 강력한 보장
3. **예측 가능한 순서**: 주문 시스템에 적합
4. **데드락 위험**: 하지만 순서가 정해져 있어 관리 가능

이제 주문/결제 시스템에서 동시성 문제가 해결되어 재고 차감, 잔액 차감, 쿠폰 사용이 안전하게 처리됩니다! 🚀