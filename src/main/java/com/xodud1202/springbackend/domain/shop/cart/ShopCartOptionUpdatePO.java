package com.xodud1202.springbackend.domain.shop.cart;

import lombok.Data;

@Data
// 쇼핑몰 장바구니 옵션 변경 요청 데이터를 전달합니다.
public class ShopCartOptionUpdatePO {
	// 변경 대상 상품코드입니다.
	private String goodsId;
	// 변경 대상 현재 사이즈 코드입니다.
	private String sizeId;
	// 변경할 목표 사이즈 코드입니다.
	private String targetSizeId;
	// 변경할 목표 수량입니다.
	private Integer qty;
}
