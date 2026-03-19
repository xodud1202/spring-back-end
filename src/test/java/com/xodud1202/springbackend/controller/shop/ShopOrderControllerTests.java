package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.domain.shop.cart.ShopCartPageVO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartSiteInfoVO;
import com.xodud1202.springbackend.service.GoodsService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
// 쇼핑몰 주문서 컨트롤러 API 응답을 검증합니다.
class ShopOrderControllerTests {
	@Mock
	private GoodsService goodsService;

	@InjectMocks
	private ShopOrderController shopOrderController;

	private MockMvc mockMvc;

	@BeforeEach
	// 컨트롤러 단위 검증용 MockMvc를 초기화합니다.
	void setUp() {
		// 단일 컨트롤러 기준 standalone MockMvc를 생성합니다.
		mockMvc = MockMvcBuilders.standaloneSetup(shopOrderController).build();
	}

	@Test
	@DisplayName("주문서 페이지 조회 API는 로그인 사용자가 유효한 cartId를 전달하면 데이터를 반환한다")
	// 주문서 페이지 조회 성공 시 200 응답과 페이지 필드 반환 여부를 검증합니다.
	void getShopOrderPage_returnsOk() throws Exception {
		// 주문서 페이지 응답 객체를 구성합니다.
		ShopCartSiteInfoVO siteInfo = new ShopCartSiteInfoVO();
		siteInfo.setSiteId("xodud1202");
		siteInfo.setDeliveryFee(3000);
		siteInfo.setDeliveryFeeLimit(30000);

		ShopCartPageVO page = new ShopCartPageVO();
		page.setCartList(List.of());
		page.setCartCount(0);
		page.setSiteInfo(siteInfo);
		when(goodsService.getShopOrderPage(List.of(12L, 15L), 1L)).thenReturn(page);

		// 로그인 쿠키와 함께 요청하면 200 응답과 주문서 기본 필드를 검증합니다.
		mockMvc.perform(
				get("/api/shop/order/page")
					.cookie(new Cookie("cust_no", "1"))
					.param("cartId", "12")
					.param("cartId", "15")
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.cartCount").value(0))
			.andExpect(jsonPath("$.siteInfo.deliveryFee").value(3000))
			.andExpect(jsonPath("$.siteInfo.deliveryFeeLimit").value(30000));
	}

	@Test
	@DisplayName("주문서 페이지 조회 API는 비로그인 요청이면 401을 반환한다")
	// 비로그인 상태에서 주문서 페이지 조회 요청 시 401 응답을 검증합니다.
	void getShopOrderPage_returnsUnauthorizedWhenNotLoggedIn() throws Exception {
		// 로그인 쿠키 없이 요청하면 401 응답과 메시지를 검증합니다.
		mockMvc.perform(
				get("/api/shop/order/page")
					.param("cartId", "12")
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("주문서 페이지 조회 API는 유효하지 않은 cartId가 포함되면 400을 반환한다")
	// 주문서 페이지 조회 시 cartId 검증 예외가 발생하면 400 응답으로 변환되는지 검증합니다.
	void getShopOrderPage_returnsBadRequestWhenCartIdInvalid() throws Exception {
		// 주문 정보 검증 예외를 발생하도록 목 동작을 설정합니다.
		when(goodsService.getShopOrderPage(List.of(12L, 19L), 1L))
			.thenThrow(new IllegalArgumentException("주문 정보가 맞지 않습니다."));

		// 로그인 쿠키와 함께 잘못된 cartId를 요청하면 400 응답과 메시지를 검증합니다.
		mockMvc.perform(
				get("/api/shop/order/page")
					.cookie(new Cookie("cust_no", "1"))
					.param("cartId", "12")
					.param("cartId", "19")
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("주문 정보가 맞지 않습니다."));
	}
}
