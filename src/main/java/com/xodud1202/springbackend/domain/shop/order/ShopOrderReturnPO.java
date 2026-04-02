package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

import java.util.List;

@Data
// 주문반품 요청 본문을 전달합니다.
public class ShopOrderReturnPO {
	// 주문번호입니다.
	private String ordNo;
	// 반품 요청 주문상품 목록입니다.
	private List<ShopOrderReturnItemPO> returnItemList;
	// 화면에서 계산한 반품 예정 금액 요약입니다.
	private ShopOrderReturnPreviewAmountPO previewAmount;
	// 회수지 정보입니다.
	private ShopOrderReturnPickupAddressPO pickupAddress;
}
