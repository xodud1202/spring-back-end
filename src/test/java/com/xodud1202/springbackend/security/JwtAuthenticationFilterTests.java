package com.xodud1202.springbackend.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// JWT 인증 필터의 토큰 타입별 인증 처리 동작을 확인합니다.
class JwtAuthenticationFilterTests {

	@Mock
	private JwtTokenProvider tokenProvider;

	@Mock
	private UserDetailsService userDetailsService;

	/**
	 * 테스트 간 SecurityContext를 초기화합니다.
	 */
	@AfterEach
	void tearDown() {
		// 이전 테스트 인증 정보가 다음 테스트에 누수되지 않도록 정리합니다.
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("Bearer 액세스 토큰은 인증 컨텍스트를 생성한다")
	// 유효한 ACCESS 토큰만 사용자 인증으로 변환되는지 검증합니다.
	void doFilterInternalAuthenticatesAccessToken() throws ServletException, IOException {
		// 액세스 토큰과 사용자 조회 결과를 구성합니다.
		when(tokenProvider.validateToken("access-token")).thenReturn(true);
		when(tokenProvider.isAccessToken("access-token")).thenReturn(true);
		when(tokenProvider.getUsernameFromJWT("access-token")).thenReturn("admin");
		UserDetails userDetails = User.withUsername("admin")
			.password("password")
			.authorities("ROLE_ADMIN")
			.build();
		when(userDetailsService.loadUserByUsername("admin")).thenReturn(userDetails);

		// Authorization 헤더가 있는 요청을 필터에 통과시킵니다.
		JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider, userDetailsService);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer access-token");
		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		// SecurityContext에 액세스 토큰 사용자 인증이 저장되어야 합니다.
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
		assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("admin");
	}

	@Test
	@DisplayName("Bearer 리프레시 토큰은 인증 컨텍스트를 생성하지 않는다")
	// 유효한 JWT라도 ACCESS 토큰이 아니면 보호 API 인증에 사용할 수 없음을 검증합니다.
	void doFilterInternalRejectsRefreshTokenAuthentication() throws ServletException, IOException {
		// 리프레시 토큰은 JWT 검증은 통과하지만 ACCESS 타입 검증은 실패하도록 구성합니다.
		when(tokenProvider.validateToken("refresh-token")).thenReturn(true);
		when(tokenProvider.isAccessToken("refresh-token")).thenReturn(false);

		// Authorization 헤더가 있는 요청을 필터에 통과시킵니다.
		JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider, userDetailsService);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer refresh-token");
		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		// refresh 토큰은 사용자 조회와 인증 컨텍스트 생성을 유발하지 않아야 합니다.
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verifyNoInteractions(userDetailsService);
	}
}
