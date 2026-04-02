## spring-back-end 데이터 접근 책임 기준

### 1) 목적
- 본 문서는 `SITE_INFO`, `CART`처럼 여러 기능에서 반복 참조되는 공통 테이블의 조회/수정 책임을 고정하기 위한 운영 기준이다.
- 기능 개발 중 동일 테이블 쿼리를 기능별 매퍼에 다시 만들지 않도록 기준을 문서화한다.
- 서비스는 공통 조회 결과를 화면/기능별 응답 객체로 변환하고, 매퍼는 테이블 책임 기준으로 유지한다.

### 2) SITE_INFO 조회 규칙
- `SITE_INFO` 조회는 반드시 `SiteInfoMapper.getShopSiteInfo(siteId)` 1개를 기준으로 재사용한다.
- `JOIN_POINT`, `DELIVERY_FEE`, `DELIVERY_FEE_LIMIT`, `WH_POST_NO`, `WH_ADDR_BASE`, `WH_ADDR_DTL`, `CS_TELL`도 별도 전용 쿼리를 만들지 않고 `getShopSiteInfo` 결과에서 꺼내 사용한다.
- 기능별 응답에 필요한 필드 조합이 달라도 매퍼에 새 `select`를 추가하지 않고 서비스에서 `ShopSiteInfoVO`를 각 기능용 VO로 변환한다.
- `GoodsMapper`, `OrderMapper`, `ShopAuthMapper` 등 다른 매퍼에 `SITE_INFO` 조회 쿼리를 추가하지 않는다.
- 예외가 필요하면 기존 `ShopSiteInfoVO` 확장 가능 여부를 먼저 검토하고, 정말 공통 조회로 해결할 수 없는 경우에만 사유를 문서와 코드 주석으로 남긴다.

### 3) ShopAuth와 사이트 설정 재사용 규칙
- 회원가입 포인트(`JOIN_POINT`)는 `ShopAuthMapper`가 직접 조회하지 않는다.
- `ShopAuthService`를 포함한 인증/회원가입 로직도 `SiteInfoMapper.getShopSiteInfo`를 통해 사이트 설정을 재사용한다.
- 사이트 설정값을 기능별 매퍼에서 재조회하는 방식은 금지한다.

### 4) CART 조회 및 변경 규칙
- `CART` 테이블 접근은 반드시 `CartMapper`에 둔다.
- 장바구니 조회/등록/수정/삭제 쿼리를 `GoodsMapper`, `OrderMapper` 등 다른 매퍼에 섞어 넣지 않는다.
- 특정 장바구니 행을 직접 수정하거나 삭제하는 작업은 `CART_ID` 기준으로 처리한다.
- 직접 변경 쿼리에는 항상 고객 소유권 검증을 위해 `CUST_NO` 조건을 함께 사용한다.
- `GOODS_ID + SIZE_ID` 조건은 “동일 상품/사이즈 병합 여부 판단” 같은 논리적 병합 규칙에만 사용한다.
- `cartId`가 있는데도 수정/삭제를 `goodsId`, `sizeId` 기준으로 처리하는 구현은 추가하지 않는다.

### 5) CART 병합 규칙
- 장바구니 담기 시 동일 일반 장바구니 행 존재 여부를 판단할 때만 `goodsId + sizeId` 기준 병합을 사용한다.
- 옵션 변경 시 목표 사이즈에 이미 다른 장바구니 행이 있는지 확인할 때만 `goodsId + targetSizeId` 기준 병합을 사용한다.
- 병합 판단 이후 실제 수량 변경과 기존 행 삭제는 `cartId` 기준으로 수행한다.
- `CART` 테이블은 `CART_ID`가 PK이며, `(CUST_NO, GOODS_ID, SIZE_ID, CART_GB_CD)` 유니크 제약이 없다는 점을 전제로 구현한다.

### 6) 테스트 및 리뷰 기준
- 매퍼 책임을 이동하거나 공통 조회를 재사용하도록 변경하면 테스트의 mock 대상과 시그니처도 함께 갱신한다.
- 새 기능 개발 시 아래 항목을 코드 리뷰 체크리스트로 확인한다.
- `SITE_INFO` 조회가 기존 `getShopSiteInfo`로 해결 가능한데 새 쿼리를 만들지 않았는가
- `CART` 쿼리가 `CartMapper` 밖으로 새로 생기지 않았는가
- 장바구니 직접 변경이 `cartId + custNo` 기준으로 처리되는가
- `goodsId + sizeId`는 병합 판단에만 사용되는가

### 7) 운영 원칙
- 공통 테이블은 “기능 화면 기준”이 아니라 “테이블 책임 기준”으로 매퍼를 유지한다.
- 화면별 응답 차이는 서비스에서 흡수하고, DB 조회 중복으로 해결하지 않는다.
- 작업 중 본 기준을 어기는 기존 코드가 보이면 현재 작업 범위에서 함께 정리하는 것을 기본으로 한다.
