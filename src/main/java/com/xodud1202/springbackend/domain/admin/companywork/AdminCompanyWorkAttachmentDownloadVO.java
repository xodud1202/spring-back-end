package com.xodud1202.springbackend.domain.admin.companywork;

import lombok.Data;

@Data
// 관리자 회사 업무 공통 첨부파일 다운로드 응답 정보를 정의합니다.
public class AdminCompanyWorkAttachmentDownloadVO {
	// 원본 파일명입니다.
	private String fileNm;
	// 다운로드할 파일 데이터입니다.
	private byte[] fileData;
}
