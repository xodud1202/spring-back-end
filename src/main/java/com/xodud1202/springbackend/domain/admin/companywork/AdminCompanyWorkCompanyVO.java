package com.xodud1202.springbackend.domain.admin.companywork;

import lombok.Data;

@Data
// 관리자 회사 업무 회사 선택 항목을 정의합니다.
public class AdminCompanyWorkCompanyVO {
	// 회사 번호입니다.
	private Integer workCompanySeq;
	// 회사명입니다.
	private String workCompanyNm;
	// 사용 플랫폼명입니다.
	private String workPlatformNm;
	// 표시 순서입니다.
	private Integer dispOrd;
}
