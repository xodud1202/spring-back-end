package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 쇼핑몰 주문교환 회수지 또는 교환 배송지 요청 정보를 전달합니다.
public class ShopOrderExchangeAddressPO {
	// 수령인 또는 회수지명입니다.
	private String rsvNm;
	// 우편번호입니다.
	private String postNo;
	// 기본 주소입니다.
	private String baseAddress;
	// 상세 주소입니다.
	private String detailAddress;
}
