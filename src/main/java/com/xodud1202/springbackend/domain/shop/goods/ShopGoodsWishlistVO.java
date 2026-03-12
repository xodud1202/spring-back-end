package com.xodud1202.springbackend.domain.shop.goods;

import lombok.Data;

@Data
// 쇼핑몰 상품 위시리스트 상태 정보를 전달합니다.
public class ShopGoodsWishlistVO {
	// 위시리스트 등록 여부입니다.
	private boolean wished;
}
