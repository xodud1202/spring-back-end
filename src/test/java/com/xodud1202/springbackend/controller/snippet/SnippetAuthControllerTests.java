package com.xodud1202.springbackend.controller.snippet;

import com.xodud1202.springbackend.common.exception.GlobalRestExceptionHandler;
import com.xodud1202.springbackend.config.properties.JwtProperties;
import com.xodud1202.springbackend.security.SignedLoginTokenService;
import com.xodud1202.springbackend.domain.snippet.SnippetUserSessionVO;
import com.xodud1202.springbackend.security.AuthCookieFactory;
import com.xodud1202.springbackend.service.SnippetAuthService;
import jakarta.servlet.http.Cookie;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
// 스니펫 인증 컨트롤러의 세션 복구 동작을 검증합니다.
class SnippetAuthControllerTests {
	private static final String JWT_SECRET = "test-secret-key-test-secret-key-test-secret-key";

	@Mock
	private SnippetAuthService snippetAuthService;

	private MockMvc mockMvc;
	private SignedLoginTokenService signedLoginTokenService;

	@BeforeEach
	// 스니펫 인증 컨트롤러 단위 검증용 MockMvc를 초기화합니다.
	void setUp() {
		// 쿠키 정책과 예외 처리기를 포함한 standalone MockMvc를 생성합니다.
		JwtProperties jwtProperties = new JwtProperties(JWT_SECRET, Duration.ofMinutes(30), Duration.ofDays(30), false);
		AuthCookieFactory authCookieFactory = new AuthCookieFactory(jwtProperties);
		signedLoginTokenService = new SignedLoginTokenService(jwtProperties);
		SnippetAuthController controller = new SnippetAuthController(snippetAuthService, authCookieFactory, signedLoginTokenService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
			.setControllerAdvice(new GlobalRestExceptionHandler())
			.build();
	}

	@Test
	@DisplayName("스니펫 세션 복구 API는 세션이 없어도 유효한 로그인 쿠키로 복구한다")
	// 브라우저 재시작 뒤 세션이 없어도 로그인 쿠키가 남아 있으면 인증을 복구하는지 검증합니다.
	void refreshSnippetSession_restoresAuthenticationFromCookie() throws Exception {
		// 로그인 쿠키 사용자번호에 해당하는 활성 사용자 정보를 목으로 구성합니다.
		when(snippetAuthService.findActiveSnippetUser(7L)).thenReturn(
			new SnippetUserSessionVO(7L, "google-sub-7", "tester@example.com", "테스터", "https://example.com/profile.png", "Y", "N")
		);

		// 세션 복구 API를 호출합니다.
		mockMvc.perform(
				post("/api/snippet/auth/session/refresh")
					.cookie(new Cookie("snippet_user_no", signedLoginTokenService.generateSnippetAuthToken(7L)))
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.authenticated").value(true))
			.andExpect(jsonPath("$.snippetUserNo").value(7))
			.andExpect(jsonPath("$.userNm").value("테스터"))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("snippet_user_no=")));

		// 활성 사용자 조회가 1회 수행되는지 확인합니다.
		verify(snippetAuthService, times(1)).findActiveSnippetUser(7L);
	}

	@Test
	@DisplayName("스니펫 세션 복구 API는 raw 숫자 쿠키면 비로그인과 만료 쿠키를 반환한다")
	// 서명되지 않은 raw 숫자 쿠키는 더 이상 인증 증빙으로 신뢰하지 않는지 검증합니다.
	void refreshSnippetSession_returnsUnauthenticatedWhenCookieTokenInvalid() throws Exception {
		// 세션 복구 API를 호출합니다.
		mockMvc.perform(
				post("/api/snippet/auth/session/refresh")
					.cookie(new Cookie("snippet_user_no", "9"))
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.authenticated").value(false))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.allOf(
				Matchers.containsString("snippet_user_no="),
				Matchers.containsString("Max-Age=0")
			)));
	}

	@Test
	@DisplayName("스니펫 세션 복구 API는 비활성 사용자의 서명 토큰이면 비로그인과 만료 쿠키를 반환한다")
	// 서명 토큰이 유효해도 현재 활성 사용자가 아니면 세션을 복구하지 않는지 검증합니다.
	void refreshSnippetSession_returnsUnauthenticatedWhenCookieUserMissing() throws Exception {
		// 로그인 쿠키 사용자번호 조회 결과가 없도록 목을 구성합니다.
		when(snippetAuthService.findActiveSnippetUser(9L)).thenReturn(null);

		// 세션 복구 API를 호출합니다.
		mockMvc.perform(
				post("/api/snippet/auth/session/refresh")
					.cookie(new Cookie("snippet_user_no", signedLoginTokenService.generateSnippetAuthToken(9L)))
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.authenticated").value(false))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.allOf(
				Matchers.containsString("snippet_user_no="),
				Matchers.containsString("Max-Age=0")
			)));
	}
}
