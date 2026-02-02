package com.xodud1202.springbackend.domain.admin.brand;

import lombok.Data;

// 관리자 브랜드 검색/등록/수정 요청 데이터를 정의합니다.
@Data
public class BrandPO {
	private Integer brandNo;
	private String brandNm;
	private String brandLogoPath;
	private String brandNoti;
	private Integer dispOrd;
	private String useYn;
	private String delYn;
	private Long regNo;
	private Long udtNo;
	private Integer page;
	private Integer pageSize;
	private Integer offset;
}
