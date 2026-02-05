package com.xodud1202.springbackend.domain.admin.category;

import lombok.Data;

// 카테고리별 상품 정보를 전달합니다.
@Data
public class CategoryGoodsVO {
	// 카테고리코드입니다.
	private String categoryId;
	// 상품코드입니다.
	private String goodsId;
	// 정렬순서입니다.
	private Integer dispOrd;
	// 품번코드입니다.
	private String erpStyleCd;
	// 상품명입니다.
	private String goodsNm;
	// 상품상태 코드입니다.
	private String goodsStatCd;
	// 상품상태명입니다.
	private String goodsStatNm;
	// 상품구분 코드입니다.
	private String goodsDivCd;
	// 상품구분명입니다.
	private String goodsDivNm;
	// 상품 이미지 경로입니다.
	private String imgPath;
	// 상품 이미지 URL입니다.
	private String imgUrl;
}
