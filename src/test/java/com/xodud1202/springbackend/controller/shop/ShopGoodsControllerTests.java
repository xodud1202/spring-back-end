package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsBasicVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsDetailVO;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
// 쇼핑몰 상품상세 컨트롤러 API 응답을 검증합니다.
class ShopGoodsControllerTests {
	@Mock
	private GoodsService goodsService;

	@InjectMocks
	private ShopGoodsController shopGoodsController;

	private MockMvc mockMvc;

	@BeforeEach
	// 컨트롤러 단위 검증용 MockMvc를 초기화합니다.
	void setUp() {
		// 단일 컨트롤러 기준 standalone MockMvc를 생성합니다.
		mockMvc = MockMvcBuilders.standaloneSetup(shopGoodsController).build();
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
	@DisplayName("상품상세 API는 상품이 없으면 404와 에러 메시지를 반환한다")
	// 서비스 조회 결과가 없을 때 404 응답으로 변환되는지 검증합니다.
	void getShopGoodsDetail_returnsNotFoundWhenGoodsMissing() throws Exception {
		// 서비스 응답을 null로 고정합니다.
		when(goodsService.getShopGoodsDetail(eq("UNKNOWN"), isNull(), isNull())).thenReturn(null);

		// 상품상세 API 요청 후 404 응답과 메시지를 검증합니다.
		mockMvc.perform(
				get("/api/shop/goods/detail")
					.param("goodsId", "UNKNOWN")
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("상품 정보를 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("상품상세 API는 예외 발생 시 500과 에러 메시지를 반환한다")
	// 서비스 예외 발생 시 500 응답으로 변환되는지 검증합니다.
	void getShopGoodsDetail_returnsInternalServerErrorWhenExceptionOccurs() throws Exception {
		// 서비스 예외를 발생하도록 목 동작을 설정합니다.
		when(goodsService.getShopGoodsDetail(eq("CAMEUEP02MG"), isNull(), isNull())).thenThrow(new IllegalStateException("boom"));

		// 상품상세 API 요청 후 500 응답과 메시지를 검증합니다.
		mockMvc.perform(
				get("/api/shop/goods/detail")
					.param("goodsId", "CAMEUEP02MG")
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.message").value("상품상세 조회에 실패했습니다."));
	}

	@Test
	@DisplayName("위시리스트 토글 API는 로그인 사용자가 요청하면 wished 상태를 반환한다")
	// 위시리스트 토글 성공 시 200 응답과 wished 값을 반환하는지 검증합니다.
	void toggleShopGoodsWishlist_returnsOk() throws Exception {
		// 토글 결과를 wished=true로 반환하도록 설정합니다.
		when(goodsService.toggleShopGoodsWishlist("CAMEUEP02MG", 1L)).thenReturn(true);

		// 로그인 쿠키와 함께 요청하면 200 응답과 wished=true를 검증합니다.
		mockMvc.perform(
				post("/api/shop/goods/wishlist/toggle")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"goodsId":"CAMEUEP02MG"}
						""")
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.wished").value(true));
	}

	@Test
	@DisplayName("위시리스트 토글 API는 비로그인 요청이면 401을 반환한다")
	// 비로그인 상태에서 위시리스트 토글 요청 시 401 응답을 검증합니다.
	void toggleShopGoodsWishlist_returnsUnauthorizedWhenNotLoggedIn() throws Exception {
		// 로그인 쿠키 없이 요청하면 401 응답과 메시지를 검증합니다.
		mockMvc.perform(
				post("/api/shop/goods/wishlist/toggle")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"goodsId":"CAMEUEP02MG"}
						""")
			)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("위시리스트 토글 API는 상품코드가 없으면 400을 반환한다")
	// 필수 파라미터(goodsId) 누락 시 400 응답을 검증합니다.
	void toggleShopGoodsWishlist_returnsBadRequestWhenGoodsIdMissing() throws Exception {
		// goodsId 없이 요청하면 400 응답과 메시지를 검증합니다.
		mockMvc.perform(
				post("/api/shop/goods/wishlist/toggle")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}")
			)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("상품코드를 확인해주세요."));
	}

	@Test
	@DisplayName("위시리스트 토글 API는 조회 불가 상품이면 404를 반환한다")
	// 서비스에서 상품 미존재 예외를 반환하면 404 응답으로 변환되는지 검증합니다.
	void toggleShopGoodsWishlist_returnsNotFoundWhenGoodsMissing() throws Exception {
		// 상품 미존재 예외를 발생하도록 목 동작을 설정합니다.
		when(goodsService.toggleShopGoodsWishlist("UNKNOWN", 1L))
			.thenThrow(new IllegalArgumentException("상품 정보를 찾을 수 없습니다."));

		// 토글 요청 후 404 응답과 메시지를 검증합니다.
		mockMvc.perform(
				post("/api/shop/goods/wishlist/toggle")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"goodsId":"UNKNOWN"}
						""")
			)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("상품 정보를 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("장바구니 등록 API는 로그인 사용자가 요청하면 최종 수량을 반환한다")
	// 장바구니 등록 성공 시 200 응답과 최종 수량 반환 여부를 검증합니다.
	void addShopGoodsCart_returnsOk() throws Exception {
		// 장바구니 최종 수량을 5로 반환하도록 설정합니다.
		when(goodsService.addShopGoodsCart("CAMEUEP02MG", "095", 2, 1L)).thenReturn(5);

		// 로그인 쿠키와 함께 요청하면 200 응답과 수량/메시지를 검증합니다.
		mockMvc.perform(
				post("/api/shop/goods/cart/add")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"goodsId":"CAMEUEP02MG","sizeId":"095","qty":2}
						""")
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.qty").value(5))
			.andExpect(jsonPath("$.message").value("장바구니에 담았습니다."));
	}

	@Test
	@DisplayName("장바구니 등록 API는 비로그인 요청이면 401을 반환한다")
	// 비로그인 상태에서 장바구니 등록 요청 시 401 응답을 검증합니다.
	void addShopGoodsCart_returnsUnauthorizedWhenNotLoggedIn() throws Exception {
		// 로그인 쿠키 없이 요청하면 401 응답과 메시지를 검증합니다.
		mockMvc.perform(
				post("/api/shop/goods/cart/add")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"goodsId":"CAMEUEP02MG","sizeId":"095","qty":1}
						""")
			)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("장바구니 등록 API는 사이즈가 없으면 400을 반환한다")
	// 필수 파라미터(sizeId) 누락 시 400 응답을 검증합니다.
	void addShopGoodsCart_returnsBadRequestWhenSizeMissing() throws Exception {
		// sizeId 없이 요청하면 400 응답과 메시지를 검증합니다.
		mockMvc.perform(
				post("/api/shop/goods/cart/add")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"goodsId":"CAMEUEP02MG","qty":1}
						""")
			)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("사이즈를 선택해주세요."));
	}

	@Test
	@DisplayName("장바구니 등록 API는 조회 불가 상품이면 404를 반환한다")
	// 서비스에서 상품 미존재 예외를 반환하면 404 응답으로 변환되는지 검증합니다.
	void addShopGoodsCart_returnsNotFoundWhenGoodsMissing() throws Exception {
		// 상품 미존재 예외를 발생하도록 목 동작을 설정합니다.
		when(goodsService.addShopGoodsCart("UNKNOWN", "095", 1, 1L))
			.thenThrow(new IllegalArgumentException("상품 정보를 찾을 수 없습니다."));

		// 장바구니 등록 요청 후 404 응답과 메시지를 검증합니다.
		mockMvc.perform(
				post("/api/shop/goods/cart/add")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"goodsId":"UNKNOWN","sizeId":"095","qty":1}
						""")
			)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("상품 정보를 찾을 수 없습니다."));
	}
}
