## 로컬 Spring Boot와 SSH DB 연결

`application-local.yml`의 JDBC 주소 `127.0.0.1:13306`은 SSH 터널의 입구입니다. 이 주소를 내부 DB 주소로 바꾸지 않고, 앱 실행 전에 터널을 열어 사용합니다. DB 비밀번호나 개인 키 내용은 스크립트에 저장하지 않습니다.

### 앱과 터널 함께 실행

프로젝트 루트에서 다음 명령을 실행합니다. 기본값은 PC의 기존 `~/.ssh/config`에 등록된 `oci-mysql` 프로필입니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/start-local.ps1
```

프로필 대신 개인 키 파일을 지정할 수도 있습니다. 기본 SSH 서버는 `ubuntu@168.107.16.227`, 내부 DB는 `10.0.1.249:3306`, 로컬 포트는 `13306`입니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/start-local.ps1 -SshKeyPath "$env:USERPROFILE\.ssh\oci.key"
```

키 경로를 `SPRING_LOCAL_SSH_KEY` 환경변수에 저장해 두어도 됩니다. 서버 호스트 키는 신뢰할 수 있는 절차로 미리 등록되어 있어야 하며, 대화형 비밀번호 입력이 필요한 키는 사용할 수 없습니다.

현재 PC에서는 사용자가 지정한 원본 키와 `~/.ssh/oci.key`가 동일한 키임을 확인했습니다. 원본 파일은 접근 권한이 넓어 Windows OpenSSH에서 거부되므로 정상 권한이 설정된 기존 사본 또는 기본 `oci-mysql` 프로필을 사용합니다. 원본 키 파일의 권한은 변경하지 않았습니다.

### IDE로 실행

터널 전용 모드를 별도 PowerShell 창에서 실행하고 IDE의 기존 `local` 프로필로 앱을 시작합니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/start-local.ps1 -TunnelOnly
```

터널 전용 창을 유지하는 동안 IDE의 JDBC 접속이 가능합니다. 일반 IDE 실행 자체가 터널을 자동으로 여는 것은 아닙니다. 뉴스 요약 API도 테스트하려면 IDE 실행 인자에 `--news.daily.enabled=true`를 추가합니다. 앱과 터널을 함께 실행하는 기본 모드는 이 인자를 이미 적용합니다. `SpringArguments`로 앱 인자를 변경할 수 있습니다.

### 연결 확인과 종료

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/start-local.ps1 -CheckOnly
```

연결 확인은 로컬 TCP 포트가 열리는지 검사합니다. DB 인증과 테이블 권한은 앱 또는 허용된 조회 계정으로 별도 확인합니다. 앱이나 터널 전용 실행은 `Ctrl+C`로 끝냅니다. 이번 실행이 생성한 SSH 프로세스는 종료 시 정리하며, 시작 전에 이미 열려 있던 터널은 유지합니다.
