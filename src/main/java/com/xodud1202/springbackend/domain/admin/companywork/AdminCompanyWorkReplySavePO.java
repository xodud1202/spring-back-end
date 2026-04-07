package com.xodud1202.springbackend.domain.admin.companywork;

import lombok.Data;

@Data
// 관리자 회사 업무 댓글 등록 요청 정보를 정의합니다.
public class AdminCompanyWorkReplySavePO {
	// 댓글 시퀀스입니다.
	private Long replySeq;
	// 업무 시퀀스입니다.
	private Long workSeq;
	// 댓글 내용입니다.
	private String replyComment;
	// 등록자 번호입니다.
	private Long regNo;
	// 수정자 번호입니다.
	private Long udtNo;
}
