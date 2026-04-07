package com.xodud1202.springbackend.domain.admin.companywork;

import lombok.Data;

@Data
// 관리자 회사 업무 첨부파일 정보를 정의합니다.
public class AdminCompanyWorkFileVO {
	// 첨부파일 시퀀스입니다.
	private Integer workJobFileSeq;
	// 업무 시퀀스입니다.
	private Long workSeq;
	// 첨부파일명입니다.
	private String workJobFileNm;
	// 첨부파일 URL입니다.
	private String workJobFileUrl;
	// 등록 일시입니다.
	private String regDt;
	// 수정 일시입니다.
	private String udtDt;
}
