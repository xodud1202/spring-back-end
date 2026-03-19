package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 주문 결제용 포인트 상세 이력 저장 파라미터를 전달합니다.
public class ShopOrderPointDetailSavePO {
	// 포인트번호입니다.
	private Long pntNo;
	// 포인트 금액입니다.
	private Integer pntAmt;
	// 주문번호입니다.
	private String ordNo;
	// 비고입니다.
	private String bigo;
	// 등록자 번호입니다.
	private Long regNo;
}
