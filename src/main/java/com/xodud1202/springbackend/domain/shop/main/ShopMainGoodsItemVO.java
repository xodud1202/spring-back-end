package com.xodud1202.springbackend.domain.shop.main;

import lombok.Data;

@Data
// 쇼핑몰 메인 상품 정보를 전달합니다.
public class ShopMainGoodsItemVO {
	// 배너 번호입니다.
	private Integer bannerNo;
	// 배너 탭 번호입니다.
	private Integer bannerTabNo;
	// 상품 코드입니다.
	private String goodsId;
	// 상품명입니다.
	private String goodsNm;
	// 브랜드명입니다.
	private String brandNm;
	// 판매 금액입니다.
	private Integer saleAmt;
	// 상품 이미지 경로입니다.
	private String imgPath;
	// 상품 이미지 URL입니다.
	private String imgUrl;
	// 두 번째 상품 이미지 경로입니다.
	private String secondaryImgPath;
	// 두 번째 상품 이미지 URL입니다.
	private String secondaryImgUrl;
	// 노출 순서입니다.
	private Integer dispOrd;
}
