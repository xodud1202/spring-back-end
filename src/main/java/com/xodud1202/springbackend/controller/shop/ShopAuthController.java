package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.domain.shop.auth.ShopGoogleLoginRequest;
import com.xodud1202.springbackend.domain.shop.auth.ShopGoogleLoginResponse;
import com.xodud1202.springbackend.service.ShopAuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
// 쇼핑몰 고객 로그인 API를 제공합니다.
public class ShopAuthController {
	private static final String COOKIE_CUST_NO = "cust_no";
	private static final String COOKIE_CUST_NM = "cust_nm";
	private static final String COOKIE_CUST_GRADE_CD = "cust_grade_cd";
	private static final String SESSION_ATTR_CUST_NO = "shopCustNo";
	private static final int SESSION_TIMEOUT_SECONDS = 60 * 60;

	private final ShopAuthService shopAuthService;

	@Value("${jwt.cookie-secure:false}")
	private boolean jwtCookieSecure;

	// 구글 로그인 결과로 기존 회원 로그인 여부를 판정합니다.
	@PostMapping("/api/shop/auth/google/login")
	public ResponseEntity<Object> loginWithGoogle(
		@RequestBody ShopGoogleLoginRequest request,
		HttpServletRequest httpRequest
	) {
		try {
			// 기존 고객 여부를 판정합니다.
			ShopGoogleLoginResponse response = shopAuthService.loginWithGoogle(request);

			// 기존 고객이면 세션과 로그인 쿠키를 함께 갱신합니다.
			if (response.isLoginSuccess() && response.getCustNo() != null) {
				HttpSession session = httpRequest.getSession(true);
				session.setAttribute(SESSION_ATTR_CUST_NO, response.getCustNo());
				session.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);

				return ResponseEntity.ok()
					.header(HttpHeaders.SET_COOKIE, buildCookie(COOKIE_CUST_NO, String.valueOf(response.getCustNo())).toString())
					.header(HttpHeaders.SET_COOKIE, buildCookie(COOKIE_CUST_NM, encodeCookieValue(response.getCustNm())).toString())
					.header(HttpHeaders.SET_COOKIE, buildCookie(COOKIE_CUST_GRADE_CD, encodeCookieValue(response.getCustGradeCd())).toString())
					.body(response);
			}

			// 신규 가입 대상이면 추가 정보 입력 응답을 그대로 반환합니다.
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException exception) {
			// 유효성 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 기타 예외는 500 응답으로 반환합니다.
			log.error("쇼핑몰 구글 로그인 판정 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "구글 로그인 처리에 실패했습니다."));
		}
	}

	// 현재 쇼핑몰 로그인 세션과 쿠키 만료 시간을 1시간으로 갱신합니다.
	@PostMapping("/api/shop/auth/session/refresh")
	public ResponseEntity<Object> refreshShopSession(HttpServletRequest request) {
		try {
			// 로그인 쿠키 값이 없으면 비회원 응답을 반환합니다.
			String custNoValue = findCookieValue(request, COOKIE_CUST_NO);
			String custNmValue = findCookieValue(request, COOKIE_CUST_NM);
			String custGradeCdValue = findCookieValue(request, COOKIE_CUST_GRADE_CD);
			if (isBlank(custNoValue) || isBlank(custNmValue) || isBlank(custGradeCdValue)) {
				return ResponseEntity.ok(Map.of("authenticated", false));
			}

			// 고객 번호를 세션에 저장하고 세션 유효시간을 1시간으로 갱신합니다.
			Long custNo = Long.valueOf(custNoValue);
			HttpSession session = request.getSession(true);
			session.setAttribute(SESSION_ATTR_CUST_NO, custNo);
			session.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);

			// 로그인 쿠키도 다시 발급해 만료 시간을 1시간으로 초기화합니다.
			return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, buildCookie(COOKIE_CUST_NO, custNoValue).toString())
				.header(HttpHeaders.SET_COOKIE, buildCookie(COOKIE_CUST_NM, custNmValue).toString())
				.header(HttpHeaders.SET_COOKIE, buildCookie(COOKIE_CUST_GRADE_CD, custGradeCdValue).toString())
				.body(Map.of(
					"authenticated", true,
					"custNo", custNo,
					"custNm", decodeCookieValue(custNmValue),
					"custGradeCd", decodeCookieValue(custGradeCdValue)
				));
		} catch (Exception exception) {
			// 세션 갱신 실패 시 에러 로그와 실패 메시지를 반환합니다.
			log.error("쇼핑몰 세션 갱신 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "로그인 세션 갱신에 실패했습니다."));
		}
	}

	// 쿠키 응답 객체를 생성합니다.
	private ResponseCookie buildCookie(String name, String value) {
		return ResponseCookie.from(name, value)
			.httpOnly(true)
			.secure(jwtCookieSecure)
			.path("/")
			.maxAge(Duration.ofSeconds(SESSION_TIMEOUT_SECONDS))
			.sameSite("Lax")
			.build();
	}

	// 요청 쿠키에서 지정한 이름의 값을 찾습니다.
	private String findCookieValue(HttpServletRequest request, String cookieName) {
		if (request.getCookies() == null) {
			return null;
		}
		return Arrays.stream(request.getCookies())
			.filter(cookie -> cookieName.equals(cookie.getName()))
			.findFirst()
			.map(Cookie::getValue)
			.orElse(null);
	}

	// 쿠키에 저장 가능한 문자열로 인코딩합니다.
	private String encodeCookieValue(String value) {
		return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
	}

	// 쿠키 문자열을 원래 값으로 디코드합니다.
	private String decodeCookieValue(String value) {
		return URLDecoder.decode(value == null ? "" : value, StandardCharsets.UTF_8);
	}

	// 문자열 공백 여부를 확인합니다.
	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
