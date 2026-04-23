package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.common.shop.ShopSessionPolicy;
import com.xodud1202.springbackend.config.properties.JwtProperties;
import com.xodud1202.springbackend.domain.shop.auth.ShopCustomerSessionVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsBasicVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsDetailVO;
import com.xodud1202.springbackend.security.SignedLoginTokenService;
import com.xodud1202.springbackend.service.GoodsService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
// 쇼핑몰 상품 컨트롤러의 현재 상품상세/쿠폰/위시리스트 API 계약을 검증합니다.
class ShopGoodsControllerTests {
	private static final String JWT_SECRET = "test-secret-key-test-secret-key-test-secret-key";

	@Mock
	private GoodsService goodsService;

	@Mock
	private ShopAuthService shopAuthService;

	private MockMvc mockMvc;
	private SignedLoginTokenService signedLoginTokenService;

	@BeforeEach
	// 컨트롤러 단위 검증용 MockMvc와 서명 쿠키 서비스를 초기화합니다.
	void setUp() {
		// ShopControllerSupport의 공통 인증 의존성을 수동 주입합니다.
		JwtProperties jwtProperties = new JwtProperties(JWT_SECRET, Duration.ofMinutes(30), Duration.ofDays(30), false);
		signedLoginTokenService = new SignedLoginTokenService(jwtProperties);

		ShopGoodsController controller = new ShopGoodsController(goodsService, shopAuthService);
		ReflectionTestUtils.setField(controller, ShopControllerSupport.class, "shopAuthService", shopAuthService, null);
		ReflectionTestUtils.setField(controller, "signedLoginTokenService", signedLoginTokenService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	@Test
	@DisplayName("상품상세 API는 정상 조회 시 200과 상품 기본 정보를 반환한다")
	// 상품상세 API 정상 응답 구조를 검증합니다.
	void getShopGoodsDetail_returnsOk() throws Exception {
		// 서비스 반환용 상품상세 응답 객체를 구성합니다.
		ShopGoodsBasicVO goods = new ShopGoodsBasicVO();
		goods.setGoodsId("CAMEUEP02MG");
		goods.setGoodsNm("테스트 상품");

		ShopGoodsDetailVO detail = new ShopGoodsDetailVO();
		detail.setGoods(goods);
		when(goodsService.getShopGoodsDetail(eq("CAMEUEP02MG"), isNull(), isNull())).thenReturn(detail);

		// 상품상세 API 요청 후 200 응답과 상품 필드를 검증합니다.
		mockMvc.perform(
				get("/api/shop/goods/detail")
					.param("goodsId", "CAMEUEP02MG")
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.goods.goodsId").value("CAMEUEP02MG"))
			.andExpect(jsonPath("$.goods.goodsNm").value("테스트 상품"));
	}

	@Test
	@DisplayName("상품상세 API는 상품코드가 없으면 400과 에러 메시지를 반환한다")
	// 필수 파라미터 누락 시 400 응답으로 변환되는지 검증합니다.
	void getShopGoodsDetail_returnsBadRequestWhenGoodsIdMissing() throws Exception {
		// goodsId 없이 요청했을 때 400 응답과 메시지를 검증합니다.
		mockMvc.perform(get("/api/shop/goods/detail").contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("상품코드를 확인해주세요."));
	}

	@Test
	@DisplayName("위시리스트 토글 API는 서명된 shop_auth 쿠키가 있으면 wished 상태를 반환한다")
	// 위시리스트 토글 성공 시 200 응답과 wished 값을 반환하는지 검증합니다.
	void toggleShopGoodsWishlist_returnsOkWithSignedShopAuth() throws Exception {
		// 활성 고객과 토글 결과를 목으로 구성합니다.
		mockShopCustomer(7L);
		when(goodsService.toggleShopGoodsWishlist("CAMEUEP02MG", 7L)).thenReturn(true);

		// 서명 쿠키와 함께 요청하면 200 응답과 wished=true를 검증합니다.
		mockMvc.perform(
				post("/api/shop/goods/wishlist/toggle")
					.cookie(shopAuthCookie(7L))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"goodsId":"CAMEUEP02MG"}
						""")
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.wished").value(true));
	}

	@Test
	@DisplayName("위시리스트 토글 API는 legacy cust_no 쿠키만 있으면 401을 반환한다")
	// raw 고객번호 쿠키만으로는 보호 API 인증을 통과할 수 없는지 검증합니다.
	void toggleShopGoodsWishlist_rejectsLegacyCustNoCookieOnly() throws Exception {
		// 서명되지 않은 legacy 쿠키로 요청하면 인증 실패를 반환합니다.
		mockMvc.perform(
				post("/api/shop/goods/wishlist/toggle")
					.cookie(new Cookie(ShopSessionPolicy.COOKIE_CUST_NO, "7"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"goodsId":"CAMEUEP02MG"}
						""")
			)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("상품상세 쿠폰 다운로드 API는 서비스 검증 실패를 400으로 반환한다")
	// 쿠폰 다운로드 서비스 검증 예외를 400 응답으로 변환하는지 검증합니다.
	void downloadShopGoodsCoupon_returnsBadRequestWhenCouponInvalidForGoods() throws Exception {
		// 활성 고객과 쿠폰 검증 실패 예외를 목으로 구성합니다.
		mockShopCustomer(7L);
		doThrow(new IllegalArgumentException("다운로드 가능한 상품쿠폰을 확인해주세요."))
			.when(goodsService)
			.downloadShopGoodsCoupon("CAMEUEP02MG", 21L, 7L);

		// 서명 쿠키와 함께 요청하면 400 응답과 메시지를 검증합니다.
		mockMvc.perform(
				post("/api/shop/goods/coupon/download")
					.cookie(shopAuthCookie(7L))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"goodsId":"CAMEUEP02MG","cpnNo":21}
						""")
			)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("다운로드 가능한 상품쿠폰을 확인해주세요."));
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
