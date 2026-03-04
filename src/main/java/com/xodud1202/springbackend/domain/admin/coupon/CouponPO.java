package com.xodud1202.springbackend.domain.admin.coupon;

import lombok.Data;

@Data
// 관리자 쿠폰 검색 조건을 정의합니다.
public class CouponPO {
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
	// 기간 검색 구분입니다.
	private String dateGb;
	// 조회 시작일시입니다.
	private String searchStartDt;
	// 조회 종료일시입니다.
	private String searchEndDt;
	// 쿠폰 상태 코드입니다.
	private String cpnStatCd;
	// 쿠폰 종류 코드입니다.
	private String cpnGbCd;
	// 쿠폰 타겟 코드입니다.
	private String cpnTargetCd;
	// 고객 다운로드 가능 여부입니다.
	private String cpnDownAbleYn;
}
