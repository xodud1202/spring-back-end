package com.xodud1202.springbackend.domain.notion;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
// Notion 페이지 조회 결과를 NOTION_DATA_LIST에 저장하기 위한 파라미터 객체입니다.
public class NotionDataListUpsertPO {
	private String id;
	private String databaseId;
	private String dataSourceId;
	private String title;
	private String notes;
	private String url;
	private String notionUrl;
	private String delYn;
	private String categoryId;
	private LocalDateTime createDt;
}
