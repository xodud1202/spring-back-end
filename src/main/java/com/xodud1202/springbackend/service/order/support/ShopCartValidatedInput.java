package com.xodud1202.springbackend.service.order.support;

/**
 * @param goodsId 상품코드입니다.
 * @param sizeId  사이즈코드입니다.
 * @param qty     수량입니다.
 */ // 장바구니 저장 검증 완료 입력값을 전달합니다.
public record ShopCartValidatedInput(String goodsId, String sizeId, int qty) {
	// 검증 완료된 장바구니 입력값을 생성합니다.
	public ShopCartValidatedInput {
	}

	// 상품코드를 반환합니다.
	public String getGoodsId() {
		return goodsId;
	}

	// 사이즈코드를 반환합니다.
	public String getSizeId() {
		return sizeId;
	}

	// 수량을 반환합니다.
	public int getQty() {
		return qty;
	}
}

