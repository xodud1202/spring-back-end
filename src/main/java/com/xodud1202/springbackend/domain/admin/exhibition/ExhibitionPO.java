package com.xodud1202.springbackend.domain.admin.exhibition;

import lombok.Data;

@Data
// 기획전 조회 조건을 전달하는 객체입니다.
public class ExhibitionPO {
	// 조회 페이지입니다.
	private Integer page;
	// 페이지 크기입니다.
	private Integer pageSize;
	// 조회 시작 인덱스입니다.
	private Integer offset;
	// 검색 구분입니다.
	private String searchGb;
	// 검색 값입니다.
	private String searchValue;
	// 조회 시작일시입니다.
	private String searchStartDt;
	// 조회 종료일시입니다.
	private String searchEndDt;
}

