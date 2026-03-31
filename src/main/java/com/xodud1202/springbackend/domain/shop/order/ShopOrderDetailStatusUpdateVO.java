package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 쇼핑몰 마이페이지 주문상세 상태 변경 결과를 전달합니다.
public class ShopOrderDetailStatusUpdateVO {
	// 주문번호입니다.
	private String ordNo;
	// 주문상세번호입니다.
	private Integer ordDtlNo;
	// 변경된 주문상세 상태코드입니다.
	private String ordDtlStatCd;
	// 변경 건수입니다.
	private Integer updatedCount;
}
