package com.xodud1202.springbackend.domain.shop.main;

import lombok.Data;

import java.util.List;

@Data
// 쇼핑몰 메인 섹션 정보를 전달합니다.
public class ShopMainSectionVO {
	// 배너 번호입니다.
	private Integer bannerNo;
	// 배너 구분 코드입니다.
	private String bannerDivCd;
	// 배너명입니다.
	private String bannerNm;
	// 노출 순서입니다.
	private Integer dispOrd;
	// 이미지 배너 아이템 목록입니다.
	private List<ShopMainImageBannerItemVO> imageItems;
	// 상품배너 탭 목록입니다.
	private List<ShopMainGoodsTabVO> tabItems;
	// 상품 목록입니다.
	private List<ShopMainGoodsItemVO> goodsItems;
}
