# 뉴스 MCP OAuth — USER_BASE 전용 계정

## 현재 인증과 운영 경계

뉴스 OAuth 로그인은 기존 `eshop.USER_BASE`의 **USR_NO=3**만 허용합니다.
입력 ID, 설정 `news.oauth.username`, 해당 행의 LOGIN_ID가 일치해야 합니다.
기본 설정 ID는 `gpt_news_insert`이며 실제 계정 ID가 다르면 운영 설정을 그 값에 맞춰야 합니다.
사이트와 같은 PasswordEncoder 빈(BCrypt)을 주입받아 입력 비밀번호와 PWD를 검증합니다.
별도의 사용자 비밀번호 해시 파일은 필요하지 않습니다. `password-hash-file` 및
`NEWS_OAUTH_PASSWORD_HASH_FILE`은 제거했습니다.

일반 사이트 회원 또는 관리자 전체에 뉴스 OAuth 권한을 부여하지 않습니다.
사이트 AuthenticationManager로 실패 인증을 넘기지 않고, 전용 세션/CSRF/승인 흐름을 유지합니다.
전용 OAuth 로그인 성공만으로 사이트에 로그인되는 것도 아닙니다.

운영 설정은 `news.oauth.enabled=false`입니다. 기존 `news.daily.enabled=true`는 유지했습니다.
현재 소스는 OAuth 비활성 시 MCP에 503을 반환합니다. 운영 배포·재시작·계정 활성화는 수행하지 않았습니다.

## 계정 상태와 토큰 연결

- 기존 ResumeService/ResumeMapper의 정상 사용자 조회 조건인 `USR_STAT_CD='02'`만 허용합니다.
- AdminUserService는 `99`를 탈퇴 상태로 처리하고 LOGIN_ID를 치환합니다.
- 02 이외 값, NULL, 탈퇴, 행 삭제, 설정과 다른 LOGIN_ID는 모두 거부합니다.
- UserBaseEntity에는 삭제 플래그 매핑이 없습니다. 물리 삭제는 조회 결과 없음으로 처리합니다.
- 상태 의미는 저장소 코드로 확인했습니다. 운영 USER_BASE의 비밀번호나 실제 3번 계정 행은 조회하지 않았습니다.
- 공통 UserMapper에 인증용 최소 필드 조회를 추가했습니다. `eshop.USER_BASE`를 명시하고
  MyBatis 1차/2차 캐시를 우회하여 계정 변경을 다음 인증 검사에 반영합니다.
- 새 토큰의 `news_actor=3`은 검증된 전용 계정에 연결됩니다. 저장 시 NEWS_DAILY.REG_NO와 UDT_NO,
  NEWS_DAILY_BATCH.REG_NO 모두 3을 사용합니다. 클라이언트가 등록자 번호를 지정하지 않습니다.
- 비밀번호, LOGIN_ID, 상태, 등록/수정 시각, OAuth TOKEN_VERSION 및 클라이언트 자격의 지문을 비교합니다.
  PWD 원문 해시 자체를 OAuth 메타데이터에 복사하지 않습니다.
- 계정 비활성·삭제 또는 자격 변경을 발견하면 승인 전체를 삭제하여 access/refresh 모두 차단합니다.
  이미 인증을 마치고 처리 중인 요청을 소급 취소하지는 않습니다.
- 계정을 비활성화했다가 원상복구하는 사이 인증 요청이 전혀 없으면 중간 상태 변화 자체는 관측하지 못합니다.
  운영 변경은 기존 사용자 관리 경로의 UDT_DT 갱신을 유지하고, 강제 폐기에는 OAuth TOKEN_VERSION 증가를 사용합니다.
- NEWS_OAUTH_LOGIN_GUARD의 ENABLED/TOKEN_VERSION 및 5회 실패 후 15분 잠금도 계속 적용합니다.
  USER_BASE 정상 상태와 OAuth 가드 ENABLED가 모두 필요합니다.

## OAuth 및 갱신 정책

