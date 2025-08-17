# 캐시 전략 가이드 - 핵심 기능 중심

## 개요

모든 기능에 캐시를 적용하는 것은 오버엔지니어링입니다. 실제로 **자주 조회되고 대용량으로 처리되는 핵심 기능들**만 선별적으로 캐시를 적용하는 것이 효율적입니다.

## 🎯 캐시 적용 기준

### 1. 조회 빈도 (Read Frequency)
- **높음**: 초당 수십~수백 회 조회
- **중간**: 분당 수십 회 조회  
- **낮음**: 시간당 수 회 조회

### 2. 데이터 크기 (Data Size)
- **대용량**: 수천~수만 건의 데이터
- **중간**: 수백 건의 데이터
- **소용량**: 수십 건 이하의 데이터

### 3. 데이터 변경 빈도 (Update Frequency)
- **낮음**: 하루에 1-2회 변경
- **중간**: 시간당 1-2회 변경
- **높음**: 분당 수십 회 변경

## 📊 캐시 적용 대상 선별

### ✅ **캐시 적용 대상 (핵심 기능)**

#### 1. 상품 도메인
```java
// 🎯 인기 상품 목록 조회
- 조회 빈도: 높음 (초당 수백 회)
- 데이터 크기: 대용량 (수천 건)
- 변경 빈도: 낮음 (하루 1-2회)
- TTL: 1시간

// 🎯 상품 상세 정보 조회  
- 조회 빈도: 높음 (초당 수백 회)
- 데이터 크기: 중간 (개별 상품)
- 변경 빈도: 낮음 (시간당 1-2회)
- TTL: 30분

// 🎯 상품 목록 조회
- 조회 빈도: 중간 (분당 수십 회)
- 데이터 크기: 대용량 (수천 건)
- 변경 빈도: 중간 (시간당 1-2회)
- TTL: 10분
```

#### 2. 쿠폰 도메인
```java
// 🎯 사용자 쿠폰 목록 조회
- 조회 빈도: 높음 (초당 수십 회)
- 데이터 크기: 중간 (수백 건)
- 변경 빈도: 중간 (시간당 수십 회)
- TTL: 5분

// 🎯 쿠폰 정보 조회
- 조회 빈도: 높음 (초당 수백 회)
- 데이터 크기: 중간 (개별 쿠폰)
- 변경 빈도: 낮음 (하루 1-2회)
- TTL: 30분

// 🎯 사용 가능한 쿠폰 목록
- 조회 빈도: 높음 (초당 수십 회)
- 데이터 크기: 중간 (수백 건)
- 변경 빈도: 높음 (분당 수십 회)
- TTL: 3분
```

### ❌ **캐시 미적용 대상**

#### 1. 잔액 도메인
```java
// ❌ 잔액 조회 - 캐시 미적용
- 이유: 실시간 정확성이 중요, 변경 빈도가 높음
- 대안: DB 조회 최적화 (인덱스, 락 최적화)

// ❌ 잔액 거래 내역 - 캐시 미적용  
- 이유: 개인정보, 실시간 정확성 필요
- 대안: 페이징 처리, 인덱스 최적화
```

#### 2. 주문 도메인
```java
// ❌ 주문 조회 - 캐시 미적용
- 이유: 실시간 정확성, 개인정보
- 대안: DB 조회 최적화

// ❌ 주문 생성 - 캐시 미적용
- 이유: 트랜잭션 처리, 실시간 처리 필요
- 대안: 분산락, 트랜잭션 최적화
```

#### 3. 사용자 도메인
```java
// ❌ 사용자 정보 조회 - 캐시 미적용
- 이유: 개인정보, 보안상 민감
- 대안: DB 조회 최적화
```

## 🔧 구현 예시

