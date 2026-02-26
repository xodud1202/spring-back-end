package com.xodud1202.springbackend.domain.notion;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// Notion 카테고리 정보를 NOTION_CATEGORY에 저장하기 위한 파라미터 객체입니다.
public class NotionCategoryUpsertPO {
	private String categoryId;
	private String categoryNm;
	private String color;
}
