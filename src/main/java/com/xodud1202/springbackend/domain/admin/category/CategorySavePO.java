package com.xodud1202.springbackend.domain.admin.category;

import lombok.Data;

@Data
// 카테고리 저장 요청 정보를 전달합니다.
public class CategorySavePO {
	private String categoryId;
	private String parentCategoryId;
	private Integer categoryLevel;
	private String categoryNm;
	private Integer dispOrd;
	private String showYn;
	private Long regNo;
	private Long udtNo;
}
