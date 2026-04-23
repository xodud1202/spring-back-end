package com.xodud1202.springbackend.controller.work;

import com.xodud1202.springbackend.common.response.ApiMessageResponse;
import com.xodud1202.springbackend.common.work.WorkSessionPolicy;
import com.xodud1202.springbackend.domain.LoginRequest;
import com.xodud1202.springbackend.domain.work.WorkSessionRefreshResponse;
import com.xodud1202.springbackend.entity.UserBaseEntity;
import com.xodud1202.springbackend.security.AuthCookieFactory;
import com.xodud1202.springbackend.security.SignedLoginTokenService;
import com.xodud1202.springbackend.service.UserBaseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// 업무관리 로그인과 세션 복구 API를 제공합니다.
@Slf4j
@RestController
@RequiredArgsConstructor
public class WorkAuthController {
	private static final String AUTH_FAILED_MESSAGE = "아이디 또는 비밀번호가 일치하지 않습니다.";

	private final AuthenticationManager authenticationManager;
	private final UserBaseService userBaseService;
	private final AuthCookieFactory authCookieFactory;
	private final SignedLoginTokenService signedLoginTokenService;

	@PostMapping("/api/work/auth/login")
	// 로그인 아이디와 비밀번호를 검증하고 업무관리 로그인 세션을 발급합니다.
	public ResponseEntity<WorkSessionRefreshResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
		try {
			// 로그인 아이디 존재 여부를 먼저 확인합니다.
			UserBaseEntity user = userBaseService.loadUserByLoginId(request.loginId())
				.orElseThrow(() -> {
					log.warn("업무관리 로그인 실패 reason=user_not_found");
					return new SecurityException(AUTH_FAILED_MESSAGE);
				});

			// 비밀번호를 검증해 인증에 성공하면 세션과 쿠키를 함께 발급합니다.
			authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.loginId(), request.pwd())
			);

			WorkSessionRefreshResponse response = WorkSessionRefreshResponse.authenticated(user.getUsrNo(), user.getLoginId(), user.getUserNm());
			return createLoginSuccessResponse(response, httpRequest);
		} catch (BadCredentialsException exception) {
			log.warn("업무관리 로그인 실패 reason=bad_credentials");
			throw new SecurityException(AUTH_FAILED_MESSAGE);
		} catch (SecurityException exception) {
			throw exception;
		} catch (IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("업무관리 로그인 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("업무관리 로그인 처리에 실패했습니다.", exception);
		}
	}

	@PostMapping("/api/work/auth/session/refresh")
	// 업무관리 현재 세션 기준 로그인 상태를 확인하고 유지시간을 연장합니다.
	public ResponseEntity<WorkSessionRefreshResponse> refreshWorkSession(HttpServletRequest request) {
		try {
			// 세션 우선, 없으면 서명된 로그인 쿠키 기준으로 업무관리 사용자번호를 복구합니다.
			HttpSession session = request.getSession(false);
			Long workUserNo = session == null ? null : WorkSessionPolicy.resolveWorkUserNo(session.getAttribute(WorkSessionPolicy.SESSION_ATTR_WORK_USER_NO));
			if (workUserNo == null) {
				workUserNo = WorkSessionPolicy.resolveWorkUserNoFromRequest(request, signedLoginTokenService);
			}
			if (workUserNo == null) {
				return buildUnauthenticatedWorkSessionResponse();
			}

			// 세션 사용자번호로 실제 사용자를 조회합니다.
			UserBaseEntity user = userBaseService.getUserEntityByUsrNo(workUserNo).orElse(null);
			if (user == null || user.getUsrNo() == null) {
				clearWorkSession(request);
				return buildUnauthenticatedWorkSessionResponse();
			}

			// 세션이 없었다면 새 세션을 복구하고 서명 로그인 쿠키 만료시간도 함께 갱신합니다.
			HttpSession authenticatedSession = session == null ? request.getSession(true) : session;
			WorkSessionPolicy.applyAuthenticatedSession(authenticatedSession, user.getUsrNo());
			return ResponseEntity.ok()
				.header(
					HttpHeaders.SET_COOKIE,
					authCookieFactory.createWorkLoginCookie(
						WorkSessionPolicy.COOKIE_WORK_USER_NO,
						signedLoginTokenService.generateWorkAuthToken(user.getUsrNo())
					).toString()
				)
				.body(WorkSessionRefreshResponse.authenticated(user.getUsrNo(), user.getLoginId(), user.getUserNm()));
		} catch (IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("업무관리 세션 복구 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("업무관리 세션 복구에 실패했습니다.", exception);
		}
	}

	@PostMapping("/api/work/auth/logout")
	// 업무관리 로그아웃 시 세션과 로그인 쿠키를 모두 만료 처리합니다.
	public ResponseEntity<ApiMessageResponse> logoutWork(HttpServletRequest request) {
		try {
			// 업무관리 세션 속성만 제거하고 로그인 쿠키를 제거합니다.
			clearWorkSession(request);
			return ResponseEntity.ok()
				.header(
					HttpHeaders.SET_COOKIE,
					authCookieFactory.createExpiredWorkLoginCookie(WorkSessionPolicy.COOKIE_WORK_USER_NO).toString()
				)
				.body(new ApiMessageResponse("로그아웃 처리되었습니다."));
		} catch (Exception exception) {
			log.error("업무관리 로그아웃 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("로그아웃 처리에 실패했습니다.", exception);
		}
	}

	// 로그인 성공 응답과 함께 세션과 쿠키를 발급합니다.
	private ResponseEntity<WorkSessionRefreshResponse> createLoginSuccessResponse(WorkSessionRefreshResponse response, HttpServletRequest httpRequest) {
		// 세션에 로그인 사용자번호를 기록하고 5시간 만료를 설정합니다.
		HttpSession session = httpRequest.getSession(true);
		WorkSessionPolicy.applyAuthenticatedSession(session, response.workUserNo());

		// 로그인 쿠키도 서명 토큰으로 함께 발급합니다.
		return ResponseEntity.ok()
			.header(
				HttpHeaders.SET_COOKIE,
				authCookieFactory.createWorkLoginCookie(
					WorkSessionPolicy.COOKIE_WORK_USER_NO,
					signedLoginTokenService.generateWorkAuthToken(response.workUserNo())
				).toString()
			)
			.body(response);
	}

	// 비로그인 업무관리 세션 응답과 만료 쿠키를 생성합니다.
	private ResponseEntity<WorkSessionRefreshResponse> buildUnauthenticatedWorkSessionResponse() {
		return ResponseEntity.ok()
			.header(
				HttpHeaders.SET_COOKIE,
				authCookieFactory.createExpiredWorkLoginCookie(WorkSessionPolicy.COOKIE_WORK_USER_NO).toString()
			)
			.body(WorkSessionRefreshResponse.unauthenticated());
	}

	// 업무관리 세션 속성만 제거합니다.
	private void clearWorkSession(HttpServletRequest request) {
		WorkSessionPolicy.clearAuthenticatedSession(request == null ? null : request.getSession(false));
	}
}
