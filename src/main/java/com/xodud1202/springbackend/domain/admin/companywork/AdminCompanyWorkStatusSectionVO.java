package com.xodud1202.springbackend.domain.admin.companywork;

import lombok.Data;

import java.util.List;

@Data
// 관리자 회사 업무 상태별 영역 정보를 정의합니다.
public class AdminCompanyWorkStatusSectionVO {
	// 업무 상태 코드입니다.
	private String workStatCd;
	// 해당 상태의 업무 목록입니다.
	private List<AdminCompanyWorkListRowVO> list;
}
