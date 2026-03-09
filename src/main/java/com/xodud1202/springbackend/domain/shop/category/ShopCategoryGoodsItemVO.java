package com.xodud1202.springbackend.domain.shop.category;

import lombok.Data;

@Data
// 쇼핑몰 카테고리 화면 상품 정보를 전달합니다.
public class ShopCategoryGoodsItemVO {
	// 카테고리 아이디입니다.
	private String categoryId;
	// 상품 코드입니다.
	private String goodsId;
	// 상품명입니다.
	private String goodsNm;
	// 브랜드명입니다.
	private String brandNm;
	// 공급 금액입니다.
	private Integer supplyAmt;
	// 판매 금액입니다.
	private Integer saleAmt;
	// 이미지 경로입니다.
	private String imgPath;
	// 이미지 URL입니다.
	private String imgUrl;
	// 두 번째 이미지 경로입니다.
	private String secondaryImgPath;
	// 두 번째 이미지 URL입니다.
	private String secondaryImgUrl;
	// 노출 순서입니다.
	private Integer dispOrd;
}
