package com.xodud1202.springbackend.common.shop;

import com.xodud1202.springbackend.common.web.HttpCookieUtils;
import com.xodud1202.springbackend.security.SignedLoginTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.time.Duration;

// 쇼핑몰 로그인 세션 정책 상수를 중앙화합니다.
public final class ShopSessionPolicy {
	public static final String COOKIE_SHOP_AUTH = "shop_auth";
	public static final String COOKIE_CUST_NO = "cust_no";
	public static final String COOKIE_CUST_NM = "cust_nm";
	public static final String COOKIE_CUST_GRADE_CD = "cust_grade_cd";
	public static final String SESSION_ATTR_CUST_NO = "shopCustNo";
	public static final int SESSION_TIMEOUT_SECONDS = 60 * 60;
	public static final Duration SESSION_COOKIE_MAX_AGE = Duration.ofHours(1);

	// 유틸리티 클래스 인스턴스 생성을 방지합니다.
	private ShopSessionPolicy() {
		throw new UnsupportedOperationException("쇼핑몰 세션 정책 클래스는 인스턴스화할 수 없습니다.");
	}

	// 세션 속성 값을 쇼핑몰 고객번호 Long 값으로 변환합니다.
	public static Long resolveShopCustNo(Object sessionValue) {
		// 저장 타입이 달라도 현재 로그인 고객번호를 일관되게 읽을 수 있도록 처리합니다.
		if (sessionValue instanceof Long custNo && custNo > 0L) {
			return custNo;
		}
		if (sessionValue instanceof Integer custNo && custNo > 0) {
			return custNo.longValue();
		}
		if (sessionValue instanceof String custNoText) {
			try {
				Long custNo = Long.valueOf(custNoText);
				if (custNo > 0L) {
					return custNo;
				}
			} catch (NumberFormatException ignored) {
				// 문자열 파싱 실패는 null 반환으로 처리합니다.
			}
		}
		return null;
	}

	// 요청 쿠키의 서명 토큰에서 쇼핑몰 고객번호를 복구합니다.
	public static Long resolveShopCustNoFromRequest(HttpServletRequest request, SignedLoginTokenService signedLoginTokenService) {
		// 신규 shop_auth 쿠키만 신뢰하고 레거시 raw 고객번호 쿠키는 더 이상 사용하지 않습니다.
		if (signedLoginTokenService == null) {
			return null;
		}
		return signedLoginTokenService.parseShopCustNo(HttpCookieUtils.findCookieValue(request, COOKIE_SHOP_AUTH));
	}

	// 쇼핑몰 인증 세션에 고객번호를 저장하고 만료시간을 갱신합니다.
	public static void applyAuthenticatedSession(HttpSession session, Long custNo) {
		// 세션 또는 고객번호가 없으면 아무 작업도 하지 않습니다.
		if (session == null || custNo == null || custNo < 1L) {
			return;
		}
		session.setAttribute(SESSION_ATTR_CUST_NO, custNo);
		refreshSessionTimeout(session);
	}

	// 쇼핑몰 인증 세션에서 고객번호를 제거합니다.
	public static void clearAuthenticatedSession(HttpSession session) {
		// 세션이 없으면 제거할 값도 없습니다.
		if (session == null) {
			return;
		}
		session.removeAttribute(SESSION_ATTR_CUST_NO);
	}

	// 쇼핑몰 세션의 비활성 타임아웃을 정책 시간으로 갱신합니다.
	public static void refreshSessionTimeout(HttpSession session) {
		// 로그인 유지시간 1시간 기준으로 세션 만료시간을 다시 맞춥니다.
		if (session != null) {
			session.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);
		}
	}
}
