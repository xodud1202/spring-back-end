package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 주문번호 기준 포인트 사용 상세 이력을 전달합니다.
public class ShopOrderPointDetailVO {
	// 포인트번호입니다.
	private Long pntNo;
	// 포인트 금액입니다.
	private Integer pntAmt;
	// 주문번호입니다.
	private String ordNo;
	// 비고입니다.
	private String bigo;
}
