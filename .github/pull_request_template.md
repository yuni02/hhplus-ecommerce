## **커밋 링크**
- 7개의 api에 대한 mock api 작성 : [e6f34](https://github.com/yuni02/hhplus-ecommerce/commit/e6f34c9d45a6d2d6e764850ccea9663be6f53eb1)
- 스웨거주석 추가로 api 테스트 화면 나오게 함  : [d49e9](https://github.com/yuni02/hhplus-ecommerce/commit/e6f34c9d45a6d2d6e764850ccea9663be6f53eb1)

## 참고 자료
- [이커머스 과제 요구사항](https://github.com/yuni02/hhplus-ecommerce/blob/step3/docs/%EC%9A%94%EA%B5%AC%EC%82%AC%ED%95%AD.md)
- [시퀀스 다이어그램 참고 문서](https://github.com/yuni02/hhplus-ecommerce/blob/step3/docs/sequence_diagram.md)
- [ERD 설계 가이드](https://github.com/yuni02/hhplus-ecommerce/blob/step3/docs/erd.md)

## PR 설명
### 작업 내용
- 이커머스 7개 주요 API에 대한 Mock API 구현
- Swagger 문서 작성으로 API 스펙 정의
- Spring Boot 3.4 + springdoc-openapi를 활용한 API 문서화

### 구현된 API 목록
- 상품 목록 조회 API
- 보유 쿠폰 목록 조회 API
- 선착순 쿠폰 발급 API
- 주문 및 결재 생성 API
- 상위 상품 조회 API
- 잔액 충전 API
- 잔액 조회 API

### 주요 변경사항
- Mock 데이터 구조 정의
- 공통 응답 형태 표준화
- 에러 코드 정의
- API 문서 자동화 설정
-
## 리뷰 포인트
- API 설명(description)이 개발자가 바로 이해할 수 있을 정도로 상세한지
- ERD 및 Sequence Diagram과 API 스펙이 일치하는지 확인

## Definition of Done (DoD)
    - [x] 7개의 API에 대한 MOCK API 작성
    - [x] 7개의 API에 대한 스웨거 문서 작성 (코드 주석으로 완성)
