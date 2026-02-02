package com.xodud1202.springbackend.domain.admin.goods;

import lombok.Data;

// 관리자 상품 목록 조회 파라미터를 정의합니다.
@Data
public class GoodsPO {
	// 검색 구분 값입니다.
	private String searchGb;
	// 검색어입니다.
	private String searchValue;
	// 상품명 검색용 FULLTEXT 키워드입니다.
	private String searchKeyword;
	// 상품 상태 코드입니다.
	private String goodsStatCd;
	// 상품 분류 코드입니다.
	private String goodsDivCd;
	// 노출 여부 값입니다.
	private String showYn;
	// 브랜드 번호입니다.
	private String brandNo;
	// 페이지 번호입니다.
	private Integer page;
	// 페이지 당 건수입니다.
	private Integer pageSize;
	// 조회 시작 위치입니다.
	private Integer offset;
}
