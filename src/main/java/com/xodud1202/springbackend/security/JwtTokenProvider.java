package com.xodud1202.springbackend.security;

import com.xodud1202.springbackend.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Collections;
import java.util.Date;

// JWT 토큰 발급과 검증을 담당합니다.
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
	private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

	private static final String ACCESS_TOKEN_TYPE = "ACCESS";
	private static final String REFRESH_TOKEN_TYPE = "REFRESH";

	private final UserDetailsService userDetailService;
	private final JwtProperties jwtProperties;

	// 서명 키를 생성합니다.
	private Key getSigningKey() {
		return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
	}

	// 로그인 ID로 액세스 토큰을 생성합니다.
	public String generateAccessTokenByLoginId(String loginId) {
		// loginId 기준 사용자 인증 객체를 생성합니다.
		UserDetails userDetails = userDetailService.loadUserByUsername(loginId);
		Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, Collections.emptyList());
		return generateAccessToken(authentication);
	}

	// 액세스 토큰을 생성합니다.
	public String generateAccessToken(Authentication authentication) {
		UserDetails userDetails = (UserDetails) authentication.getPrincipal();
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + jwtProperties.accessTokenExpiration().toMillis());
		return buildToken(userDetails.getUsername(), ACCESS_TOKEN_TYPE, now, expiryDate);
	}

	// 리프레시 토큰을 생성합니다.
	public String generateRefreshToken(String username) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + jwtProperties.refreshTokenExpiration().toMillis());
		return buildToken(username, REFRESH_TOKEN_TYPE, now, expiryDate);
	}

	// 토큰에서 사용자명을 추출합니다.
	public String getUsernameFromJWT(String token) {
		return parseClaims(token).getSubject();
	}

	// 토큰에서 타입 클레임을 추출합니다.
	public String getTokenTypeFromJWT(String token) {
		Object tokenType = parseClaims(token).get("tokenType");
		return tokenType == null ? null : String.valueOf(tokenType);
	}

	// 액세스 토큰 타입인지 확인합니다.
	public boolean isAccessToken(String token) {
		try {
			return ACCESS_TOKEN_TYPE.equalsIgnoreCase(getTokenTypeFromJWT(token));
		} catch (Exception exception) {
			return false;
		}
	}

	// 리프레시 토큰 타입인지 확인합니다.
	public boolean isRefreshToken(String token) {
		try {
			return REFRESH_TOKEN_TYPE.equalsIgnoreCase(getTokenTypeFromJWT(token));
		} catch (Exception exception) {
			return false;
		}
	}

	// 토큰 검증 결과를 기존 문자열 규약으로 반환합니다.
	public String validateCheckToken(String authToken) {
		try {
			parseClaims(authToken);
			return "OK";
		} catch (SignatureException exception) {
			return "Invalid JWT signature";
		} catch (MalformedJwtException exception) {
			return "Invalid JWT token";
		} catch (ExpiredJwtException exception) {
			return "EXPIRED";
		} catch (UnsupportedJwtException exception) {
			return "Unsupported JWT token";
		} catch (IllegalArgumentException exception) {
			return "JWT claims string is empty";
		} catch (Exception exception) {
			return "Error";
		}
	}

	// 토큰 유효성을 검증합니다.
	public boolean validateToken(String authToken) {
		try {
			parseClaims(authToken);
			return true;
		} catch (SignatureException exception) {
			log.error("Invalid JWT signature");
		} catch (MalformedJwtException exception) {
			log.error("Invalid JWT token");
		} catch (ExpiredJwtException exception) {
			log.error("Expired JWT token");
		} catch (UnsupportedJwtException exception) {
			log.error("Unsupported JWT token");
		} catch (IllegalArgumentException exception) {
			log.error("JWT claims string is empty");
		}
		return false;
	}

	// 토큰 공통 본문을 구성해 서명 토큰 문자열을 생성합니다.
	private String buildToken(String subject, String tokenType, Date issuedAt, Date expiration) {
		return Jwts.builder()
			.setSubject(subject)
			.claim("tokenType", tokenType)
			.setIssuedAt(issuedAt)
			.setExpiration(expiration)
			.signWith(getSigningKey())
			.compact();
	}

	// 토큰 문자열을 파싱해 클레임을 반환합니다.
	private Claims parseClaims(String token) {
		return Jwts.parserBuilder()
			.setSigningKey(getSigningKey())
			.build()
			.parseClaimsJws(token)
			.getBody();
	}
}
