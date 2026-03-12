package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.service.ShopAuthService;
import jakarta.servlet.http.Cookie;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
// 쇼핑몰 인증 컨트롤러 API 응답을 검증합니다.
class ShopAuthControllerTests {
	@Mock
	private ShopAuthService shopAuthService;

	@InjectMocks
	private ShopAuthController shopAuthController;

	private MockMvc mockMvc;

	@BeforeEach
	// 컨트롤러 단위 검증용 MockMvc를 초기화합니다.
	void setUp() {
		// 단일 컨트롤러 기준 standalone MockMvc를 생성합니다.
		mockMvc = MockMvcBuilders.standaloneSetup(shopAuthController).build();
	}

	@Test
	@DisplayName("세션 갱신 API는 로그인 쿠키가 있으면 등급명을 포함한 인증 정보를 반환한다")
	// 세션 갱신 성공 시 고객명/등급코드/등급명을 포함한 응답 구조를 검증합니다.
	void refreshShopSession_returnsAuthenticatedPayloadWithCustGradeNm() throws Exception {
		// 고객 등급명 조회 목 응답을 구성합니다.
		when(shopAuthService.getCustomerGradeName("CUST_GRADE_03")).thenReturn("GOLD");

		// 세션 갱신 API를 호출합니다.
		mockMvc.perform(
				post("/api/shop/auth/session/refresh")
					.cookie(
						new Cookie("cust_no", "7"),
						new Cookie("cust_nm", URLEncoder.encode("홍길동", StandardCharsets.UTF_8)),
						new Cookie("cust_grade_cd", URLEncoder.encode("CUST_GRADE_03", StandardCharsets.UTF_8))
					)
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.authenticated").value(true))
			.andExpect(jsonPath("$.custNo").value(7))
			.andExpect(jsonPath("$.custNm").value("홍길동"))
			.andExpect(jsonPath("$.custGradeCd").value("CUST_GRADE_03"))
			.andExpect(jsonPath("$.custGradeNm").value("GOLD"))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.iterableWithSize(3)));

		// 고객 등급명 조회 호출 여부를 검증합니다.
		verify(shopAuthService, times(1)).getCustomerGradeName("CUST_GRADE_03");
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
						new Cookie("cust_no", "1"),
						new Cookie("cust_nm", "테스터"),
						new Cookie("cust_grade_cd", "CUST_GRADE_01")
					)
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("로그아웃 처리되었습니다."))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.iterableWithSize(3)))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.hasItem(Matchers.containsString("cust_no="))))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.hasItem(Matchers.containsString("cust_nm="))))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.hasItem(Matchers.containsString("cust_grade_cd="))))
			.andExpect(header().stringValues(HttpHeaders.SET_COOKIE, Matchers.hasItem(Matchers.containsString("Max-Age=0"))));
	}
}
