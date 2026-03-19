package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 주문 결제용 사용 가능 포인트 마스터 정보를 전달합니다.
public class ShopOrderPointBaseVO {
	// 포인트번호입니다.
	private Long pntNo;
	// 고객번호입니다.
	private Long custNo;
	// 지급포인트입니다.
	private Integer saveAmt;
	// 사용포인트입니다.
	private Integer useAmt;
	// 잔여포인트입니다.
	private Integer rmnAmt;
	// 만료일시입니다.
	private String expireDt;
	// 지급일시입니다.
	private String saveDt;
	// 주문번호입니다.
	private String ordNo;
}
