package com.xodud1202.springbackend.domain.admin.companywork;

import lombok.Data;

import java.util.List;

@Data
// 관리자 회사 업무 댓글 수정 요청 정보를 정의합니다.
public class AdminCompanyWorkReplyUpdatePO {
	// 댓글 시퀀스입니다.
	private Long replySeq;
	// 업무 시퀀스입니다.
	private Long workSeq;
	// 댓글 내용입니다.
	private String replyComment;
	// 삭제할 댓글 첨부파일 시퀀스 목록입니다.
	private List<Integer> deleteReplyFileSeqList;
	// 수정자 번호입니다.
	private Long udtNo;
}
