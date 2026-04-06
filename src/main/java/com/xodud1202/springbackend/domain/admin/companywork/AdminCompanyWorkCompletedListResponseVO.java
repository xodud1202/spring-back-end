package com.xodud1202.springbackend.domain.admin.companywork;

import lombok.Data;

import java.util.List;

@Data
// 관리자 회사 업무 완료 목록 응답을 정의합니다.
public class AdminCompanyWorkCompletedListResponseVO {
	// 완료 업무 목록입니다.
	private List<AdminCompanyWorkListRowVO> list;
	// 전체 건수입니다.
	private Integer totalCount;
	// 현재 페이지입니다.
	private Integer page;
	// 페이지 크기입니다.
	private Integer pageSize;
}
