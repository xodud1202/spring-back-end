-- 제안 전용: 애플리케이션에서 자동 실행하지 않습니다. 운영 실행은 별도 승인 대상입니다.
-- Spring Authorization Server 1.5 표준 JDBC 스키마를 기준으로 합니다.
-- 적용 DB는 애플리케이션 DataSource의 기본 스키마입니다. news 스키마라고 추정하지 마세요.
-- 운영 DB 종류/버전/기존 테이블/권한을 검토한 후 DBA가 적용합니다.
CREATE TABLE oauth2_authorization (
    id varchar(100) NOT NULL,
    registered_client_id varchar(100) NOT NULL,
    principal_name varchar(200) NOT NULL,
    authorization_grant_type varchar(100) NOT NULL,
    authorized_scopes varchar(1000) DEFAULT NULL,
    attributes blob DEFAULT NULL,
    state varchar(500) DEFAULT NULL,
    authorization_code_value blob DEFAULT NULL,
    authorization_code_issued_at timestamp NULL DEFAULT NULL,
    authorization_code_expires_at timestamp NULL DEFAULT NULL,
    authorization_code_metadata blob DEFAULT NULL,
    access_token_value blob DEFAULT NULL,
    access_token_issued_at timestamp NULL DEFAULT NULL,
    access_token_expires_at timestamp NULL DEFAULT NULL,
    access_token_metadata blob DEFAULT NULL,
    access_token_type varchar(100) DEFAULT NULL,
    access_token_scopes varchar(1000) DEFAULT NULL,
    oidc_id_token_value blob DEFAULT NULL,
    oidc_id_token_issued_at timestamp NULL DEFAULT NULL,
    oidc_id_token_expires_at timestamp NULL DEFAULT NULL,
    oidc_id_token_metadata blob DEFAULT NULL,
    refresh_token_value blob DEFAULT NULL,
    refresh_token_issued_at timestamp NULL DEFAULT NULL,
    refresh_token_expires_at timestamp NULL DEFAULT NULL,
    refresh_token_metadata blob DEFAULT NULL,
    user_code_value blob DEFAULT NULL,
    user_code_issued_at timestamp NULL DEFAULT NULL,
    user_code_expires_at timestamp NULL DEFAULT NULL,
    user_code_metadata blob DEFAULT NULL,
    device_code_value blob DEFAULT NULL,
    device_code_issued_at timestamp NULL DEFAULT NULL,
    device_code_expires_at timestamp NULL DEFAULT NULL,
    device_code_metadata blob DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;
CREATE TABLE oauth2_authorization_consent (
    registered_client_id varchar(100) NOT NULL,
    principal_name varchar(200) NOT NULL,
    authorities varchar(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
) ENGINE=InnoDB;
CREATE TABLE NEWS_OAUTH_LOCK (ID INT PRIMARY KEY) ENGINE=InnoDB;
INSERT INTO NEWS_OAUTH_LOCK (ID) VALUES (1);
CREATE TABLE NEWS_OAUTH_LOGIN_GUARD (
    ID INT PRIMARY KEY,
    FAILURES INT NOT NULL,
    LOCKED_UNTIL timestamp NULL DEFAULT NULL,
    ENABLED BOOLEAN NOT NULL DEFAULT FALSE,
    TOKEN_VERSION BIGINT NOT NULL DEFAULT 1
) ENGINE=InnoDB;
INSERT INTO NEWS_OAUTH_LOGIN_GUARD (ID, FAILURES, LOCKED_UNTIL, ENABLED, TOKEN_VERSION) VALUES (1, 0, NULL, FALSE, 1);
-- 실제 소유자 계정, OAuth grant, 클라이언트 비밀은 생성하지 않습니다.
-- 회전해서 소모한 refresh token의 SHA-256만 저장합니다. 원문은 저장하지 않습니다.
-- 승인 행 삭제 이후에도 재사용 이력을 보존하며 활성 승인에 대한 이력을 조기 삭제하지 않습니다.
CREATE TABLE NEWS_OAUTH_REFRESH_HISTORY (
    TOKEN_HASH CHAR(64) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    AUTHORIZATION_ID VARCHAR(100) NOT NULL,
    REGISTERED_CLIENT_ID VARCHAR(100) NOT NULL,
    USED_AT timestamp NOT NULL,
    INDEX IDX_NEWS_OAUTH_REFRESH_FAMILY (AUTHORIZATION_ID)
) ENGINE=InnoDB;
-- 기존 제안 버전으로 이미 생성한 환경: LOGIN_GUARD의 ENABLED/TOKEN_VERSION 추가와
-- HISTORY 신규 생성이 필요합니다. 실제 스키마 확인 후 별도 승인된 ALTER를 준비합니다.
-- 계정 활성화는 초기 DDL과 분리한 별도 승인 작업입니다. 이 파일은 활성화하지 않습니다.
-- 비활성화 시 NEWS_OAUTH_LOCK(ID=1)을 FOR UPDATE로 잠근 단일 트랜잭션 안에서
-- ENABLED=FALSE, TOKEN_VERSION=TOKEN_VERSION+1로 변경해야 재활성화 후에도 기존 승인이 복구되지 않습니다.