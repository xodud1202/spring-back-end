package com.xodud1202.springbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "USER_REFRESH_TOKEN")
// 사용자 리프레시 토큰 엔티티를 정의합니다.
public class UserRefreshTokenEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "TOKEN_ID")
	private Long tokenId;

	@Column(name = "USR_NO", nullable = false)
	private Long usrNo;

	@Column(name = "REFRESH_TOKEN_HASH", nullable = false)
	private String refreshTokenHash;

	@Column(name = "EXPIRES_AT", nullable = false)
	private Date expiresAt;

	@Column(name = "LAST_USED_AT")
	private Date lastUsedAt;

	@Column(name = "CLIENT_IP")
	private String clientIp;

	@Column(name = "USER_AGENT")
	private String userAgent;

	@Column(name = "IS_REVOKED", nullable = false)
	private String isRevoked;

	@Column(name = "REG_DT")
	private Date regDt;

	@Column(name = "UDT_DT")
	private Date udtDt;
}
