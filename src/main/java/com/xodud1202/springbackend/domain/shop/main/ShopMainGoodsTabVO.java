package com.xodud1202.springbackend.domain.shop.main;

import lombok.Data;

import java.util.List;

@Data
// 쇼핑몰 메인 상품배너 탭 정보를 전달합니다.
public class ShopMainGoodsTabVO {
	// 배너 탭 번호입니다.
	private Integer bannerTabNo;
	// 배너 번호입니다.
	private Integer bannerNo;
	// 탭명입니다.
	private String tabNm;
	// 노출 순서입니다.
	private Integer dispOrd;
	// 탭별 상품 목록입니다.
	private List<ShopMainGoodsItemVO> goodsItems;
}
