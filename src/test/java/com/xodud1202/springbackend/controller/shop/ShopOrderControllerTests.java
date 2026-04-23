package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.common.shop.ShopSessionPolicy;
import com.xodud1202.springbackend.config.properties.JwtProperties;
import com.xodud1202.springbackend.config.properties.ShopProperties;
import com.xodud1202.springbackend.domain.shop.auth.ShopCustomerSessionVO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartSiteInfoVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPageVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPaymentConfirmVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPaymentPrepareVO;
import com.xodud1202.springbackend.security.SignedLoginTokenService;
import com.xodud1202.springbackend.service.OrderService;
import com.xodud1202.springbackend.service.ShopAuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
// 쇼핑몰 주문서 컨트롤러의 현재 인증/결제 API 계약을 검증합니다.
class ShopOrderControllerTests {
	private static final String JWT_SECRET = "test-secret-key-test-secret-key-test-secret-key";
	private static final String SHOP_FRONT_BASE_URL = "http://shop.example.test";

	@Mock
	private OrderService orderService;

	@Mock
	private ShopAuthService shopAuthService;

	private MockMvc mockMvc;
	private SignedLoginTokenService signedLoginTokenService;

	@BeforeEach
	// 컨트롤러 단위 검증용 MockMvc와 서명 쿠키 서비스를 초기화합니다.
	void setUp() {
		// ShopControllerSupport의 공통 인증/Origin 의존성을 수동 주입합니다.
		JwtProperties jwtProperties = new JwtProperties(JWT_SECRET, Duration.ofMinutes(30), Duration.ofDays(30), false);
		signedLoginTokenService = new SignedLoginTokenService(jwtProperties);

		ShopOrderController controller = new ShopOrderController(orderService);
		ReflectionTestUtils.setField(controller, "shopAuthService", shopAuthService);
		ReflectionTestUtils.setField(controller, "signedLoginTokenService", signedLoginTokenService);
		ReflectionTestUtils.setField(controller, "shopProperties", new ShopProperties(SHOP_FRONT_BASE_URL));
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	@Test
	@DisplayName("주문서 페이지 조회 API는 서명된 shop_auth 쿠키와 설정 Origin으로 서비스를 호출한다")
	// 주문서 페이지 조회 시 raw 요청 Origin 대신 설정된 쇼핑몰 Origin을 전달하는지 검증합니다.
	void getShopOrderPage_usesSignedShopAuthAndConfiguredOrigin() throws Exception {
		// 인증 고객과 주문서 페이지 응답을 목으로 구성합니다.
		mockShopCustomer(7L);
		ShopCartSiteInfoVO siteInfo = new ShopCartSiteInfoVO();
		siteInfo.setDeliveryFee(3000);
		siteInfo.setDeliveryFeeLimit(30000);
		ShopOrderPageVO page = new ShopOrderPageVO();
		page.setCartList(List.of());
		page.setCartCount(0);
		page.setSiteInfo(siteInfo);
		when(orderService.getShopOrderPage(eq(List.of(12L, 15L)), eq(7L), eq("PC"), eq(SHOP_FRONT_BASE_URL))).thenReturn(page);

		// 조작된 Origin 헤더가 있어도 설정 Origin 기준으로 정상 조회되는지 확인합니다.
		mockMvc.perform(
				get("/api/shop/order/page")
					.cookie(shopAuthCookie(7L))
					.header("Origin", "https://evil.example")
					.param("cartId", "12")
					.param("cartId", "15")
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.cartCount").value(0))
			.andExpect(jsonPath("$.siteInfo.deliveryFee").value(3000));

		// 서비스 호출 인자가 설정 Origin으로 고정되는지 검증합니다.
		verify(orderService).getShopOrderPage(List.of(12L, 15L), 7L, "PC", SHOP_FRONT_BASE_URL);
	}

	@Test
	@DisplayName("주문서 페이지 조회 API는 legacy cust_no 쿠키만 있으면 401을 반환한다")
	// raw 고객번호 쿠키만으로는 보호 API 인증을 통과할 수 없는지 검증합니다.
	void getShopOrderPage_rejectsLegacyCustNoCookieOnly() throws Exception {
		// 서명되지 않은 legacy 쿠키로 요청하면 인증 실패를 반환합니다.
		mockMvc.perform(
				get("/api/shop/order/page")
					.cookie(new Cookie(ShopSessionPolicy.COOKIE_CUST_NO, "7"))
					.param("cartId", "12")
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("주문 결제 준비 API는 조작된 Origin을 무시하고 설정 Origin으로 결제 URL을 반환한다")
	// 결제 준비 요청의 success/fail URL 기준이 요청 헤더가 아닌 설정값인지 검증합니다.
	void prepareShopOrderPayment_usesConfiguredOriginOnly() throws Exception {
		// 인증 고객과 결제 준비 응답을 목으로 구성합니다.
		mockShopCustomer(7L);
		ShopOrderPaymentPrepareVO result = new ShopOrderPaymentPrepareVO();
		result.setOrdNo("ORD202604230001");
		result.setPayNo(101L);
		result.setSuccessUrl(SHOP_FRONT_BASE_URL + "/order/success?payNo=101");
		result.setFailUrl(SHOP_FRONT_BASE_URL + "/order/fail?payNo=101");
		when(orderService.prepareShopOrderPayment(any(), eq(7L), anyString(), eq(SHOP_FRONT_BASE_URL))).thenReturn(result);

		// 조작된 프록시 헤더가 있어도 응답 URL은 설정 Origin을 사용해야 합니다.
		mockMvc.perform(
				post("/api/shop/order/payment/prepare")
					.cookie(shopAuthCookie(7L))
					.header("Origin", "https://evil.example")
					.header("X-Forwarded-Host", "evil.example")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"from":"cart","cartIdList":[12],"addressNm":"집","discountSelection":{},"pointUseAmt":0,"paymentMethodCd":"PAY_METHOD_01"}
						""")
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.successUrl").value(SHOP_FRONT_BASE_URL + "/order/success?payNo=101"))
			.andExpect(jsonPath("$.failUrl").value(SHOP_FRONT_BASE_URL + "/order/fail?payNo=101"));
	}

	@Test
	@DisplayName("주문 결제 승인 API는 서명된 shop_auth 쿠키가 있으면 승인 결과를 반환한다")
	// 결제 승인 성공 시 주문번호와 결제번호를 응답하는지 검증합니다.
	void confirmShopOrderPayment_returnsOkWithSignedShopAuth() throws Exception {
		// 인증 고객과 결제 승인 결과를 목으로 구성합니다.
		mockShopCustomer(7L);
		ShopOrderPaymentConfirmVO result = new ShopOrderPaymentConfirmVO();
		result.setOrdNo("ORD202604230001");
		result.setPayNo(101L);
		result.setAmount(39000L);
		when(orderService.confirmShopOrderPayment(any(), eq(7L))).thenReturn(result);

		// 서명 쿠키와 함께 승인 API를 호출합니다.
		mockMvc.perform(
				post("/api/shop/order/payment/confirm")
					.cookie(shopAuthCookie(7L))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"payNo":101,"ordNo":"ORD202604230001","paymentKey":"payment-key","amount":39000}
						""")
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.ordNo").value("ORD202604230001"))
			.andExpect(jsonPath("$.payNo").value(101));
	}

	@Test
	@DisplayName("Toss 웹훅 API는 검증 실패가 발생하면 403을 반환한다")
	// 서비스의 웹훅 검증 실패를 403 응답으로 변환하는지 검증합니다.
	void handleShopOrderPaymentWebhook_returnsForbiddenWhenVerificationFails() throws Exception {
		// 웹훅 검증 실패 예외를 목으로 구성합니다.
		doThrow(new SecurityException("웹훅 검증에 실패했습니다."))
			.when(orderService)
			.handleShopOrderPaymentWebhook(anyString());

		// 위조된 웹훅 요청 시 403 응답을 반환해야 합니다.
		mockMvc.perform(
				post("/api/shop/order/payment/webhook")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"eventType":"PAYMENT_STATUS_CHANGED","data":{"paymentKey":"payment-key","status":"DONE"}}
						""")
			)
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value("웹훅 검증에 실패했습니다."));
	}

	// 테스트용 서명 shop_auth 쿠키를 생성합니다.
	private Cookie shopAuthCookie(Long custNo) {
		// 보호 API가 현재 인증 계약을 사용하도록 서명 토큰 값을 담습니다.
		return new Cookie(ShopSessionPolicy.COOKIE_SHOP_AUTH, signedLoginTokenService.generateShopAuthToken(custNo));
	}

	// 테스트용 활성 쇼핑몰 고객을 목으로 구성합니다.
	private void mockShopCustomer(Long custNo) {
		// 서명 쿠키 복구 뒤 실제 활성 고객 확인까지 통과하도록 설정합니다.
		when(shopAuthService.getShopCustomerByCustNo(custNo)).thenReturn(
			new ShopCustomerSessionVO(custNo, "google_" + custNo, "홍길동", "CUST_GRADE_03", "ci-" + custNo, "hong@test.com")
		);
	}
}
