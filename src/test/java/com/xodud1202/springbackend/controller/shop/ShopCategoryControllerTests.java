package com.xodud1202.springbackend.controller.shop;

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
}
