package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.domain.shop.exhibition.ShopExhibitionDetailVO;
import com.xodud1202.springbackend.domain.shop.exhibition.ShopExhibitionGoodsItemVO;
import com.xodud1202.springbackend.domain.shop.exhibition.ShopExhibitionGoodsPageVO;
import com.xodud1202.springbackend.domain.shop.exhibition.ShopExhibitionItemVO;
import com.xodud1202.springbackend.domain.shop.exhibition.ShopExhibitionPageVO;
import com.xodud1202.springbackend.domain.shop.exhibition.ShopExhibitionTabVO;
import com.xodud1202.springbackend.service.ExhibitionService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
// 쇼핑몰 기획전 컨트롤러 API 응답을 검증합니다.
class ShopExhibitionControllerTests {
	@Mock
	private ExhibitionService exhibitionService;

	@InjectMocks
	private ShopExhibitionController shopExhibitionController;

	private MockMvc mockMvc;

	@BeforeEach
	// 컨트롤러 단위 검증용 MockMvc를 초기화합니다.
	void setUp() {
		// 단일 컨트롤러 기준 standalone MockMvc를 생성합니다.
		mockMvc = MockMvcBuilders.standaloneSetup(shopExhibitionController).build();
	}

	@Test
	@DisplayName("기획전 목록 API는 정상 조회 시 200과 페이지 데이터를 반환한다")
	// 기획전 목록 API 정상 응답 구조를 검증합니다.
	void getShopExhibitionList_returnsOk() throws Exception {
		// 서비스 반환용 기획전 페이지 응답 객체를 구성합니다.
		ShopExhibitionItemVO item = new ShopExhibitionItemVO();
		item.setExhibitionNo(2);
		item.setExhibitionNm("2026 S/S 신상품 기획전");
		item.setThumbnailUrl("https://image.xodud1202.kro.kr/exhibition/2.png");

		ShopExhibitionPageVO pageVO = new ShopExhibitionPageVO();
		pageVO.setExhibitionList(List.of(item));
		pageVO.setTotalCount(1);
		pageVO.setPageNo(1);
		pageVO.setPageSize(20);
		pageVO.setTotalPageCount(1);
		when(exhibitionService.getShopExhibitionPage(1)).thenReturn(pageVO);

		// 기획전 목록 API 요청 후 200 응답과 데이터 필드를 검증합니다.
		mockMvc.perform(get("/api/shop/exhibition/list").param("pageNo", "1").contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.exhibitionList[0].exhibitionNo").value(2))
			.andExpect(jsonPath("$.exhibitionList[0].exhibitionNm").value("2026 S/S 신상품 기획전"))
			.andExpect(jsonPath("$.pageNo").value(1))
			.andExpect(jsonPath("$.pageSize").value(20))
			.andExpect(jsonPath("$.totalPageCount").value(1));
	}

	@Test
	@DisplayName("기획전 목록 API는 pageNo 없이 요청되면 null pageNo로 서비스에 전달한다")
	// 선택 파라미터 pageNo 생략 시 컨트롤러 전달값을 검증합니다.
	void getShopExhibitionList_passesNullPageNoWhenMissing() throws Exception {
		// pageNo 미지정 요청의 목 동작을 설정합니다.
		ShopExhibitionPageVO pageVO = new ShopExhibitionPageVO();
		pageVO.setExhibitionList(List.of());
		pageVO.setTotalCount(0);
		pageVO.setPageNo(1);
		pageVO.setPageSize(20);
		pageVO.setTotalPageCount(0);
		when(exhibitionService.getShopExhibitionPage(null)).thenReturn(pageVO);

		// pageNo 없이 요청하고 서비스 호출 인자를 검증합니다.
		mockMvc.perform(get("/api/shop/exhibition/list").contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());
		verify(exhibitionService).getShopExhibitionPage(isNull());
	}

	@Test
	@DisplayName("기획전 상세 API는 정상 조회 시 200과 상세 데이터를 반환한다")
	// 기획전 상세 API 정상 응답 구조를 검증합니다.
	void getShopExhibitionDetail_returnsOk() throws Exception {
		// 서비스 반환용 상세 응답 객체를 구성합니다.
		ShopExhibitionTabVO tab = new ShopExhibitionTabVO();
		tab.setExhibitionTabNo(1);
		tab.setExhibitionTabNm("Spring");

		ShopExhibitionDetailVO detailVO = new ShopExhibitionDetailVO();
		detailVO.setExhibitionNo(2);
		detailVO.setExhibitionNm("2026 S/S 신상품 기획전");
		detailVO.setVisibleHtml("<div>PC</div>");
		detailVO.setDefaultTabNo(1);
		detailVO.setTabList(List.of(tab));
		when(exhibitionService.getShopExhibitionDetail(2)).thenReturn(detailVO);

		// 기획전 상세 API 요청 후 200 응답과 데이터 필드를 검증합니다.
		mockMvc.perform(get("/api/shop/exhibition/detail").param("exhibitionNo", "2").contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.exhibitionNo").value(2))
			.andExpect(jsonPath("$.defaultTabNo").value(1))
			.andExpect(jsonPath("$.tabList[0].exhibitionTabNm").value("Spring"));
	}

	@Test
	@DisplayName("기획전 상세 API는 미노출 기획전이면 404를 반환한다")
	// 기획전 상세 미존재/미노출 예외가 404 응답으로 변환되는지 검증합니다.
	void getShopExhibitionDetail_returnsNotFoundWhenExhibitionDoesNotExist() throws Exception {
		// 404 성격의 서비스 예외를 목으로 설정합니다.
		when(exhibitionService.getShopExhibitionDetail(eq(999))).thenThrow(new IllegalArgumentException("기획전 정보를 찾을 수 없습니다."));

		// 기획전 상세 API 요청 후 404 응답과 메시지를 검증합니다.
		mockMvc.perform(get("/api/shop/exhibition/detail").param("exhibitionNo", "999").contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("기획전 정보를 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("기획전 상품 API는 정상 조회 시 200과 페이징 데이터를 반환한다")
	// 기획전 탭 상품 API 정상 응답 구조를 검증합니다.
	void getShopExhibitionGoods_returnsOk() throws Exception {
		// 서비스 반환용 상품 페이지 응답 객체를 구성합니다.
		ShopExhibitionGoodsItemVO goodsItem = new ShopExhibitionGoodsItemVO();
		goodsItem.setExhibitionNo(2);
		goodsItem.setExhibitionTabNo(1);
		goodsItem.setGoodsId("KPFESBP05PK");
		goodsItem.setGoodsNm("테스트 상품");

		ShopExhibitionGoodsPageVO pageVO = new ShopExhibitionGoodsPageVO();
		pageVO.setGoodsList(List.of(goodsItem));
		pageVO.setTotalCount(1);
		pageVO.setPageNo(1);
		pageVO.setPageSize(20);
		pageVO.setHasMore(false);
		pageVO.setNextPageNo(null);
		when(exhibitionService.getShopExhibitionGoodsPage(2, 1, 1)).thenReturn(pageVO);

		// 기획전 상품 API 요청 후 200 응답과 데이터 필드를 검증합니다.
		mockMvc.perform(
			get("/api/shop/exhibition/goods")
				.param("exhibitionNo", "2")
				.param("exhibitionTabNo", "1")
				.param("pageNo", "1")
				.contentType(MediaType.APPLICATION_JSON)
		)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.goodsList[0].goodsId").value("KPFESBP05PK"))
			.andExpect(jsonPath("$.pageNo").value(1))
			.andExpect(jsonPath("$.pageSize").value(20))
			.andExpect(jsonPath("$.hasMore").value(false));
	}

	@Test
	@DisplayName("기획전 상품 API는 미노출 탭이면 404를 반환한다")
	// 기획전 탭 미존재/미노출 예외가 404 응답으로 변환되는지 검증합니다.
	void getShopExhibitionGoods_returnsNotFoundWhenTabDoesNotExist() throws Exception {
		// 404 성격의 서비스 예외를 목으로 설정합니다.
		when(exhibitionService.getShopExhibitionGoodsPage(eq(2), eq(99), eq(1)))
			.thenThrow(new IllegalArgumentException("기획전 탭 정보를 찾을 수 없습니다."));

		// 기획전 상품 API 요청 후 404 응답과 메시지를 검증합니다.
		mockMvc.perform(
			get("/api/shop/exhibition/goods")
				.param("exhibitionNo", "2")
				.param("exhibitionTabNo", "99")
				.param("pageNo", "1")
				.contentType(MediaType.APPLICATION_JSON)
		)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("기획전 탭 정보를 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("기획전 목록 API는 예외 발생 시 500과 에러 메시지를 반환한다")
	// 서비스 예외 발생 시 500 응답으로 변환되는지 검증합니다.
	void getShopExhibitionList_returnsInternalServerErrorWhenExceptionOccurs() throws Exception {
		// 서비스 예외를 발생하도록 목 동작을 설정합니다.
		when(exhibitionService.getShopExhibitionPage(eq(3))).thenThrow(new IllegalStateException("boom"));

		// 기획전 목록 API 요청 후 500 응답과 메시지를 검증합니다.
		mockMvc.perform(get("/api/shop/exhibition/list").param("pageNo", "3").contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.message").value("기획전 목록 조회에 실패했습니다."));
	}
}
