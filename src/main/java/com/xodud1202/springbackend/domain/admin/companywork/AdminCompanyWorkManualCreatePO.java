package com.xodud1202.springbackend.domain.admin.companywork;

import lombok.Data;

@Data
// 관리자 회사 업무 수기 등록 요청 정보를 정의합니다.
public class AdminCompanyWorkManualCreatePO {
	// 회사 번호입니다.
	private Integer workCompanySeq;
	// 프로젝트 번호입니다.
	private Integer workCompanyProjectSeq;
	// 업무 타이틀입니다.
	private String title;
	// 업무 본문 HTML입니다.
	private String content;
	// 업무 담당자명입니다.
	private String coManager;
	// 업무 우선순위 코드입니다.
	private String workPriorCd;
	// 등록자 번호입니다.
	private Long regNo;
	// 수정자 번호입니다.
	private Long udtNo;
}
