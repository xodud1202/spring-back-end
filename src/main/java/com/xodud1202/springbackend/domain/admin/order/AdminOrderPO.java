package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

@Data
// 관리자 주문 목록 검색 조건을 정의합니다.
public class AdminOrderPO {
	// 현재 페이지입니다.
	private Integer page;
	// 페이지 크기입니다.
	private Integer pageSize;
	// 조회 시작 오프셋입니다.
	private Integer offset;
	// 검색 구분입니다.
	private String searchGb;
	// 검색 값입니다.
	private String searchValue;
	// 기간 구분입니다.
	private String dateGb;
	// 조회 시작일입니다.
	private String searchStartDt;
	// 조회 종료일입니다.
	private String searchEndDt;
	// SQL 비교용 조회 시작일시입니다.
	private String startDateTime;
	// SQL 비교용 조회 종료 다음일 00시입니다.
	private String endExclusiveDateTime;
	// 주문상세 상태 코드입니다.
	private String ordDtlStatCd;
	// 클레임상세 상태 코드입니다.
	private String chgDtlStatCd;
}
