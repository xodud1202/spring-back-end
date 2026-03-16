package com.xodud1202.springbackend.domain.shop.cart;

import lombok.Data;

@Data
// 쇼핑몰 장바구니 상품의 선택 가능한 사이즈 옵션을 전달합니다.
public class ShopCartSizeOptionVO {
	// 사이즈 코드입니다.
	private String sizeId;
	// 사이즈 재고 수량입니다.
	private Integer stockQty;
	// 사이즈 품절 여부입니다.
	private boolean soldOut;
}
