package com.xodud1202.springbackend.domain.admin.companywork;

import lombok.Data;

@Data
// 관리자 회사 업무 조회 조건을 정의합니다.
public class AdminCompanyWorkSearchPO {
	// 회사 번호입니다.
	private Integer workCompanySeq;
	// 프로젝트 번호입니다.
	private Integer workCompanyProjectSeq;
	// 타이틀 검색어입니다.
	private String title;
	// 본문 포함 검색 여부입니다.
	private String includeBodyYn;
	// 선택 상태 코드 목록입니다.
	private java.util.List<String> workStatCdList;
	// 현재 페이지입니다.
	private Integer page;
	// 페이지 크기입니다.
	private Integer pageSize;
	// 조회 시작 오프셋입니다.
	private Integer offset;
}
