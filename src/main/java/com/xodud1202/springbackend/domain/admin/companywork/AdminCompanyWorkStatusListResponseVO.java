package com.xodud1202.springbackend.domain.admin.companywork;

import lombok.Data;

import java.util.List;

@Data
// 관리자 회사 업무 상태별 목록 응답을 정의합니다.
public class AdminCompanyWorkStatusListResponseVO {
	// 상태별 영역 목록입니다.
	private List<AdminCompanyWorkStatusSectionVO> statusSectionList;
}
