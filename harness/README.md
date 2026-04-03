## spring-back-end 검증 하네스

### 기본 명령
- 아래 PowerShell 환경 설정을 먼저 적용한다.
- `$env:JAVA_HOME='D:\react_project\bin\jdk-21.0.9+10'`
- `$env:Path="$env:JAVA_HOME\bin;$env:Path"`
- `.\gradlew.bat compileJava`
- `.\gradlew.bat processResources`
- `.\gradlew.bat build`
- `.\gradlew.bat test`

### 검증 기준
- 컴파일 성공 여부를 먼저 확인한다.
- API 변경 시 대표 성공 케이스와 실패 케이스를 함께 확인한다.
- DB 관련 변경이 있으면 실제 데이터 조회 결과를 기준으로 검증한다.

### 참고 사항
- `spring-back-end`의 운영 업무 JDK 1.8 유지 여부와 무관하게, Gradle 검증용 JDK는 항상 `D:\react_project\bin\jdk-21.0.9+10`으로 고정한다.
- 시스템 기본 `JAVA_HOME`, `Get-Command java`, `where java`, 설치 경로 탐색 결과로 검증용 JDK를 결정하지 않는다.
- 검증 명령은 위 기본 명령 순서를 그대로 사용한다.
- 공통 체크리스트는 `../../AGENTS/harness/checklists/backend-api-change.md`와 `../../AGENTS/harness/checklists/db-change.md`를 함께 참고한다.
- DB 의존이나 환경 제약으로 `test` 전체를 실행하지 못하면, 미실행 사유와 영향 범위를 결과 보고에 반드시 남긴다.
