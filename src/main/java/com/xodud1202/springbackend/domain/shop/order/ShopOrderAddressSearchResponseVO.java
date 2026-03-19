package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

import java.util.List;

@Data
// 쇼핑몰 주문서 주소 검색 응답 데이터를 전달합니다.
public class ShopOrderAddressSearchResponseVO {
	// 검색 공통 응답 정보입니다.
	private ShopOrderAddressSearchCommonVO common;
	// 주소 검색 결과 목록입니다.
	private List<ShopOrderAddressSearchItemVO> jusoList;
}
