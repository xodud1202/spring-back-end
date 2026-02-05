package com.xodud1202.springbackend.domain.admin.category;

import lombok.Data;

import java.util.List;

// 카테고리 상품 삭제 요청 정보를 전달합니다.
@Data
public class CategoryGoodsDeletePO {
	// 카테고리코드입니다.
	private String categoryId;
	// 삭제할 상품코드 목록입니다.
	private List<String> goodsIds;
	// 수정자 번호입니다.
	private Long udtNo;
}
