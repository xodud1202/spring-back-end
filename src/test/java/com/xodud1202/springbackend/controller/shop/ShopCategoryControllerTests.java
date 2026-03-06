package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.domain.shop.category.ShopCategoryPageVO;
import com.xodud1202.springbackend.domain.shop.header.ShopHeaderCategoryTreeVO;
import com.xodud1202.springbackend.service.CategoryService;
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
// 쇼핑몰 카테고리 컨트롤러 API 응답을 검증합니다.
class ShopCategoryControllerTests {
	@Mock
	private CategoryService categoryService;

	@InjectMocks
	private ShopCategoryController shopCategoryController;

	private MockMvc mockMvc;

	@BeforeEach
	// 컨트롤러 단위 검증용 MockMvc를 초기화합니다.
	void setUp() {
		// 단일 컨트롤러 기준 standalone MockMvc를 생성합니다.
		mockMvc = MockMvcBuilders.standaloneSetup(shopCategoryController).build();
	}

	@Test
	@DisplayName("카테고리 트리 API는 정상 조회 시 200과 트리 데이터를 반환한다")
	// 카테고리 트리 API 응답의 정상 상태를 확인합니다.
	void getShopHeaderCategories_returnsOk() throws Exception {
		// 서비스 반환용 카테고리 트리 응답 객체를 생성합니다.
		ShopHeaderCategoryTreeVO level1 = new ShopHeaderCategoryTreeVO();
		level1.setCategoryId("10");
		level1.setCategoryNm("남성");
		level1.setCategoryLevel(1);
		level1.setChildren(List.of());
		when(categoryService.getShopHeaderCategoryTree()).thenReturn(List.of(level1));

		// 카테고리 트리 API 요청 후 200 응답과 데이터 필드를 검증합니다.
		mockMvc.perform(get("/api/shop/header/categories").contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].categoryId").value("10"))
			.andExpect(jsonPath("$[0].categoryNm").value("남성"));
	}

	@Test
	@DisplayName("카테고리 트리 API는 예외 발생 시 500과 에러 메시지를 반환한다")
	// 서비스 예외 발생 시 500 응답으로 변환되는지 확인합니다.
	void getShopHeaderCategories_returnsInternalServerErrorWhenExceptionOccurs() throws Exception {
		// 서비스 예외를 발생하도록 목 동작을 설정합니다.
		when(categoryService.getShopHeaderCategoryTree()).thenThrow(new IllegalStateException("boom"));

		// 카테고리 트리 API 요청 후 500 응답과 메시지를 검증합니다.
		mockMvc.perform(get("/api/shop/header/categories").contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.message").value("헤더 카테고리 조회에 실패했습니다."));
	}

	@Test
	@DisplayName("카테고리 페이지 API는 정상 조회 시 200과 선택 카테고리 데이터를 반환한다")
	// 카테고리 페이지 API 응답의 정상 상태를 확인합니다.
	void getShopCategoryPage_returnsOk() throws Exception {
		// 서비스 반환용 카테고리 페이지 응답 객체를 생성합니다.
		ShopCategoryPageVO pageVO = new ShopCategoryPageVO();
		pageVO.setCategoryTree(List.of());
		pageVO.setSelectedCategoryId("100001");
		pageVO.setSelectedCategoryNm("아우터");
		pageVO.setGoodsList(List.of());
		pageVO.setGoodsCount(25);
		pageVO.setPageNo(2);
		pageVO.setPageSize(20);
		pageVO.setTotalPageCount(3);
		when(categoryService.getShopCategoryPage("100001", 2)).thenReturn(pageVO);

		// 카테고리 페이지 API 요청 후 200 응답과 데이터 필드를 검증합니다.
		mockMvc.perform(
				get("/api/shop/category/page")
					.param("categoryId", "100001")
					.param("pageNo", "2")
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.selectedCategoryId").value("100001"))
			.andExpect(jsonPath("$.selectedCategoryNm").value("아우터"))
			.andExpect(jsonPath("$.goodsCount").value(25))
			.andExpect(jsonPath("$.pageNo").value(2))
			.andExpect(jsonPath("$.pageSize").value(20))
			.andExpect(jsonPath("$.totalPageCount").value(3));
	}

	@Test
	@DisplayName("카테고리 페이지 API는 pageNo 없이 요청되면 null pageNo로 서비스에 전달한다")
	// 선택 파라미터 pageNo 생략 시 컨트롤러 전달값을 검증합니다.
	void getShopCategoryPage_passesNullPageNoWhenMissing() throws Exception {
		// pageNo 미지정 요청의 목 동작을 설정합니다.
		ShopCategoryPageVO pageVO = new ShopCategoryPageVO();
		pageVO.setCategoryTree(List.of());
		pageVO.setSelectedCategoryId("100001");
		pageVO.setSelectedCategoryNm("아우터");
		pageVO.setGoodsList(List.of());
		pageVO.setGoodsCount(0);
		pageVO.setPageNo(1);
		pageVO.setPageSize(20);
		pageVO.setTotalPageCount(0);
		when(categoryService.getShopCategoryPage("100001", null)).thenReturn(pageVO);

		// pageNo 없이 요청하고 서비스 호출 인자를 검증합니다.
		mockMvc.perform(get("/api/shop/category/page").param("categoryId", "100001").contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());
		verify(categoryService).getShopCategoryPage(eq("100001"), isNull());
	}

	@Test
	@DisplayName("카테고리 페이지 API는 예외 발생 시 500과 에러 메시지를 반환한다")
	// 카테고리 페이지 서비스 예외 발생 시 500 응답으로 변환되는지 확인합니다.
	void getShopCategoryPage_returnsInternalServerErrorWhenExceptionOccurs() throws Exception {
		// 서비스 예외를 발생하도록 목 동작을 설정합니다.
		when(categoryService.getShopCategoryPage("999999", 9)).thenThrow(new IllegalStateException("boom"));

		// 카테고리 페이지 API 요청 후 500 응답과 메시지를 검증합니다.
		mockMvc.perform(
				get("/api/shop/category/page")
					.param("categoryId", "999999")
					.param("pageNo", "9")
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.message").value("카테고리 페이지 조회에 실패했습니다."));
	}
}