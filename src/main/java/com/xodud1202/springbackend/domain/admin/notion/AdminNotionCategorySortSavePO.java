package com.xodud1202.springbackend.domain.admin.notion;

import lombok.Data;

import java.util.List;

@Data
// 관리자 Notion 카테고리 정렬 저장 요청 정보를 전달하는 객체입니다.
public class AdminNotionCategorySortSavePO {
	private List<AdminNotionCategorySortRowPO> rows;
	private Long udtNo;
}
