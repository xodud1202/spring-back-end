package com.xodud1202.springbackend.service;

import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_SITE_ID;

import com.xodud1202.springbackend.domain.shop.site.ShopSiteInfoVO;
import com.xodud1202.springbackend.mapper.SiteInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 쇼핑몰 사이트 기본 정보 비즈니스 로직을 처리합니다.
@Service
@RequiredArgsConstructor
public class SiteInfoService {
	private final SiteInfoMapper siteInfoMapper;

	// 로그인/상품상세 등 쇼핑몰 공통 화면에 필요한 사이트 기본 정보를 조회합니다.
	public ShopSiteInfoVO getShopSiteInfo() {
		// 고정 사이트 아이디 기준 기본 정보를 단건 조회합니다.
		ShopSiteInfoVO siteInfo = siteInfoMapper.getShopSiteInfo(SHOP_SITE_ID);
		if (siteInfo != null) {
			return siteInfo;
		}

		// 조회 결과가 없으면 화면에서 사용할 기본 응답을 반환합니다.
		ShopSiteInfoVO fallback = new ShopSiteInfoVO();
		fallback.setSiteId(SHOP_SITE_ID);
		fallback.setSiteNm("");
		return fallback;
	}
}
