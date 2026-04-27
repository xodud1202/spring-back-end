package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

import java.util.List;

@Data
// 쇼핑몰 주문교환 신청 요청 본문을 전달합니다.
public class ShopOrderExchangePO {
	// 원 주문번호입니다.
	private String ordNo;
	// 교환 요청 주문상품 목록입니다.
	private List<ShopOrderExchangeItemPO> exchangeItemList;
	// 회수지 정보입니다.
	private ShopOrderExchangeAddressPO pickupAddress;
	// 교환 배송지 정보입니다.
	private ShopOrderExchangeAddressPO deliveryAddress;
	// 교환 배송비 결제수단 코드입니다.
	private String paymentMethodCd;
}
