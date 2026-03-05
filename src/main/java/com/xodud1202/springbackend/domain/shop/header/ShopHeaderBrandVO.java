package com.xodud1202.springbackend.domain.shop.header;

import lombok.Data;

@Data
// 쇼핑몰 헤더 브랜드 정보를 전달합니다.
public class ShopHeaderBrandVO {
	// 브랜드 번호입니다.
	private Integer brandNo;
	// 브랜드명입니다.
	private String brandNm;
	// 브랜드 로고 경로(또는 URL)입니다.
	private String brandLogoPath;
}
