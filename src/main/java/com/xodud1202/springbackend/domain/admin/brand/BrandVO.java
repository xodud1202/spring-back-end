package com.xodud1202.springbackend.domain.admin.brand;

import lombok.Data;

// 관리자 브랜드 목록 응답 데이터를 정의합니다.
@Data
public class BrandVO {
	private Integer brandNo;
	private String brandNm;
}
