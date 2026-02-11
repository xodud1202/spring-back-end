package com.xodud1202.springbackend.domain.admin.banner;

import lombok.Data;

@Data
// 관리자 배너 검색 조건을 정의합니다.
public class BannerPO {
	// 현재 페이지입니다.
	private Integer page;
	// 페이지 크기입니다.
	private Integer pageSize;
	// 조회 시작 오프셋입니다.
	private Integer offset;
	// 배너 구분 코드입니다.
	private String bannerDivCd;
	// 노출 여부입니다.
	private String showYn;
	// 검색어입니다.
	private String searchValue;
	// 검색 노출 시작일시입니다.
	private String searchStartDt;
	// 검색 노출 종료일시입니다.
	private String searchEndDt;
}
