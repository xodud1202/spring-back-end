package com.xodud1202.springbackend.domain.admin.companywork;

import lombok.Data;

import java.util.List;

@Data
// 관리자 회사 업무 댓글 정보를 정의합니다.
public class AdminCompanyWorkReplyVO {
	// 댓글 시퀀스입니다.
	private Long replySeq;
	// 업무 시퀀스입니다.
	private Long workSeq;
	// 댓글 내용입니다.
	private String replyComment;
	// 등록자 번호입니다.
	private Long regNo;
	// 등록 일시입니다.
	private String regDt;
	// 수정 일시입니다.
	private String udtDt;
	// 댓글 첨부파일 목록입니다.
	private List<AdminCompanyWorkReplyFileVO> replyFileList;
}
