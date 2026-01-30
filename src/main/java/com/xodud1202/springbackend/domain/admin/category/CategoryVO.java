package com.xodud1202.springbackend.domain.admin.category;

import lombok.Data;

@Data
public class CategoryVO {
	private String categoryId;
	private String parentCategoryId;
	private Integer categoryLevel;
	private String categoryNm;
	private Integer dispOrd;
}
