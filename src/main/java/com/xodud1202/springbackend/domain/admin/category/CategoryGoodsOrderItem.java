package com.xodud1202.springbackend.domain.admin.category;

import lombok.Data;

// 카테고리 상품 정렬 순서 항목을 전달합니다.
@Data
public class CategoryGoodsOrderItem {
	// 상품코드입니다.
	private String goodsId;
	// 정렬순서입니다.
	private Integer dispOrd;
}
