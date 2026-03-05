package com.xodud1202.springbackend.domain.shop.main;

import lombok.Data;

@Data
// 쇼핑몰 메인 이미지 배너 아이템 정보를 전달합니다.
public class ShopMainImageBannerItemVO {
	// 이미지 배너 번호입니다.
	private Long imageBannerNo;
	// 배너 번호입니다.
	private Integer bannerNo;
	// 이미지 배너명입니다.
	private String bannerNm;
	// 이미지 경로입니다.
	private String imgPath;
	// 이동 URL입니다.
	private String url;
	// 배너 오픈 구분 코드입니다.
	private String bannerOpenCd;
	// 노출 순서입니다.
	private Integer dispOrd;
}