- Authorization Code + PKCE S256, 정확한 HTTPS redirect URI, resource 검증을 유지합니다.
- code는 2분·단일 사용, access token은 opaque·10분입니다.
- refresh token은 매 성공 갱신마다 회전합니다. 기본 미사용 만료는 30일입니다.
- `news.oauth.refresh-inactivity-timeout=30d`로 설정하며 최소 10분입니다.
  성공한 refresh만 기한을 연장합니다. 고정 30일 재승인은 없습니다.
- 뉴스 조회는 news.read, 저장/시도 변경은 news.read와 news.write가 필요합니다.
- 사용한 refresh의 SHA-256, 승인 ID, 클라이언트 ID, 사용 시각을 NEWS_OAUTH_REFRESH_HISTORY에 저장합니다.
- 같은 클라이언트의 이전 refresh 재사용은 승인 전체를 폐기합니다. 동시 중복 갱신 시 하나는 200,
  다른 하나는 400이며 첫 응답의 새 토큰도 폐기됩니다. 응답 유실 후 같은 토큰 재시도 역시 재연결이 필요합니다.
- code 교환/회전/철회와 이력 저장은 동일 DataSource의 행 잠금·트랜잭션으로 처리합니다.
  저장 실패 시 함께 롤백하고 토큰 응답은 커밋 성공 후 전송합니다.
- 기존 JDBC 승인/동의와 재사용 이력은 재시작 후에도 유지합니다. 활성 승인의 이력을 조기 삭제하지 않습니다.
- 실제 ChatGPT 연결, 자동 refresh 및 예약 실행 호환성은 아직 검증하지 않았습니다.

## 설정과 아직 필요한 값

운영 YAML과 project-setting 백업을 동기화했습니다. 기존 DataSource를 유지하며 OAuth 5개 테이블은
그 기본 스키마를 사용합니다. 사용자는 eshop에 생성했다고 알려주었으나 이번 작업에서 운영 DB를 조회하지 않았습니다.
이번 인증 변경에 **추가 DDL 또는 기존 5개 테이블 변경은 필요하지 않습니다.**
DDL 제안은 자동 실행하지 않습니다.

```yaml
news:
  oauth:
    enabled: false
    username: gpt_news_insert
    owner-usr-no: 3
    issuer: https://be.xodud1202.kro.kr
    resource: https://be.xodud1202.kro.kr/api/news/mcp
    client-id: "${NEWS_OAUTH_CLIENT_ID:}"
    client-secret-hash-file: "${NEWS_OAUTH_CLIENT_SECRET_HASH_FILE:}"
    redirect-uri: "${NEWS_OAUTH_REDIRECT_URI:}"
    refresh-inactivity-timeout: 30d
```

- CLIENT_ID: 운영에서 정한 클라이언트 식별 문자열. 파일 경로가 아닙니다.
- CLIENT_SECRET_HASH_FILE: 클라이언트 secret BCrypt 파일의 서버 절대 경로.
- REDIRECT_URI: ChatGPT 설정에서 확인한 정확한 HTTPS callback. 파일 경로가 아닙니다.
- 환경변수가 없으면 위 세 값은 빈 값입니다. 운영값을 채우기 전 enabled를 true로 바꾸지 않습니다.
- 사용자 비밀번호와 클라이언트 secret은 별개입니다. 클라이언트 secret 원문은 ChatGPT 연결 설정에 직접 입력합니다.
- 서버에는 클라이언트 secret의 BCrypt 해시만 외부 파일로 둡니다.
  로더는 비용 계수 12~16, 접두사 없는 $2a$/$2b$/$2y$ 형식을 허용합니다.
- 사용자 PWD는 기존 PasswordEncoder로 검증하므로 사이트 기본 비용 계수 10도 호환됩니다.
- 기존 파일 인증으로 발급된 승인은 자격 지문 형식이 달라져 무효화됩니다. 운영 전환 시 재연결이 필요합니다.

## 클라이언트 secret 해시 도구

