package com.xodud1202.springbackend.domain.work.vacation;

import lombok.Data;

import java.util.List;

@Data
// 휴가관리 목록 응답 정보를 정의합니다.
public class WorkVacationListResponseVO {
	// 현재 선택된 회사 번호입니다.
	private Integer selectedWorkCompanySeq;
	// 선택 가능한 휴가년도 목록입니다.
	private List<Integer> yearList;
	// 현재 선택된 휴가년도입니다.
	private Integer selectedYear;
	// 연차 사용 요약 목록입니다.
	private List<WorkVacationSummaryRowVO> summaryList;
	// 휴가 사용 상세 목록입니다.
	private List<WorkVacationListRowVO> vacationList;
}
