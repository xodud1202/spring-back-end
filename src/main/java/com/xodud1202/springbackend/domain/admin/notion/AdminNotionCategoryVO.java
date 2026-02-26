package com.xodud1202.springbackend.domain.admin.notion;

import lombok.Data;

@Data
// 관리자 Notion 카테고리 목록 정보를 전달하는 객체입니다.
public class AdminNotionCategoryVO {
	private String categoryId;
	private String categoryNm;
	private String color;
	private Integer sortSeq;
	private String regDt;
}
