package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 주문반품 신청 결과를 전달합니다.
public class ShopOrderReturnResultVO {
	// 클레임번호입니다.
	private String clmNo;
	// 주문번호입니다.
	private String ordNo;
}
