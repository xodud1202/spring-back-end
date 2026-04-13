package com.xodud1202.springbackend.controller.snippet;

import com.xodud1202.springbackend.common.response.ApiMessageResponse;
import com.xodud1202.springbackend.domain.snippet.SnippetGoogleLoginRequest;
import com.xodud1202.springbackend.domain.snippet.SnippetGoogleLoginResponse;
import com.xodud1202.springbackend.domain.snippet.SnippetSessionRefreshResponse;
import com.xodud1202.springbackend.domain.snippet.SnippetUserSessionVO;
import com.xodud1202.springbackend.security.AuthCookieFactory;
import com.xodud1202.springbackend.service.SnippetAuthService;
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

import java.util.Arrays;

@Slf4j
@RestController
@RequiredArgsConstructor
// 스니펫 구글 로그인과 세션 복구 API를 제공합니다.
public class SnippetAuthController {
	private static final String COOKIE_SNIPPET_USER_NO = "snippet_user_no";
	private static final String SESSION_ATTR_SNIPPET_USER_NO = "snippetUserNo";
	private static final int SESSION_TIMEOUT_SECONDS = 60 * 60;

	private final SnippetAuthService snippetAuthService;
	private final AuthCookieFactory authCookieFactory;

	@PostMapping("/api/snippet/auth/google/login")
	// 구글 credential을 검증하고 스니펫 로그인 세션을 발급합니다.
	public ResponseEntity<SnippetGoogleLoginResponse> loginWithGoogle(
		@Valid @RequestBody SnippetGoogleLoginRequest request,
		HttpServletRequest httpRequest
	) {
		try {
			// 구글 로그인 처리 후 세션/쿠키를 함께 발급합니다.
			SnippetGoogleLoginResponse response = snippetAuthService.loginWithGoogle(request);
			return createLoginSuccessResponse(response, httpRequest);
		} catch (IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("스니펫 구글 로그인 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("구글 로그인 처리에 실패했습니다.", exception);
		}
	}

	@PostMapping("/api/snippet/auth/session/refresh")
	// 스니펫 사용자번호 쿠키를 기준으로 세션을 복구합니다.
	public ResponseEntity<SnippetSessionRefreshResponse> refreshSnippetSession(HttpServletRequest request) {
		try {
			// 사용자번호 쿠키가 없으면 비로그인 응답을 반환합니다.
			String snippetUserNoValue = findCookieValue(request, COOKIE_SNIPPET_USER_NO);
			if (isBlank(snippetUserNoValue)) {
				return ResponseEntity.ok(SnippetSessionRefreshResponse.unauthenticated());
			}

			// 쿠키 사용자번호로 실제 활성 사용자를 조회합니다.
			Long snippetUserNo = parseSnippetUserNo(snippetUserNoValue);
			SnippetUserSessionVO snippetUser = snippetAuthService.findActiveSnippetUser(snippetUserNo);
			if (snippetUser == null || snippetUser.snippetUserNo() == null) {
				invalidateSnippetSession(request);
				return ResponseEntity.ok()
					.header(HttpHeaders.SET_COOKIE, authCookieFactory.createExpiredSnippetLoginCookie(COOKIE_SNIPPET_USER_NO).toString())
					.body(SnippetSessionRefreshResponse.unauthenticated());
			}

			// 세션과 로그인 쿠키 만료시간을 함께 갱신합니다.
			HttpSession session = request.getSession(true);
			session.setAttribute(SESSION_ATTR_SNIPPET_USER_NO, snippetUser.snippetUserNo());
			session.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);
			return ResponseEntity.ok()
				.header(
					HttpHeaders.SET_COOKIE,
					authCookieFactory.createSnippetLoginCookie(COOKIE_SNIPPET_USER_NO, String.valueOf(snippetUser.snippetUserNo())).toString()
				)
				.body(
					SnippetSessionRefreshResponse.authenticated(
						snippetUser.snippetUserNo(),
						snippetUser.userNm(),
						snippetUser.email(),
						snippetUser.profileImgUrl()
					)
				);
		} catch (IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("스니펫 세션 복구 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("스니펫 세션 복구에 실패했습니다.", exception);
		}
	}

	@PostMapping("/api/snippet/auth/logout")
	// 스니펫 로그아웃 시 세션과 로그인 쿠키를 모두 만료 처리합니다.
	public ResponseEntity<ApiMessageResponse> logoutSnippet(HttpServletRequest request) {
		try {
			// 스니펫 세션을 무효화하고 로그인 쿠키를 제거합니다.
			invalidateSnippetSession(request);
			return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, authCookieFactory.createExpiredSnippetLoginCookie(COOKIE_SNIPPET_USER_NO).toString())
				.body(new ApiMessageResponse("로그아웃 처리되었습니다."));
		} catch (Exception exception) {
			log.error("스니펫 로그아웃 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("로그아웃 처리에 실패했습니다.", exception);
		}
	}

	// 로그인 성공 응답과 함께 세션/쿠키를 발급합니다.
	private ResponseEntity<SnippetGoogleLoginResponse> createLoginSuccessResponse(
		SnippetGoogleLoginResponse response,
		HttpServletRequest httpRequest
	) {
		// 세션에 로그인 사용자번호를 기록하고 1시간 만료를 설정합니다.
		HttpSession session = httpRequest.getSession(true);
		session.setAttribute(SESSION_ATTR_SNIPPET_USER_NO, response.snippetUserNo());
		session.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);

		// 로그인 쿠키도 함께 발급합니다.
		return ResponseEntity.ok()
			.header(
				HttpHeaders.SET_COOKIE,
				authCookieFactory.createSnippetLoginCookie(COOKIE_SNIPPET_USER_NO, String.valueOf(response.snippetUserNo())).toString()
			)
			.body(response);
	}

	// 스니펫 세션을 무효화합니다.
	private void invalidateSnippetSession(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return;
		}
		session.removeAttribute(SESSION_ATTR_SNIPPET_USER_NO);
		session.invalidate();
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

	// 쿠키 문자열을 사용자번호 Long 값으로 변환합니다.
	private Long parseSnippetUserNo(String value) {
		try {
			return Long.valueOf(value);
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException("스니펫 사용자번호를 확인해주세요.");
		}
	}

	// 문자열 공백 여부를 확인합니다.
	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
