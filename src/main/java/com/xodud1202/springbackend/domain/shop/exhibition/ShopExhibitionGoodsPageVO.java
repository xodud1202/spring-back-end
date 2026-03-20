package com.xodud1202.springbackend.domain.shop.exhibition;

import lombok.Data;

import java.util.List;

@Data
// 쇼핑몰 기획전 탭 상품 더보기 응답 데이터를 전달합니다.
public class ShopExhibitionGoodsPageVO {
	// 상품 목록입니다.
	private List<ShopExhibitionGoodsItemVO> goodsList;
	// 전체 상품 건수입니다.
	private Integer totalCount;
	// 현재 페이지 번호입니다.
	private Integer pageNo;
	// 페이지 크기입니다.
	private Integer pageSize;
	// 다음 페이지가 존재하는지 여부입니다.
	private Boolean hasMore;
	// 다음 페이지 번호입니다.
	private Integer nextPageNo;
}
