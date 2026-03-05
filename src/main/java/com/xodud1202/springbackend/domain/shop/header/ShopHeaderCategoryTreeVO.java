package com.xodud1202.springbackend.domain.shop.header;

import lombok.Data;

import java.util.List;

@Data
// 쇼핑몰 헤더 카테고리 트리 정보를 전달합니다.
public class ShopHeaderCategoryTreeVO {
	// 카테고리 아이디입니다.
	private String categoryId;
	// 카테고리명입니다.
	private String categoryNm;
	// 카테고리 레벨(1/2/3)입니다.
	private Integer categoryLevel;
	// 정렬 순서입니다.
	private Integer dispOrd;
	// 하위 카테고리 목록입니다.
	private List<ShopHeaderCategoryTreeVO> children;
}
