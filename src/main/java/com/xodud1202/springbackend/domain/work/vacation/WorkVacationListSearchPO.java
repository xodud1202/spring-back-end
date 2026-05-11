package com.xodud1202.springbackend.domain.work.vacation;

import lombok.Data;

@Data
// 휴가관리 목록 검색 조건을 정의합니다.
public class WorkVacationListSearchPO {
	// 휴가자 번호입니다.
	private Integer personSeq;
	// 회사 번호입니다.
	private Integer workCompanySeq;
	// 휴가년도입니다.
	private Integer vacationYear;
}
