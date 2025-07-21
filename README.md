## 프로젝트

## Getting Started

### Prerequisites

#### Running Docker Containers

`local` profile 로 실행하기 위하여 인프라가 설정되어 있는 Docker 컨테이너를 실행해주셔야 합니다.

```bash
docker-compose up -d
```

## UseCase (Application Layer)

애플리케이션 계층에 위치
비즈니스 유스케이스(시나리오) 구현
여러 도메인 서비스를 조합하여 완전한 기능 제공
트랜잭션 관리, 외부 시스템 연동 담당

## Service (Domain Layer)

도메인 계층에 위치
순수한 도메인 로직만 포함
도메인 지식과 비즈니스 규칙 구현
프레임워크나 외부 의존성 없음

balance/
├── domain/                           # 도메인 레이어 (핵심 비즈니스 로직)
│   ├── Balance.java                  # 잔액 엔티티
│   ├── BalanceTransaction.java       # 거래 내역 엔티티
│   ├── BalanceDomainService.java     # 순수한 도메인 서비스 (비즈니스 규칙)
│   ├── BalanceRepository.java        # 잔액 리포지토리 인터페이스
│   └── BalanceTransactionRepository.java # 거래 내역 리포지토리 인터페이스
├── application/                      # 애플리케이션 레이어 (UseCase)
│   ├── ChargeBalanceUseCase.java     # 잔액 충전 UseCase
│   └── GetBalanceUseCase.java        # 잔액 조회 UseCase
└── infrastructure/                   # 인프라스트럭처 레이어 (외부 인터페이스)
    ├── BalanceController.java        # 웹 컨트롤러
    ├── InMemoryBalanceRepository.java # 인메모리 잔액 리포지토리
    └── InMemoryBalanceTransactionRepository.java # 인메모리 거래 내역 리포지토리

coupon/
├── domain/                           # 도메인 레이어 (핵심 비즈니스 로직)
│   ├── Coupon.java                   # 쿠폰 엔티티
│   ├── UserCoupon.java               # 사용자 쿠폰 엔티티
│   ├── CouponDomainService.java      # 순수한 도메인 서비스 (비즈니스 규칙)
│   ├── CouponRepository.java         # 쿠폰 리포지토리 인터페이스
│   └── UserCouponRepository.java     # 사용자 쿠폰 리포지토리 인터페이스
├── application/                      # 애플리케이션 레이어 (UseCase)
│   ├── IssueCouponUseCase.java       # 쿠폰 발급 UseCase
│   └── GetUserCouponsUseCase.java    # 사용자 쿠폰 조회 UseCase
└── infrastructure/                   # 인프라스트럭처 레이어 (외부 인터페이스)
    ├── CouponController.java         # 웹 컨트롤러
    ├── InMemoryCouponRepository.java # 인메모리 쿠폰 리포지토리
    └── InMemoryUserCouponRepository.java # 인메모리 사용자 쿠폰 리포지토리

product/
├── domain/                           # 도메인 레이어 (핵심 비즈니스 로직)
│   ├── Product.java                  # 상품 엔티티
│   ├── ProductStats.java             # 상품 통계 엔티티
│   ├── ProductDomainService.java     # 순수한 도메인 서비스 (비즈니스 규칙)
│   ├── ProductRepository.java        # 상품 리포지토리 인터페이스
│   └── ProductStatsRepository.java   # 상품 통계 리포지토리 인터페이스
├── application/                      # 애플리케이션 레이어 (UseCase)
│   ├── GetAllProductsUseCase.java    # 상품 목록 조회 UseCase
│   └── GetPopularProductsUseCase.java # 인기 상품 조회 UseCase (여러 도메인 서비스 조합)
└── infrastructure/                   # 인프라스트럭처 레이어 (외부 인터페이스)
    ├── ProductController.java        # 웹 컨트롤러
    ├── InMemoryProductRepository.java # 인메모리 상품 리포지토리
    └── InMemoryProductStatsRepository.java # 인메모리 상품 통계 리포지토리