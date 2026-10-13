# 최종 검토 / MCP 연결 범위

이 문서는 이전 성공 묶음 번호 설계를 대체한 **시도 번호 보존 및 최신 요약 컬럼 모델** 기준입니다. 사용자 제공 DDL을 반영했으며 실제 운영 DDL 조회·실행은 하지 않았습니다.

## 검증

실제 MyBatis + Spring 트랜잭션 + H2(MySQL mode)에서 예약 독립 커밋, 최초 날짜 경쟁, 동일 키 동시 예약, 동시 저장/재시도, 실패 번호 보존, 완료/실패 경합, terminal 상태 변경 거부, 오래된 PENDING 처리, commit 응답 유실, 미커밋 비노출, 최신 COMPLETED 선택을 검증합니다.

길이는 800/801 한글·줄바꿈·이모지·결합문자, 빈값/공백류, 깨진 surrogate, 카테고리 10/11 경계 및 순서를 검사합니다. API/MCP 오류 상세와 본문 로그 마스킹도 테스트합니다. 현재 테스트 DB 스키마와 입력 fixture에는 최신 요약 컬럼만 있습니다.

H2는 MySQL InnoDB와 동일하지 않습니다. 실제 MySQL 격리수준/잠금/문자셋/grants 검증은 활성화 전 남은 조건입니다. 전체 외부 FTP/DB 의존 테스트는 무차별 실행하지 않습니다.

## 실제 SDK

공식 @modelcontextprotocol/sdk 1.32.1 / Node 22.16.0으로 실제 embedded Tomcat의 127.0.0.1 임의 포트에 접속합니다. 운영 SecurityConfig/JwtAuthenticationFilter/NewsMcpController를 쓰되 JWT 검증·사용자 조회·서비스 외부 IO는 대역입니다. 암호 검증·운영 JWT·운영 DB 연결 완료를 뜻하지 않습니다.

- SDK 2025-11-25 제안 → 서버 2025-06-18 협상.
- initialize 200 application/json, initialized 알림 202 빈 응답.
- tools/list의 5개 도구, tools/call 원본/시작/실패/저장/상태 및 길이 상세 오류 파싱.
- ping, GET SSE/DELETE 405, 미인증/잘못된 Bearer 403(WWW-Authenticate 없음).
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

## ChatGPT OAuth 연결: 필요한 최소 다음 단계

**현재 관리자 JWT를 직접 전달하는 MCP 연결과 ChatGPT 사용자의 OAuth 계정 연결은 다릅니다.** 후자는 구현되지 않았습니다. 현재 403 응답에는 OAuth 발견 challenge가 없습니다.

1. 사용할 기존 OAuth/OIDC 공급자와 뉴스 읽기/쓰기 권한·기존 USR_NO 매핑을 결정하고 승인받습니다. 임의 새 인증 서버/자격증명은 만들지 않습니다.
2. 보호 리소스 메타데이터와 공급자 discovery, authorization/token endpoint, authorization-code + PKCE S256를 구성합니다. 클라이언트 등록 방식(CIMD/DCR/사전 등록)과 실제 연결 화면의 redirect URI를 확정합니다.
3. 토큰 issuer/audience/만료/scope 검증, 최소 권한 도구 scope, 사용자 매핑 및 갱신/폐기 정책을 구현·검증합니다. 필요한 경우 MCP 경로 전용 401 WWW-Authenticate resource_metadata 및 도구 인증 메타데이터를 승인된 범위에서 추가합니다.
4. DDL/실제 MySQL 검증 후 HTTPS 테스트 환경에 승인받아 배포하고 ChatGPT 연결에서 로그인·동의·읽기·쓰기·토큰 만료/권한 거절을 검증합니다. 그 후 운영 적용을 별도 승인받습니다.

이 항목들은 [OpenAI 공식 인증 문서](https://developers.openai.com/plugins/build/auth)에 근거한 다음 작업 범위입니다. 계정별 기능 접근 가능 여부 및 정확한 callback URI는 연결 시 실제 UI/공급자 설정으로 확인해야 합니다. 현재 구현이나 이 문서 작성만으로 연결이 완료되지 않습니다.

운영 DB/DDL/배포/방화벽/계정 연결/인증 생성/push/OCI CLI 설치는 계속 미실행입니다.


## 최종 실행 결과

2026-10-13: 관련 테스트 71개, 실패 0, 오류 0, 건너뜀 0. compileJava/processResources 및 build -x test 성공. 공식 SDK HTTP 테스트도 포함했습니다. 테스트 결과 XML과 SDK 관측 기록은 build/test-results/test 및 build/reports/news-mcp-sdk-client.json에 있습니다.
