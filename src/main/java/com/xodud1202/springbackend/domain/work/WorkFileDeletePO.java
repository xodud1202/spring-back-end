package com.xodud1202.springbackend.domain.work;

import lombok.Data;

// 업무 첨부파일 삭제 요청 정보를 정의합니다.
@Data
public class WorkFileDeletePO {
	// 업무 시퀀스입니다.
	private Long workSeq;
	// 첨부파일 시퀀스입니다.
	private Integer workJobFileSeq;
}
