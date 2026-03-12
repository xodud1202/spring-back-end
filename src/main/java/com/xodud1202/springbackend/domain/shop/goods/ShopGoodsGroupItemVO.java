package com.xodud1202.springbackend.domain.shop.goods;

import lombok.Data;

@Data
// 동일 그룹 상품(컬러 옵션) 정보를 전달합니다.
public class ShopGoodsGroupItemVO {
	// 상품 코드입니다.
	private String goodsId;
	// ERP 컬러 코드입니다.
	private String erpColorCd;
	// 컬러명입니다.
	private String colorNm;
	// 컬러 RGB 값입니다.
	private String colorRgb;
	// 대표 이미지 경로입니다.
	private String firstImgPath;
	// 대표 이미지 URL입니다.
	private String firstImgUrl;
}
