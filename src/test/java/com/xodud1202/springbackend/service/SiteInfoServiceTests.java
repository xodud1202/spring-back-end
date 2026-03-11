package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.shop.site.ShopSiteInfoVO;
import com.xodud1202.springbackend.mapper.SiteInfoMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// SiteInfoService의 사이트 기본 정보 조회 로직을 검증합니다.
class SiteInfoServiceTests {

	// 사이트 기본 정보 매퍼 목 객체입니다.
	@Mock
	private SiteInfoMapper siteInfoMapper;

	// 테스트 대상 서비스입니다.
	@InjectMocks
	private SiteInfoService siteInfoService;

	@Test
	@DisplayName("사이트 정보 조회: DB 데이터가 있으면 그대로 반환한다")
	// 사이트 기본 정보가 존재할 때 조회 결과를 그대로 반환합니다.
	void getShopSiteInfo_returnsMapperDataWhenExists() {
		// 매퍼 응답 데이터를 구성합니다.
		ShopSiteInfoVO siteInfo = new ShopSiteInfoVO();
		siteInfo.setSiteId("xodud1202");
		siteInfo.setSiteNm("xodud1202");
		when(siteInfoMapper.getShopSiteInfo("xodud1202")).thenReturn(siteInfo);

		// 서비스 조회 결과를 검증합니다.
		ShopSiteInfoVO result = siteInfoService.getShopSiteInfo();
		assertEquals("xodud1202", result.getSiteId());
		assertEquals("xodud1202", result.getSiteNm());
	}

	@Test
	@DisplayName("사이트 정보 조회: DB 데이터가 없으면 기본 응답을 반환한다")
	// 사이트 기본 정보가 없을 때 기본 응답 객체를 반환합니다.
	void getShopSiteInfo_returnsFallbackWhenMissing() {
		// 매퍼 응답을 null로 고정합니다.
		when(siteInfoMapper.getShopSiteInfo("xodud1202")).thenReturn(null);

		// 서비스 조회 결과를 검증합니다.
		ShopSiteInfoVO result = siteInfoService.getShopSiteInfo();
		assertEquals("xodud1202", result.getSiteId());
		assertEquals("", result.getSiteNm());
	}
}
