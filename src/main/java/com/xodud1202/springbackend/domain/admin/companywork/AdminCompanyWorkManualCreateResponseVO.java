package com.xodud1202.springbackend.domain.admin.companywork;

import lombok.Data;

@Data
// 관리자 회사 업무 수기 등록 응답 정보를 정의합니다.
public class AdminCompanyWorkManualCreateResponseVO {
	// 처리 결과 메시지입니다.
	private String message;
	// 저장된 업무 시퀀스입니다.
	private Long workSeq;
	// 저장된 업무 키입니다.
	private String workKey;
}
