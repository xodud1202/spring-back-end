package com.xodud1202.springbackend.domain.admin.news;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
// 관리자 뉴스 카테고리 저장 요청 데이터를 보관합니다.
public class AdminNewsCategorySavePO {
	private Long pressNo;
	private List<AdminNewsCategorySaveRowPO> rows;
	private Long regNo;
	private Long udtNo;
}
