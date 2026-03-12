package com.xodud1202.springbackend.domain.shop.goods;

import lombok.Data;

@Data
// 쇼핑몰 상품상세의 기기별 설명 데이터를 전달합니다.
public class ShopGoodsDescVO {
	// PC 상세설명입니다.
	private String pcDesc;
	// MO 상세설명입니다.
	private String moDesc;
}
