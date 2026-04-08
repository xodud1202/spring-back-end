package com.xodud1202.springbackend.domain.admin.companywork;

import lombok.Data;

@Data
// 관리자 회사 업무 상세 정보를 정의합니다.
public class AdminCompanyWorkDetailVO {
	// 업무 시퀀스입니다.
	private Long workSeq;
	// 회사 번호입니다.
	private Integer workCompanySeq;
	// 프로젝트 번호입니다.
	private Integer workCompanyProjectSeq;
	// 회사명입니다.
	private String workCompanyNm;
	// 프로젝트명입니다.
	private String workCompanyProjectNm;
	// 업무 상태 코드입니다.
	private String workStatCd;
	// 업무 키입니다.
	private String workKey;
	// 업무 타이틀입니다.
	private String title;
	// 댓글 개수입니다.
	private Integer replyCount;
	// 업무 본문입니다.
	private String content;
	// 업무 생성 일시입니다.
	private String workCreateDt;
	// 업무 시작 일시입니다.
	private String workStartDt;
	// 업무 종료 일시입니다.
	private String workEndDt;
	// 업무 공수시간입니다.
	private Integer workTime;
	// 업무 우선순위 코드입니다.
	private String workPriorCd;
	// 업무 우선순위명입니다.
	private String workPriorNm;
	// IT 담당자명입니다.
	private String itManager;
	// 업무 담당자명입니다.
	private String coManager;
	// 등록 일시입니다.
	private String regDt;
	// 수정 일시입니다.
	private String udtDt;
}
