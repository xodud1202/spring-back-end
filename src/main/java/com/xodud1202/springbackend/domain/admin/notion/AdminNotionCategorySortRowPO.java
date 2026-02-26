package com.xodud1202.springbackend.domain.admin.notion;

import lombok.Data;

@Data
// 관리자 Notion 카테고리 정렬 저장 시 단건 카테고리 식별자를 전달하는 객체입니다.
public class AdminNotionCategorySortRowPO {
	private String categoryId;
}
