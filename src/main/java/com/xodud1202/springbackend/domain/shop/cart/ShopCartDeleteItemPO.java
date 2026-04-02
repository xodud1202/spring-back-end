package com.xodud1202.springbackend.domain.shop.cart;

import lombok.Data;

@Data
// 쇼핑몰 장바구니 선택 삭제 대상 1건을 전달합니다.
public class ShopCartDeleteItemPO {
	// 삭제 대상 장바구니번호입니다.
	private Long cartId;
	// 삭제 대상 상품코드입니다.
	private String goodsId;
	// 삭제 대상 사이즈 코드입니다.
	private String sizeId;
}
