package com.xodud1202.springbackend.domain.admin.companywork;

import lombok.Data;

import java.util.List;

@Data
// 관리자 회사 업무 상세 저장 요청 정보를 정의합니다.
public class AdminCompanyWorkDetailUpdatePO {
	// 업무 시퀀스입니다.
	private Long workSeq;
	// 업무 타이틀입니다.
	private String title;
	// 업무 상태 코드입니다.
	private String workStatCd;
	// IT 담당자명입니다.
	private String itManager;
	// 업무 담당자명입니다.
	private String coManager;
	// 업무 생성 일시입니다.
	private String workCreateDt;
	// 업무 시작 일시입니다.
	private String workStartDt;
	// 업무 종료 일시입니다.
	private String workEndDt;
	// 업무 공수시간입니다.
	private Integer workTime;
	// 업무 본문입니다.
	private String content;
	// 삭제할 업무 첨부파일 시퀀스 목록입니다.
	private List<Integer> deleteWorkJobFileSeqList;
	// 수정자 번호입니다.
	private Long udtNo;
}
