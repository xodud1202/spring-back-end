package com.xodud1202.springbackend.domain.work.vacation;

import lombok.Data;

@Data
// 휴가 삭제 요청 정보를 정의합니다.
public class WorkVacationDeletePO {
	// 삭제할 휴가 번호입니다.
	private Long vacationSeq;
	// 수정자 번호입니다.
	private Long udtNo;
}
