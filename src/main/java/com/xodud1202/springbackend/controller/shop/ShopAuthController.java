package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.common.response.ApiMessageResponse;
import com.xodud1202.springbackend.common.shop.ShopSessionPolicy;
import com.xodud1202.springbackend.domain.shop.auth.ShopAuthMeResponse;
import com.xodud1202.springbackend.domain.shop.auth.ShopCustomerSessionVO;
import com.xodud1202.springbackend.domain.shop.auth.ShopGoogleJoinRequest;
import com.xodud1202.springbackend.domain.shop.auth.ShopGoogleLoginRequest;
import com.xodud1202.springbackend.domain.shop.auth.ShopGoogleLoginResponse;
import com.xodud1202.springbackend.security.AuthCookieFactory;
import com.xodud1202.springbackend.security.SignedLoginTokenService;
import com.xodud1202.springbackend.service.ShopAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
// 쇼핑몰 고객 로그인 API를 제공합니다.
public class ShopAuthController {
	private final ShopAuthService shopAuthService;
	private final AuthCookieFactory authCookieFactory;
	private final SignedLoginTokenService signedLoginTokenService;

	@PostMapping("/api/shop/auth/google/login")
	// 구글 로그인 결과로 기존 회원 로그인 여부를 판정합니다.
	public ResponseEntity<ShopGoogleLoginResponse> loginWithGoogle(
		@Valid @RequestBody ShopGoogleLoginRequest request,
		HttpServletRequest httpRequest
	) {
		try {
			// 기존 고객 여부를 판정합니다.
			ShopGoogleLoginResponse response = shopAuthService.loginWithGoogle(request);

			// 기존 고객이면 쇼핑몰 세션만 갱신합니다.
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

			// 가입 후 로그인 성공이면 쇼핑몰 세션만 갱신합니다.
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

	@GetMapping("/api/shop/auth/me")
	// 현재 쇼핑몰 로그인 세션 기준 사용자 정보를 조회합니다.
	public ResponseEntity<ShopAuthMeResponse> getCurrentShopAuth(HttpServletRequest request) {
		try {
			// 세션 우선, 없으면 서명된 shop_auth 쿠키 기준으로 고객번호를 복구합니다.
			Long custNo = resolveAuthenticatedShopCustNo(request);
			if (custNo == null) {
				return buildUnauthenticatedShopAuthResponse(request);
			}

			// 현재 고객번호로 유효한 고객 정보를 다시 조회합니다.
			ShopCustomerSessionVO customer = shopAuthService.getShopCustomerByCustNo(custNo);
			if (customer == null || customer.custNo() == null) {
				clearShopSession(request);
				return buildUnauthenticatedShopAuthResponse(request);
			}

			// 정상 고객이면 세션과 서명 로그인 쿠키를 모두 정책 기준으로 갱신합니다.
			HttpSession session = request.getSession(true);
			ShopSessionPolicy.applyAuthenticatedSession(session, customer.custNo());
			return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, buildAuthenticatedShopCookieHeaderValues(customer.custNo()))
				.body(buildShopAuthMeResponse(customer));
		} catch (IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("쇼핑몰 현재 로그인 상태 조회 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("로그인 상태 조회에 실패했습니다.", exception);
		}
	}

	@PostMapping("/api/shop/auth/logout")
	// 쇼핑몰 로그아웃 시 쇼핑몰 세션과 로그인 쿠키를 모두 정리합니다.
	public ResponseEntity<ApiMessageResponse> logoutShop(HttpServletRequest request) {
		try {
			// 쇼핑몰 세션이 존재하면 고객번호 속성을 제거하고 신규/레거시 로그인 쿠키를 모두 만료합니다.
			clearShopSession(request);
			return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, buildExpiredShopCookieHeaderValues())
				.body(new ApiMessageResponse("로그아웃 처리되었습니다."));
		} catch (Exception exception) {
			log.error("쇼핑몰 로그아웃 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("로그아웃 처리에 실패했습니다.", exception);
		}
	}

	// 세션 또는 서명된 shop_auth 쿠키에서 현재 고객번호를 복구합니다.
	private Long resolveAuthenticatedShopCustNo(HttpServletRequest request) {
		// 세션 고객번호가 있으면 우선 사용합니다.
		HttpSession session = request.getSession(false);
		Long sessionCustNo = session == null ? null : ShopSessionPolicy.resolveShopCustNo(session.getAttribute(ShopSessionPolicy.SESSION_ATTR_CUST_NO));
		if (sessionCustNo != null) {
			return sessionCustNo;
		}

		// 세션이 없으면 서명된 shop_auth 쿠키로 복구를 시도합니다.
		return ShopSessionPolicy.resolveShopCustNoFromRequest(request, signedLoginTokenService);
	}

	// 쇼핑몰 세션 속성만 제거합니다.
	private void clearShopSession(HttpServletRequest request) {
		ShopSessionPolicy.clearAuthenticatedSession(request == null ? null : request.getSession(false));
	}

	// 로그인 성공 응답을 쇼핑몰 세션과 함께 반환합니다.
	private ResponseEntity<ShopGoogleLoginResponse> createLoginSuccessResponse(
		ShopGoogleLoginResponse response,
		HttpServletRequest httpRequest
	) {
		// 세션에 고객번호를 저장하고 정책 기준 만료시간을 갱신합니다.
		HttpSession session = httpRequest.getSession(true);
		ShopSessionPolicy.applyAuthenticatedSession(session, response.custNo());

		// 서명된 shop_auth 쿠키를 발급하고 레거시 cust_* 쿠키는 정리합니다.
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, buildAuthenticatedShopCookieHeaderValues(response.custNo()))
			.body(response);
	}

	// 현재 고객 정보를 현재 로그인 상태 응답으로 변환합니다.
	private ShopAuthMeResponse buildShopAuthMeResponse(ShopCustomerSessionVO customer) {
		String custGradeCd = customer == null ? null : customer.custGradeCd();
		return ShopAuthMeResponse.authenticated(
			customer == null ? null : customer.custNo(),
			customer == null ? null : customer.custNm(),
			custGradeCd,
			shopAuthService.getCustomerGradeName(custGradeCd)
		);
	}

	// 비로그인 응답과 함께 쇼핑몰 로그인 쿠키를 모두 만료합니다.
	private ResponseEntity<ShopAuthMeResponse> buildUnauthenticatedShopAuthResponse(HttpServletRequest request) {
		// 남아 있는 쇼핑몰 세션 속성을 먼저 정리합니다.
		clearShopSession(request);
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, buildExpiredShopCookieHeaderValues())
			.body(ShopAuthMeResponse.unauthenticated());
	}

