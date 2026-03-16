package com.xodud1202.springbackend.domain.shop.cart;

import lombok.Data;

import java.util.List;

@Data
// 쇼핑몰 장바구니 페이지 응답 데이터를 전달합니다.
public class ShopCartPageVO {
	// 장바구니 상품 목록입니다.
	private List<ShopCartItemVO> cartList;
	// 장바구니 상품 건수입니다.
	private Integer cartCount;
	// 배송비 기준 사이트 정보입니다.
	private ShopCartSiteInfoVO siteInfo;
}
