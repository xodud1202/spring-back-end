package com.xodud1202.springbackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

// 사용자 기본 정보를 저장하는 엔티티입니다.
@Data
@Entity
@Table(name = "USER_BASE")
public class UserBaseEntity implements UserDetails {
	
	@Id
	@Column(name = "USR_NO")
	@GeneratedValue( strategy = GenerationType.IDENTITY)
	private Long usrNo;
	
	@Column(name = "LOGIN_ID", unique = true, nullable = false)
	private String loginId;
	
	@Column(name = "PWD", nullable = false)
	private String pwd;
	
	@Column(name = "USER_NM")
	private String userNm;
	
	@Column(name = "USR_GRADE_CD", nullable = false)
	private String usrGradeCd = "10"; // 기본 역할
	
	@Column(name = "USR_STAT_CD")
	private String usrStatCd;
	
	@Column(name = "ACCESS_DT")
	private Date accessDt;
	
	@Column(name = "REG_NO")
	private Long regNo;
	
	@Column(name = "REG_DT")
	private Date regDt;
	
	@Column(name = "UDT_NO")
	private Long udtNo;
	
	@Column(name = "UDT_DT")
	private Date udtDt;
	
	@Override
	public String getUsername() {
		return loginId;
	}
	
	@Override
	public String getPassword() {
		return pwd;
	}
	
	@Override
	@JsonIgnore
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return Collections.singletonList(new SimpleGrantedAuthority(usrGradeCd));
	}
}
