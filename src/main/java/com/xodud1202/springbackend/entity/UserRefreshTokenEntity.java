package com.xodud1202.springbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
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

	// 신규 리프레시 토큰 발급 정보를 엔티티에 반영합니다.
	public void issue(Long usrNo, String refreshTokenHash, Date expiresAt, Date lastUsedAt, String clientIp, String userAgent) {
		this.usrNo = usrNo;
		this.refreshTokenHash = refreshTokenHash;
		this.expiresAt = expiresAt;
		this.lastUsedAt = lastUsedAt;
		this.clientIp = clientIp;
		this.userAgent = userAgent;
		this.isRevoked = "N";
	}

	// 리프레시 토큰 회전 결과를 엔티티에 반영합니다.
	public void rotate(String refreshTokenHash, Date expiresAt, Date lastUsedAt) {
		this.refreshTokenHash = refreshTokenHash;
		this.expiresAt = expiresAt;
		this.lastUsedAt = lastUsedAt;
	}
}
