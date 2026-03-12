package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsBasicVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsDetailVO;
import com.xodud1202.springbackend.service.GoodsService;
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
}
