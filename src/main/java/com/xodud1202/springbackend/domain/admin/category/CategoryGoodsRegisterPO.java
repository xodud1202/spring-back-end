package com.xodud1202.springbackend.domain.admin.category;

import lombok.Data;

import java.util.List;

// 카테고리 상품 등록 요청 정보를 전달합니다.
@Data
public class CategoryGoodsRegisterPO {
	// 카테고리코드입니다.
	private String categoryId;
	// 등록할 상품코드 목록입니다.
	private List<String> goodsIds;
	// 등록자 번호입니다.
	private Long regNo;
	// 수정자 번호입니다.
	private Long udtNo;
}
