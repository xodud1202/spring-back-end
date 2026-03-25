package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.common.response.ApiMessageResponse;
import com.xodud1202.springbackend.domain.shop.auth.ShopGoogleJoinRequest;
import com.xodud1202.springbackend.domain.shop.auth.ShopGoogleLoginRequest;
import com.xodud1202.springbackend.domain.shop.auth.ShopGoogleLoginResponse;
import com.xodud1202.springbackend.domain.shop.auth.ShopSessionRefreshResponse;
import com.xodud1202.springbackend.security.AuthCookieFactory;
import com.xodud1202.springbackend.service.ShopAuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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
	private final AuthCookieFactory authCookieFactory;

	@PostMapping("/api/shop/auth/google/login")
	// 구글 로그인 결과로 기존 회원 로그인 여부를 판정합니다.
	public ResponseEntity<ShopGoogleLoginResponse> loginWithGoogle(
		@Valid @RequestBody ShopGoogleLoginRequest request,
		HttpServletRequest httpRequest
	) {
		try {
			// 기존 고객 여부를 판정합니다.
			ShopGoogleLoginResponse response = shopAuthService.loginWithGoogle(request);

			// 기존 고객이면 세션과 로그인 쿠키를 함께 갱신합니다.
			if (response.loginSuccess() && response.custNo() != null) {
				return createLoginSuccessResponse(response, httpRequest);
			}

			// 신규 가입 대상이면 추가 정보 입력 응답을 그대로 반환합니다.
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("쇼핑몰 구글 로그인 판정 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("구글 로그인 처리에 실패했습니다.", exception);
		}
	}

	@PostMapping("/api/shop/auth/google/join")
	// 구글 신규 회원가입을 저장하고 로그인 처리합니다.
	public ResponseEntity<ShopGoogleLoginResponse> joinWithGoogle(
		@Valid @RequestBody ShopGoogleJoinRequest request,
		HttpServletRequest httpRequest
	) {
		try {
			// 구글 신규 회원가입을 처리합니다.
			ShopGoogleLoginResponse response = shopAuthService.joinWithGoogle(request);

			// 가입 후 로그인 성공이면 세션과 로그인 쿠키를 함께 갱신합니다.
			if (response.loginSuccess() && response.custNo() != null) {
				return createLoginSuccessResponse(response, httpRequest);
			}

			// 비정상 응답은 그대로 반환합니다.
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("쇼핑몰 구글 회원가입 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("구글 회원가입 처리에 실패했습니다.", exception);
		}
	}

	@PostMapping("/api/shop/auth/session/refresh")
	// 현재 쇼핑몰 로그인 세션과 쿠키 만료 시간을 1시간으로 갱신합니다.
	public ResponseEntity<ShopSessionRefreshResponse> refreshShopSession(HttpServletRequest request) {
		try {
			// 로그인 쿠키 값이 없으면 비회원 응답을 반환합니다.
			String custNoValue = findCookieValue(request, COOKIE_CUST_NO);
			String custNmValue = findCookieValue(request, COOKIE_CUST_NM);
			String custGradeCdValue = findCookieValue(request, COOKIE_CUST_GRADE_CD);
			if (isBlank(custNoValue) || isBlank(custNmValue) || isBlank(custGradeCdValue)) {
				return ResponseEntity.ok(ShopSessionRefreshResponse.unauthenticated());
			}

			// 고객 번호를 세션에 저장하고 세션 유효시간을 1시간으로 갱신합니다.
			Long custNo = parseCustomerNumber(custNoValue);
			HttpSession session = request.getSession(true);
			session.setAttribute(SESSION_ATTR_CUST_NO, custNo);
			session.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);

			// 고객명/등급코드를 디코드하고 등급명을 조회합니다.
			String decodedCustNm = decodeCookieValue(custNmValue);
			String decodedCustGradeCd = decodeCookieValue(custGradeCdValue);
			String decodedCustGradeNm = shopAuthService.getCustomerGradeName(decodedCustGradeCd);

			// 로그인 쿠키도 다시 발급해 만료 시간을 1시간으로 초기화합니다.
			return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, authCookieFactory.createShopLoginCookie(COOKIE_CUST_NO, custNoValue).toString())
				.header(HttpHeaders.SET_COOKIE, authCookieFactory.createShopLoginCookie(COOKIE_CUST_NM, custNmValue).toString())
				.header(HttpHeaders.SET_COOKIE, authCookieFactory.createShopLoginCookie(COOKIE_CUST_GRADE_CD, custGradeCdValue).toString())
				.body(ShopSessionRefreshResponse.authenticated(custNo, decodedCustNm, decodedCustGradeCd, decodedCustGradeNm));
		} catch (IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("쇼핑몰 세션 갱신 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("로그인 세션 갱신에 실패했습니다.", exception);
		}
	}

	@PostMapping("/api/shop/auth/logout")
	// 쇼핑몰 로그아웃 시 세션과 로그인 쿠키를 모두 만료 처리합니다.
	public ResponseEntity<ApiMessageResponse> logoutShop(HttpServletRequest request) {
		try {
			// 쇼핑몰 세션이 존재하면 고객번호 속성을 제거하고 세션을 무효화합니다.
			invalidateShopSession(request);

			// 로그인 쿠키 3종을 모두 만료 처리한 뒤 성공 응답을 반환합니다.
			return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, authCookieFactory.createExpiredShopLoginCookie(COOKIE_CUST_NO).toString())
				.header(HttpHeaders.SET_COOKIE, authCookieFactory.createExpiredShopLoginCookie(COOKIE_CUST_NM).toString())
				.header(HttpHeaders.SET_COOKIE, authCookieFactory.createExpiredShopLoginCookie(COOKIE_CUST_GRADE_CD).toString())
				.body(new ApiMessageResponse("로그아웃 처리되었습니다."));
		} catch (Exception exception) {
			log.error("쇼핑몰 로그아웃 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("로그아웃 처리에 실패했습니다.", exception);
		}
	}

	// 쇼핑몰 세션을 무효화합니다.
	private void invalidateShopSession(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return;
		}
		session.removeAttribute(SESSION_ATTR_CUST_NO);
		session.invalidate();
	}

	// 로그인 성공 응답을 세션/쿠키와 함께 반환합니다.
	private ResponseEntity<ShopGoogleLoginResponse> createLoginSuccessResponse(
		ShopGoogleLoginResponse response,
		HttpServletRequest httpRequest
	) {
		// 세션에 고객번호를 저장하고 1시간 만료로 갱신합니다.
		HttpSession session = httpRequest.getSession(true);
		session.setAttribute(SESSION_ATTR_CUST_NO, response.custNo());
		session.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);

		// 로그인 쿠키 3종을 발급해 브라우저 로그인 상태를 유지합니다.
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, authCookieFactory.createShopLoginCookie(COOKIE_CUST_NO, String.valueOf(response.custNo())).toString())
			.header(HttpHeaders.SET_COOKIE, authCookieFactory.createShopLoginCookie(COOKIE_CUST_NM, encodeCookieValue(response.custNm())).toString())
			.header(HttpHeaders.SET_COOKIE, authCookieFactory.createShopLoginCookie(COOKIE_CUST_GRADE_CD, encodeCookieValue(response.custGradeCd())).toString())
			.body(response);
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

	// 쿠키 문자열을 고객번호 Long 값으로 변환합니다.
	private Long parseCustomerNumber(String custNoValue) {
		try {
			return Long.valueOf(custNoValue);
		} catch (NumberFormatException exception) {
			throw new IllegalStateException("로그인 세션 갱신에 실패했습니다.", exception);
		}
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
