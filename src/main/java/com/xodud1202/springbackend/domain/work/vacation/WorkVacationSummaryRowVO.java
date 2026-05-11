package com.xodud1202.springbackend.domain.work.vacation;

import lombok.Data;

import java.math.BigDecimal;

@Data
// 휴가자별 회사별 연차 사용 요약 정보를 정의합니다.
public class WorkVacationSummaryRowVO {
	// 휴가자 번호입니다.
	private Integer personSeq;
	// 휴가자명입니다.
	private String personNm;
	// 회사 번호입니다.
	private Integer workCompanySeq;
	// 회사명입니다.
	private String workCompanyNm;
	// 회사별 전체 연차입니다.
	private Integer vacationLimitCnt;
	// 전체 소진 연차입니다.
	private BigDecimal usedVacationCnt;
	// 연차 사용일입니다.
	private BigDecimal fullVacationCnt;
	// 오전반차 사용일입니다.
	private BigDecimal morningHalfCnt;
	// 오후반차 사용일입니다.
	private BigDecimal afternoonHalfCnt;
	// 잔여 휴가입니다.
	private BigDecimal remainingVacationCnt;
}
