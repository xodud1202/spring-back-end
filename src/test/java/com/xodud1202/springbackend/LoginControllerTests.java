package com.xodud1202.springbackend;

import com.xodud1202.springbackend.common.exception.GlobalRestExceptionHandler;
import com.xodud1202.springbackend.config.properties.JwtProperties;
import com.xodud1202.springbackend.controller.bo.AdminAuthController;
import com.xodud1202.springbackend.entity.UserBaseEntity;
import com.xodud1202.springbackend.repository.UserRepository;
import com.xodud1202.springbackend.security.AuthCookieFactory;
import com.xodud1202.springbackend.security.JwtTokenProvider;
import com.xodud1202.springbackend.service.UserBaseService;
import com.xodud1202.springbackend.service.UserRefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Duration;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
// 관리자 로그인 컨트롤러의 성공/실패 응답을 단위 테스트합니다.
class LoginControllerTests {
	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private JwtTokenProvider tokenProvider;

	@Mock
	private UserBaseService userBaseService;

	@Mock
	private UserRefreshTokenService userRefreshTokenService;

	@Mock
	private UserRepository userRepository;

	private MockMvc mockMvc;

	@BeforeEach
	// MockMvc standalone 환경을 초기화합니다.
	void setUp() {
		JwtProperties jwtProperties = new JwtProperties(
			"test-secret-key-test-secret-key-test-secret-key",
			Duration.ofMinutes(30),
			Duration.ofDays(30),
			false
		);
		AuthCookieFactory authCookieFactory = new AuthCookieFactory(jwtProperties);
		AdminAuthController adminAuthController = new AdminAuthController(
			authenticationManager,
			tokenProvider,
			userBaseService,
			userRefreshTokenService,
			userRepository,
			jwtProperties,
			authCookieFactory
		);
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders.standaloneSetup(adminAuthController)
			.setControllerAdvice(new GlobalRestExceptionHandler())
			.setValidator(validator)
			.build();
	}

	@Test
	// 로그인 성공 시 accessToken이 반환되는지 확인합니다.
	void loginSuccess() throws Exception {
		UserBaseEntity user = new UserBaseEntity();
		user.setUsrNo(1L);
		user.setLoginId("xodud1202");
		user.setUserNm("테스트");

		when(userBaseService.loadUserByLoginId("xodud1202")).thenReturn(Optional.of(user));
		Authentication authentication = mock(Authentication.class);
		when(authenticationManager.authenticate(any())).thenReturn(authentication);
		when(tokenProvider.generateAccessToken(any(Authentication.class))).thenReturn("test-access-token");

		mockMvc.perform(post("/api/backoffice/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"xodud1202\",\"pwd\":\"qwer\",\"rememberMe\":false}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").exists());
	}

	@Test
	// 존재하지 않는 계정 로그인 실패 시 공통 401 응답을 반환하는지 확인합니다.
	void loginFailWhenUserMissing() throws Exception {
		when(userBaseService.loadUserByLoginId("bad")).thenReturn(Optional.empty());

		mockMvc.perform(post("/api/backoffice/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"bad\",\"pwd\":\"user\",\"rememberMe\":false}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.result").value("AUTH_FAILED"))
			.andExpect(jsonPath("$.resultMsg").value("아이디 또는 비밀번호가 일치하지 않습니다."));
	}

	@Test
	// 비밀번호 불일치 로그인 실패도 존재하지 않는 계정과 동일한 응답을 반환하는지 확인합니다.
	void loginFailWhenPasswordMismatch() throws Exception {
		UserBaseEntity user = new UserBaseEntity();
		user.setUsrNo(1L);
		user.setLoginId("xodud1202");
		user.setUserNm("테스트");

		when(userBaseService.loadUserByLoginId("xodud1202")).thenReturn(Optional.of(user));
		when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad credentials"));

		mockMvc.perform(post("/api/backoffice/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"xodud1202\",\"pwd\":\"wrong\",\"rememberMe\":false}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.result").value("AUTH_FAILED"))
			.andExpect(jsonPath("$.resultMsg").value("아이디 또는 비밀번호가 일치하지 않습니다."));
	}
}
