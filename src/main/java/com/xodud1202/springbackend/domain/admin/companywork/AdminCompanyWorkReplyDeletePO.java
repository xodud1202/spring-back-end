package com.xodud1202.springbackend.domain.admin.companywork;

import lombok.Data;

@Data
// 관리자 회사 업무 댓글 삭제 요청 정보를 정의합니다.
public class AdminCompanyWorkReplyDeletePO {
	// 댓글 시퀀스입니다.
	private Long replySeq;
	// 업무 시퀀스입니다.
	private Long workSeq;
	// 수정자 번호입니다.
	private Long udtNo;
}
