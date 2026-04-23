package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.common.shop.ShopSessionPolicy;
import com.xodud1202.springbackend.config.properties.JwtProperties;
import com.xodud1202.springbackend.domain.shop.auth.ShopCustomerSessionVO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartPageVO;
import com.xodud1202.springbackend.security.SignedLoginTokenService;
import com.xodud1202.springbackend.service.CartService;
import com.xodud1202.springbackend.service.ShopAuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
// 쇼핑몰 장바구니 보호 API의 서명 토큰 인증 동작을 검증합니다.
class ShopCartControllerTests {
	private static final String JWT_SECRET = "test-secret-key-test-secret-key-test-secret-key";

	@Mock
	private CartService cartService;

	@Mock
	private ShopAuthService shopAuthService;

	private MockMvc mockMvc;
	private SignedLoginTokenService signedLoginTokenService;

	@BeforeEach
	// 쇼핑몰 장바구니 컨트롤러 단위 검증용 MockMvc를 초기화합니다.
	void setUp() {
		// 보호 API 인증 해석에 필요한 의존성을 수동으로 주입합니다.
		JwtProperties jwtProperties = new JwtProperties(JWT_SECRET, Duration.ofMinutes(30), Duration.ofDays(30), false);
		signedLoginTokenService = new SignedLoginTokenService(jwtProperties);

		ShopCartController controller = new ShopCartController(cartService);
		ReflectionTestUtils.setField(controller, "shopAuthService", shopAuthService);
		ReflectionTestUtils.setField(controller, "signedLoginTokenService", signedLoginTokenService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	@Test
	@DisplayName("쇼핑몰 장바구니 페이지 API는 유효한 shop_auth 쿠키가 있으면 통과한다")
	// 세션이 없어도 서명된 shop_auth 쿠키가 있으면 보호 API가 인증된 고객번호로 처리되는지 검증합니다.
	void getShopCartPage_allowsSignedShopAuthCookie() throws Exception {
		// 활성 고객 조회와 장바구니 페이지 응답을 목으로 설정합니다.
		when(shopAuthService.getShopCustomerByCustNo(7L)).thenReturn(
			new ShopCustomerSessionVO(7L, "google_7", "홍길동", "CUST_GRADE_03", "ci-7", "hong@test.com")
		);
		when(cartService.getShopCartPage(7L)).thenReturn(new ShopCartPageVO());

		// 보호 API를 호출합니다.
		mockMvc.perform(
				get("/api/shop/cart/page")
					.cookie(new Cookie(ShopSessionPolicy.COOKIE_SHOP_AUTH, signedLoginTokenService.generateShopAuthToken(7L)))
			)
			.andExpect(status().isOk());

		// 인증 고객번호 기준으로 장바구니 조회가 수행되는지 검증합니다.
		verify(shopAuthService).getShopCustomerByCustNo(7L);
		verify(cartService).getShopCartPage(7L);
	}

	@Test
	@DisplayName("쇼핑몰 장바구니 페이지 API는 raw cust_no 쿠키만 있으면 401을 반환한다")
	// 서명되지 않은 raw 고객번호 쿠키만으로는 보호 API 인증을 통과할 수 없는지 검증합니다.
	void getShopCartPage_rejectsLegacyCustNoCookieOnly() throws Exception {
		// 보호 API를 호출합니다.
		mockMvc.perform(
				get("/api/shop/cart/page")
					.cookie(new Cookie(ShopSessionPolicy.COOKIE_CUST_NO, "7"))
			)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));

		// 인증 실패 시 장바구니 조회가 호출되지 않는지 검증합니다.
		verify(shopAuthService, never()).getShopCustomerByCustNo(7L);
		verify(cartService, never()).getShopCartPage(7L);
	}
}
