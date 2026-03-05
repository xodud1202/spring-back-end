package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.domain.shop.main.ShopMainSectionVO;
import com.xodud1202.springbackend.service.BannerService;
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
// 쇼핑몰 배너 컨트롤러 API 응답을 검증합니다.
class ShopBannerControllerTests {
	@Mock
	private BannerService bannerService;

	@InjectMocks
	private ShopBannerController shopBannerController;

	private MockMvc mockMvc;

	@BeforeEach
	// 컨트롤러 단위 검증용 MockMvc를 초기화합니다.
	void setUp() {
		// 단일 컨트롤러 기준 standalone MockMvc를 생성합니다.
		mockMvc = MockMvcBuilders.standaloneSetup(shopBannerController).build();
	}

	@Test
	@DisplayName("배너 섹션 API는 정상 조회 시 200과 섹션 목록을 반환한다")
	// 배너 섹션 API 정상 응답 구조를 검증합니다.
	void getShopBannerSections_returnsOk() throws Exception {
		// 서비스 반환용 섹션 데이터를 구성합니다.
		ShopMainSectionVO section = new ShopMainSectionVO();
		section.setBannerNo(1);
		section.setBannerDivCd("BANNER_DIV_01");
		section.setBannerNm("메인 대배너");
		when(bannerService.getShopMainSectionList()).thenReturn(List.of(section));

		// 배너 섹션 API 요청 후 200 응답과 sections 필드를 검증합니다.
		mockMvc.perform(get("/api/shop/main/sections").contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.sections[0].bannerNo").value(1))
			.andExpect(jsonPath("$.sections[0].bannerDivCd").value("BANNER_DIV_01"));
	}

	@Test
	@DisplayName("배너 섹션 API는 예외 발생 시 500과 에러 메시지를 반환한다")
	// 서비스 예외 발생 시 500 응답으로 변환되는지 검증합니다.
	void getShopBannerSections_returnsInternalServerErrorWhenExceptionOccurs() throws Exception {
		// 서비스 예외를 발생하도록 목 동작을 설정합니다.
		when(bannerService.getShopMainSectionList()).thenThrow(new IllegalStateException("boom"));

		// 배너 섹션 API 요청 후 500 응답과 메시지를 검증합니다.
		mockMvc.perform(get("/api/shop/main/sections").contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.message").value("쇼핑 메인 배너 조회에 실패했습니다."));
	}
}
