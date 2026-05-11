package com.xodud1202.springbackend.domain.work.vacation;

import lombok.Data;

@Data
// 휴가관리 회사 선택 항목을 정의합니다.
public class WorkVacationCompanyVO {
	// 회사 번호입니다.
	private Integer workCompanySeq;
	// 회사명입니다.
	private String workCompanyNm;
	// 플랫폼명입니다.
	private String workPlatformNm;
	// 회사별 사용 가능 연차 수입니다.
	private Integer vacationLimitCnt;
	// 표시 순서입니다.
	private Integer dispOrd;
}
