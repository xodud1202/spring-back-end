package com.xodud1202.springbackend.domain.shop.goods;

import lombok.Data;

@Data
// 상품 상세설명 원본 행 데이터를 전달합니다.
public class ShopGoodsDescItemVO {
	// 상품 코드입니다.
	private String goodsId;
	// 디바이스 구분 코드입니다.
	private String deviceGbCd;
	// 상세설명 HTML입니다.
	private String goodsDesc;
}
