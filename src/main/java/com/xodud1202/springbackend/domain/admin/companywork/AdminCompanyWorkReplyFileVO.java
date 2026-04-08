package com.xodud1202.springbackend.domain.admin.companywork;

import lombok.Data;

@Data
// 관리자 회사 업무 댓글 첨부파일 정보를 정의합니다.
public class AdminCompanyWorkReplyFileVO {
	// 댓글 첨부파일 시퀀스입니다.
	private Integer replyFileSeq;
	// 댓글 시퀀스입니다.
	private Long replySeq;
	// 업무 시퀀스입니다.
	private Long workSeq;
	// 원본 파일명입니다.
	private String replyFileNm;
	// 첨부파일 URL입니다.
	private String replyFileUrl;
	// 첨부파일 크기입니다.
	private Long replyFileSize;
	// 등록 일시입니다.
	private String regDt;
	// 수정 일시입니다.
	private String udtDt;
}
