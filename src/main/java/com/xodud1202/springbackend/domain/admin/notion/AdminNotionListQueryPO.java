package com.xodud1202.springbackend.domain.admin.notion;

import lombok.Data;

@Data
// 관리자 Notion 저장 목록 조회 조건을 전달하는 파라미터 객체입니다.
public class AdminNotionListQueryPO {
	private String categoryId;
	private String title;
	private String createDtStart;
	private String createDtEnd;
	private Integer page;
	private Integer pageSize;
	private Integer offset;
}
