package com.xodud1202.springbackend.security;

import com.xodud1202.springbackend.common.snippet.SnippetSessionPolicy;
import com.xodud1202.springbackend.common.work.WorkSessionPolicy;
import com.xodud1202.springbackend.config.properties.JwtProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 인증 쿠키 생성 정책을 검증합니다.
class AuthCookieFactoryTests {
	private static final String JWT_SECRET = "test-secret-key-test-secret-key-test-secret-key";

	@Test
	@DisplayName("cookie-secure가 true이면 인증 쿠키에 Secure 속성을 포함한다")
	// 운영 기본 설정에서 모든 인증 쿠키가 Secure로 발급되는지 검증합니다.
	void authCookiesIncludeSecureWhenCookieSecureTrue() {
		// Secure 설정이 켜진 쿠키 팩토리를 구성합니다.
		AuthCookieFactory authCookieFactory = createAuthCookieFactory(true);

		// 주요 인증 쿠키 문자열을 수집합니다.
		List<ResponseCookie> cookies = List.of(
			authCookieFactory.createRefreshTokenCookie("refresh-token"),
			authCookieFactory.createShopAuthCookie("shop-token"),
			authCookieFactory.createWorkLoginCookie(WorkSessionPolicy.COOKIE_WORK_USER_NO, "work-token"),
			authCookieFactory.createSnippetLoginCookie(SnippetSessionPolicy.COOKIE_SNIPPET_USER_NO, "snippet-token")
		);

		// 각 인증 쿠키가 Secure 속성을 포함하는지 확인합니다.
		for (ResponseCookie cookie : cookies) {
			assertTrue(cookie.toString().contains("; Secure"));
		}
	}

	@Test
	@DisplayName("cookie-secure가 false이면 local 인증 쿠키에 Secure 속성을 포함하지 않는다")
	// local override 설정에서 인증 쿠키가 기존 개발 편의 동작을 유지하는지 검증합니다.
	void authCookiesOmitSecureWhenCookieSecureFalse() {
		// Secure 설정이 꺼진 쿠키 팩토리를 구성합니다.
		AuthCookieFactory authCookieFactory = createAuthCookieFactory(false);

		// 주요 인증 쿠키 문자열을 수집합니다.
		List<ResponseCookie> cookies = List.of(
			authCookieFactory.createRefreshTokenCookie("refresh-token"),
			authCookieFactory.createShopAuthCookie("shop-token"),
			authCookieFactory.createWorkLoginCookie(WorkSessionPolicy.COOKIE_WORK_USER_NO, "work-token"),
			authCookieFactory.createSnippetLoginCookie(SnippetSessionPolicy.COOKIE_SNIPPET_USER_NO, "snippet-token")
		);

		// 각 인증 쿠키가 Secure 속성을 포함하지 않는지 확인합니다.
		for (ResponseCookie cookie : cookies) {
			assertFalse(cookie.toString().contains("; Secure"));
		}
	}

	// 테스트용 인증 쿠키 팩토리를 생성합니다.
	private AuthCookieFactory createAuthCookieFactory(boolean cookieSecure) {
		JwtProperties jwtProperties = new JwtProperties(
			JWT_SECRET,
			Duration.ofMinutes(30),
			Duration.ofDays(30),
			cookieSecure
		);
		return new AuthCookieFactory(jwtProperties);
	}
}
