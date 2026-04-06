package com.xodud1202.springbackend.domain.admin.companywork;

import lombok.Data;

@Data
// 관리자 회사 업무 프로젝트 선택 항목을 정의합니다.
public class AdminCompanyWorkProjectVO {
	// 프로젝트 번호입니다.
	private Integer workCompanyProjectSeq;
	// 회사 번호입니다.
	private Integer workCompanySeq;
	// 프로젝트명입니다.
	private String workCompanyProjectNm;
	// 표시 순서입니다.
	private Integer dispOrd;
}
