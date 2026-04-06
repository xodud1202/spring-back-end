package com.xodud1202.springbackend.domain.admin.companywork;

import lombok.Data;

@Data
// 관리자 회사 업무 가져오기 대상 회사 정보를 정의합니다.
public class AdminCompanyWorkImportCompanyInfoVO {
	// 회사 번호입니다.
	private Integer workCompanySeq;
	// 회사명입니다.
	private String workCompanyNm;
	// 사용 플랫폼명입니다.
	private String workPlatformNm;
	// API 요청 URL입니다.
	private String apiUrl;
}
