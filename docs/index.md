## spring-back-end 문서 허브

### 문서 구성
- 데이터 접근 책임과 공통 테이블 재사용 기준: `data-access-rules.md`
- 백엔드 도메인 구조와 배치 기준: `domain-map.md`
- 프로젝트 검증 기준: `../harness/README.md`
- 공통 DB 조회 절차: `../../AGENTS/runbooks/db-query.md`

### 기본 원칙
- 프론트 `/api` 연동 백엔드 구현은 본 프로젝트에서 담당한다.
- 서비스, 매퍼, DTO, 보조 타입, 테스트는 도메인 책임 기준으로 관리한다.
- 공통 테이블 조회는 기능별 중복 쿼리보다 도메인 공용 매퍼 재사용을 우선한다.
- 배송비 계산 기준은 `../../AGENTS/references/DELIVERY_FEE.md`를 따른다.
