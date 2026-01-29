package com.xodud1202.springbackend.domain.admin.resume;

import lombok.Data;

@Data
public class ResumePO {
	// 검색 구분 값입니다.
	private String searchGb;
	// 검색어 값입니다.
	private String searchValue;
	// 조회 페이지 번호입니다.
	private Integer page;
	// 페이지당 조회 건수입니다.
	private Integer pageSize;
	// 조회 시작 오프셋입니다.
	private Integer offset;
}
