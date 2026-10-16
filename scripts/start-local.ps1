<# 로컬 DB의 SSH 터널을 준비한 뒤 Spring Boot를 실행하고 직접 생성한 터널만 종료합니다. #>
[CmdletBinding()]
param(
    [string]$SshProfile = 'oci-mysql',
    [string]$SshKeyPath = $env:SPRING_LOCAL_SSH_KEY,
    [string]$SshHostName = '168.107.16.227',
    [string]$SshUserName = 'ubuntu',
    [string]$DatabaseHost = '10.0.1.249',
    [int]$DatabasePort = 3306,
    [int]$LocalPort = 13306,
    [string[]]$SpringArguments = @('--news.daily.enabled=true'),
    [switch]$TunnelOnly,
    [switch]$CheckOnly
)

$ErrorActionPreference = 'Stop'
$ownedTunnel = $null
$tunnelErrorFile = $null
$projectDirectory = Split-Path -Parent $PSScriptRoot

<# TCP 연결만 확인하며 DB 계정이나 인증정보를 읽지 않습니다. #>
function Test-LocalDatabasePort {
    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        # 짧은 제한 시간으로 아직 열리지 않은 로컬 포트를 확인합니다.
        $connection = $client.ConnectAsync('127.0.0.1', $LocalPort)
        return $connection.Wait(300) -and $client.Connected
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

try {
    if (Test-LocalDatabasePort) {
        Write-Host "기존 DB 터널을 재사용합니다: 127.0.0.1:$LocalPort"
    } else {
        # 키 경로가 지정되면 명시적 포워딩을, 없으면 기존 SSH 프로필을 사용합니다.
        $sshArguments = @('-N', '-o', 'BatchMode=yes', '-o', 'ExitOnForwardFailure=yes',
            '-o', 'StrictHostKeyChecking=yes', '-o', 'ConnectTimeout=10',
            '-o', 'ServerAliveInterval=60', '-o', 'ServerAliveCountMax=3')
        if ($SshKeyPath) {
            $resolvedKey = (Resolve-Path -LiteralPath $SshKeyPath).Path
            $sshArguments += @('-i', ('"' + $resolvedKey + '"'), '-L',
                "127.0.0.1:${LocalPort}:${DatabaseHost}:$DatabasePort", "$SshUserName@$SshHostName")
        } else {
            $sshArguments += $SshProfile
        }
        $tunnelErrorFile = Join-Path ([System.IO.Path]::GetTempPath()) ("spring-local-ssh-{0}.log" -f [guid]::NewGuid())
        $ownedTunnel = Start-Process -FilePath "$env:WINDIR\System32\OpenSSH\ssh.exe" `
            -ArgumentList $sshArguments -WindowStyle Hidden -PassThru -RedirectStandardError $tunnelErrorFile
        # 연결 실패나 키 인증 실패 시 앱을 띄우지 않고 원인을 표시합니다.
        $deadline = [DateTime]::UtcNow.AddSeconds(15)
        while (-not (Test-LocalDatabasePort)) {
            if ($ownedTunnel.HasExited -or [DateTime]::UtcNow -ge $deadline) {
                $details = Get-Content -Raw -LiteralPath $tunnelErrorFile -ErrorAction SilentlyContinue
                throw "DB SSH 터널을 열지 못했습니다. 키·호스트 등록·접속 권한을 확인해 주세요. $details"
            }
            Start-Sleep -Milliseconds 200
        }
        Write-Host "DB SSH 터널을 연결했습니다: 127.0.0.1:$LocalPort"
    }

    if ($CheckOnly) {
        Write-Host 'DB 터널 연결 확인이 완료되었습니다.'
    } elseif ($TunnelOnly) {
        # IDE에서 앱을 실행할 때는 이 창의 터널만 유지하고 종료 신호를 기다립니다.
        Write-Host 'IDE에서 Spring Boot를 실행하세요. 터널을 끝내려면 Ctrl+C를 누르세요.'
        while ($true) {
            if ($ownedTunnel -and $ownedTunnel.HasExited) { throw 'DB SSH 터널 연결이 종료되었습니다.' }
            Start-Sleep -Seconds 1
        }
    } else {
        # 워크스페이스에서 지정한 JDK와 기존 local 프로필의 JDBC 설정을 유지합니다.
        $env:JAVA_HOME = 'D:\react_project\bin\jdk-21.0.9+10'
        $env:Path = "$env:JAVA_HOME\bin;$env:Path"
        Push-Location -LiteralPath $projectDirectory
        try {
            & '.\gradlew.bat' bootRun ("--args=" + ($SpringArguments -join ' '))
            if ($LASTEXITCODE -ne 0) { throw "Spring Boot 실행이 실패했습니다. 종료 코드: $LASTEXITCODE" }
        } finally {
            Pop-Location
        }
    }
} finally {
    # 재사용한 터널은 유지하고 이번 실행에서 만든 프로세스만 정리합니다.
    if ($ownedTunnel -and -not $ownedTunnel.HasExited) {
        Stop-Process -Id $ownedTunnel.Id -Force
        $ownedTunnel.WaitForExit()
        Write-Host '이번 실행에서 생성한 DB SSH 터널을 종료했습니다.'
    }
    if ($tunnelErrorFile -and (Test-Path -LiteralPath $tunnelErrorFile)) {
        Remove-Item -LiteralPath $tunnelErrorFile -Force
    }
}
