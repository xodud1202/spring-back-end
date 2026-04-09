package com.xodud1202.springbackend.domain.admin.companywork;

import lombok.Data;

@Data
// 관리자 회사 업무 수기 등록 저장 파라미터를 정의합니다.
public class AdminCompanyWorkManualSavePO {
	// 저장된 업무 번호입니다.
	private Long workSeq;
	// 회사 번호입니다.
	private Integer workCompanySeq;
	// 프로젝트 번호입니다.
	private Integer workCompanyProjectSeq;
	// 업무 상태 코드입니다.
	private String workStatCd;
	// 업무 우선순위 코드입니다.
	private String workPriorCd;
	// 업무 키입니다.
	private String workKey;
	// 업무 타이틀입니다.
	private String title;
	// 업무 본문 HTML입니다.
	private String content;
	// 업무 담당자명입니다.
	private String coManager;
	// 등록자 번호입니다.
	private Long regNo;
	// 수정자 번호입니다.
	private Long udtNo;
}
