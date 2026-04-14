package com.xodud1202.springbackend.domain.admin.companywork;

import lombok.Data;

@Data
// 관리자 회사 업무 첨부파일 저장 파라미터를 정의합니다.
public class AdminCompanyWorkImportFileSavePO {
	// 첨부파일 번호입니다.
	private Integer workJobFileSeq;
	// 업무 번호입니다.
	private Long workSeq;
	// 첨부파일명입니다.
	private String workJobFileNm;
	// 첨부파일 URL입니다.
	private String workJobFileUrl;
	// 등록자 번호입니다.
	private Long regNo;
	// 수정자 번호입니다.
	private Long udtNo;
}
