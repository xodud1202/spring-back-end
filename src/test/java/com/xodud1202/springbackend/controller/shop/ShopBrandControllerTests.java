package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.domain.shop.header.ShopHeaderBrandVO;
import com.xodud1202.springbackend.service.BrandService;
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
// 쇼핑몰 브랜드 컨트롤러 API 응답을 검증합니다.
class ShopBrandControllerTests {
	@Mock
	private BrandService brandService;

	@InjectMocks
	private ShopBrandController shopBrandController;

	private MockMvc mockMvc;

	@BeforeEach
	// 컨트롤러 단위 검증용 MockMvc를 초기화합니다.
	void setUp() {
		// 단일 컨트롤러 기준 standalone MockMvc를 생성합니다.
		mockMvc = MockMvcBuilders.standaloneSetup(shopBrandController).build();
	}

	@Test
	@DisplayName("브랜드 목록 API는 정상 조회 시 200과 브랜드 데이터를 반환한다")
	// 브랜드 목록 API 응답의 정상 상태를 확인합니다.
	void getShopHeaderBrands_returnsOk() throws Exception {
		// 서비스 반환용 브랜드 응답 객체를 생성합니다.
		ShopHeaderBrandVO brand = new ShopHeaderBrandVO();
		brand.setBrandNo(1);
		brand.setBrandNm("xodud1202");
		when(brandService.getShopHeaderBrandList()).thenReturn(List.of(brand));

		// 브랜드 목록 API 요청 후 200 응답과 데이터 필드를 검증합니다.
		mockMvc.perform(get("/api/shop/header/brands").contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].brandNo").value(1))
			.andExpect(jsonPath("$[0].brandNm").value("xodud1202"));
	}

	@Test
	@DisplayName("브랜드 목록 API는 예외 발생 시 500과 에러 메시지를 반환한다")
	// 서비스 예외 발생 시 500 응답으로 변환되는지 확인합니다.
	void getShopHeaderBrands_returnsInternalServerErrorWhenExceptionOccurs() throws Exception {
		// 서비스 예외를 발생하도록 목 동작을 설정합니다.
		when(brandService.getShopHeaderBrandList()).thenThrow(new IllegalStateException("boom"));

		// 브랜드 목록 API 요청 후 500 응답과 메시지를 검증합니다.
		mockMvc.perform(get("/api/shop/header/brands").contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.message").value("헤더 브랜드 조회에 실패했습니다."));
	}
}
