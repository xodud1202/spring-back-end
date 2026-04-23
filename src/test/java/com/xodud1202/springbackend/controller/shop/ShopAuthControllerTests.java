package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.common.exception.GlobalRestExceptionHandler;
import com.xodud1202.springbackend.common.shop.ShopSessionPolicy;
import com.xodud1202.springbackend.config.properties.JwtProperties;
import com.xodud1202.springbackend.domain.shop.auth.ShopCustomerSessionVO;
import com.xodud1202.springbackend.domain.shop.auth.ShopGoogleLoginResponse;
import com.xodud1202.springbackend.security.AuthCookieFactory;
import com.xodud1202.springbackend.security.SignedLoginTokenService;
import com.xodud1202.springbackend.service.ShopAuthService;
import jakarta.servlet.http.Cookie;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Duration;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
// 쇼핑몰 인증 컨트롤러 API 응답을 검증합니다.
class ShopAuthControllerTests {
	private static final String JWT_SECRET = "test-secret-key-test-secret-key-test-secret-key";

	@Mock
	private ShopAuthService shopAuthService;

	private MockMvc mockMvc;
	private SignedLoginTokenService signedLoginTokenService;

	@BeforeEach
	// 컨트롤러 단위 검증용 MockMvc를 초기화합니다.
	void setUp() {
		// JWT 쿠키 정책과 예외 처리기를 포함한 standalone MockMvc를 생성합니다.
		JwtProperties jwtProperties = new JwtProperties(JWT_SECRET, Duration.ofMinutes(30), Duration.ofDays(30), false);
		AuthCookieFactory authCookieFactory = new AuthCookieFactory(jwtProperties);
		signedLoginTokenService = new SignedLoginTokenService(jwtProperties);
		ShopAuthController shopAuthController = new ShopAuthController(shopAuthService, authCookieFactory, signedLoginTokenService);
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders.standaloneSetup(shopAuthController)
			.setControllerAdvice(new GlobalRestExceptionHandler())
			.setValidator(validator)
			.build();
	}

