package com.xodud1202.springbackend.domain.admin.companywork;

import lombok.Data;

import java.util.List;

@Data
// 관리자 회사 업무 상태별 추가 조회 응답 정보를 정의합니다.
public class AdminCompanyWorkStatusSectionPageResponseVO {
	// 업무 상태 코드입니다.
	private String workStatCd;
	// 이번 요청으로 조회한 업무 목록입니다.
	private List<AdminCompanyWorkListRowVO> list;
	// 해당 상태의 전체 업무 건수입니다.
	private Integer totalCount;
	// 현재 오프셋입니다.
	private Integer offset;
	// 현재 요청 제한 건수입니다.
	private Integer limit;
	// 추가 조회 가능 여부입니다.
	private Boolean hasMore;
}
