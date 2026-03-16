package com.xodud1202.springbackend.domain.shop.cart;

import lombok.Data;

@Data
// 쇼핑몰 장바구니 배송 기준 정보를 전달합니다.
public class ShopCartSiteInfoVO {
	// 사이트 아이디입니다.
	private String siteId;
	// 기본 배송비입니다.
	private Integer deliveryFee;
	// 무료배송 기준 금액입니다.
	private Integer deliveryFeeLimit;
}
