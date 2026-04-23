package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.common.shop.ShopSessionPolicy;
import com.xodud1202.springbackend.common.util.CommonTextUtils;
import com.xodud1202.springbackend.config.properties.ShopProperties;
import com.xodud1202.springbackend.domain.shop.auth.ShopCustomerSessionVO;
import com.xodud1202.springbackend.security.SignedLoginTokenService;
import com.xodud1202.springbackend.service.ShopAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static com.xodud1202.springbackend.common.Constants.Shop.DEVICE_GB_MO;
import static com.xodud1202.springbackend.common.Constants.Shop.DEVICE_GB_PC;

// 쇼핑몰 컨트롤러 공통 요청 해석 기능을 제공합니다.
abstract class ShopControllerSupport {
	@Autowired
	private ShopAuthService shopAuthService;

	@Autowired
	private SignedLoginTokenService signedLoginTokenService;

	@Autowired
	private ShopProperties shopProperties;

	// 현재 요청 세션에서 로그인된 고객번호를 조회합니다.
	protected Long resolveAuthenticatedCustNo(HttpServletRequest request) {
		// 세션 고객번호가 있으면 가장 먼저 사용합니다.
		HttpSession session = request == null ? null : request.getSession(false);
		Long sessionCustNo = session == null ? null : ShopSessionPolicy.resolveShopCustNo(session.getAttribute(ShopSessionPolicy.SESSION_ATTR_CUST_NO));
		if (sessionCustNo != null) {
			ShopCustomerSessionVO customer = shopAuthService.getShopCustomerByCustNo(sessionCustNo);
			if (customer != null && customer.custNo() != null) {
				ShopSessionPolicy.applyAuthenticatedSession(session, customer.custNo());
				return customer.custNo();
			}
			ShopSessionPolicy.clearAuthenticatedSession(session);
		}

		// 세션이 없으면 서명된 shop_auth 쿠키로 고객번호 복구를 시도하고 활성 고객인지 재확인합니다.
		Long cookieCustNo = ShopSessionPolicy.resolveShopCustNoFromRequest(request, signedLoginTokenService);
		if (cookieCustNo == null) {
			return null;
		}

		ShopCustomerSessionVO customer = shopAuthService.getShopCustomerByCustNo(cookieCustNo);
		if (customer == null || customer.custNo() == null) {
			return null;
		}

		// 보호 API 첫 진입에서도 이후 요청이 세션을 재사용할 수 있도록 세션을 복구합니다.
		ShopSessionPolicy.applyAuthenticatedSession(request.getSession(true), customer.custNo());
		return customer.custNo();
	}

	// 로그인된 고객번호를 필수로 요구하고 미로그인 시 예외를 발생시킵니다.
	protected Long requireAuthenticatedCustNo(HttpServletRequest request) {
		Long custNo = resolveAuthenticatedCustNo(request);
		if (custNo == null) {
			throw new SecurityException("로그인이 필요합니다.");
		}
		return custNo;
	}

	// 미인증 요청에 대한 공통 401 응답을 반환합니다.
	protected ResponseEntity<Object> unauthorizedResponse() {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
	}

	// 숫자 또는 문자열 값을 정수로 변환합니다.
	protected Integer parseIntegerValue(Object rawValue) {
		// 값이 없으면 null을 반환합니다.
		switch (rawValue) {
			case null -> {
				return null;
			}

			// 숫자 타입은 int 값으로 변환합니다.
			case Number numberValue -> {
				return numberValue.intValue();
			}

			// 문자열 타입은 공백 제거 후 정수 변환을 시도합니다.
			case String stringValue -> {
				String normalizedValue = stringValue.trim();
				if (normalizedValue.isEmpty()) {
					return null;
				}
				try {
					return Integer.valueOf(normalizedValue);
				} catch (NumberFormatException exception) {
					return null;
				}
			}
			default -> {
			}
		}
		return null;
	}

	// 요청 본문 맵에서 필수 문자열 값을 trim 처리해 반환합니다.
	protected String requireRequestBodyTextValue(Map<String, Object> requestBody, String fieldName, String message) {
		Object rawValue = requestBody == null ? null : requestBody.get(fieldName);
		String normalizedValue = rawValue instanceof String ? ((String) rawValue).trim() : "";
		if (normalizedValue.isEmpty()) {
			throw new IllegalArgumentException(message);
		}
		return normalizedValue;
	}

	// 요청 본문 값에서 1 이상의 필수 정수 값을 반환합니다.
	protected Integer requirePositiveIntegerValue(Object rawValue, String message) {
		Integer parsedValue = parseIntegerValue(rawValue);
		if (parsedValue == null || parsedValue < 1) {
			throw new IllegalArgumentException(message);
		}
		return parsedValue;
	}

	// 선택 기획전 번호를 양수 정수 또는 null로 정규화합니다.
	protected Integer normalizeOptionalExhibitionNo(Object rawValue) {
		// 숫자/문자열 값을 정수로 바꾼 뒤 1 이상만 허용합니다.
		Integer parsedValue = parseIntegerValue(rawValue);
		return parsedValue == null || parsedValue < 1 ? null : parsedValue;
	}

	// 요청 헤더를 기준으로 결제 디바이스 코드를 반환합니다.
	protected String resolveDeviceGbCd(HttpServletRequest request) {
		// 모바일 User-Agent면 MO, 그 외는 PC로 판단합니다.
		String userAgent = request == null ? null : request.getHeader("User-Agent");
		if (userAgent == null) {
			return DEVICE_GB_PC;
		}
		String normalizedUserAgent = userAgent.toLowerCase();
		if (normalizedUserAgent.contains("android")
			|| normalizedUserAgent.contains("iphone")
			|| normalizedUserAgent.contains("ipad")
			|| normalizedUserAgent.contains("mobile")) {
			return DEVICE_GB_MO;
		}
		return DEVICE_GB_PC;
	}

	// 설정된 쇼핑몰 프론트 절대 Origin 값을 반환합니다.
	protected String resolveShopOrigin(HttpServletRequest request) {
		// 결제 리다이렉트 호스트는 요청 헤더를 신뢰하지 않고 환경별 설정값만 사용합니다.
		String configuredOrigin = trimToNull(shopProperties == null ? null : shopProperties.frontBaseUrl());
		return configuredOrigin == null ? "" : configuredOrigin;
	}

	// 문자열을 trim 처리하고 비어 있으면 null을 반환합니다.
	protected String trimToNull(String value) {
		return CommonTextUtils.trimToNull(value);
	}
}
