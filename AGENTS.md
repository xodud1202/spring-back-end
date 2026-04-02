## spring-back-end 작업 진입점

### 1) 먼저 읽을 문서
- 공통 규칙: `../AGENTS.md`
- 프로젝트 개요: `docs/index.md`
- 데이터 접근 책임 기준: `docs/data-access-rules.md`
- 도메인 구조: `docs/domain-map.md`
- 검증 기준: `harness/README.md`
- DB 조회 절차: `../AGENTS/runbooks/db-query.md`

### 2) 자동 선독 세부 규칙
- `spring-back-end` 작업이 식별되면 `docs/index.md`, `docs/domain-map.md`, `harness/README.md`를 먼저 읽고 작업한다.
- 매퍼 책임, 공통 테이블 재사용, 장바구니 조건 검토가 포함되면 `docs/data-access-rules.md`를 함께 읽고 작업한다.
- API 추가, 수정, 리팩토링, 컨트롤러/서비스/매퍼 변경은 `../AGENTS/harness/checklists/backend-api-change.md`를 함께 확인한다.
- DB 관련 요청이나 테이블, 컬럼, 코드값, 정합성, 통계 검토는 `../AGENTS/runbooks/db-query.md`를 함께 확인한다.
- 배송비 계산 로직을 추가, 수정, 검토할 때는 `../AGENTS/references/DELIVERY_FEE.md`를 함께 확인한다.

### 3) 프로젝트 경계
- 본 프로젝트는 전체 백엔드 API와 DB 연동을 담당한다.
- 프론트에서 `/api`로 호출하는 백엔드 코드는 반드시 본 프로젝트에 구현한다.
- 배송비 계산 로직을 추가, 수정, 검토할 때는 반드시 `../AGENTS/references/DELIVERY_FEE.md`를 먼저 읽는다.

### 4) 기본 검증
- `.\gradlew.bat compileJava`
- `.\gradlew.bat test`
- JDK 경로는 `D:\react_project\bin\jdk-21.0.9+10`을 기준으로 사용한다.
- 백엔드 API 변경 체크는 공통 체크리스트 `../AGENTS/harness/checklists/backend-api-change.md`를 따른다.

### 5) 금지 및 주의사항
- DB 관련 요청은 실제 조회 없이 처리하지 않는다.
- 상세 도메인 규칙은 본 문서에 계속 누적하지 않고 `docs/`와 `harness/`에 기록한다.
