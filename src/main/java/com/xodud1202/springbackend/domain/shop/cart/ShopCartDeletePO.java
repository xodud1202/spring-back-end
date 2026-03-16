package com.xodud1202.springbackend.domain.shop.cart;

import lombok.Data;

import java.util.List;

@Data
// 쇼핑몰 장바구니 선택 삭제 요청 데이터를 전달합니다.
public class ShopCartDeletePO {
	// 선택 삭제할 장바구니 상품 목록입니다.
	private List<ShopCartDeleteItemPO> cartItemList;
}
