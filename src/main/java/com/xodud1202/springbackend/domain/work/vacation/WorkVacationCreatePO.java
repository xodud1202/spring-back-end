package com.xodud1202.springbackend.domain.work.vacation;

import lombok.Data;

@Data
// 휴가 등록 요청 정보를 정의합니다.
public class WorkVacationCreatePO {
	// 등록된 휴가 번호입니다.
	private Long vacationSeq;
	// 회사 번호입니다.
	private Integer workCompanySeq;
	// 휴가자 번호입니다.
	private Integer personSeq;
	// 휴가 구분 코드입니다.
	private String vacationCd;
	// 휴가 시작일입니다.
	private String startDt;
	// 휴가 종료일입니다.
	private String endDt;
	// 휴가 사유입니다.
	private String vacationMemo;
	// 등록자 번호입니다.
	private Long regNo;
	// 수정자 번호입니다.
	private Long udtNo;
}
