# 최종 검토 / MCP 연결 범위

이 문서는 이전 성공 묶음 번호 설계를 대체한 **시도 번호 보존 및 최신 요약 컬럼 모델** 기준입니다. 사용자 제공 DDL을 반영했으며 실제 운영 DDL 조회·실행은 하지 않았습니다.

## 검증

실제 MyBatis + Spring 트랜잭션 + H2(MySQL mode)에서 예약 독립 커밋, 최초 날짜 경쟁, 동일 키 동시 예약, 동시 저장/재시도, 실패 번호 보존, 완료/실패 경합, terminal 상태 변경 거부, 오래된 PENDING 처리, commit 응답 유실, 미커밋 비노출, 최신 COMPLETED 선택을 검증합니다.

길이는 800/801 한글·줄바꿈·이모지·결합문자, 빈값/공백류, 깨진 surrogate, 카테고리 10/11 경계 및 순서를 검사합니다. API/MCP 오류 상세와 본문 로그 마스킹도 테스트합니다. 현재 테스트 DB 스키마와 입력 fixture에는 최신 요약 컬럼만 있습니다.

H2는 MySQL InnoDB와 동일하지 않습니다. 실제 MySQL 격리수준/잠금/문자셋/grants 검증은 활성화 전 남은 조건입니다. 전체 외부 FTP/DB 의존 테스트는 무차별 실행하지 않습니다.

## 실제 SDK

공식 @modelcontextprotocol/sdk 1.32.1 / Node 22.16.0으로 실제 embedded Tomcat의 127.0.0.1 임의 포트에 접속합니다. 운영 OAuth Resource Server 체인/NewsMcpController를 쓰되 이 SDK 테스트의 토큰 introspection·서비스 외부 IO는 대역입니다. 실제 OAuth 발급·갱신·철회는 별도 NewsOAuthFlowTests에서 표준 서버와 H2로 검증합니다. 암호 검증·운영 JWT·운영 DB 연결 완료를 뜻하지 않습니다.

- SDK 2025-11-25 제안 → 서버 2025-06-18 협상.
- initialize 200 application/json, initialized 알림 202 빈 응답.
- tools/list의 5개 도구, tools/call 원본/시작/실패/저장/상태 및 길이 상세 오류 파싱.
- ping, GET SSE/DELETE 405, 미인증/잘못된 Bearer 401(resource_metadata challenge 포함).
- 미지원 버전/협상 후 헤더 누락 400, 잘못된 Content-Type 415, Accept 누락/q=0은 406, Origin 거절 403.
- 잘못된 JSON/후행 토큰 400/-32700, JSON-RPC batch -32600, id 없는 저장 알림 거절.

지원하지 않는 기능: SSE/재개, 세션, 구형 HTTP+SSE, JSON-RPC batch, resources/prompts, sampling, subscriptions, progress, 실제 취소, 서버발 요청, 비동기 tasks. notifications/*는 수신만 하며 실제 작업 취소를 보장하지 않습니다. stateless 서버는 초기화 이력을 저장하지 않으므로 클라이언트가 초기화 순서를 지켜야 합니다.

결과 파일: build/reports/news-mcp-sdk-client.json.
테스트 harness: harness/news-mcp-client/client-test.mjs.

```powershell
$env:JAVA_HOME='C:\Users\xodud\AppData\Local\Programs\Eclipse Adoptium\jdk-21.0.12.1+1'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
npm --prefix harness/news-mcp-client ci --ignore-scripts --no-fund --no-audit
$env:NEWS_MCP_SDK_TEST='true'
.\gradlew.bat test --tests '*NewsDaily*Tests' --tests '*NewsSourceServiceTests' --tests '*NewsMcp*Tests' --tests '*SecurityConfigTests' --tests '*SqlLoggingInterceptorTests'
.\gradlew.bat build -x test
```

SDK는 test harness의 devDependency이며 전역 CLI/서버 런타임 설치가 아닙니다. SDK HTTP 테스트는 NEWS_MCP_SDK_TEST=true로 opt-in입니다.

## ChatGPT OAuth 연결

2026-10-15 변경으로 전용 Spring Authorization Server/Resource Server를 구현했습니다.
현재 인증, 안전한 비밀번호 설정, 승인 수명, DDL, 활성화 절차는 [뉴스 OAuth 안내](news-oauth.md)를 따릅니다.
아래 2026-10-13 실행 수치는 이전 구현 검증 기록입니다. 최신 검증 결과는 OAuth 안내에 별도로 기록합니다.
운영 계정/자격/DDL/배포/연결/commit/push는 실행하지 않았습니다.
## 최종 실행 결과

2026-10-13: 관련 테스트 71개, 실패 0, 오류 0, 건너뜀 0. compileJava/processResources 및 build -x test 성공. 공식 SDK HTTP 테스트도 포함했습니다. 테스트 결과 XML과 SDK 관측 기록은 build/test-results/test 및 build/reports/news-mcp-sdk-client.json에 있습니다.

2026-10-15: 전용 OAuth 구현 후 지정 JDK 21.0.9+10으로 전체 292개 중 290개 통과, 실패 0, 제외 2. OAuth 13개 및 공식 SDK HTTP 검증 포함. build 성공. 상세 조건은 news-oauth.md 참고.
