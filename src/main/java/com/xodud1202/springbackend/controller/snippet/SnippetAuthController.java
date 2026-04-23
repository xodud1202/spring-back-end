package com.xodud1202.springbackend.controller.snippet;

import com.xodud1202.springbackend.common.response.ApiMessageResponse;
import com.xodud1202.springbackend.common.snippet.SnippetSessionPolicy;
import com.xodud1202.springbackend.domain.snippet.SnippetGoogleLoginRequest;
import com.xodud1202.springbackend.domain.snippet.SnippetGoogleLoginResponse;
import com.xodud1202.springbackend.domain.snippet.SnippetSessionRefreshResponse;
import com.xodud1202.springbackend.domain.snippet.SnippetUserSessionVO;
import com.xodud1202.springbackend.security.AuthCookieFactory;
import com.xodud1202.springbackend.security.SignedLoginTokenService;
import com.xodud1202.springbackend.service.SnippetAuthService;
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

@Slf4j
@RestController
@RequiredArgsConstructor
// 스니펫 구글 로그인과 세션 복구 API를 제공합니다.
public class SnippetAuthController {
	private final SnippetAuthService snippetAuthService;
	private final AuthCookieFactory authCookieFactory;
	private final SignedLoginTokenService signedLoginTokenService;

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
	// 스니펫 현재 세션 기준 로그인 상태를 확인하고 유지시간을 연장합니다.
	public ResponseEntity<SnippetSessionRefreshResponse> refreshSnippetSession(HttpServletRequest request) {
		try {
			// 세션 우선, 없으면 서명된 로그인 쿠키 기준으로 스니펫 사용자번호를 복구합니다.
			HttpSession session = request.getSession(false);
			Long snippetUserNo = session == null
				? null
				: SnippetSessionPolicy.resolveSnippetUserNo(session.getAttribute(SnippetSessionPolicy.SESSION_ATTR_SNIPPET_USER_NO));
			if (snippetUserNo == null) {
				snippetUserNo = SnippetSessionPolicy.resolveSnippetUserNoFromRequest(request, signedLoginTokenService);
			}
			if (snippetUserNo == null) {
				return buildUnauthenticatedSnippetSessionResponse();
			}

			// 세션 사용자번호로 실제 활성 사용자를 조회합니다.
			SnippetUserSessionVO snippetUser = snippetAuthService.findActiveSnippetUser(snippetUserNo);
			if (snippetUser == null || snippetUser.snippetUserNo() == null) {
				clearSnippetSession(request);
				return buildUnauthenticatedSnippetSessionResponse();
			}

			// 세션이 없었다면 새 세션을 복구하고 서명 로그인 쿠키 만료시간도 함께 갱신합니다.
			HttpSession authenticatedSession = session == null ? request.getSession(true) : session;
			SnippetSessionPolicy.applyAuthenticatedSession(authenticatedSession, snippetUser.snippetUserNo());
			return ResponseEntity.ok()
				.header(
					HttpHeaders.SET_COOKIE,
					authCookieFactory.createSnippetLoginCookie(
						SnippetSessionPolicy.COOKIE_SNIPPET_USER_NO,
						signedLoginTokenService.generateSnippetAuthToken(snippetUser.snippetUserNo())
					).toString()
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
			// 스니펫 세션 속성만 제거하고 로그인 쿠키를 제거합니다.
			clearSnippetSession(request);
			return ResponseEntity.ok()
				.header(
					HttpHeaders.SET_COOKIE,
					authCookieFactory.createExpiredSnippetLoginCookie(SnippetSessionPolicy.COOKIE_SNIPPET_USER_NO).toString()
				)
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
		// 세션에 로그인 사용자번호를 기록하고 5시간 만료를 설정합니다.
		HttpSession session = httpRequest.getSession(true);
		SnippetSessionPolicy.applyAuthenticatedSession(session, response.snippetUserNo());

		// 로그인 쿠키도 서명 토큰으로 함께 발급합니다.
		return ResponseEntity.ok()
			.header(
				HttpHeaders.SET_COOKIE,
				authCookieFactory.createSnippetLoginCookie(
					SnippetSessionPolicy.COOKIE_SNIPPET_USER_NO,
					signedLoginTokenService.generateSnippetAuthToken(response.snippetUserNo())
				).toString()
			)
			.body(response);
	}

	// 비로그인 스니펫 세션 응답과 만료 쿠키를 생성합니다.
	private ResponseEntity<SnippetSessionRefreshResponse> buildUnauthenticatedSnippetSessionResponse() {
		return ResponseEntity.ok()
			.header(
				HttpHeaders.SET_COOKIE,
				authCookieFactory.createExpiredSnippetLoginCookie(SnippetSessionPolicy.COOKIE_SNIPPET_USER_NO).toString()
			)
			.body(SnippetSessionRefreshResponse.unauthenticated());
	}

	// 스니펫 세션 속성만 제거합니다.
	private void clearSnippetSession(HttpServletRequest request) {
		SnippetSessionPolicy.clearAuthenticatedSession(request == null ? null : request.getSession(false));
	}
}
