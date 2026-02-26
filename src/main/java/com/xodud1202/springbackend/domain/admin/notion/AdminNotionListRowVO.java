package com.xodud1202.springbackend.domain.admin.notion;

import lombok.Data;

@Data
// 관리자 Notion 저장 목록 화면에 표시할 행 데이터를 전달하는 객체입니다.
public class AdminNotionListRowVO {
	private String id;
	private String categoryId;
	private String categoryNm;
	private String title;
	private String notes;
	private String url;
	private String notionUrl;
	private String createDt;
}