	// 서명된 shop_auth 발급과 레거시 cust_* 만료 헤더를 함께 생성합니다.
	private String[] buildAuthenticatedShopCookieHeaderValues(Long custNo) {
		return new String[] {
			authCookieFactory.createShopAuthCookie(signedLoginTokenService.generateShopAuthToken(custNo)).toString(),
			authCookieFactory.createExpiredShopLoginCookie(ShopSessionPolicy.COOKIE_CUST_NO).toString(),
			authCookieFactory.createExpiredShopLoginCookie(ShopSessionPolicy.COOKIE_CUST_NM).toString(),
			authCookieFactory.createExpiredShopLoginCookie(ShopSessionPolicy.COOKIE_CUST_GRADE_CD).toString()
		};
	}

	// shop_auth와 레거시 cust_* 로그인 쿠키 만료 헤더를 생성합니다.
	private String[] buildExpiredShopCookieHeaderValues() {
		return new String[] {
			authCookieFactory.createExpiredShopAuthCookie().toString(),
			authCookieFactory.createExpiredShopLoginCookie(ShopSessionPolicy.COOKIE_CUST_NO).toString(),
			authCookieFactory.createExpiredShopLoginCookie(ShopSessionPolicy.COOKIE_CUST_NM).toString(),
			authCookieFactory.createExpiredShopLoginCookie(ShopSessionPolicy.COOKIE_CUST_GRADE_CD).toString()
		};
	}
}
