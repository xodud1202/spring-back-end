package com.xodud1202.springbackend.domain.work.vacation;

import lombok.Data;

@Data
// 휴가 등록 응답 정보를 정의합니다.
public class WorkVacationCreateResponseVO {
	// 처리 메시지입니다.
	private String message;
	// 등록된 휴가 번호입니다.
	private Long vacationSeq;
}
