package com.xodud1202.springbackend.domain.admin.category;

import lombok.Data;

import java.util.List;

// 카테고리 상품 정렬 순서 저장 요청 정보를 전달합니다.
@Data
public class CategoryGoodsOrderSavePO {
	// 카테고리코드입니다.
	private String categoryId;
	// 정렬 순서 목록입니다.
	private List<CategoryGoodsOrderItem> orders;
	// 수정자 번호입니다.
	private Long udtNo;
}
