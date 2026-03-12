package com.xodud1202.springbackend.domain.shop.goods;

import lombok.Data;

@Data
// 쇼핑몰 상품상세 이미지 정보를 전달합니다.
public class ShopGoodsImageVO {
	// 이미지 번호입니다.
	private Integer imgNo;
	// 상품 코드입니다.
	private String goodsId;
	// 노출 순서입니다.
	private Integer dispOrd;
	// 원본 이미지 경로입니다.
	private String imgPath;
	// 화면 표시용 이미지 URL입니다.
	private String imgUrl;
}
