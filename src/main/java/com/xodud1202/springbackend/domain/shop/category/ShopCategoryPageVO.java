package com.xodud1202.springbackend.domain.shop.category;

import com.xodud1202.springbackend.domain.shop.header.ShopHeaderCategoryTreeVO;
import lombok.Data;

import java.util.List;

@Data
// 쇼핑몰 카테고리 화면 응답 데이터를 전달합니다.
public class ShopCategoryPageVO {
	// 카테고리 트리 목록입니다.
	private List<ShopHeaderCategoryTreeVO> categoryTree;
	// 선택 카테고리 아이디입니다.
	private String selectedCategoryId;
	// 선택 카테고리명입니다.
	private String selectedCategoryNm;
	// 선택 카테고리 상품 목록입니다.
	private List<ShopCategoryGoodsItemVO> goodsList;
	// 선택 카테고리 상품 건수입니다.
	private Integer goodsCount;
}
