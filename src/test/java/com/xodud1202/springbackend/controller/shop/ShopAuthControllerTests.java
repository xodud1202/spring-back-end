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
