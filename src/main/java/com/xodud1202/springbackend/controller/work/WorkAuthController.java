package com.xodud1202.springbackend.controller.work;

import com.xodud1202.springbackend.common.response.ApiMessageResponse;
import com.xodud1202.springbackend.common.work.WorkSessionPolicy;
import com.xodud1202.springbackend.domain.LoginRequest;
import com.xodud1202.springbackend.domain.work.WorkSessionRefreshResponse;
import com.xodud1202.springbackend.entity.UserBaseEntity;
import com.xodud1202.springbackend.security.AuthCookieFactory;
import com.xodud1202.springbackend.service.UserBaseService;
import jakarta.servlet.http.Cookie;
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

import java.util.Arrays;

// 업무관리 로그인과 세션 복구 API를 제공합니다.
@Slf4j
@RestController
@RequiredArgsConstructor
public class WorkAuthController {
	private final AuthenticationManager authenticationManager;
	private final UserBaseService userBaseService;
	private final AuthCookieFactory authCookieFactory;

	@PostMapping("/api/work/auth/login")
	// 로그인 아이디와 비밀번호를 검증하고 업무관리 로그인 세션을 발급합니다.
	public ResponseEntity<WorkSessionRefreshResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
		try {
			// 로그인 아이디 존재 여부를 먼저 확인합니다.
			UserBaseEntity user = userBaseService.loadUserByLoginId(request.loginId())
				.orElseThrow(() -> new IllegalArgumentException("계정정보가 존재하지 않습니다."));

			// 비밀번호를 검증해 인증에 성공하면 세션과 쿠키를 함께 발급합니다.
			authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.loginId(), request.pwd())
			);

			WorkSessionRefreshResponse response = WorkSessionRefreshResponse.authenticated(user.getUsrNo(), user.getLoginId(), user.getUserNm());
			return createLoginSuccessResponse(response, httpRequest);
		} catch (IllegalArgumentException exception) {
			throw exception;
		} catch (BadCredentialsException exception) {
			throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
		} catch (Exception exception) {
			log.error("업무관리 로그인 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("업무관리 로그인 처리에 실패했습니다.", exception);
		}
	}

	@PostMapping("/api/work/auth/session/refresh")
	// 업무관리 사용자번호 쿠키를 기준으로 세션을 복구합니다.
	public ResponseEntity<WorkSessionRefreshResponse> refreshWorkSession(HttpServletRequest request) {
		try {
			// 사용자번호 쿠키가 없으면 비로그인 응답을 반환합니다.
			String workUserNoValue = findCookieValue(request, WorkSessionPolicy.COOKIE_WORK_USER_NO);
			if (isBlank(workUserNoValue)) {
				return ResponseEntity.ok(WorkSessionRefreshResponse.unauthenticated());
			}

			// 쿠키 사용자번호로 실제 사용자를 조회합니다.
			Long workUserNo = parseWorkUserNo(workUserNoValue);
			UserBaseEntity user = userBaseService.getUserEntityByUsrNo(workUserNo).orElse(null);
			if (user == null || user.getUsrNo() == null) {
				invalidateWorkSession(request);
				return ResponseEntity.ok()
					.header(
						HttpHeaders.SET_COOKIE,
						authCookieFactory.createExpiredWorkLoginCookie(WorkSessionPolicy.COOKIE_WORK_USER_NO).toString()
					)
					.body(WorkSessionRefreshResponse.unauthenticated());
			}

			// 세션과 로그인 쿠키 만료시간을 함께 갱신합니다.
			HttpSession session = request.getSession(true);
			session.setAttribute(WorkSessionPolicy.SESSION_ATTR_WORK_USER_NO, user.getUsrNo());
			WorkSessionPolicy.refreshSessionTimeout(session);
			return ResponseEntity.ok()
				.header(
					HttpHeaders.SET_COOKIE,
					authCookieFactory.createWorkLoginCookie(
						WorkSessionPolicy.COOKIE_WORK_USER_NO,
						String.valueOf(user.getUsrNo())
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
			// 업무관리 세션을 무효화하고 로그인 쿠키를 제거합니다.
			invalidateWorkSession(request);
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
		session.setAttribute(WorkSessionPolicy.SESSION_ATTR_WORK_USER_NO, response.workUserNo());
		WorkSessionPolicy.refreshSessionTimeout(session);

		// 로그인 쿠키도 함께 발급합니다.
		return ResponseEntity.ok()
			.header(
				HttpHeaders.SET_COOKIE,
				authCookieFactory.createWorkLoginCookie(
					WorkSessionPolicy.COOKIE_WORK_USER_NO,
					String.valueOf(response.workUserNo())
				).toString()
			)
			.body(response);
	}

	// 업무관리 세션을 무효화합니다.
	private void invalidateWorkSession(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return;
		}
		session.removeAttribute(WorkSessionPolicy.SESSION_ATTR_WORK_USER_NO);
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
	private Long parseWorkUserNo(String value) {
		try {
			return Long.valueOf(value);
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException("업무관리 사용자번호를 확인해주세요.");
		}
	}

	// 문자열 공백 여부를 확인합니다.
	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
