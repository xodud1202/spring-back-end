package com.xodud1202.springbackend.domain.shop.site;

import lombok.Data;

@Data
// 쇼핑몰 사이트 기본 정보를 전달합니다.
public class ShopSiteInfoVO {
	// 사이트 아이디입니다.
	private String siteId;
	// 사이트명입니다.
	private String siteNm;
}
