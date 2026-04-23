package com.xodud1202.springbackend.security;

import com.xodud1202.springbackend.common.shop.ShopSessionPolicy;
import com.xodud1202.springbackend.common.snippet.SnippetSessionPolicy;
import com.xodud1202.springbackend.common.work.WorkSessionPolicy;
import com.xodud1202.springbackend.config.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
// 인증 관련 httpOnly 쿠키 생성 규칙을 중앙화합니다.
public class AuthCookieFactory {
	private final JwtProperties jwtProperties;

	// 백오피스 리프레시 토큰 쿠키를 생성합니다.
	public ResponseCookie createRefreshTokenCookie(String refreshToken) {
		return ResponseCookie.from("refreshToken", refreshToken)
			.httpOnly(true)
			.secure(jwtProperties.cookieSecure())
			.path("/")
			.maxAge(jwtProperties.refreshTokenExpiration())
			.sameSite("Strict")
			.build();
	}

	// 백오피스 리프레시 토큰 만료 쿠키를 생성합니다.
	public ResponseCookie createExpiredRefreshTokenCookie() {
		return ResponseCookie.from("refreshToken", "")
			.httpOnly(true)
			.secure(jwtProperties.cookieSecure())
			.path("/")
			.maxAge(Duration.ZERO)
			.sameSite("Strict")
			.build();
	}

	// 쇼핑몰 로그인 쿠키를 생성합니다.
	public ResponseCookie createShopLoginCookie(String name, String value) {
		return ResponseCookie.from(name, value)
			.httpOnly(true)
			.secure(jwtProperties.cookieSecure())
			.path("/")
			.maxAge(ShopSessionPolicy.SESSION_COOKIE_MAX_AGE)
			.sameSite("Lax")
			.build();
	}

	// 쇼핑몰 서명 로그인 쿠키를 생성합니다.
	public ResponseCookie createShopAuthCookie(String token) {
		return createShopLoginCookie(ShopSessionPolicy.COOKIE_SHOP_AUTH, token);
	}

	// 쇼핑몰 로그인 만료 쿠키를 생성합니다.
	public ResponseCookie createExpiredShopLoginCookie(String name) {
		return ResponseCookie.from(name, "")
			.httpOnly(true)
			.secure(jwtProperties.cookieSecure())
			.path("/")
			.maxAge(Duration.ZERO)
			.sameSite("Lax")
			.build();
	}

	// 쇼핑몰 서명 로그인 만료 쿠키를 생성합니다.
	public ResponseCookie createExpiredShopAuthCookie() {
		return createExpiredShopLoginCookie(ShopSessionPolicy.COOKIE_SHOP_AUTH);
	}

	// 스니펫 로그인 쿠키를 생성합니다.
	public ResponseCookie createSnippetLoginCookie(String name, String value) {
		return ResponseCookie.from(name, value)
			.httpOnly(true)
			.secure(jwtProperties.cookieSecure())
			.path("/")
			.maxAge(SnippetSessionPolicy.SESSION_COOKIE_MAX_AGE)
			.sameSite("Lax")
			.build();
	}

	// 스니펫 로그인 만료 쿠키를 생성합니다.
	public ResponseCookie createExpiredSnippetLoginCookie(String name) {
		return ResponseCookie.from(name, "")
			.httpOnly(true)
			.secure(jwtProperties.cookieSecure())
			.path("/")
			.maxAge(Duration.ZERO)
			.sameSite("Lax")
			.build();
	}

	// 업무관리 로그인 쿠키를 생성합니다.
	public ResponseCookie createWorkLoginCookie(String name, String value) {
		return ResponseCookie.from(name, value)
			.httpOnly(true)
			.secure(jwtProperties.cookieSecure())
			.path("/")
			.maxAge(WorkSessionPolicy.SESSION_COOKIE_MAX_AGE)
			.sameSite("Lax")
			.build();
	}

	// 업무관리 로그인 만료 쿠키를 생성합니다.
	public ResponseCookie createExpiredWorkLoginCookie(String name) {
		return ResponseCookie.from(name, "")
			.httpOnly(true)
			.secure(jwtProperties.cookieSecure())
			.path("/")
			.maxAge(Duration.ZERO)
			.sameSite("Lax")
			.build();
	}
}
