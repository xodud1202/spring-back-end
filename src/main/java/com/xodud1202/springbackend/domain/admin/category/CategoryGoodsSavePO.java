package com.xodud1202.springbackend.domain.admin.category;

import lombok.Data;

// 카테고리 상품 저장 요청 정보를 전달합니다.
@Data
public class CategoryGoodsSavePO {
	// 카테고리코드입니다.
	private String categoryId;
	// 상품코드입니다.
	private String goodsId;
	// 정렬순서입니다.
	private Integer dispOrd;
	// 등록자 번호입니다.
	private Long regNo;
	// 수정자 번호입니다.
	private Long udtNo;
}
