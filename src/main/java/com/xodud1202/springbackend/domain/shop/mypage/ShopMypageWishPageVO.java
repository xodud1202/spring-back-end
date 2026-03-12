package com.xodud1202.springbackend.domain.shop.mypage;

import lombok.Data;

import java.util.List;

@Data
// 쇼핑몰 마이페이지 위시리스트 페이지 응답을 전달합니다.
public class ShopMypageWishPageVO {
	// 위시리스트 상품 목록입니다.
	private List<ShopMypageWishGoodsItemVO> goodsList;
	// 위시리스트 전체 건수입니다.
	private Integer goodsCount;
	// 현재 페이지 번호입니다.
	private Integer pageNo;
	// 페이지 크기입니다.
	private Integer pageSize;
	// 전체 페이지 수입니다.
	private Integer totalPageCount;
}
