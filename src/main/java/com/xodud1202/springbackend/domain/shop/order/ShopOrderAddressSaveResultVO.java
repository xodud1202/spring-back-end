package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

import java.util.List;

@Data
// 쇼핑몰 주문서 배송지 등록 결과를 전달합니다.
public class ShopOrderAddressSaveResultVO {
	// 전체 배송지 목록입니다.
	private List<ShopOrderAddressVO> addressList;
	// 현재 기본 배송지입니다.
	private ShopOrderAddressVO defaultAddress;
	// 이번에 등록된 배송지입니다.
	private ShopOrderAddressVO savedAddress;
}