아래는 향후 승인 후 운영자가 직접 실행할 절차입니다. 이번 작업에서 실행하거나 비밀을 생성하지 않았습니다.

```powershell
$env:JAVA_HOME='D:\react_project\bin\jdk-21.0.9+10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat newsOAuthAdminDistribution
java -cp "build/classes/java/main;build/news-oauth-admin/lib/*" com.xodud1202.springbackend.security.news.NewsOAuthPasswordConsole C:\secure\news-client.hash
```

실제 터미널에서 클라이언트 secret을 두 번 입력합니다. 20자 이상, UTF-8 72바이트 이하이며 화면에 출력하지 않습니다.
상위 폴더가 존재해야 하고 기존 파일은 덮어쓰지 않습니다. 생성 도구는 BCrypt 비용 계수 12를 사용합니다.
Linux에서는 classpath 구분자를 :로 바꿉니다. 서비스 실행 사용자의 파일 읽기 권한이 필요합니다.
생성한 해시나 비밀번호를 YAML·Git·채팅에 넣지 않습니다.

## 연결 주소 및 운영 확인

| 용도 | 주소 |
|---|---|
| MCP | https://be.xodud1202.kro.kr/api/news/mcp |
| Authorization | https://be.xodud1202.kro.kr/oauth2/news/authorize |
| Token | https://be.xodud1202.kro.kr/oauth2/news/token |
| Revoke | https://be.xodud1202.kro.kr/oauth2/news/revoke |

클라이언트 인증은 client_secret_basic/post입니다. DCR/password/client_credentials grant는 제공하지 않습니다.
가드 활성화, 자격 준비, 서버 배포/재시작, ChatGPT 최초 로그인·동의는 아직 별도 운영 단계입니다.
실서버 systemd unit, HTTPS 프록시 및 쿠키 설정은 이번 작업에서 확인하거나 변경하지 않았습니다.
OAuth/인증 SQL의 DEBUG·TRACE 결과 로그, HTTP 본문/Authorization 로그로 자격이나 토큰을 노출하지 않아야 합니다.

## 로컬 검증

고정 JDK 21.0.9+10에서 compileJava, processResources, build, test를 수행합니다.
RUN_FTP_INTEGRATION_TEST=N, NEWS_MCP_SDK_TEST=true로 운영 연결 없이 격리 H2·일회성 테스트 자격만 사용합니다.
USER_BASE 조회는 운영 UserMapper의 실제 SQL로 실행합니다.
전용 계정 허용, 타 계정 거부, 상태/삭제/이름/비밀번호 변경 시 폐기, 캐시 우회,
MCP의 actor=3 전달과 실제 기사 REG_NO/UDT_NO=3 INSERT를 검증합니다.
기존 회전·재사용·동시성·롤백·40일 가속 정책 테스트와 SDK 검증도 유지합니다.
40일 테스트는 가상 시간 기반이며 실제 운영 장기 실행이나 서버 재시작 시험은 아닙니다.
최종 결과는 build/test-results/test/*.xml 및 build/reports/tests/test/index.html을 참고합니다.

2026-10-15 최종 결과: 전체 312개 중 통과 310, 실패 0, 오류 0, 제외 2.
OAuth 흐름 32개 통과, 공식 MCP SDK 1.32.1 PASS(26개 관측).
제외 2개는 기존 외부 FTP 실제 쓰기와 외부 MySQL/MyBatis 전체 컨텍스트 테스트입니다.
운영 YAML은 엄격한 YAML 파서(중복 키 거부)와 Spring Boot Binder로 검사했습니다.
기존 news.daily.enabled=true 보존, OAuth 비활성, actor=3, 30d, 필수 환경변수 3개의 빈 값/가상 값 바인딩,
사용자 해시 파일 키 제거 및 백업 일치를 확인했습니다.
이번에 실행한 명령: gradlew.bat compileJava processResources build test newsOAuthAdminDistribution.
설정의 실제 비밀값은 출력하지 않았으며 운영 DB/계정/자격/배포는 변경하지 않았습니다.
