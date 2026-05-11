package com.xodud1202.springbackend.domain.work.vacation;

import lombok.Data;

import java.math.BigDecimal;

@Data
// 휴가 사용 상세 행 정보를 정의합니다.
public class WorkVacationListRowVO {
	// 휴가 번호입니다.
	private Long vacationSeq;
	// 휴가자 번호입니다.
	private Integer personSeq;
	// 휴가자명입니다.
	private String personNm;
	// 회사 번호입니다.
	private Integer workCompanySeq;
	// 회사명입니다.
	private String workCompanyNm;
	// 휴가 구분 코드입니다.
	private String vacationCd;
	// 휴가 구분명입니다.
	private String vacationNm;
	// 휴가 시작일입니다.
	private String startDt;
	// 휴가 종료일입니다.
	private String endDt;
	// 계산된 사용일입니다.
	private BigDecimal useDayCnt;
	// 휴가 사유입니다.
	private String vacationMemo;
	// 등록 일시입니다.
	private String regDt;
	// 수정 일시입니다.
	private String udtDt;
}
