package com.xodud1202.springbackend.domain.admin.companywork;

import lombok.Data;

import java.util.List;

@Data
// 관리자 회사 업무 상세 팝업 응답 정보를 정의합니다.
public class AdminCompanyWorkDetailResponseVO {
	// 업무 상세 정보입니다.
	private AdminCompanyWorkDetailVO detail;
	// 첨부파일 목록입니다.
	private List<AdminCompanyWorkFileVO> fileList;
	// 댓글 목록입니다.
	private List<AdminCompanyWorkReplyVO> replyList;
}
