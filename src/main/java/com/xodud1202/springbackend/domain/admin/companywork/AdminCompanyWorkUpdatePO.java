package com.xodud1202.springbackend.domain.admin.companywork;

import lombok.Data;

@Data
// 관리자 회사 업무 수정 요청 정보를 정의합니다.
public class AdminCompanyWorkUpdatePO {
	// 업무 시퀀스입니다.
	private Long workSeq;
	// 변경할 업무 상태 코드입니다.
	private String workStatCd;
	// 변경할 업무 시작일입니다.
	private String workStartDt;
	// 변경할 업무 종료일입니다.
	private String workEndDt;
	// 변경할 IT 담당자명입니다.
	private String itManager;
	// 수정자 번호입니다.
	private Long udtNo;
}
