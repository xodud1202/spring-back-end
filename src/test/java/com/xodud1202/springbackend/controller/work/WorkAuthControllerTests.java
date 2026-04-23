package com.xodud1202.springbackend.controller.work;

import com.xodud1202.springbackend.common.exception.GlobalRestExceptionHandler;
import com.xodud1202.springbackend.config.properties.JwtProperties;
import com.xodud1202.springbackend.entity.UserBaseEntity;
import com.xodud1202.springbackend.security.AuthCookieFactory;
import com.xodud1202.springbackend.security.SignedLoginTokenService;
import com.xodud1202.springbackend.service.UserBaseService;
import jakarta.servlet.http.Cookie;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
// 업무관리 인증 컨트롤러의 세션 복구 동작을 검증합니다.
class WorkAuthControllerTests {
	private static final String JWT_SECRET = "test-secret-key-test-secret-key-test-secret-key";

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private UserBaseService userBaseService;

	private MockMvc mockMvc;
	private SignedLoginTokenService signedLoginTokenService;

	@BeforeEach
	// 업무관리 인증 컨트롤러 단위 검증용 MockMvc를 초기화합니다.
	void setUp() {
		// 쿠키 정책과 예외 처리기를 포함한 standalone MockMvc를 생성합니다.
		JwtProperties jwtProperties = new JwtProperties(JWT_SECRET, Duration.ofMinutes(30), Duration.ofDays(30), false);
		AuthCookieFactory authCookieFactory = new AuthCookieFactory(jwtProperties);
		signedLoginTokenService = new SignedLoginTokenService(jwtProperties);
		WorkAuthController controller = new WorkAuthController(authenticationManager, userBaseService, authCookieFactory, signedLoginTokenService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
			.setControllerAdvice(new GlobalRestExceptionHandler())
			.build();
	}

	@Test
	@DisplayName("업무관리 로그인은 계정이 없어도 공통 인증 실패 메시지를 반환한다")
	// 계정 존재 여부가 외부 응답으로 구분되지 않는지 검증합니다.
	void login_returnsCommonFailureWhenUserMissing() throws Exception {
		// 로그인 아이디 조회 결과가 없도록 목을 구성합니다.
		when(userBaseService.loadUserByLoginId("missing")).thenReturn(Optional.empty());

		// 로그인 API를 호출해 401 공통 메시지를 확인합니다.
		mockMvc.perform(post("/api/work/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"missing\",\"pwd\":\"wrong\",\"rememberMe\":false}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 일치하지 않습니다."));
	}

	@Test
	@DisplayName("업무관리 로그인은 비밀번호 불일치도 공통 인증 실패 메시지를 반환한다")
	// 비밀번호 불일치와 계정 미존재 응답이 같아 사용자 열거가 되지 않는지 검증합니다.
	void login_returnsCommonFailureWhenPasswordMismatch() throws Exception {
		// 로그인 아이디는 존재하지만 인증 매니저가 비밀번호 불일치를 반환하도록 구성합니다.
		UserBaseEntity user = new UserBaseEntity();
		user.setUsrNo(11L);
		user.setLoginId("tester");
		user.setUserNm("테스터");
		when(userBaseService.loadUserByLoginId("tester")).thenReturn(Optional.of(user));
		when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad credentials"));

		// 로그인 API를 호출해 401 공통 메시지를 확인합니다.
		mockMvc.perform(post("/api/work/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"tester\",\"pwd\":\"wrong\",\"rememberMe\":false}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 일치하지 않습니다."));
	}

	@Test
	@DisplayName("업무관리 세션 복구 API는 세션이 없어도 유효한 로그인 쿠키로 복구한다")
	// 브라우저 재시작 뒤 세션이 없어도 로그인 쿠키가 남아 있으면 인증을 복구하는지 검증합니다.
	void refreshWorkSession_restoresAuthenticationFromCookie() throws Exception {
		// 로그인 쿠키 사용자번호에 해당하는 사용자 엔티티를 목으로 구성합니다.
		UserBaseEntity user = new UserBaseEntity();
		user.setUsrNo(11L);
		user.setLoginId("tester");
		user.setUserNm("테스터");
		when(userBaseService.getUserEntityByUsrNo(11L)).thenReturn(Optional.of(user));

		// 세션 복구 API를 호출합니다.
		mockMvc.perform(
				post("/api/work/auth/session/refresh")
					.cookie(new Cookie("work_user_no", signedLoginTokenService.generateWorkAuthToken(11L)))
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.authenticated").value(true))
			.andExpect(jsonPath("$.workUserNo").value(11))
			.andExpect(jsonPath("$.loginId").value("tester"))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("work_user_no=")));

		// 사용자 엔티티 조회가 1회 수행되는지 확인합니다.
		verify(userBaseService, times(1)).getUserEntityByUsrNo(11L);
	}

	@Test
	@DisplayName("업무관리 세션 복구 API는 raw 숫자 쿠키면 비로그인과 만료 쿠키를 반환한다")
	// 서명되지 않은 raw 숫자 쿠키는 더 이상 인증 증빙으로 신뢰하지 않는지 검증합니다.
	void refreshWorkSession_returnsUnauthenticatedWhenCookieTokenInvalid() throws Exception {
		// 세션 복구 API를 호출합니다.
		mockMvc.perform(
				post("/api/work/auth/session/refresh")
					.cookie(new Cookie("work_user_no", "15"))
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.authenticated").value(false))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.allOf(
				Matchers.containsString("work_user_no="),
				Matchers.containsString("Max-Age=0")
			)));
	}

	@Test
	@DisplayName("업무관리 세션 복구 API는 비활성 사용자의 서명 토큰이면 비로그인과 만료 쿠키를 반환한다")
	// 서명 토큰이 유효해도 현재 활성 사용자가 아니면 세션을 복구하지 않는지 검증합니다.
	void refreshWorkSession_returnsUnauthenticatedWhenCookieUserMissing() throws Exception {
		// 로그인 쿠키 사용자번호 조회 결과가 없도록 목을 구성합니다.
		when(userBaseService.getUserEntityByUsrNo(15L)).thenReturn(Optional.empty());

		// 세션 복구 API를 호출합니다.
		mockMvc.perform(
				post("/api/work/auth/session/refresh")
					.cookie(new Cookie("work_user_no", signedLoginTokenService.generateWorkAuthToken(15L)))
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.authenticated").value(false))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.allOf(
				Matchers.containsString("work_user_no="),
				Matchers.containsString("Max-Age=0")
			)));
	}
}
