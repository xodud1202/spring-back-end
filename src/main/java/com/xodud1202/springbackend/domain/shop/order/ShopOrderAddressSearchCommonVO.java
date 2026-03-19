package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 쇼핑몰 주문서 주소 검색 공통 응답 정보를 전달합니다.
public class ShopOrderAddressSearchCommonVO {
	// 검색 API 에러 코드입니다.
	private String errorCode;
	// 검색 API 에러 메시지입니다.
	private String errorMessage;
	// 전체 검색 결과 건수입니다.
	private Integer totalCount;
	// 현재 페이지 번호입니다.
	private Integer currentPage;
	// 페이지당 결과 건수입니다.
	private Integer countPerPage;
}
