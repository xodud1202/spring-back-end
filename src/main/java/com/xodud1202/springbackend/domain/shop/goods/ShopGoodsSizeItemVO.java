package com.xodud1202.springbackend.domain.shop.goods;

import lombok.Data;

@Data
// 쇼핑몰 상품상세 사이즈/재고 정보를 전달합니다.
public class ShopGoodsSizeItemVO {
	// 상품 코드입니다.
	private String goodsId;
	// 사이즈 코드입니다.
	private String sizeId;
	// 재고 수량입니다.
	private Integer stockQty;
	// 추가 금액입니다.
	private Integer addAmt;
	// 노출 순서입니다.
	private Integer dispOrd;
	// 품절 여부입니다.
	private boolean soldOut;
}
