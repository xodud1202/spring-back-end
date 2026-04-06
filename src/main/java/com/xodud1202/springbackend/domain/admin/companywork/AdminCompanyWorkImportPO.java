package com.xodud1202.springbackend.domain.admin.companywork;

import lombok.Data;

@Data
// 관리자 회사 업무 Jira 가져오기 요청 정보를 정의합니다.
public class AdminCompanyWorkImportPO {
	// 회사 번호입니다.
	private Integer workCompanySeq;
	// 프로젝트 번호입니다.
	private Integer workCompanyProjectSeq;
	// 업무 키입니다.
	private String workKey;
	// 등록자 번호입니다.
	private Long regNo;
	// 수정자 번호입니다.
	private Long udtNo;
}
