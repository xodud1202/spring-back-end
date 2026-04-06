package com.xodud1202.springbackend.domain.admin.companywork;

import lombok.Data;

@Data
// 관리자 회사 업무 Jira 가져오기 성공 응답을 정의합니다.
public class AdminCompanyWorkImportResponseVO {
	// 안내 메시지입니다.
	private String message;
	// 저장된 업무 번호입니다.
	private Long workSeq;
	// 저장된 업무 키입니다.
	private String workKey;
}
