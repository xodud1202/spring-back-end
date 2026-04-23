package com.xodud1202.springbackend.security;

import com.xodud1202.springbackend.common.shop.ShopSessionPolicy;
import com.xodud1202.springbackend.common.snippet.SnippetSessionPolicy;
import com.xodud1202.springbackend.common.work.WorkSessionPolicy;
import com.xodud1202.springbackend.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
// 쇼핑몰/스니펫/업무관리 로그인 복구용 서명 토큰 발급과 검증을 담당합니다.
public class SignedLoginTokenService {
	private static final String CLAIM_TOKEN_TYPE = "tokenType";

	private final JwtProperties jwtProperties;

	// 쇼핑몰 로그인 복구용 서명 토큰 타입을 정의합니다.
	public enum LoginTokenType {
		SHOP_AUTH,
		SNIPPET_AUTH,
		WORK_AUTH
	}

	// 쇼핑몰 고객번호 기준 서명 토큰을 발급합니다.
	public String generateShopAuthToken(Long custNo) {
		return generateSignedToken(LoginTokenType.SHOP_AUTH, custNo, ShopSessionPolicy.SESSION_COOKIE_MAX_AGE);
	}

	// 스니펫 사용자번호 기준 서명 토큰을 발급합니다.
	public String generateSnippetAuthToken(Long snippetUserNo) {
		return generateSignedToken(LoginTokenType.SNIPPET_AUTH, snippetUserNo, SnippetSessionPolicy.SESSION_COOKIE_MAX_AGE);
	}

	// 업무관리 사용자번호 기준 서명 토큰을 발급합니다.
	public String generateWorkAuthToken(Long workUserNo) {
		return generateSignedToken(LoginTokenType.WORK_AUTH, workUserNo, WorkSessionPolicy.SESSION_COOKIE_MAX_AGE);
	}

	// 쇼핑몰 로그인 복구용 토큰에서 고객번호를 복원합니다.
	public Long parseShopCustNo(String token) {
		return parseSubjectNo(token, LoginTokenType.SHOP_AUTH);
	}

	// 스니펫 로그인 복구용 토큰에서 사용자번호를 복원합니다.
	public Long parseSnippetUserNo(String token) {
		return parseSubjectNo(token, LoginTokenType.SNIPPET_AUTH);
	}

	// 업무관리 로그인 복구용 토큰에서 사용자번호를 복원합니다.
	public Long parseWorkUserNo(String token) {
		return parseSubjectNo(token, LoginTokenType.WORK_AUTH);
	}

	// 공통 서명 키를 생성합니다.
	private Key getSigningKey() {
		return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
	}

	// 도메인별 만료시간에 맞춰 로그인 복구용 서명 토큰을 생성합니다.
	private String generateSignedToken(LoginTokenType tokenType, Long subjectNo, Duration expiration) {
		// 토큰 subject는 실제 식별자 문자열로만 저장합니다.
		if (subjectNo == null || subjectNo < 1L) {
			throw new IllegalArgumentException("로그인 토큰 subject가 올바르지 않습니다.");
		}

		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + expiration.toMillis());
		return Jwts.builder()
			.setSubject(String.valueOf(subjectNo))
			.claim(CLAIM_TOKEN_TYPE, tokenType.name())
			.setIssuedAt(now)
			.setExpiration(expiryDate)
			.signWith(getSigningKey())
			.compact();
	}

	// 기대한 타입의 서명 토큰에서 양수 식별자를 추출합니다.
	private Long parseSubjectNo(String token, LoginTokenType expectedType) {
		// 빈 토큰은 복구 대상이 아니므로 null을 반환합니다.
		if (token == null || token.trim().isEmpty()) {
			return null;
		}

		try {
			Claims claims = Jwts.parserBuilder()
				.setSigningKey(getSigningKey())
				.build()
				.parseClaimsJws(token)
				.getBody();

			Object tokenType = claims.get(CLAIM_TOKEN_TYPE);
			if (tokenType == null || !expectedType.name().equals(String.valueOf(tokenType))) {
				return null;
			}

			Long subjectNo = parsePositiveLong(claims.getSubject());
			return subjectNo != null && subjectNo > 0L ? subjectNo : null;
		} catch (ExpiredJwtException exception) {
			log.debug("로그인 복구 토큰이 만료되었습니다. type={}", expectedType, exception);
		} catch (UnsupportedJwtException | MalformedJwtException | SignatureException | IllegalArgumentException exception) {
			log.debug("로그인 복구 토큰 검증에 실패했습니다. type={}", expectedType, exception);
		}
		return null;
	}

	// 문자열 subject를 양수 Long 값으로 변환합니다.
	private Long parsePositiveLong(String subject) {
		// subject가 비어 있으면 null을 반환합니다.
		if (subject == null || subject.trim().isEmpty()) {
			return null;
		}

		try {
			Long parsedValue = Long.valueOf(subject.trim());
			return parsedValue > 0L ? parsedValue : null;
		} catch (NumberFormatException exception) {
			return null;
		}
	}
}
