package com.xodud1202.springbackend.domain.admin.companywork;

import lombok.Data;

@Data
// 관리자 회사 업무 상세 저장 요청 정보를 정의합니다.
public class AdminCompanyWorkDetailUpdatePO {
	// 업무 시퀀스입니다.
	private Long workSeq;
	// 업무 상태 코드입니다.
	private String workStatCd;
	// 업무 시작 일시입니다.
	private String workStartDt;
	// 업무 종료 일시입니다.
	private String workEndDt;
	// 업무 공수시간입니다.
	private Integer workTime;
	// 수정자 번호입니다.
	private Long udtNo;
}
