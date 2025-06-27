package com.xodud1202.springbackend.domain;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

@Data
@Entity
@Table(name = "USER_BASE")
public class UserBase implements UserDetails {
	
	@Id
	@GeneratedValue( strategy = GenerationType.IDENTITY)
	private Long usrNo;
	
	@Column(name = "LOGIN_ID", unique = true, nullable = false)
	private String loginId;
	
	@Column(name = "PWD", nullable = false)
	private String pwd;
	
	@Column(name = "USER_NM", nullable = false)
	private String userNm;
	
	@Column(nullable = false)
	private String usrGradeCd = "10"; // 기본 역할
	
	@Column(name = "ACCESS_DT")
	private Date accessDt;
	
	@Column(name = "REG_NO")
	private Long regNo;
	
	@Column(name = "REG_DT")
	private Date regDt;
	
	@Column(name = "UPD_NO")
	private Long updNo;
	
	@Column(name = "UPD_DT")
	private Date updDt;
	
	@Column(name = "REFRESH_TOKEN")
	private String refreshToken;        // 자동로그인 refreshToken
	
	@Column(name = "REFRESH_TOKEN_EXPIRY")
	private Date refreshTokenExpiry;    // 자동로그인 refreshToken 만료기한
	
	private String jwtToken;            // 현재 jwt accessToken
	
	@Override
	public String getUsername() {
		return loginId;
	}
	
	@Override
	public String getPassword() {
		return pwd;
	}
	
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return Collections.singletonList(new SimpleGrantedAuthority(usrGradeCd));
	}
}