### 1. 상품 캐시 서비스
```java
@Service
public class CachedProductService {
    
    // 🎯 인기 상품 목록 조회 (캐시 적용)
    public List<ProductInfo> getPopularProducts() {
        String cacheKey = "product:stats:popular";
        
        // 1. 캐시에서 조회
        Optional<List<ProductInfo>> cached = cacheManager.get(cacheKey, List.class);
        if (cached.isPresent()) {
            return cached.get();
        }
        
        // 2. DB에서 조회 (복잡한 통계 쿼리)
        List<ProductInfo> products = loadPopularProductsFromDB();
        
        // 3. 캐시에 저장 (TTL: 1시간)
        cacheManager.set(cacheKey, products, Duration.ofHours(1));
        
        return products;
    }
    
    // 🎯 상품 상세 정보 조회 (캐시 적용)
    public Optional<ProductInfo> getProductDetail(Long productId) {
        String cacheKey = "product:detail:" + productId;
        
        // 1. 캐시에서 조회
        Optional<ProductInfo> cached = cacheManager.get(cacheKey, ProductInfo.class);
        if (cached.isPresent()) {
            return cached;
        }
        
        // 2. DB에서 조회
        Optional<ProductInfo> product = loadProductFromDB(productId);
        
        // 3. 캐시에 저장 (TTL: 30분)
        product.ifPresent(p -> cacheManager.set(cacheKey, p, Duration.ofMinutes(30)));
        
        return product;
    }
}
```

### 2. 쿠폰 캐시 서비스
```java
@Service
public class CachedCouponService {
    
    // 🎯 사용자 쿠폰 목록 조회 (캐시 적용)
    public List<UserCouponInfo> getUserCoupons(Long userId) {
        String cacheKey = "user-coupon:list:" + userId;
        
        // 1. 캐시에서 조회
        Optional<List<UserCouponInfo>> cached = cacheManager.get(cacheKey, List.class);
        if (cached.isPresent()) {
            return cached.get();
        }
        
        // 2. DB에서 조회
        List<UserCouponInfo> coupons = loadUserCouponsFromDB(userId);
        
        // 3. 캐시에 저장 (TTL: 5분)
        cacheManager.set(cacheKey, coupons, Duration.ofMinutes(5));
        
        return coupons;
    }
}
```

## 📈 성능 최적화 전략

### 1. TTL 설정 기준
```java
// 데이터 변경 빈도에 따른 TTL 설정
- 낮음 (하루 1-2회): 1시간 ~ 6시간
- 중간 (시간당 1-2회): 10분 ~ 30분  
- 높음 (분당 수십 회): 1분 ~ 5분
```

### 2. 캐시 무효화 전략
```java
// 데이터 변경 시에만 캐시 무효화
public void updateProduct(Long productId, ProductUpdateRequest request) {
    // 1. 비즈니스 로직 실행
    productRepository.update(productId, request);
    
    // 2. 관련 캐시만 무효화
    invalidateProductCache(productId);
}

private void invalidateProductCache(Long productId) {
    // 개별 상품 캐시 무효화
    cacheManager.delete("product:detail:" + productId);
    // 상품 목록 캐시 무효화
    cacheManager.delete("product:list:active");
}
```

### 3. 캐시 키 설계
```java
// 도메인:기능:식별자 형태로 설계
- product:detail:123          // 상품 상세
- product:list:active         // 활성 상품 목록
- product:stats:popular       // 인기 상품
- user-coupon:list:1001      // 사용자 쿠폰 목록
- coupon:info:456            // 쿠폰 정보
```

## 🎯 결론

### 캐시 적용 원칙
1. **자주 조회되는 데이터**만 캐시 적용
2. **대용량 조회**가 필요한 기능만 캐시 적용
3. **데이터 변경 빈도가 낮은** 기능만 캐시 적용
4. **실시간 정확성이 중요한** 기능은 캐시 미적용

### 핵심 기능 선별 결과
- ✅ **상품 조회**: 인기 상품, 상품 상세, 상품 목록
- ✅ **쿠폰 조회**: 사용자 쿠폰 목록, 쿠폰 정보
- ❌ **잔액/주문/사용자**: 실시간 정확성, 개인정보 보호

이렇게 **핵심 기능만 선별적으로 캐시를 적용**하면 성능 향상과 복잡성 최소화를 동시에 달성할 수 있습니다! 🚀