	@Test
	@DisplayName("현재 로그인 상태 조회 API는 유효한 shop_auth 쿠키가 있으면 인증 정보를 반환하고 shop_auth만 재발급한다")
	// 세션이 없어도 유효한 서명 토큰이 남아 있으면 인증 응답과 shop_auth 재발급이 함께 되는지 검증합니다.
	void getCurrentShopAuth_returnsAuthenticatedPayloadWithSignedShopCookie() throws Exception {
		// 고객 세션 조회 및 등급명 조회 목 응답을 구성합니다.
		when(shopAuthService.getShopCustomerByCustNo(7L)).thenReturn(
			new ShopCustomerSessionVO(7L, "google_7", "홍길동", "CUST_GRADE_03", "ci-7", "hong@test.com")
		);
		when(shopAuthService.getCustomerGradeName("CUST_GRADE_03")).thenReturn("GOLD");

		// 현재 로그인 상태 조회 API를 호출합니다.
		mockMvc.perform(
				get("/api/shop/auth/me")
					.cookie(new Cookie(ShopSessionPolicy.COOKIE_SHOP_AUTH, signedLoginTokenService.generateShopAuthToken(7L)))
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.authenticated").value(true))
			.andExpect(jsonPath("$.custNo").value(7))
			.andExpect(jsonPath("$.custNm").value("홍길동"))
			.andExpect(jsonPath("$.custGradeCd").value("CUST_GRADE_03"))
			.andExpect(jsonPath("$.custGradeNm").value("GOLD"))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.iterableWithSize(4)))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.hasItem(Matchers.containsString("shop_auth="))))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.hasItem(Matchers.allOf(
				Matchers.containsString("cust_no="),
				Matchers.containsString("Max-Age=0")
			))))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.hasItem(Matchers.allOf(
				Matchers.containsString("cust_nm="),
				Matchers.containsString("Max-Age=0")
			))))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.hasItem(Matchers.allOf(
				Matchers.containsString("cust_grade_cd="),
				Matchers.containsString("Max-Age=0")
			))));

		// 고객 세션 조회와 등급명 조회 호출 여부를 검증합니다.
		verify(shopAuthService, times(1)).getShopCustomerByCustNo(7L);
		verify(shopAuthService, times(1)).getCustomerGradeName("CUST_GRADE_03");
	}

	@Test
	@DisplayName("현재 로그인 상태 조회 API는 위조된 shop_auth 쿠키면 비로그인과 만료 쿠키를 반환한다")
	// 파싱되지 않는 raw 숫자 쿠키나 위조 토큰은 인증 복구에 사용하지 않고 전부 만료하는지 검증합니다.
	void getCurrentShopAuth_returnsUnauthenticatedWhenSignedShopCookieIsInvalid() throws Exception {
		// 현재 로그인 상태 조회 API를 호출합니다.
		mockMvc.perform(
				get("/api/shop/auth/me")
					.cookie(new Cookie(ShopSessionPolicy.COOKIE_SHOP_AUTH, "9"))
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.authenticated").value(false))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.iterableWithSize(4)))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.hasItem(Matchers.allOf(
				Matchers.containsString("shop_auth="),
				Matchers.containsString("Max-Age=0")
			))))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.hasItem(Matchers.allOf(
				Matchers.containsString("cust_no="),
				Matchers.containsString("Max-Age=0")
			))))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.hasItem(Matchers.allOf(
				Matchers.containsString("cust_nm="),
				Matchers.containsString("Max-Age=0")
			))))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.hasItem(Matchers.allOf(
				Matchers.containsString("cust_grade_cd="),
				Matchers.containsString("Max-Age=0")
			))));
	}

	@Test
	@DisplayName("현재 로그인 상태 조회 API는 비활성 고객의 shop_auth 쿠키면 비로그인과 만료 쿠키를 반환한다")
	// 서명 토큰이 유효해도 현재 활성 고객이 아니면 인증을 복구하지 않는지 검증합니다.
	void getCurrentShopAuth_returnsUnauthenticatedWhenCustomerMissing() throws Exception {
		// 조회 대상 고객이 없도록 목 응답을 설정합니다.
		when(shopAuthService.getShopCustomerByCustNo(9L)).thenReturn(null);

		// 현재 로그인 상태 조회 API를 호출합니다.
		mockMvc.perform(
				get("/api/shop/auth/me")
					.cookie(new Cookie(ShopSessionPolicy.COOKIE_SHOP_AUTH, signedLoginTokenService.generateShopAuthToken(9L)))
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.authenticated").value(false))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.iterableWithSize(4)))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.hasItem(Matchers.allOf(
				Matchers.containsString("shop_auth="),
				Matchers.containsString("Max-Age=0")
			))))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.hasItem(Matchers.containsString("cust_no="))))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.hasItem(Matchers.containsString("cust_nm="))))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.hasItem(Matchers.containsString("cust_grade_cd="))));
	}

	@Test
	@DisplayName("구글 로그인 API는 로그인 성공 시 shop_auth만 발급하고 레거시 cust_*는 정리한다")
	// 로그인 성공 응답이 새로운 shop_auth 계약만 사용하고 레거시 쿠키는 재발급하지 않는지 검증합니다.
	void loginWithGoogle_issuesShopAuthCookieOnly() throws Exception {
		// 로그인 성공 목 응답을 설정합니다.
		when(shopAuthService.loginWithGoogle(org.mockito.ArgumentMatchers.any())).thenReturn(
			ShopGoogleLoginResponse.loginSuccess(7L, "홍길동", "CUST_GRADE_03", "google_7")
		);

		// 로그인 API를 호출합니다.
		mockMvc.perform(
				post("/api/shop/auth/google/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{
						  "sub": "google-sub-7",
						  "email": "google-user@test.com"
						}
						""")
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.loginSuccess").value(true))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.iterableWithSize(4)))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.hasItem(Matchers.containsString("shop_auth="))))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.not(Matchers.hasItem(Matchers.containsString("cust_no=7")))));
	}

	@Test
	@DisplayName("구글 로그인 API는 필수 sub 값이 없으면 400 메시지 응답을 반환한다")
	// Bean Validation 실패 시 공통 예외 응답 구조를 검증합니다.
	void loginWithGoogle_returnsBadRequestWhenSubMissing() throws Exception {
		// 빈 sub 요청을 전송합니다.
		mockMvc.perform(
				post("/api/shop/auth/google/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{
						  "sub": "",
						  "email": "google-user@test.com"
						}
						""")
			)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("구글 사용자 식별값을 확인해주세요."));
	}

	@Test
	@DisplayName("로그아웃 API는 200 응답과 쿠키 만료 헤더를 반환한다")
	// 로그아웃 성공 시 세션/쿠키 만료 응답 구조를 검증합니다.
	void logoutShop_returnsOkAndExpiredCookies() throws Exception {
		// 로그인 세션과 쿠키를 구성한 뒤 로그아웃 API를 호출합니다.
		MockHttpSession session = new MockHttpSession();
		session.setAttribute("shopCustNo", 1L);

		mockMvc.perform(
				post("/api/shop/auth/logout")
					.session(session)
					.cookie(
						new Cookie(ShopSessionPolicy.COOKIE_SHOP_AUTH, signedLoginTokenService.generateShopAuthToken(1L)),
						new Cookie("cust_no", "1"),
						new Cookie("cust_nm", "테스터"),
						new Cookie("cust_grade_cd", "CUST_GRADE_01")
					)
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("로그아웃 처리되었습니다."))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.iterableWithSize(4)))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.hasItem(Matchers.containsString("shop_auth="))))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.hasItem(Matchers.containsString("cust_no="))))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.hasItem(Matchers.containsString("cust_nm="))))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.hasItem(Matchers.containsString("cust_grade_cd="))))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.hasItem(Matchers.containsString("Max-Age=0"))));
	}
}
