package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 쇼핑몰 마이페이지 주문상세 상태 변경 요청 본문을 전달합니다.
public class ShopOrderDetailStatusUpdatePO {
	// 주문번호입니다.
	private String ordNo;
	// 주문상세번호입니다.
	private Integer ordDtlNo;
}
